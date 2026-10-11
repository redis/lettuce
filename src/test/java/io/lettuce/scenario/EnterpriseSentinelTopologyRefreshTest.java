package io.lettuce.scenario;

import static io.lettuce.TestTags.SCENARIO_TEST;
import static io.lettuce.scenario.EnterpriseSentinelSupport.DB_NAME;
import static io.lettuce.scenario.EnterpriseSentinelSupport.INTERNAL_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.test.env.Endpoints;

/**
 * Verifies that {@link MasterReplica} over a Redis Enterprise {@code redis-sentinel://} URI follows the database endpoint when
 * it moves.
 * <p>
 * Redis Enterprise publishes {@code +switch-master} the moment a database's endpoint (DMC proxy) moves to another node, and
 * then keeps the old proxy serving for {@code endpoint_rebind_propagation_grace_time} seconds so a client that heard the
 * announcement can move across without losing anything. These tests raise that window and assert the handoff happens well
 * inside it, which is only possible if the announcement was consumed - a passive client cannot reach the new endpoint before
 * the server unbinds the old proxy.
 * <p>
 * Two things had to change in Lettuce for this to work against the discovery service, which implements only a subset of the
 * Sentinel protocol:
 * <ol>
 * <li>{@code SentinelTopologyProvider} now treats an unknown-command reply to {@code SENTINEL REPLICAS} as an empty replica
 * list instead of a connect failure. The discovery service does not implement the command, and an empty replica list is the
 * correct topology for a proxied endpoint, which exposes no client-visible replicas.</li>
 * <li>{@code SentinelTopologyRefresh} subscribes to the channels its predicates can match instead of {@code PSUBSCRIBE *}. The
 * discovery service implements {@code SUBSCRIBE} only.</li>
 * </ol>
 * {@link EnterpriseSentinelDiscoveryTest#documentsTheTwoProtocolGaps()} pins both server-side facts, so this test going red
 * with {@code ERR sentinel unknown command} means a regression in the first change, and with
 * {@code Cannot attach to Redis Sentinel for topology refresh} in the second.
 *
 * @author Redis
 */
