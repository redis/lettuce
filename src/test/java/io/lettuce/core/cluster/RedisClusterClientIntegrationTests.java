package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.event.command.CommandStartedEvent;
import io.lettuce.core.event.command.CommandSucceededEvent;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for {@link RedisClusterClient}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class RedisClusterClientIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisClusterClient clusterClient;

    private StatefulRedisConnection<String, String> redis1;

    private StatefulRedisConnection<String, String> redis2;

    private StatefulRedisConnection<String, String> redis3;

    private StatefulRedisConnection<String, String> redis4;

    private RedisCommands<String, String> redissync1;

    private RedisCommands<String, String> redissync2;

    private RedisCommands<String, String> redissync3;

    private RedisCommands<String, String> redissync4;

    private RedisAdvancedClusterCommands<String, String> sync;

    private StatefulRedisClusterConnection<String, String> connection;

    @Inject
    RedisClusterClientIntegrationTests(RedisClient client, RedisClusterClient clusterClient) {
        this.client = client;
        this.clusterClient = clusterClient;
    }

    @BeforeEach
    void before() {

        clusterClient.setOptions(ClusterClientOptions.create());

        redis1 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port1).build());
        redis2 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port2).build());
        redis3 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port3).build());
        redis4 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port4).build());

        redissync1 = redis1.sync();
        redissync2 = redis2.sync();
        redissync3 = redis3.sync();
        redissync4 = redis4.sync();

        clusterClient.reloadPartitions();
        connection = clusterClient.connect();
        sync = connection.sync();
    }

    @AfterEach
    void after() {
        connection.close();
        redis1.close();

        redissync1.getStatefulConnection().close();
        redissync2.getStatefulConnection().close();
        redissync3.getStatefulConnection().close();
        redissync4.getStatefulConnection().close();
    }

    @Test
    void statefulConnectionFromSync() {
        RedisAdvancedClusterCommands<String, String> sync = clusterClient.connect().sync();
        assertThat(sync.getStatefulConnection().sync()).isSameAs(sync);
        connection.close();
    }

    @Test
    void statefulConnectionFromAsync() {
        RedisAdvancedClusterAsyncCommands<String, String> async = clusterClient.connect().async();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
        connection.close();
    }

    @Test
    void shouldApplyTimeoutOnRegularConnection() {

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        assertThat(connection.getTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(connection.getConnection(host, ClusterTestSettings.port1).getTimeout()).isEqualTo(Duration.ofMinutes(1));

        connection.close();
    }

    @Test
    void clusterConnectionShouldSetClientName() {

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");
        Delay.delay(Duration.ofMillis(10));
        connection.sync().quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();
        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");

        StatefulRedisConnection<String, String> nodeConnection = connection
                .getConnection(connection.getPartitions().getPartition(0).getNodeId());
        assertThat(nodeConnection.sync().clientGetname()).isEqualTo("my-client");

        connection.close();
    }

    @Test
    void pubSubClusterConnectionShouldSetClientName() {

        StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub();

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");
        Delay.delay(Duration.ofMillis(10));
        connection.sync().quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");

        StatefulRedisConnection<String, String> nodeConnection = connection
                .getConnection(connection.getPartitions().getPartition(0).getNodeId());
        assertThat(nodeConnection.sync().clientGetname()).isEqualTo("my-client");

        connection.close();
    }

    @Test
    void reloadPartitions() {

        clusterClient.reloadPartitions();
        assertThat(clusterClient.getPartitions()).hasSize(4);
    }

    @Test
    void suspendedTopologyRefreshCanBeResumed() {

        RedisClusterClient client = RedisClusterClient.create(clusterClient.getResources(),
                RedisURI.Builder.redis(host, ClusterTestSettings.port1).build());
        try {

            client.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
                    .enablePeriodicRefresh(true).refreshPeriod(Duration.ofMillis(200)).build()).build());
            client.connect().close();

            Wait.untilTrue(client::isTopologyRefreshInProgress).during(Duration.ofSeconds(5)).waitOrTimeout();

            client.suspendTopologyRefresh();

            Wait.untilTrue(() -> !client.isTopologyRefreshInProgress()).during(Duration.ofSeconds(5)).waitOrTimeout();

            client.getPartitions().clear();
            client.getPartitions().updateCache();

            client.connect().close();
            Wait.untilTrue(client::isTopologyRefreshInProgress).during(Duration.ofSeconds(5)).waitOrTimeout();
            Wait.untilTrue(() -> !client.isTopologyRefreshInProgress()).during(Duration.ofSeconds(5)).waitOrTimeout();

            assertThat(client.getPartitions()).isNotEmpty();
        } finally {
            FastShutdown.shutdown(client);
        }
    }

    @Test
    void reloadPartitionsWithDynamicSourcesFallsBackToInitialSeedNodes() {

        client.setOptions(ClusterClientOptions.builder()
                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder().dynamicRefreshSources(true).build()).build());

        Partitions partitions = clusterClient.getPartitions();
        partitions.clear();
        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 1), "foo", false, null, 0, 0, 0,
                Collections.emptyList(), Collections.emptySet()));

        Partitions reloaded = clusterClient.loadPartitions();

        assertThat(reloaded).hasSize(4);
    }

    @Test
    void testClusteredOperations() {

        SlotHash.getSlot(ClusterTestSettings.KEY_B.getBytes()); // 3300-2020 -> Node 1 and Slave (Node 3)
        SlotHash.getSlot(ClusterTestSettings.KEY_A.getBytes()); // 15495 -> Node 2

        RedisFuture<String> result = redis1.async().set(ClusterTestSettings.KEY_B, value);
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redissync1.set(ClusterTestSettings.KEY_B, "value")).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.async().set(ClusterTestSettings.KEY_A, value);

        assertThatThrownBy(() -> TestFutures.awaitOrTimeout(resultMoved)).hasMessageContaining("MOVED 15495");

        clusterClient.reloadPartitions();
        RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync();

        assertThat(connection.set(ClusterTestSettings.KEY_A, value)).isEqualTo("OK");
        assertThat(connection.set(ClusterTestSettings.KEY_B, "myValue2")).isEqualTo("OK");
        assertThat(connection.set(ClusterTestSettings.KEY_D, "myValue2")).isEqualTo("OK");

        connection.getStatefulConnection().close();
    }

    @Test
    void testReset() {

        clusterClient.reloadPartitions();
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

        connection.sync().set(ClusterTestSettings.KEY_A, value);
        connection.reset();

        assertThat(connection.sync().set(ClusterTestSettings.KEY_A, value)).isEqualTo("OK");
        connection.close();
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    void testClusterCommandRedirection() {

        TestCommandListener listener = new TestCommandListener();
        clusterClient.getPartitions();

        try {

            clusterClient.addListener(listener);
            RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync();

            // Command on node within the default connection
            assertThat(connection.set(ClusterTestSettings.KEY_B, value)).isEqualTo("OK");

            // gets routing to node 3
            assertThat(connection.set(ClusterTestSettings.KEY_A, value)).isEqualTo("OK");
            connection.getStatefulConnection().close();
        } finally {
            client.removeListener(listener);
        }

        assertThat(listener.started).hasSizeGreaterThanOrEqualTo(2);
        assertThat(listener.succeeded).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    void testClusterRedirection() {

        TestCommandListener listener = new TestCommandListener();
        clusterClient.getPartitions();
        try {

            clusterClient.addListener(listener);
            RedisAdvancedClusterAsyncCommands<String, String> connection = clusterClient.connect().async();
            Partitions partitions = clusterClient.getPartitions();

            for (RedisClusterNode partition : partitions) {
                partition.setSlots(Collections.emptyList());
                if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                    partition.getFlags().add(RedisClusterNode.NodeFlag.UPSTREAM);
                    partition.setSlots(IntStream.range(0, SlotHash.SLOT_COUNT).boxed().collect(Collectors.toList()));
                }
            }
            partitions.updateCache();

            // appropriate cluster node
            RedisFuture<String> setB = connection.set(ClusterTestSettings.KEY_B, value);

            assertThat(setB.toCompletableFuture()).isInstanceOf(AsyncCommand.class);

            TestFutures.awaitOrTimeout(setB);
            assertThat(setB.getError()).isNull();
            assertThat(TestFutures.getOrTimeout(setB)).isEqualTo("OK");

            // gets redirection to node 3
            RedisFuture<String> setA = connection.set(ClusterTestSettings.KEY_A, value);

            assertThat((CompletionStage) setA).isInstanceOf(AsyncCommand.class);

            TestFutures.awaitOrTimeout(setA);
            assertThat(setA.getError()).isNull();
            assertThat(TestFutures.getOrTimeout(setA)).isEqualTo("OK");

            connection.getStatefulConnection().close();
        } finally {
            clusterClient.removeListener(listener);
        }

        assertThat(listener.started).hasSizeGreaterThanOrEqualTo(2);
        assertThat(listener.succeeded).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    void testClusterRedirectionLimit() throws Exception {

        clusterClient.setOptions(ClusterClientOptions.builder().maxRedirects(0).build());
        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterClient.connect().async();
        Partitions partitions = clusterClient.getPartitions();

        for (RedisClusterNode partition : partitions) {

            if (partition.getSlots().contains(15495)) {
                partition.setSlots(Collections.emptyList());
            } else {
                partition.setSlots(IntStream.range(0, SlotHash.SLOT_COUNT).boxed().collect(Collectors.toList()));
            }

        }
        partitions.updateCache();

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set(ClusterTestSettings.KEY_A, value);

        assertThat(setA instanceof AsyncCommand).isTrue();

        setA.await(10, TimeUnit.SECONDS);
        assertThat(setA.getError()).isEqualTo("MOVED 15495 127.0.0.1:7380");

        connection.getStatefulConnection().close();
    }

    @Test
    void closeConnection() {

        RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync();

        List<String> time = connection.time();
        assertThat(time).hasSize(2);

        connection.getStatefulConnection().close();

        assertThatThrownBy(connection::time).isInstanceOf(RedisException.class);
    }

    @Test
    void clusterAuth() {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), ClusterTestSettings.port7).withPassword("foobared").build());

        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        List<String> time = sync.time();
        assertThat(time).hasSize(2);

        TestFutures.awaitOrTimeout(connection.async().quit());

        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        time = sync.time();
        assertThat(time).hasSize(2);

        connection.close();
        FastShutdown.shutdown(clusterClient);
    }

    @Test
    void shutdownWithOpenConnectionShouldCloseCorrectly() {

        ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
                .enableAllAdaptiveRefreshTriggers().enablePeriodicRefresh(Duration.ofSeconds(1)).build();

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), ClusterTestSettings.port1).build());

        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(refreshOptions).build());

        clusterClient.connect().sync();

        FastShutdown.shutdown(clusterClient);
    }

    @Test
    void partitionRetrievalShouldFail() {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), ClusterTestSettings.port7).build());

        assertThatThrownBy(clusterClient::getPartitions).isInstanceOf(RedisException.class)
                .hasMessageContaining("Cannot obtain initial Redis Cluster topology");

        FastShutdown.shutdown(clusterClient);
    }

    @Test
    void clusterNeedsAuthButNotSupplied() {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), ClusterTestSettings.port7).build());

        try {
            assertThatThrownBy(clusterClient::connect).isInstanceOf(RedisException.class);
        } finally {
            connection.close();
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void appliesNodeFilter() {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, ClusterTestSettings.port1).build());
        try {

            clusterClient.setOptions(
                    ClusterClientOptions.builder().nodeFilter(it -> it.is(RedisClusterNode.NodeFlag.UPSTREAM)).build());
            Partitions partitions = clusterClient.getPartitions();
            assertThat(partitions).hasSize(2).allMatch(it -> it.is(RedisClusterNode.NodeFlag.UPSTREAM));
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void noClusterNodeAvailable() {

        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, 40400).build());
        try {
            clusterClient.connect();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisException.class);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void getClusterNodeConnection() {

        RedisClusterNode redis1Node = getOwnPartition(redissync2);

        RedisClusterCommands<String, String> connection = sync.getConnection(TestSettings.hostAddr(),
                ClusterTestSettings.port2);

        String result = connection.clusterMyId();
        assertThat(result).isEqualTo(redis1Node.getNodeId());

    }

    @Test
    void operateOnNodeConnection() {

        sync.set(ClusterTestSettings.KEY_A, value);
        sync.set(ClusterTestSettings.KEY_B, "d");

        StatefulRedisConnection<String, String> statefulRedisConnection = connection.getConnection(TestSettings.hostAddr(),
                ClusterTestSettings.port2);

        RedisClusterCommands<String, String> connection = statefulRedisConnection.sync();

        assertThat(connection.get(ClusterTestSettings.KEY_A)).isEqualTo(value);
        try {
            connection.get(ClusterTestSettings.KEY_B);
            fail("missing RedisCommandExecutionException: MOVED");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    void testGetConnectionAsyncByNodeId() {

        RedisClusterNode partition = connection.getPartitions().getPartition(0);

        StatefulRedisConnection<String, String> node = TestFutures
                .getOrTimeout(connection.getConnectionAsync(partition.getNodeId()));

        assertThat(node.sync().ping()).isEqualTo("PONG");
    }

    @Test
    void testGetConnectionAsyncByHostAndPort() {

        RedisClusterNode partition = connection.getPartitions().getPartition(0);

        RedisURI uri = partition.getUri();
        StatefulRedisConnection<String, String> node = connection.getConnectionAsync(uri.getHost(), uri.getPort()).join();

        assertThat(node.sync().ping()).isEqualTo("PONG");
    }

    @Test
    void testStatefulConnection() {
        RedisAdvancedClusterAsyncCommands<String, String> async = connection.async();

        assertThat(TestFutures.getOrTimeout(async.ping())).isEqualTo("PONG");
    }

    @Test
    void getButNoPartitionForSlothash() {

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            redisClusterNode.setSlots(new ArrayList<>());

        }
        RedisChannelHandler rch = (RedisChannelHandler) connection;
        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) rch.getChannelWriter();
        writer.setPartitions(clusterClient.getPartitions());
        clusterClient.getPartitions().reload(clusterClient.getPartitions().getPartitions());

        assertThatThrownBy(() -> sync.get(key)).isInstanceOf(RedisException.class);
    }

    @Test
    void readOnlyOnCluster() {

        sync.readOnly();
        // commands are dispatched to a different connection, therefore it works for us.
        sync.set(ClusterTestSettings.KEY_B, value);

        TestFutures.awaitOrTimeout(connection.async().quit());

        assertThat(connection).extracting("connectionState").extracting("readOnly").isEqualTo(Boolean.TRUE);

        sync.readWrite();

        assertThat(connection).extracting("connectionState").extracting("readOnly").isEqualTo(Boolean.FALSE);
        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, 40400).build());
        try {
            clusterClient.connect();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisException.class);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void getKeysInSlot() {

        sync.flushall();

        sync.set(ClusterTestSettings.KEY_A, value);
        sync.set(ClusterTestSettings.KEY_B, value);

        List<String> keysA = sync.clusterGetKeysInSlot(ClusterTestSettings.SLOT_A, 10);
        assertThat(keysA).isEqualTo(Collections.singletonList(ClusterTestSettings.KEY_A));

        List<String> keysB = sync.clusterGetKeysInSlot(ClusterTestSettings.SLOT_B, 10);
        assertThat(keysB).isEqualTo(Collections.singletonList(ClusterTestSettings.KEY_B));

    }

    @Test
    void countKeysInSlot() {

        sync.flushall();

        sync.set(ClusterTestSettings.KEY_A, value);
        sync.set(ClusterTestSettings.KEY_B, value);

        Long result = sync.clusterCountKeysInSlot(ClusterTestSettings.SLOT_A);
        assertThat(result).isEqualTo(1L);

        result = sync.clusterCountKeysInSlot(ClusterTestSettings.SLOT_B);
        assertThat(result).isEqualTo(1L);

        int slotZZZ = SlotHash.getSlot("ZZZ".getBytes());
        result = sync.clusterCountKeysInSlot(slotZZZ);
        assertThat(result).isEqualTo(0L);

    }

    @Test
    void testClusterCountFailureReports() {
        RedisClusterNode ownPartition = getOwnPartition(redissync1);
        assertThat(redissync1.clusterCountFailureReports(ownPartition.getNodeId())).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testClusterKeyslot() {
        assertThat(redissync1.clusterKeyslot(ClusterTestSettings.KEY_A)).isEqualTo(ClusterTestSettings.SLOT_A);
        assertThat(SlotHash.getSlot(ClusterTestSettings.KEY_A)).isEqualTo(ClusterTestSettings.SLOT_A);
    }

    @Test
    void testClusterSaveconfig() {
        assertThat(redissync1.clusterSaveconfig()).isEqualTo("OK");
    }

    @Test
    void testClusterSetConfigEpoch() {
        try {
            redissync1.clusterSetConfigEpoch(1L);
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("ERR The user can assign a config epoch only");
        }
    }

    @Test
    void testReadFrom() {
        StatefulRedisClusterConnection<String, String> statefulConnection = connection;

        assertThat(statefulConnection.getReadFrom()).isEqualTo(ReadFrom.UPSTREAM);

        statefulConnection.setReadFrom(ReadFrom.NEAREST);
        assertThat(statefulConnection.getReadFrom()).isEqualTo(ReadFrom.NEAREST);
    }

    @Test
    void testReadFromNull() {
        assertThatThrownBy(() -> connection.setReadFrom(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPfmerge() {

        RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync();

        assertThat(SlotHash.getSlot("key2660")).isEqualTo(SlotHash.getSlot("key7112")).isEqualTo(SlotHash.getSlot("key8885"));

        connection.pfadd("key2660", "rand", "mat");
        connection.pfadd("key7112", "mat", "perrin");

        connection.pfmerge("key8885", "key2660", "key7112");

        assertThat(connection.pfcount("key8885")).isEqualTo(3);

        connection.getStatefulConnection().close();
    }

    static class TestCommandListener implements CommandListener {

        final List<CommandStartedEvent> started = new Vector<>();

        final List<CommandSucceededEvent> succeeded = new Vector<>();

        final List<CommandFailedEvent> failed = new Vector<>();

        @Override
        public void commandStarted(CommandStartedEvent event) {
            started.add(event);
        }

        @Override
        public void commandSucceeded(CommandSucceededEvent event) {
            succeeded.add(event);
        }

        @Override
        public void commandFailed(CommandFailedEvent event) {
            failed.add(event);
        }

        public void clear() {
            started.clear();
            succeeded.clear();
            failed.clear();
        }

    }

    @Test
    void shouldCancelTopologyRefreshTaskOnShutdown() {
        ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(1)).build();
        RedisClusterClient clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), ClusterTestSettings.port1).build());
        clusterClient.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(refreshOptions).build());
        clusterClient.connect().sync();
        Delay.delay(Duration.ofMillis(1500));
        assertThat(clusterClient.isTopologyRefreshInProgress()).isTrue();

        clusterClient.shutdownAsync(0, 10, TimeUnit.SECONDS).join();

        assertThat(clusterClient.isTopologyRefreshInProgress()).isFalse();
        FastShutdown.shutdown(clusterClient);
    }

}
