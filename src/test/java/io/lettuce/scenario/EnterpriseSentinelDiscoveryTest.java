package io.lettuce.scenario;

import static io.lettuce.TestTags.SCENARIO_TEST;
import static io.lettuce.scenario.EnterpriseSentinelSupport.DB_NAME;
import static io.lettuce.scenario.EnterpriseSentinelSupport.INTERNAL_SUFFIX;
import static io.lettuce.scenario.EnterpriseSentinelSupport.SENTINEL_PORT;
import static io.lettuce.scenario.EnterpriseSentinelSupport.SWITCH_MASTER_CHANNEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.env.Endpoints;

/**
 * What the Redis Enterprise discovery service (Sentinel-compatible API) offers a Lettuce client today.
 * <p>
 * This class is expected to stay green: it asserts server-side behaviour and the client paths that already work. Its second
 * test pins the two protocol gaps that keep {@link io.lettuce.core.masterreplica.MasterReplica} from working against Redis
 * Enterprise, so if Redis Enterprise ever implements {@code SENTINEL REPLICAS} or {@code PSUBSCRIBE} this test goes red and
 * says the client-side compatibility work can be retired. The companion {@link EnterpriseSentinelTopologyRefreshTest}
 * reproduces those gaps from the client side.
 *
 * @author Redis
 */
