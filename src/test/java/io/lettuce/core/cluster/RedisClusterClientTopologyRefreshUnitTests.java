package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createNode;
import static io.lettuce.core.cluster.PartitionsConsensusTestSupport.createPartitions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.event.ClusterTopologyChangedEvent;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.FastShutdown;
import reactor.core.publisher.Flux;

/**
 * Unit tests for {@link RedisClusterClient} topology refresh.
 */
@Tag(UNIT_TEST)
class RedisClusterClientTopologyRefreshUnitTests {

    private final RecordingEventBus eventBus = new RecordingEventBus();

    private final ClientResources clientResources = ClientResources.builder().eventBus(eventBus).build();

    private final RedisClusterClient client = spy(
            RedisClusterClient.create(clientResources, RedisURI.create("localhost", 6379)));

    @AfterEach
    void tearDown() {
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clientResources);
    }

    @Test
    void refreshPartitionsShouldPublishTopologyChangedEventAfterApplyingPartitions() {

        Partitions before = createPartitions(createNode(1));
        Partitions after = createPartitions(createNode(2), createNode(3));

        client.setPartitions(before);
        eventBus.partitionsSupplier = client::getPartitions;
        doReturn(CompletableFuture.completedFuture(after)).when(client).loadPartitionsAsync();

        client.refreshPartitions();

        // The event must be published only after reload() applied the new topology to the client partitions. Otherwise
        // listeners that inspect the topology (e.g. through node selection on a connection) would observe stale partitions.
        assertThat(eventBus.partitionsWhenPublished.get()).as("partitions observed when the event was published")
                .containsExactlyInAnyOrderElementsOf(after);
    }

    /**
     * Captures the client partitions at the exact moment a {@link ClusterTopologyChangedEvent} is published, so the publish
     * ordering relative to the partition update can be asserted deterministically.
     */
    static class RecordingEventBus implements EventBus {

        volatile Supplier<Partitions> partitionsSupplier = Partitions::new;

        final AtomicReference<List<RedisClusterNode>> partitionsWhenPublished = new AtomicReference<>();

        @Override
        public Flux<Event> get() {
            return Flux.empty();
        }

        @Override
        public void publish(Event event) {
            if (event instanceof ClusterTopologyChangedEvent) {
                partitionsWhenPublished.set(new ArrayList<>(partitionsSupplier.get()));
            }
        }
    }
}