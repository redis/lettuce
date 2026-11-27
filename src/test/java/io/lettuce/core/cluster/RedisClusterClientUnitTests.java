package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.topology.ClusterTopologyRefresh;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.protocol.MaintenanceNotification.SlotMigration;
import io.lettuce.core.protocol.SlotMigrationNotifications;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * Unit tests for {@link RedisClusterClient}.
 */
@Tag(UNIT_TEST)
class RedisClusterClientUnitTests {

    @Test
    void connectAsyncShouldNotWrapConnectFailureInCompletionException() {

        RedisConnectionException lookupFailure = new RedisConnectionException("Cannot resolve node address");
        RedisClusterClient clusterClient = clientFailingSocketAddressLookup(lookupFailure);

        try {
            CompletableFuture<?> future = clusterClient.connectAsync(StringCodec.UTF8);

            assertConnectFailure(future, lookupFailure);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void connectPubSubAsyncShouldNotWrapConnectFailureInCompletionException() {

        RedisConnectionException lookupFailure = new RedisConnectionException("Cannot resolve node address");
        RedisClusterClient clusterClient = clientFailingSocketAddressLookup(lookupFailure);

        try {
            CompletableFuture<?> future = clusterClient.connectPubSubAsync(StringCodec.UTF8);

            assertConnectFailure(future, lookupFailure);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldMoveMigratedSlotsToKnownDestination() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);

        try {
            boolean applied = clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.2:6379", "10-19,50"));

            Partitions partitions = clusterClient.getPartitions();
            assertThat(applied).isTrue();
            assertThat(partitions.getMasterBySlot(9).getUri().getHost()).isEqualTo("10.0.0.1");
            assertThat(partitions.getMasterBySlot(10).getUri().getHost()).isEqualTo("10.0.0.2");
            assertThat(partitions.getPartitionBySlot(19).getUri().getHost()).isEqualTo("10.0.0.2");
            assertThat(partitions.getMasterBySlot(50).getUri().getHost()).isEqualTo("10.0.0.2");
            assertThat(partitions.getMasterBySlot(20).getUri().getHost()).isEqualTo("10.0.0.1");
            assertThat(partitions.getPartition("10.0.0.1", 6379).getSlots()).hasSize(89);
            assertThat(partitions.getPartition("10.0.0.2", 6379).getSlots()).hasSize(111);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldKeepSourceWithoutSlotsInTopology() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);

        try {
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.2:6379", "0-99"))).isTrue();

            Partitions partitions = clusterClient.getPartitions();
            assertThat(partitions).hasSize(2);
            assertThat(partitions.getPartition("10.0.0.1", 6379).hasNoSlots()).isTrue();
            assertThat(partitions.getMasterBySlot(0).getUri().getHost()).isEqualTo("10.0.0.2");
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldNotApplyMigrationToUnknownDestination() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);

        try {
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.9:6379", "10-19"))).isFalse();
            assertThat(clusterClient.getPartitions().getMasterBySlot(10).getUri().getHost()).isEqualTo("10.0.0.1");
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldNotApplyMigrationOfSlotsOwnedByThirdNode() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199, "10.0.0.3", 200, 299);

        try {
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.2:6379", "200-209"))).isFalse();
            assertThat(clusterClient.getPartitions().getMasterBySlot(200).getUri().getHost()).isEqualTo("10.0.0.3");
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldNotApplyAnyMigrationIfOneDestinationIsUnknown() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);

        try {
            boolean applied = clusterClient.applySlotMigrations(
                    migrations("10.0.0.1:6379", "10.0.0.2:6379", "10-19", "10.0.0.1:6379", "10.0.0.9:6379", "20-29"));

            assertThat(applied).isFalse();
            assertThat(clusterClient.getPartitions().getMasterBySlot(10).getUri().getHost()).isEqualTo("10.0.0.1");
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldTreatSlotsAlreadyOwnedByDestinationAsApplied() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);

        try {
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.2:6379", "100-109"))).isTrue();
            assertThat(clusterClient.getPartitions().getPartition("10.0.0.2", 6379).getSlots()).hasSize(100);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldNotApplyMigrationToReplicaDestination() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);
        Partitions partitions = clusterClient.getPartitions();
        partitions.add(new RedisClusterNode(RedisURI.create("10.0.0.3", 6379), "replica", true, "node-0", 0, 0, 0,
                Collections.emptyList(), Collections.singleton(RedisClusterNode.NodeFlag.REPLICA)));
        partitions.updateCache();

        try {
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.3:6379", "10-19"))).isFalse();
            assertThat(clusterClient.getPartitions().getMasterBySlot(10).getUri().getHost()).isEqualTo("10.0.0.1");
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldUpdateConnectionsWhenSlotsWereMoved() {

        CountingClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);
        int before = clusterClient.connectionUpdates.get();

        try {
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.2:6379", "10-19"))).isTrue();
            assertThat(clusterClient.connectionUpdates.get() - before).isEqualTo(1);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldNotUpdateConnectionsWhenNothingWasMoved() {

        CountingClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);
        int before = clusterClient.connectionUpdates.get();

        try {
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.2:6379", "100-109"))).isTrue();
            assertThat(clusterClient.applySlotMigrations(migrations("10.0.0.1:6379", "10.0.0.9:6379", "10-19"))).isFalse();
            assertThat(clusterClient.connectionUpdates.get() - before).isZero();
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void shouldNotApplyEmptyMigrations() {

        RedisClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99);

        try {
            assertThat(clusterClient.applySlotMigrations(Collections.emptyList())).isFalse();
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void topologyUpdateShouldMoveSlotsWithoutRefresh() {

        CountingClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);

        try {
            CompletionStage<Void> update = clusterClient
                    .applyTopologyUpdate(migrations("10.0.0.1:6379", "10.0.0.2:6379", "10-19"));

            assertThat(update.toCompletableFuture()).isCompleted();
            assertThat(clusterClient.getPartitions().getMasterBySlot(10).getUri().getHost()).isEqualTo("10.0.0.2");
            assertThat(clusterClient.topologyLoads).isEmpty();
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void topologyUpdateShouldLoadTopologyFromUnknownDestination() {

        CountingClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);
        clusterClient.topologyViews
                .add(CompletableFuture.completedFuture(Collections.singletonMap(RedisURI.create("10.0.0.9", 6379),
                        masters("10.0.0.1", 0, 9, "10.0.0.2", 100, 199, "10.0.0.9", 10, 99))));

        try {
            CompletionStage<Void> update = clusterClient
                    .applyTopologyUpdate(migrations("10.0.0.1:6379", "10.0.0.9:6379", "10-99"));

            assertThat(update.toCompletableFuture()).isCompleted();
            assertThat(clusterClient.topologyLoads).hasSize(1);
            TopologyLoad load = clusterClient.topologyLoads.get(0);
            assertThat(load.discovery).isFalse();
            assertThat(load.seeds).hasSize(1);
            assertThat(load.seeds.get(0).getHost()).isEqualTo("10.0.0.9");
            assertThat(load.seeds.get(0).getPort()).isEqualTo(6379);
            assertThat(clusterClient.getPartitions()).hasSize(3);
            assertThat(clusterClient.getPartitions().getMasterBySlot(10).getUri().getHost()).isEqualTo("10.0.0.9");
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void topologyUpdateShouldFallBackToRegularRefreshIfDestinationCannotBeLoaded() {

        CountingClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99, "10.0.0.2", 100, 199);

        try {
            CompletionStage<Void> update = clusterClient
                    .applyTopologyUpdate(migrations("10.0.0.1:6379", "10.0.0.9:6379", "10-19"));

            assertThat(update.toCompletableFuture()).isCompletedExceptionally();
            assertThat(clusterClient.topologyLoads.size()).isGreaterThan(1);
            assertThat(clusterClient.topologyLoads.get(0).discovery).isFalse();
            assertThat(clusterClient.topologyLoads.get(1).discovery).isTrue();
            assertThat(clusterClient.topologyLoads.get(1).seeds).extracting(RedisURI::getHost).contains("10.0.0.1", "10.0.0.2");
            assertThat(clusterClient.getPartitions().getMasterBySlot(10).getUri().getHost()).isEqualTo("10.0.0.1");
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void topologyUpdateWithoutMigrationsShouldRunRegularRefresh() {

        CountingClusterClient clusterClient = clientWithMasters("10.0.0.1", 0, 99);

        try {
            clusterClient.applyTopologyUpdate(Collections.emptyList());

            assertThat(clusterClient.topologyLoads).isNotEmpty();
            assertThat(clusterClient.topologyLoads.get(0).discovery).isTrue();
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    private static List<SlotMigration> migrations(String... triplets) {
        return SlotMigrationNotifications.smigrated(1, triplets).getMigrations();
    }

    /**
     * @param masters {@code host, firstSlot, lastSlot} of each master
     */
    private static CountingClusterClient clientWithMasters(Object... masters) {

        CountingClusterClient clusterClient = new CountingClusterClient();
        clusterClient.setPartitions(masters(masters));
        return clusterClient;
    }

    /**
     * @param masters {@code host, firstSlot, lastSlot} of each master
     */
    private static Partitions masters(Object... masters) {

        Partitions partitions = new Partitions();

        for (int i = 0; i + 2 < masters.length; i += 3) {

            List<Integer> slots = new ArrayList<>();
            for (int slot = (Integer) masters[i + 1]; slot <= (Integer) masters[i + 2]; slot++) {
                slots.add(slot);
            }

            partitions.add(new RedisClusterNode(RedisURI.create((String) masters[i], 6379), "node-" + i, true, null, 0, 0, 0,
                    slots, Collections.singleton(RedisClusterNode.NodeFlag.UPSTREAM)));
        }

        partitions.updateCache();
        return partitions;
    }

    /**
     * A request to load the topology.
     */
    private static class TopologyLoad {

        final List<RedisURI> seeds;

        final boolean discovery;

        TopologyLoad(List<RedisURI> seeds, boolean discovery) {
            this.seeds = seeds;
            this.discovery = discovery;
        }

    }

    /**
     * Counts how often the topology is handed to the cluster connections. Topology loads are recorded and answered from
     * {@link #topologyViews}; a load without a prepared view fails.
     */
    private static class CountingClusterClient extends RedisClusterClient {

        final AtomicInteger connectionUpdates = new AtomicInteger();

        final List<TopologyLoad> topologyLoads = new ArrayList<>();

        final Deque<CompletionStage<Map<RedisURI, Partitions>>> topologyViews = new ArrayDeque<>();

        CountingClusterClient() {
            super(TestClientResources.get(), Collections.singletonList(RedisURI.create("redis://foo")));
        }

        @Override
        protected ClusterTopologyRefresh createTopologyRefresh() {
            return (seeds, connectTimeout, discovery) -> loadViews(seeds, discovery);
        }

        private CompletionStage<Map<RedisURI, Partitions>> loadViews(Iterable<RedisURI> seeds, boolean discovery) {

            List<RedisURI> uris = new ArrayList<>();
            seeds.forEach(uris::add);
            topologyLoads.add(new TopologyLoad(uris, discovery));

            CompletionStage<Map<RedisURI, Partitions>> views = topologyViews.poll();
            return views != null ? views : Futures.failed(new RedisException("No topology view"));
        }

        @Override
        protected void updatePartitionsInConnections() {
            connectionUpdates.incrementAndGet();
            super.updatePartitionsInConnections();
        }

    }

    private static RedisClusterClient clientFailingSocketAddressLookup(RedisConnectionException lookupFailure) {

        RedisClusterClient clusterClient = new RedisClusterClient(TestClientResources.get(),
                Collections.singletonList(RedisURI.create("redis://foo"))) {

            @Override
            protected Supplier<CompletionStage<SocketAddress>> getSocketAddressSupplier(Supplier<Partitions> partitionsSupplier,
                    Function<Partitions, Collection<RedisClusterNode>> sortFunction) {
                return () -> Futures.failed(lookupFailure);
            }

        };
        clusterClient.setPartitions(new Partitions());
        return clusterClient;
    }

    private static void assertConnectFailure(CompletableFuture<?> future, RedisConnectionException lookupFailure) {

        Throwable error = catchThrowable(() -> future.get(5, TimeUnit.SECONDS));

        assertThat(error).isInstanceOf(ExecutionException.class);
        assertThat(error.getCause()).isInstanceOf(RedisConnectionException.class)
                .hasMessage("Unable to connect to [redis://foo]").hasCause(lookupFailure);
        assertThat(causeChain(error.getCause())).doesNotHaveAnyElementsOfTypes(CompletionException.class);
    }

    private static List<Throwable> causeChain(Throwable throwable) {

        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = throwable; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
        }
        return chain;
    }

}