@Tag(SCENARIO_TEST)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MasterReplica follows the endpoint over the Redis Enterprise Sentinel discovery service")
public class EnterpriseSentinelTopologyRefreshTest {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseSentinelTopologyRefreshTest.class);

    /**
     * Grace window to configure for the test. Redis Enterprise defaults to 15s; a longer window removes any doubt about whether
     * the client had time to react before the old proxy went away.
     */
    private static final int GRACE_TIME_SECONDS = 60;

    private static final int DEFAULT_GRACE_TIME_SECONDS = 15;

    /**
     * How long a Sentinel-aware client may take to act on {@code +switch-master}. Comfortably inside the grace window, so a
     * failure here means "never reacted", not "reacted slowly".
     */
    private static final Duration REACTION_TIMEOUT = Duration.ofSeconds(20);

    private static final Duration SWITCH_MASTER_TIMEOUT = Duration.ofMinutes(2);

    /**
     * Connection-activation events reach the event bus asynchronously, so the initial endpoint is awaited rather than asserted
     * synchronously after connect.
     */
    private static final Duration ACTIVATION_TIMEOUT = Duration.ofSeconds(30);

    /**
     * How long the discovery service may lag CCS before the endpoint it reports has to match the endpoint's only proxy.
     */
    private static final Duration DISCOVERY_AGREEMENT_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration RLADMIN_CHECK_INTERVAL = Duration.ofSeconds(3);

    private static final Duration RLADMIN_TIMEOUT = Duration.ofMinutes(2);

    private static final Duration MAINTENANCE_TIMEOUT = Duration.ofMinutes(5);

    private static Endpoints.Endpoint mStandard;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private RedisEnterpriseConfig clusterConfig;

    private List<RedisURI> sentinels;

    private RedisURI reachableSentinel;

    private String bdbId;

    private String endpointId;

    /**
     * Node the discovery service currently reports for the database. Draining this node is what makes Redis Enterprise publish
     * {@code +switch-master}; draining any other proxy of the same endpoint is silent.
     */
    private String trackedNodeUid;

    private int dbPort;

    private RedisClient client;

    private StatefulRedisMasterReplicaConnection<String, String> connection;

    private EnterpriseSentinelSupport.SwitchMasterCapture capture;

    private EnterpriseSentinelSupport.ConnectionEventCapture events;

    private boolean maintenanceModeEnabled;

    private boolean endpointMoved;

    private boolean graceTimeChanged;

    private String pendingActionId;

    @BeforeAll
    public static void setup() {
        mStandard = Endpoints.DEFAULT.getEndpoint(DB_NAME);
        assumeTrue(mStandard != null, "Skipping test because no '" + DB_NAME + "' Redis endpoint is configured!");
    }

    @BeforeEach
    public void discoverCluster() {

        sentinels = EnterpriseSentinelSupport.sentinelUris(mStandard);
        assumeTrue(!sentinels.isEmpty(), EnterpriseSentinelSupport.missingDiscoveryEndpoints());
        reachableSentinel = EnterpriseSentinelSupport.firstReachableSentinel(sentinels);
        assumeTrue(reachableSentinel != null, EnterpriseSentinelSupport.unreachableDiscoveryService(sentinels));

        bdbId = String.valueOf(mStandard.getBdbId());
        dbPort = mStandard.getRawEndpoints().get(0).getPort();
        clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, bdbId);
        assertThat(clusterConfig.getDbName()).as("database name reported by rladmin for bdb %s", bdbId).isEqualTo(DB_NAME);

        endpointId = clusterConfig.getFirstEndpointId();
        maintenanceModeEnabled = false;
        endpointMoved = false;
        pendingActionId = null;

        // Collapse the endpoint onto a single proxy before measuring anything. A previous test may have left it with two -
        // 'bind endpoint <id> include <node>' unions the node onto the proxy set instead of moving the endpoint - and with
        // two proxies the node rladmin lists and the node the discovery service reports can differ.
        trackedNodeUid = normaliseToSingleProxy();

        // Widen the window during which the old proxy keeps serving, so that "did the client react" is separable from
        // "was the client given any time to react".
        graceTimeChanged = runRladmin("tune cluster endpoint_rebind_propagation_grace_time " + GRACE_TIME_SECONDS,
                RLADMIN_TIMEOUT);

        client = RedisClient.create();
        client.setOptions(RecommendedSettingsProvider.forConnectionInterruption());
        events = new EnterpriseSentinelSupport.ConnectionEventCapture(client);
        capture = new EnterpriseSentinelSupport.SwitchMasterCapture(reachableSentinel);

        log.info("Database {} (bdb {}): endpoint {} on the single proxy node {}, port {}", DB_NAME, bdbId, endpointId,
                trackedNodeUid, dbPort);
    }

    @AfterEach
    public void restoreClusterAndClose() {

        if (capture != null) {
            // Restoring moves the endpoint again; keep that out of the next test's assertions.
            capture.stopRecording();
        }

        drainPendingAction(MAINTENANCE_TIMEOUT);

        if (maintenanceModeEnabled) {
            runRladmin(String.format("node %s maintenance_mode off", trackedNodeUid), MAINTENANCE_TIMEOUT);
            resetMaintenanceState();
        }

        if (endpointMoved) {
            // 'policy single' resets both overriding constraints - rladmin sends empty include_proxies and
            // exclude_proxies alongside the policy - and collapses the endpoint back onto one proxy. 'include <node>'
            // would clear the exclude but leave a two-proxy endpoint behind for the next test.
            runRladmin(String.format("bind endpoint %s policy single", endpointId), RLADMIN_TIMEOUT);
        }

        if (graceTimeChanged) {
            runRladmin("tune cluster endpoint_rebind_propagation_grace_time " + DEFAULT_GRACE_TIME_SECONDS, RLADMIN_TIMEOUT);
        }

        if (connection != null) {
            connection.close();
            connection = null;
        }
        if (capture != null) {
            capture.close();
            capture = null;
        }
        if (events != null) {
            events.close();
            events = null;
        }
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

    @Test
    @Order(1)
    @DisplayName("moves to the new master after an endpoint rebind, without waiting to be disconnected")
    public void shouldFollowSwitchMasterAfterEndpointRebind() {

        String before = connectThroughSentinel();

        String command = String.format("bind endpoint %s exclude %s", endpointId, trackedNodeUid);
        endpointMoved = true;
        startRladmin(command);

        assertHandoffWithinGraceWindow(before);
    }

    @Test
    @Order(2)
    @DisplayName("moves to the new master when node maintenance mode drains the endpoint")
    public void shouldFollowSwitchMasterOnMaintenanceMode() {

        String before = connectThroughSentinel();

        // The operator-facing version of the same event: maintenance mode sets accept_servers=false and max_listeners=0
        // on the node, evicts its shards and moves the endpoint off it.
        String command = String.format("node %s maintenance_mode on", trackedNodeUid);
        maintenanceModeEnabled = true;
        endpointMoved = true;
        startRladmin(command);

        assertHandoffWithinGraceWindow(before);
    }

    /**
     * Connect through Sentinel and return the endpoint address the connection actually landed on.
     */
    private String connectThroughSentinel() {

        RedisURI uri = EnterpriseSentinelSupport.sentinelDiscoveryUri(mStandard, DB_NAME);
        log.info("Connecting with {}", uri);

        connection = MasterReplica.connect(client, StringCodec.UTF8, uri);
        // A proxied endpoint has no client-visible replicas, so the topology is a single upstream node and replica reads
        // are not available over Sentinel discovery.
        connection.setReadFrom(ReadFrom.UPSTREAM);
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        String reported = EnterpriseSentinelSupport
                .format(EnterpriseSentinelSupport.reportedMaster(reachableSentinel, DB_NAME));

        // A MasterReplica connection has no single Netty channel to read: MasterReplicaChannelWriter routes commands
        // through the connection provider to per-node connections. Which endpoint is actually in use is therefore observed
        // through connection-activation events on the client event bus, filtered to the database port so the
        // discovery-service connections on 8001 do not interfere.
        await().atMost(ACTIVATION_TIMEOUT).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(events.remotesOnPort(dbPort))
                        .as("Sentinel discovery put the connection on the master it reports").containsOnly(reported));

        log.info("Connected to {} (master reported by the discovery service)", reported);
        return reported;
    }

    /**
     * Await the announcement, then assert the client acts on it inside the grace window: it should be talking to the new master
     * without having been disconnected from the old one.
     */
    private void assertHandoffWithinGraceWindow(String before) {

        await().atMost(SWITCH_MASTER_TIMEOUT).pollInterval(Duration.ofMillis(500))
                .until(() -> capture.firstSwitchMasterFor(DB_NAME).isPresent());

        String payload = capture.firstSwitchMasterFor(DB_NAME).get();
        log.info("+switch-master payload: {}", payload);

        // One event per master name; only the plain name is addressed to a client that asked for "m-standard".
        assertThat(capture.switchMasterPayloads(DB_NAME)).as("+switch-master for the plain master name").hasSize(1);
        assertThat(capture.switchMasterPayloads(DB_NAME + INTERNAL_SUFFIX)).as("+switch-master for the @internal master name")
                .hasSize(1);

        String[] parts = payload.split(" ");
        assertThat(parts).as("payload is '<name> <old-ip> <old-port> <new-ip> <new-port>'").hasSize(5);
        assertThat(parts[1] + ":" + parts[2]).as("old address").isEqualTo(before);

        String after = parts[3] + ":" + parts[4];
        assertThat(after).as("new address differs from the old one").isNotEqualTo(before);
        log.info("Announced move: {} -> {}. Client has {}s of grace window to act.", before, after, GRACE_TIME_SECONDS);

        long announced = System.nanoTime();

        // The old proxy is still serving here, which is the whole point of the grace window: a client that consumed the
        // announcement can move over it without losing a command. The activation on the new endpoint can only come from
        // +switch-master driving a topology refresh, because the reconnect handler on its own would keep retrying the old
        // ConnectionKey.
        await().atMost(REACTION_TIMEOUT).pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(events.remotesOnPort(dbPort))
                        .as("a connection was activated on the master announced by +switch-master").contains(after));

        Duration reaction = Duration.ofNanos(System.nanoTime() - announced);
        log.info("Connection moved to {} in {}ms (activations: {})", after, reaction.toMillis(), events.remotesOnPort(dbPort));

        // Timing is what separates an announcement-driven handoff from recovery-by-disconnect, and the await above already
        // enforces it: the server keeps the old proxy serving for the whole grace window, so a client that never heard the
        // announcement could not reach the new endpoint before that window expires. Note a handoff here legitimately
        // *does* close the old connection - that is how MasterReplicaConnectionProvider#closeStaleConnections performs it -
        // so disconnect events on the database port are expected and say nothing about what drove the move.
        assertThat(reaction)
                .as("reacted inside the %ss grace window rather than waiting to be disconnected", GRACE_TIME_SECONDS)
                .isLessThan(Duration.ofSeconds(GRACE_TIME_SECONDS));
        assertThat(connection.sync().ping()).as("connection is usable after the handoff").isEqualTo("PONG");
    }

    /**
     * Collapse the endpoint onto exactly one proxy and return the node uid of that proxy.
     * <p>
     * Redis Enterprise publishes {@code +switch-master} only when the address it reports for the database changes, and
     * {@code sentinel_ccs.go} keeps reporting the same node for as long as that node stays in the endpoint's
     * {@code proxy_uids}. With more than one proxy, draining the node rladmin happens to list first is therefore not enough: if
     * it is not the node being tracked, the reported address never changes and nothing is published. Starting from a single
     * proxy removes the ambiguity, because then rladmin and the discovery service cannot disagree.
     *
     * @return the bare node uid of the single proxy, which is also the node the discovery service reports.
     */
    private String normaliseToSingleProxy() {

        String fullEndpointId = "endpoint:" + endpointId;

        if (clusterConfig.getEndpointNodes(fullEndpointId).size() > 1) {
            log.info("Endpoint {} has proxies {}; collapsing onto one", endpointId,
                    clusterConfig.getEndpointNodes(fullEndpointId));
            // rladmin exits non-zero when there is nothing to do, so the outcome is checked by re-reading the endpoint
            // rather than trusted from the command.
            runRladmin(String.format("bind endpoint %s policy single", endpointId), RLADMIN_TIMEOUT);
            await().atMost(RLADMIN_TIMEOUT).pollInterval(RLADMIN_CHECK_INTERVAL).untilAsserted(() -> {
                clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, bdbId);
                assertThat(clusterConfig.getEndpointNodes(fullEndpointId)).as("proxies of endpoint %s", endpointId).hasSize(1);
            });
        }

        List<String> proxies = clusterConfig.getEndpointNodes(fullEndpointId);
        assertThat(proxies).as("proxies of endpoint %s", endpointId).hasSize(1);
        String proxyNodeUid = proxies.get(0).replace("node:", "");

        // A single-proxy endpoint takes the len(nodeUids)==1 path in sentinel_ccs.go, which reports that node
        // unconditionally - but the discovery service watches CCS and can still be a moment behind it. Waiting for the two
        // to agree is what makes the drain below meaningful; without it the test could drain a node that is not the one
        // being reported, and nothing would be published.
        await().atMost(DISCOVERY_AGREEMENT_TIMEOUT).pollInterval(Duration.ofSeconds(2)).untilAsserted(
                () -> assertThat(EnterpriseSentinelSupport.trackedNodeUid(reachableSentinel, DB_NAME, clusterConfig))
                        .as("node the discovery service reports for %s", DB_NAME).isEqualTo(proxyNodeUid));

        return proxyNodeUid;
    }

    /**
     * Start an rladmin command and return immediately.
     * <p>
     * The endpoint-rebind state machine waits {@code endpoint_rebind_propagation_grace_time} between publishing
     * {@code +switch-master} and unbinding the old proxy, and that wait is inside the action. Blocking on completion would
     * therefore only begin measuring once the window had already closed and the old proxy had gone away - which is exactly the
     * interval the client is supposed to use. The action is drained in {@link #restoreClusterAndClose()}.
     */
    private void startRladmin(String command) {

        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("bdb_id", bdbId);
        parameters.put("rladmin_command", command);

        log.info("Starting (not awaiting) rladmin {}", command);
        FaultInjectionClient.TriggerActionResponse response = faultClient.triggerAction("execute_rladmin_command", parameters)
                .block(RLADMIN_TIMEOUT);

        assertThat(response).as("fault injector accepted 'rladmin %s'", command).isNotNull();
        pendingActionId = response.getActionId();
        log.info("rladmin {} running as action {}", command, pendingActionId);
    }

    /**
     * Let an in-flight rladmin action finish, so restore operations do not race the state machine.
     */
    private void drainPendingAction(Duration timeout) {

        if (pendingActionId == null) {
            return;
        }

        try {
            Boolean completed = faultClient.waitForCompletion(pendingActionId, RLADMIN_CHECK_INTERVAL, Duration.ZERO, timeout)
                    .onErrorReturn(Boolean.FALSE).block(timeout);
            log.info("Pending action {} completed: {}", pendingActionId, completed);
        } catch (RuntimeException e) {
            log.warn("Pending action {} did not complete cleanly: {}", pendingActionId, e.toString());
        } finally {
            pendingActionId = null;
        }
    }

    private boolean runRladmin(String command, Duration timeout) {

        try {
            Boolean result = faultClient.executeRladminCommand(bdbId, command, RLADMIN_CHECK_INTERVAL, timeout)
                    .onErrorReturn(Boolean.FALSE).block(timeout);
            log.info("rladmin {} -> {}", command, result);
            return Boolean.TRUE.equals(result);
        } catch (RuntimeException e) {
            // rladmin exits non-zero on a no-op ("Nothing to do"), which the fault injector reports as a failure.
            log.warn("rladmin {} did not complete cleanly: {}", command, e.toString());
            return false;
        }
    }

    /**
     * Maintenance mode leaves {@code accept_servers=false} and {@code max_listeners=0} behind together with a node snapshot;
     * clearing only one of them leaves the node unable to host a proxy and cascades into later tests.
     */
    private void resetMaintenanceState() {

        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("cluster_index", 0);
        parameters.put("clean_maintenance_mode", true);
        parameters.put("clean_node_settings", true);

        try {
            faultClient.triggerActionAndWait("reset_cluster", parameters, Duration.ofSeconds(3), Duration.ofSeconds(1),
                    MAINTENANCE_TIMEOUT).onErrorReturn(Boolean.FALSE).block(MAINTENANCE_TIMEOUT);
            log.info("Cleared maintenance-mode state on node {}", trackedNodeUid);
        } catch (RuntimeException e) {
            log.warn("Could not clear maintenance-mode state: {}", e.toString());
        } finally {
            maintenanceModeEnabled = false;
        }
    }

}
