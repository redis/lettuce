/*
 * Copyright 2025
 */
package io.lettuce.core.cluster;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.test.LoggingTestUtils;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisAdvancedClusterReactiveCommandsImplTest {

    private RedisAdvancedClusterReactiveCommandsImpl<String, String> reactive;

    @Mock
    private StatefulRedisClusterConnection<String, String> clusterConn;

    @Mock
    private StatefulRedisConnection<String, String> nodeConn;

    @Mock
    private RedisReactiveCommands<String, String> nodeReactive;

    @Mock
    private ClusterDistributionChannelWriter writer;

    @Mock(extraInterfaces = AsyncClusterConnectionProvider.class)
    private ClusterConnectionProvider provider;

    private AsyncClusterConnectionProvider asyncProvider;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        asyncProvider = (AsyncClusterConnectionProvider) provider;

        Partitions partitions = new Partitions();
        RedisClusterNode node = new RedisClusterNode();
        node.setNodeId("node-1");
        node.setUri(RedisURI.Builder.redis("127.0.0.1").withPort(6379).build());

        List<Integer> allSlots = new ArrayList<>();
        for (int i = 0; i < SlotHash.SLOT_COUNT; i++) {
            allSlots.add(i);
        }
        node.setSlots(allSlots);
        node.setFlags(EnumSet.of(RedisClusterNode.NodeFlag.UPSTREAM));

        partitions.addPartition(node);
        partitions.updateCache();

        when(nodeConn.reactive()).thenReturn(nodeReactive);
        when(nodeReactive.clusterMyId()).thenReturn(Mono.just("node-1"));

        when(clusterConn.getChannelWriter()).thenReturn(writer);
        when(writer.getClusterConnectionProvider()).thenReturn(provider);

        when(asyncProvider.getConnectionAsync(eq(ConnectionIntent.WRITE), eq("node-1")))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(nodeConn));
        when(asyncProvider.getRandomConnectionAsync(eq(ConnectionIntent.WRITE)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(nodeConn));
        when(asyncProvider.getRandomConnectionAsync(eq(ConnectionIntent.READ)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(nodeConn));

        when(clusterConn.getConnection(eq("node-1"), eq(ConnectionIntent.WRITE))).thenReturn(nodeConn);
        when(clusterConn.getPartitions()).thenReturn(partitions);
        when(clusterConn.getOptions()).thenReturn(ClientOptions.builder().build());

        reactive = mock(RedisAdvancedClusterReactiveCommandsImpl.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        when(reactive.getStatefulConnection()).thenReturn(clusterConn);
    }

    @Test
    void ftAggregate_stampsNodeId_whenCursorCreated() {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .withCursor(AggregateArgs.WithCursor.of(1L)).build();

        AggregationReply<String, String> replyWithCursor = new AggregationReply<>();
        replyWithCursor.setCursor(AggregationReply.Cursor.of(42L, null));

        when(nodeReactive.ftAggregate(anyString(), anyString(), any())).thenReturn(Mono.just(replyWithCursor));
        when(nodeReactive.clusterMyId()).thenReturn(Mono.just("node-1"));

        AggregationReply<String, String> out = reactive.ftAggregate("idx", "*", args).block();

        assertThat(out).isNotNull();
        assertThat(out.getCursor()).isPresent();
        assertThat(out.getCursor().get().getCursorId()).isEqualTo(42L);
        assertThat(out.getCursor().get().getNodeId()).contains("node-1");
    }

    @Test
    void ftAggregate_withoutCursor_returnsReplyWithoutNodeId() {
        AggregationReply<String, String> replyNoCursor = new AggregationReply<>();
        when(nodeReactive.ftAggregate(anyString(), anyString(), any())).thenReturn(Mono.just(replyNoCursor));
        when(nodeReactive.clusterMyId()).thenReturn(Mono.just("node-1"));

        AggregationReply<String, String> out = reactive
                .ftAggregate("idx", "*", AggregateArgs.<String, String> builder().build()).block();

        assertThat(out).isNotNull();
        assertThat(out.getCursor()).isEmpty();
    }

    @Test
    void ftCursorread_routesToNodeId_andStampsNodeId() {
        AggregationReply.Cursor cursor = AggregationReply.Cursor.of(7L, "node-1");

        AggregationReply<String, String> reply = new AggregationReply<>();
        reply.setCursor(AggregationReply.Cursor.of(7L, null));

        when(nodeReactive.ftCursorread(anyString(), any(), anyInt())).thenReturn(Mono.just(reply));

        AggregationReply<String, String> out = reactive.ftCursorread("idx", cursor, 100).block();

        assertThat(out).isNotNull();
        assertThat(out.getCursor()).isPresent();
        assertThat(out.getCursor().get().getNodeId()).contains("node-1");
        verify(clusterConn).getConnection(eq("node-1"), eq(ConnectionIntent.WRITE));
    }

    @Test
    void ftCursordel_routesToNodeIdWithWRITE() {
        AggregationReply.Cursor cursor = AggregationReply.Cursor.of(9L, "node-1");
        when(nodeReactive.ftCursordel(anyString(), any())).thenReturn(Mono.just("OK"));

        String out = reactive.ftCursordel("idx", cursor).block();

        assertThat(out).isEqualTo("OK");
        verify(clusterConn).getConnection(eq("node-1"), eq(ConnectionIntent.WRITE));
    }

    @Test
    void routeKeyless_function_useRoutedCall_returnsResult() {
        Supplier<Mono<String>> superCall = () -> Mono.just("SUPER");
        Mono<String> m = reactive.routeKeyless(superCall,
                (RedisClusterReactiveCommands<String, String> c) -> Mono.just("ROUTED"), CommandType.FT_EXPLAIN);
        assertThat(m.block()).isEqualTo("ROUTED");
    }

    @Test
    void routeKeyless_function_exception_logsError_andFallsBack() {
        try (LoggingTestUtils.CapturingAppender logs = LoggingTestUtils
                .attachAppenderFor(RedisAdvancedClusterReactiveCommandsImpl.class, Level.ERROR)) {
            when(asyncProvider.getRandomConnectionAsync(any())).thenReturn(failedFuture(new RuntimeException("provider")));

            String out = reactive
                    .routeKeyless(() -> Mono.just("SUPER"),
                            (RedisClusterReactiveCommands<String, String> c) -> Mono.just("IGNORED"), CommandType.FT_EXPLAIN)
                    .block();

            assertThat(out).isEqualTo("SUPER");
            boolean logged = logs.messages().stream()
                    .anyMatch(m -> m.contains("Cluster routing failed for FT.EXPLAIN - falling back to superCall"));
            assertThat(logged).isTrue();
        }
    }

    @Test
    void routeKeyless_biFunction_useRoutedCall_returnsResult() {
        Supplier<Mono<String>> superCall = () -> Mono.just("SUPER");
        Mono<String> m = reactive.routeKeyless(superCall,
                (String nodeId, RedisClusterReactiveCommands<String, String> c) -> Mono.just("ROUTED"), CommandType.FT_EXPLAIN);
        assertThat(m.block()).isEqualTo("ROUTED");
    }

    @Test
    void routeKeyless_biFunction_exception_logsError_andFallsBack() {
        try (LoggingTestUtils.CapturingAppender logs = LoggingTestUtils
                .attachAppenderFor(RedisAdvancedClusterReactiveCommandsImpl.class, Level.ERROR)) {
            when(asyncProvider.getRandomConnectionAsync(any())).thenReturn(failedFuture(new RuntimeException("provider")));

            String out = reactive.routeKeyless(() -> Mono.just("SUPER"),
                    (String nodeId, RedisClusterReactiveCommands<String, String> c) -> Mono.just("IGNORED"),
                    CommandType.FT_EXPLAIN).block();

            assertThat(out).isEqualTo("SUPER");
            boolean logged = logs.messages().stream()
                    .anyMatch(m -> m.contains("Cluster routing failed for FT.EXPLAIN - falling back to superCall"));
            assertThat(logged).isTrue();
        }
    }

    @Test
    void routeKeylessMany_useRoutedFlux_returnsResults() {
        Flux<String> f = reactive.routeKeylessMany(() -> Flux.just("S"),
                (RedisClusterReactiveCommands<String, String> c) -> Flux.just("R1", "R2"), CommandType.FT_LIST);
        assertThat(f.collectList().block()).containsExactly("R1", "R2");
    }

    @Test
    void routeKeylessMany_exception_logsError_andFallsBack() {
        try (LoggingTestUtils.CapturingAppender logs = LoggingTestUtils
                .attachAppenderFor(RedisAdvancedClusterReactiveCommandsImpl.class, Level.ERROR)) {
            when(asyncProvider.getRandomConnectionAsync(any())).thenReturn(failedFuture(new RuntimeException("provider")));

            List<String> out = reactive
                    .routeKeylessMany(() -> Flux.just("S"),
                            (RedisClusterReactiveCommands<String, String> c) -> Flux.just("IGNORED"), CommandType.FT_LIST)
                    .collectList().block();

            assertThat(out).containsExactly("S");
            boolean logged = logs.messages().stream()
                    .anyMatch(m -> m.contains("Cluster routing failed for FT._LIST - falling back to superCall"));
            assertThat(logged).isTrue();
        }
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable t) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        cf.completeExceptionally(t);
        return cf;
    }

}