@Tag(SCENARIO_TEST)
@DisplayName("Redis Enterprise Sentinel discovery service")
public class EnterpriseSentinelDiscoveryTest {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseSentinelDiscoveryTest.class);

    private static final Duration SWITCH_MASTER_TIMEOUT = Duration.ofMinutes(2);

    private static final Duration ENDPOINT_MOVE_TIMEOUT = Duration.ofMinutes(2);

    private static final Duration RLADMIN_CHECK_INTERVAL = Duration.ofSeconds(3);

    private static final Duration RLADMIN_TIMEOUT = Duration.ofMinutes(2);

    private static final Duration ACTIVATION_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration FAILOVER_TIMEOUT = Duration.ofMinutes(5);

    /**
     * How long to watch for a +switch-master that must not arrive.
     */
    private static final Duration SILENCE_WINDOW = Duration.ofSeconds(45);

    private static Endpoints.Endpoint mStandard;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private RedisEnterpriseConfig clusterConfig;

    private List<RedisURI> sentinels;

    private String bdbId;

    private String endpointId;

    /**
     * Node the discovery service currently reports for the database. Removing this node from the endpoint's proxies is what
     * makes Redis Enterprise publish {@code +switch-master}; removing any other proxy is silent.
     */
    private String trackedNodeUid;

    /**
     * Set by the tests that actually move the endpoint, so read-only tests skip the restore entirely.
     */
    private boolean endpointMoved;

    @BeforeAll
    public static void setup() {
        mStandard = Endpoints.DEFAULT.getEndpoint(DB_NAME);
        assumeTrue(mStandard != null, "Skipping test because no '" + DB_NAME + "' Redis endpoint is configured!");
    }

    @BeforeEach
    public void discoverCluster() {

        sentinels = EnterpriseSentinelSupport.sentinelUris(mStandard);
        assumeTrue(!sentinels.isEmpty(), EnterpriseSentinelSupport.missingDiscoveryEndpoints());
        assumeTrue(EnterpriseSentinelSupport.anySentinelReachable(sentinels),
                EnterpriseSentinelSupport.unreachableDiscoveryService(sentinels));

        bdbId = String.valueOf(mStandard.getBdbId());
        clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, bdbId);

        // The endpoint configuration key, the Redis Enterprise database name and the Sentinel master name are the same
        // string. Cross-check it so a rename surfaces here instead of as "No such master with that name".
        assertThat(clusterConfig.getDbName()).as("database name reported by rladmin for bdb %s", bdbId).isEqualTo(DB_NAME);

        endpointId = clusterConfig.getFirstEndpointId();
        // getFirstEndpointId() strips the prefix ("2:1") but endpointToNodes is keyed with it ("endpoint:2:1").
        List<String> proxies = clusterConfig.getEndpointNodes("endpoint:" + endpointId);
        assertThat(proxies).as("nodes proxying endpoint %s", endpointId).isNotEmpty();

        // Ask the discovery service which node it reports, rather than assuming it is the node rladmin lists for the
        // endpoint. Those agree only while the endpoint has a single proxy, and rladmin's row order is not stable.
        trackedNodeUid = EnterpriseSentinelSupport.trackedNodeUid(sentinels.get(0), DB_NAME, clusterConfig);
        assertThat(trackedNodeUid).as("node the discovery service reports for %s", DB_NAME).isNotNull();
        endpointMoved = false;

        log.info("Database {} (bdb {}): endpoint {} proxied by {}, discovery service tracks node {}", DB_NAME, bdbId,
                endpointId, proxies, trackedNodeUid);
    }

    @AfterEach
    public void restoreEndpointBinding() {

        if (!endpointMoved) {
            return;
        }

        // 'policy single' resets both overriding constraints - rladmin sends empty include_proxies and exclude_proxies
        // alongside the policy - and collapses the endpoint back onto one proxy. 'include <node>' would clear the exclude
        // but leave a two-proxy endpoint behind, which then makes the next test's 'exclude' a coin flip: removing a proxy
        // the discovery service is not tracking changes nothing and publishes nothing.
        String command = String.format("bind endpoint %s policy single", endpointId);
        try {
            Boolean restored = faultClient.executeRladminCommand(bdbId, command, RLADMIN_CHECK_INTERVAL, RLADMIN_TIMEOUT)
                    .onErrorReturn(Boolean.FALSE).block(RLADMIN_TIMEOUT);
            log.info("Restore '{}' completed: {}", command, restored);
        } catch (RuntimeException e) {
            // rladmin exits non-zero on a no-op ("Nothing to do"), which the fault injector reports as a failure.
            log.warn("Restore '{}' did not complete cleanly: {}", command, e.toString());
        }
    }

    @Test
    @DisplayName("announces the database under both its external and internal master name")
    public void sentinelServiceAnnouncesTheDatabase() {

        int dbPort = mStandard.getRawEndpoints().get(0).getPort();

        RedisClient client = RedisClient.create();
        client.setOptions(EnterpriseSentinelSupport.sentinelClientOptions());

        try (StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(StringCodec.UTF8,
                sentinels.get(0))) {

            assertThat(connection.sync().ping()).isEqualTo("PONG");

            Map<String, String> master = connection.sync().master(DB_NAME);
            log.info("SENTINEL MASTER {} -> {}", DB_NAME, master);
            assertThat(master).containsEntry("name", DB_NAME).containsEntry("flags", "master")
                    .containsEntry("num-other-sentinels", "0").containsEntry("port", String.valueOf(dbPort));
            assertThat(master.get("ip")).isNotEmpty();

            // Every database is announced twice: under its plain name (external address) and under <db>@internal.
            List<Map<String, String>> masters = connection.sync().masters();
            assertThat(masters).extracting(entry -> entry.get("name")).contains(DB_NAME, DB_NAME + INTERNAL_SUFFIX);

            SocketAddress reported = connection.sync().getMasterAddrByName(DB_NAME);
            assertThat(EnterpriseSentinelSupport.format(reported)).isEqualTo(master.get("ip") + ":" + dbPort);
        } finally {
            client.shutdown();
        }
    }

    @Test
    @DisplayName("every cluster node answers for the database, so any node is a usable sentinel")
    public void everyNodeAnswersForTheDatabase() {

        String expected = EnterpriseSentinelSupport.format(EnterpriseSentinelSupport.reportedMaster(sentinels.get(0), DB_NAME));

        for (RedisURI sentinel : sentinels) {
            String reported = EnterpriseSentinelSupport.format(EnterpriseSentinelSupport.reportedMaster(sentinel, DB_NAME));
            log.info("{} reports master {} at {}", sentinel, DB_NAME, reported);
            assertThat(reported).as("master address reported by %s", sentinel).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("implements SENTINEL SLAVES and SUBSCRIBE but neither SENTINEL REPLICAS nor PSUBSCRIBE")
    public void documentsTheTwoProtocolGaps() {

        RedisClient client = RedisClient.create();
        client.setOptions(EnterpriseSentinelSupport.sentinelClientOptions());

        try (StatefulRedisSentinelConnection<String, String> sentinel = client.connectSentinel(StringCodec.UTF8,
                sentinels.get(0))) {

            // Gap 1: SentinelTopologyProvider.getNodes() issues SENTINEL REPLICAS, which is unimplemented. This is what
            // makes MasterReplica.connect() fail before pub/sub is even reached.
            assertThatThrownBy(() -> sentinel.sync().replicas(DB_NAME)).isInstanceOf(RedisCommandExecutionException.class)
                    .hasMessageContaining("unknown command");

            // Lettuce does not issue the pre-5.0 spelling; it takes the unknown-command reply above as an empty replica
            // list. This asserts the server agrees that this is the right answer: SENTINEL SLAVES is implemented and
            // reports nothing, because a proxied endpoint has no client-visible replicas.
            assertThat(sentinel.sync().slaves(DB_NAME)).isEmpty();
        } finally {
            client.shutdown();
        }

        RedisClient pubSubClient = RedisClient.create();
        pubSubClient.setOptions(EnterpriseSentinelSupport.sentinelClientOptions());

        try (StatefulRedisPubSubConnection<String, String> pubSub = pubSubClient.connectPubSub(StringCodec.UTF8,
                sentinels.get(0))) {

            // Gap 2: SentinelTopologyRefresh only ever issues PSUBSCRIBE *, which is unimplemented, so no Sentinel event
            // ever reaches Lettuce's topology-refresh predicates.
            assertThatThrownBy(() -> pubSub.sync().psubscribe("*")).isInstanceOf(RedisCommandExecutionException.class)
                    .hasMessageContaining("unknown command");

            // Explicit SUBSCRIBE of the channels Redis Enterprise does publish works.
            pubSub.sync().subscribe(SWITCH_MASTER_CHANNEL, EnterpriseSentinelSupport.PLUS_MASTER_CHANNEL,
                    EnterpriseSentinelSupport.MINUS_MASTER_CHANNEL);
        } finally {
            pubSubClient.shutdown();
        }
    }

    @Test
    @DisplayName("publishes +switch-master on an endpoint move, and a new sentinel lookup resolves the new master")
    public void plainSentinelUriDiscoversAndFollowsTheEndpoint() {

        int dbPort = mStandard.getRawEndpoints().get(0).getPort();
        RedisURI sentinelUri = EnterpriseSentinelSupport.sentinelDiscoveryUri(mStandard, DB_NAME);

        RedisClient client = RedisClient.create();
        client.setOptions(RecommendedSettingsProvider.forConnectionInterruption());

        try (EnterpriseSentinelSupport.SwitchMasterCapture capture = new EnterpriseSentinelSupport.SwitchMasterCapture(
                sentinels.get(0));
                EnterpriseSentinelSupport.ConnectionEventCapture activations = new EnterpriseSentinelSupport.ConnectionEventCapture(
                        client)) {

            // A plain sentinel URI resolves through SENTINEL GET-MASTER-ADDR-BY-NAME only, which Redis Enterprise
            // implements - so this connection path works today.
            StatefulRedisConnection<String, String> connection = client.connect(StringCodec.UTF8, sentinelUri);
            try {
                assertThat(connection.sync().ping()).isEqualTo("PONG");

                String before = EnterpriseSentinelSupport
                        .format(EnterpriseSentinelSupport.reportedMaster(sentinels.get(0), DB_NAME));
                await().atMost(ACTIVATION_TIMEOUT).pollInterval(Duration.ofMillis(200)).untilAsserted(
                        () -> assertThat(activations.remotesOnPort(dbPort)).as("initial data connection").contains(before));

                String key = "sentinel-discovery-" + System.currentTimeMillis();
                connection.sync().set(key, "before-move");

                // Move the endpoint off the node the discovery service tracks. That is the deterministic producer of
                // +switch-master: it removes the tracked node from proxy_uids, so the reported address has to change.
                String command = String.format("bind endpoint %s exclude %s", endpointId, trackedNodeUid);
                log.info("Triggering endpoint move: rladmin {}", command);
                endpointMoved = true;
                Boolean moved = faultClient.executeRladminCommand(bdbId, command, RLADMIN_CHECK_INTERVAL, RLADMIN_TIMEOUT)
                        .block(RLADMIN_TIMEOUT);
                assertThat(moved).as("rladmin %s", command).isTrue();

                // Redis Enterprise publishes one +switch-master per master name: the plain one and <db>@internal.
                await().atMost(SWITCH_MASTER_TIMEOUT).pollInterval(Duration.ofMillis(500))
                        .until(() -> capture.firstSwitchMasterFor(DB_NAME).isPresent());

                assertThat(capture.switchMasterPayloads(DB_NAME)).as("+switch-master for the plain master name").hasSize(1);
                assertThat(capture.switchMasterPayloads(DB_NAME + INTERNAL_SUFFIX))
                        .as("+switch-master for the @internal master name").hasSize(1);

                String payload = capture.firstSwitchMasterFor(DB_NAME).get();
                log.info("+switch-master payload: {}", payload);
                String[] parts = payload.split(" ");
                assertThat(parts).as("payload is '<name> <old-ip> <old-port> <new-ip> <new-port>'").hasSize(5);
                assertThat(parts[0]).isEqualTo(DB_NAME);
                assertThat(parts[1] + ":" + parts[2]).as("old address").isEqualTo(before);
                assertThat(parts[2]).as("the port never changes on an endpoint move").isEqualTo(parts[4]);

                String after = parts[3] + ":" + parts[4];
                assertThat(after).as("new address differs from the old one").isNotEqualTo(before);

                // Independently confirm Redis Enterprise really moved the endpoint, so an infrastructure no-op cannot be
                // misread as a client-side problem.
                await().atMost(ENDPOINT_MOVE_TIMEOUT).pollInterval(Duration.ofSeconds(1)).until(() -> after.equals(
                        EnterpriseSentinelSupport.format(EnterpriseSentinelSupport.reportedMaster(sentinels.get(0), DB_NAME))));

                // A newly established connection over the same sentinel URI now resolves to the new endpoint node.
                StatefulRedisConnection<String, String> afterMove = client.connect(StringCodec.UTF8, sentinelUri);
                try {
                    assertThat(afterMove.sync().ping()).isEqualTo("PONG");
                    assertThat(afterMove.sync().get(key)).as("data survives the endpoint move").isEqualTo("before-move");
                    assertThat(activations.remotesOnPort(dbPort)).as("a data connection was activated on the new endpoint")
                            .contains(after);
                } finally {
                    afterMove.close();
                }

                // The pre-existing connection recovers too, but only once the old proxy stops serving it, which Redis
                // Enterprise defers by endpoint_rebind_propagation_grace_time. Report it rather than assert on the timing.
                log.info("Pre-existing connection still open: {}; activations on port {}: {}", connection.isOpen(), dbPort,
                        activations.remotesOnPort(dbPort));
                assertThat(connection.sync().ping()).as("pre-existing connection still serves commands").isEqualTo("PONG");

                capture.stopRecording();
                assertThat(capture.isAlive()).as("the discovery-service subscriber stayed connected").isTrue();
            } finally {
                connection.close();
            }
        } finally {
            client.shutdown();
        }
    }

    @Test
    @DisplayName("negative control: a shard failover moves no endpoint and publishes no +switch-master")
    public void shardFailoverDoesNotMoveTheEndpoint() {

        String before = EnterpriseSentinelSupport.format(EnterpriseSentinelSupport.reportedMaster(sentinels.get(0), DB_NAME));
        List<String> masterShardsBefore = new ArrayList<>(clusterConfig.getMasterShardIds());

        try (EnterpriseSentinelSupport.SwitchMasterCapture capture = new EnterpriseSentinelSupport.SwitchMasterCapture(
                sentinels.get(0))) {

            // A shard failover permutes proxy_uids without growing it, and Redis Enterprise only rebinds an endpoint when
            // the new proxy set is a strict superset of the current one. The endpoint - and so the address clients connect
            // to - does not move, and nothing is published. The fault injector's first-class 'failover' action behaves the
            // same way, which is worth pinning because a reader expects a Sentinel event here.
            String nodeWithMasterShards = clusterConfig.getNodeWithMasterShards();
            log.info("Triggering shard failover on node {}", nodeWithMasterShards);
            Boolean failedOver = faultClient.triggerShardFailover(bdbId, nodeWithMasterShards, clusterConfig)
                    .block(FAILOVER_TIMEOUT);
            assertThat(failedOver).as("shard failover on node %s", nodeWithMasterShards).isTrue();

            // Prove the failover really happened, so the silence below is not vacuous: promoting the replicas changes
            // which shard uids are masters.
            RedisEnterpriseConfig afterFailover = RedisEnterpriseConfig.refreshClusterConfig(faultClient, bdbId);
            assertThat(afterFailover.getMasterShardIds()).as("master shard ids changed, so a failover did occur")
                    .isNotEqualTo(masterShardsBefore);

            sleep(SILENCE_WINDOW);

            assertThat(capture.switchMasterPayloads(DB_NAME)).as("no +switch-master for the plain master name").isEmpty();
            assertThat(capture.switchMasterPayloads(DB_NAME + INTERNAL_SUFFIX))
                    .as("no +switch-master for the @internal master name").isEmpty();
            assertThat(EnterpriseSentinelSupport.format(EnterpriseSentinelSupport.reportedMaster(sentinels.get(0), DB_NAME)))
                    .as("the endpoint address is unchanged").isEqualTo(before);
            // Otherwise "no event" would be indistinguishable from "the subscriber was disconnected".
            assertThat(capture.isAlive()).as("the discovery-service subscriber stayed connected").isTrue();
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", e);
        }
    }

}
