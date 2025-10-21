/*
 * Copyright 2025
 */
package io.lettuce.core.cluster;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.lettuce.test.LoggingTestUtils;

import org.apache.logging.log4j.Level;

import org.mockito.Spy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisAdvancedClusterAsyncCommandsImplTest {

    @Spy
    @InjectMocks
    private RedisAdvancedClusterAsyncCommandsImpl<String, String> async;

    @Mock
    private StatefulRedisClusterConnection<String, String> clusterConn;

    @Mock
    private StatefulRedisConnection<String, String> nodeConn;

    @Mock
    private RedisAsyncCommands<String, String> nodeAsync;

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

        when(nodeConn.async()).thenReturn(nodeAsync);
        when(nodeAsync.clusterMyId()).thenReturn(new PipelinedRedisFuture<>(CompletableFuture.completedFuture("node-1")));
        when(clusterConn.getChannelWriter()).thenReturn(writer);
        when(writer.getClusterConnectionProvider()).thenReturn(provider);
        when(asyncProvider.getConnectionAsync(eq(ConnectionIntent.WRITE), eq("node-1")))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(nodeConn));
        when(asyncProvider.getRandomConnectionAsync(eq(ConnectionIntent.WRITE)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(nodeConn));
        when(asyncProvider.getRandomConnectionAsync(eq(ConnectionIntent.READ)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(nodeConn));
        when(clusterConn.getConnection(eq("node-1"), eq(ConnectionIntent.READ))).thenReturn(nodeConn);
        when(clusterConn.getPartitions()).thenReturn(partitions);
        when(clusterConn.getConnection(eq("node-1"), eq(ConnectionIntent.WRITE))).thenReturn(nodeConn);
        when(clusterConn.getOptions()).thenReturn(ClientOptions.builder().build());
        when(async.getStatefulConnection()).thenReturn(clusterConn);
    }

    @Test
    void ftAggregate_stampsNodeId_whenCursorCreated() {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .withCursor(AggregateArgs.WithCursor.of(1L)).build();

        AggregationReply<String, String> replyWithCursor = new AggregationReply<>();
        replyWithCursor.setCursor(AggregationReply.Cursor.of(42L, null));

        CompletableFuture<AggregationReply<String, String>> cf = new CompletableFuture<>();
        cf.complete(replyWithCursor);
        RedisFuture<AggregationReply<String, String>> nodeFuture = new PipelinedRedisFuture<>(cf);

        when(nodeAsync.ftAggregate(anyString(), anyString(), any())).thenReturn(nodeFuture);
        when(nodeAsync.clusterMyId()).thenReturn(new PipelinedRedisFuture<>(CompletableFuture.completedFuture("node-1")));

        AggregationReply<String, String> out = async.ftAggregate("idx", "*", args).toCompletableFuture().join();

        assertThat(out.getCursor()).isPresent();
        assertThat(out.getCursor().get().getCursorId()).isEqualTo(42L);
        assertThat(out.getCursor().get().getNodeId()).contains("node-1");
    }

    @Test
    void ftCursordel_throwsWhenMissingNodeId() {
        AggregationReply.Cursor cursor = AggregationReply.Cursor.of(5L, null);
        CompletableFuture<String> cf = async.ftCursordel("idx", cursor).toCompletableFuture();
        assertThatThrownBy(cf::join).hasCauseInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing nodeId");
    }

    @Test
    void ftCursorread_routesToNodeIdWithREAD_andStampsNodeId() {
        AggregationReply.Cursor cursor = AggregationReply.Cursor.of(7L, "node-1");

        AggregationReply<String, String> reply = new AggregationReply<>();
        reply.setCursor(AggregationReply.Cursor.of(7L, null));

        CompletableFuture<AggregationReply<String, String>> cf = CompletableFuture.completedFuture(reply);
        when(nodeAsync.ftCursorread(anyString(), any(), anyInt())).thenReturn(new PipelinedRedisFuture<>(cf));

        AggregationReply<String, String> out = async.ftCursorread("idx", cursor, 100).toCompletableFuture().join();

        assertThat(out.getCursor()).isPresent();
        assertThat(out.getCursor().get().getNodeId()).contains("node-1");
        verify(clusterConn).getConnection(eq("node-1"), eq(ConnectionIntent.READ));
    }

    @Test
    void ftCursordel_routesToNodeIdWithWRITE() {
        AggregationReply.Cursor cursor = AggregationReply.Cursor.of(9L, "node-1");
        when(nodeAsync.ftCursordel(anyString(), any()))
                .thenReturn(new PipelinedRedisFuture<>(CompletableFuture.completedFuture("OK")));

        String out = async.ftCursordel("idx", cursor).toCompletableFuture().join();

        assertThat(out).isEqualTo("OK");
        verify(clusterConn).getConnection(eq("node-1"), eq(ConnectionIntent.WRITE));
    }

    @Test
    void ftAggregate_withoutCursor_returnsReplyWithoutNodeId() {
        AggregationReply<String, String> replyNoCursor = new AggregationReply<>();
        CompletableFuture<AggregationReply<String, String>> cf = CompletableFuture.completedFuture(replyNoCursor);
        when(nodeAsync.ftAggregate(anyString(), anyString(), any())).thenReturn(new PipelinedRedisFuture<>(cf));
        when(nodeAsync.clusterMyId()).thenReturn(new PipelinedRedisFuture<>(CompletableFuture.completedFuture("node-1")));

        AggregationReply<String, String> out = async.ftAggregate("idx", "*", AggregateArgs.<String, String> builder().build())
                .toCompletableFuture().join();

        assertThat(out.getCursor()).isEmpty();
    }

    @Test
    void routeKeyless_function_useRoutedCall_returnsResult() {
        Supplier<RedisFuture<String>> superCall = () -> new PipelinedRedisFuture<>(CompletableFuture.completedFuture("SUPER"));
        RedisFuture<String> f = async.routeKeyless(superCall,
                (RedisAsyncCommands<String, String> c) -> CompletableFuture.completedFuture("ROUTED"), CommandType.FT_EXPLAIN);
        assertThat(f.toCompletableFuture().join()).isEqualTo("ROUTED");
    }

    @Test
    void routeKeyless_function_exception_logsError_andFallsBack() {
        try (LoggingTestUtils.CapturingAppender logs = LoggingTestUtils
                .attachAppenderFor(RedisAdvancedClusterAsyncCommandsImpl.class, Level.ERROR)) {
            when(asyncProvider.getRandomConnectionAsync(any())).thenReturn(failedFututre(new RuntimeException("provider")));

            String out = async.routeKeyless(() -> new PipelinedRedisFuture<>(CompletableFuture.completedFuture("SUPER")),
                    (RedisAsyncCommands<String, String> c) -> CompletableFuture.completedFuture("IGNORED"),
                    CommandType.FT_EXPLAIN).toCompletableFuture().join();

            assertThat(out).isEqualTo("SUPER");
            boolean logged = logs.messages().stream()
                    .anyMatch(m -> m.contains("Cluster routing failed for FT.EXPLAIN - falling back to superCall"));
            assertThat(logged).isTrue();
        }
    }

    @Test
    void routeKeyless_biFunction_useRoutedCall_returnsResult() {
        Supplier<RedisFuture<String>> superCall = () -> new PipelinedRedisFuture<>(CompletableFuture.completedFuture("SUPER"));
        RedisFuture<String> f = async.routeKeyless(superCall,
                (String nodeId, RedisClusterAsyncCommands<String, String> c) -> CompletableFuture.completedFuture("ROUTED"),
                CommandType.FT_EXPLAIN);
        assertThat(f.toCompletableFuture().join()).isEqualTo("ROUTED");
    }

    @Test
    void routeKeyless_biFunction_exception_logsError_andFallsBack() {
        try (LoggingTestUtils.CapturingAppender logs = LoggingTestUtils
                .attachAppenderFor(RedisAdvancedClusterAsyncCommandsImpl.class, Level.ERROR)) {
            when(asyncProvider.getRandomConnectionAsync(any())).thenReturn(failedFututre(new RuntimeException("provider")));

            String out = async.routeKeyless(
                    () -> new PipelinedRedisFuture<>(CompletableFuture.completedFuture("SUPER")), (String nodeId,
                            RedisClusterAsyncCommands<String, String> c) -> CompletableFuture.completedFuture("IGNORED"),
                    CommandType.FT_EXPLAIN).toCompletableFuture().join();

            assertThat(out).isEqualTo("SUPER");
            boolean logged = logs.messages().stream()
                    .anyMatch(m -> m.contains("Cluster routing failed for FT.EXPLAIN - falling back to superCall"));
            assertThat(logged).isTrue();
        }
    }

    private static <T> CompletableFuture<T> failedFututre(Throwable t) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        cf.completeExceptionally(t);
        return cf;
    }

}
