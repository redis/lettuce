/*
 * Copyright 2025
 */
package io.lettuce.core.search;

import io.lettuce.core.TestSupport;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import io.lettuce.test.LettuceExtension;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import org.junit.jupiter.api.*;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.HashSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for RediSearch aggregation cursor functionality in cluster mode.
 *
 * Covers sync, async, and reactive APIs. Demonstrates correct nodeId stamping, cursor stickiness, error handling for missing
 * nodeId, and the full cursor lifecycle (create → read → delete).
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
public class RediSearchClusterCursorIntegrationTests extends TestSupport {

    private static final String INDEX = "books-cursor-cluster-idx";

    private static final String PREFIX = "book:cursor:cluster:";

    private final RedisClusterClient clusterClient;

    private StatefulRedisClusterConnection<String, String> connection;

    private RedisAdvancedClusterCommands<String, String> sync;

    private RedisAdvancedClusterAsyncCommands<String, String> async;

    private RedisAdvancedClusterReactiveCommands<String, String> reactive;

    @Inject
    RediSearchClusterCursorIntegrationTests(RedisClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    @BeforeEach
    void open() {
        connection = clusterClient.connect();
        sync = connection.sync();
        async = connection.async();
        reactive = connection.reactive();
    }

    @AfterEach
    void close() {
        if (connection != null)
            connection.close();
    }

    @BeforeEach
    void setUp() {
        // Require Redis 8+ to match CI expectations for RediSearch behavior
        assumeTrue(RedisConditions.of(sync).hasVersionGreaterOrEqualsTo("8.0"));
        sync.flushall();

        // Create schema
        FieldArgs<String> title = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> author = TagFieldArgs.<String> builder().name("author").build();
        FieldArgs<String> year = NumericFieldArgs.<String> builder().name("year").sortable().build();
        FieldArgs<String> rating = NumericFieldArgs.<String> builder().name("rating").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(sync.ftCreate(INDEX, createArgs, Arrays.asList(title, author, year, rating))).isEqualTo("OK");

        // Insert data across slots
        String[][] books = { { "Dune", "frank_herbert", "1965", "4.2" }, { "Lord of the Rings", "tolkien", "1954", "4.5" },
                { "Sherlock Holmes", "doyle", "1887", "4.1" }, { "Pride and Prejudice", "austen", "1813", "4.0" },
                { "Gone Girl", "flynn", "2012", "3.9" }, { "Steve Jobs", "isaacson", "2011", "4.3" },
                { "Sapiens", "harari", "2011", "4.4" }, { "Cosmos", "sagan", "1980", "4.6" } };
        for (int i = 0; i < books.length; i++) {
            Map<String, String> doc = new HashMap<>();
            doc.put("title", books[i][0]);
            doc.put("author", books[i][1]);
            doc.put("year", books[i][2]);
            doc.put("rating", books[i][3]);
            sync.hmset(PREFIX + i, doc);
        }
    }

    @AfterEach
    void tearDown() {
        try {
            sync.ftDropindex(INDEX);
        } catch (Exception ignore) {
        }
        sync.flushall();
    }

    @Test
    void sync_cursorLifecycle_and_stickiness() {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .groupBy(AggregateArgs.GroupBy.<String, String> of("author")
                        .reduce(AggregateArgs.Reducer.<String, String> avg("@rating").as("avg_rating")))
                .withCursor(AggregateArgs.WithCursor.of(2L)).build();

        AggregationReply<String, String> first = sync.ftAggregate(INDEX, "*", args);
        assertThat(first.getCursor().get().getCursorId()).isGreaterThan(0);
        assertThat(first.getCursor().get().getNodeId()).isPresent();
        assertThat(first.getReplies()).isNotEmpty();
        String nodeId = first.getCursor().get().getNodeId().get();

        // Stickiness: reads route to the same node and pages advance
        AggregationReply<String, String> page2 = sync.ftCursorread(INDEX, first.getCursor().get());
        assertThat(page2).isNotNull();
        assertThat(page2.getCursor().get().getNodeId()).isPresent();
        assertThat(page2.getCursor().get().getNodeId().get()).isEqualTo(nodeId);
        assertThat(page2.getReplies()).isNotEmpty();
        assertThat(page2.getReplies()).isNotEqualTo(first.getReplies());

        AggregationReply<String, String> page3 = sync.ftCursorread(INDEX, page2.getCursor().get());
        assertThat(page3.getCursor().get().getNodeId()).isPresent();
        assertThat(page3.getCursor().get().getNodeId().get()).isEqualTo(nodeId);
        assertThat(page3.getReplies()).isNotEmpty();
        assertThat(page3.getReplies()).isNotEqualTo(page2.getReplies());

        // Delete cursor
        String del = sync.ftCursordel(INDEX, page3.getCursor().get());
        assertThat(del).isEqualTo("OK");
    }

    @Test
    void async_cursorLifecycle_and_stickiness() {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .groupBy(AggregateArgs.GroupBy.<String, String> of("author")
                        .reduce(AggregateArgs.Reducer.<String, String> avg("@rating").as("avg_rating")))
                .withCursor(AggregateArgs.WithCursor.of(2L)).build();

        AggregationReply<String, String> first = async.ftAggregate(INDEX, "*", args).toCompletableFuture().join();
        assertThat(first.getCursor().get().getCursorId()).isGreaterThan(0);
        assertThat(first.getCursor().get().getNodeId()).isPresent();
        assertThat(first.getReplies()).isNotEmpty();
        String nodeId = first.getCursor().get().getNodeId().get();

        AggregationReply<String, String> page2 = async.ftCursorread(INDEX, first.getCursor().get()).toCompletableFuture()
                .join();
        assertThat(page2.getCursor().get().getNodeId()).isPresent();
        assertThat(page2.getCursor().get().getNodeId().get()).isEqualTo(nodeId);
        assertThat(page2.getReplies()).isNotEmpty();
        assertThat(page2.getReplies()).isNotEqualTo(first.getReplies());

        AggregationReply<String, String> page3 = async.ftCursorread(INDEX, page2.getCursor().get()).toCompletableFuture()
                .join();
        assertThat(page3.getCursor().get().getNodeId()).isPresent();
        assertThat(page3.getCursor().get().getNodeId().get()).isEqualTo(nodeId);
        assertThat(page3.getReplies()).isNotEmpty();
        assertThat(page3.getReplies()).isNotEqualTo(page2.getReplies());

        String del = async.ftCursordel(INDEX, page3.getCursor().get()).toCompletableFuture().join();
        assertThat(del).isEqualTo("OK");
    }

    @Test
    void reactive_cursorLifecycle_and_stickiness() {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .groupBy(AggregateArgs.GroupBy.<String, String> of("author")
                        .reduce(AggregateArgs.Reducer.<String, String> avg("@rating").as("avg_rating")))
                .withCursor(AggregateArgs.WithCursor.of(2L)).build();

        AggregationReply<String, String> first = reactive.ftAggregate(INDEX, "*", args).block();
        assertThat(first).isNotNull();
        assertThat(first.getCursor().get().getCursorId()).isGreaterThan(0);
        assertThat(first.getCursor().get().getNodeId()).isPresent();
        assertThat(first.getReplies()).isNotEmpty();
        String nodeId = first.getCursor().get().getNodeId().get();

        AggregationReply<String, String> page2 = reactive.ftCursorread(INDEX, first.getCursor().get()).block();
        assertThat(page2).isNotNull();
        assertThat(page2.getCursor().get().getNodeId()).isPresent();
        assertThat(page2.getCursor().get().getNodeId().get()).isEqualTo(nodeId);
        assertThat(page2.getReplies()).isNotEmpty();
        assertThat(page2.getReplies()).isNotEqualTo(first.getReplies());

        AggregationReply<String, String> page3 = reactive.ftCursorread(INDEX, page2.getCursor().get()).block();
        assertThat(page3.getCursor().get().getNodeId()).isPresent();
        assertThat(page3.getCursor().get().getNodeId().get()).isEqualTo(nodeId);
        assertThat(page3.getReplies()).isNotEmpty();
        assertThat(page3.getReplies()).isNotEqualTo(page2.getReplies());

        String del = reactive.ftCursordel(INDEX, page3.getCursor().get()).block();
        assertThat(del).isEqualTo("OK");
    }

    @Test
    void sync_errorHandling_missingNodeId_throws() {
        AggregationReply.Cursor c = AggregationReply.Cursor.of(5L, null);
        assertThatThrownBy(() -> sync.ftCursorread(INDEX, c)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing nodeId");
        assertThatThrownBy(() -> sync.ftCursordel(INDEX, c)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing nodeId");
    }

    @Test
    void async_errorHandling_missingNodeId_throws() {
        AggregationReply.Cursor c2 = AggregationReply.Cursor.of(5L, null);
        assertThatThrownBy(() -> async.ftCursorread(INDEX, c2).toCompletableFuture().join())
                .hasCauseInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing nodeId");
        assertThatThrownBy(() -> async.ftCursordel(INDEX, c2).toCompletableFuture().join())
                .hasCauseInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing nodeId");
    }

    @Test
    void reactive_errorHandling_missingNodeId_emitsError() {
        AggregationReply.Cursor c3 = AggregationReply.Cursor.of(5L, null);
        StepVerifier.create(reactive.ftCursorread(INDEX, c3))
                .expectErrorSatisfies(
                        t -> assertThat(t).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing nodeId"))
                .verify();
        StepVerifier.create(reactive.ftCursordel(INDEX, c3))
                .expectErrorSatisfies(
                        t -> assertThat(t).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing nodeId"))
                .verify();
    }

    @Test
    void async_firstIteration_rotatesAcrossUpstreamNodes() {
        // Ensure we have at least two upstream nodes in the cluster; otherwise skip to avoid flakiness
        long upstreams = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.UPSTREAM)).count();
        assumeTrue(upstreams >= 2, "requires >= 2 upstream nodes");

        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .groupBy(AggregateArgs.GroupBy.<String, String> of("author")
                        .reduce(AggregateArgs.Reducer.<String, String> avg("@rating").as("avg_rating")))
                .withCursor(AggregateArgs.WithCursor.of(1L)).build();

        Set<String> nodeIds = new HashSet<>();
        int observedCursors = 0;
        for (int i = 0; i < 30 && nodeIds.size() < upstreams; i++) {
            AggregationReply<String, String> first = async.ftAggregate(INDEX, "*", args).toCompletableFuture().join();
            assertThat(first).isNotNull();
            if (first.getCursor().isPresent() && first.getCursor().get().getCursorId() > 0) {
                observedCursors++;
                first.getCursor().get().getNodeId().ifPresent(nodeId -> {
                    nodeIds.add(nodeId);
                    async.ftCursordel(INDEX, first.getCursor().get()).toCompletableFuture().join();
                });
            }
        }

        assumeTrue(observedCursors > 0, "no cursors were created; cannot validate rotation");
        assertThat(nodeIds.size()).isEqualTo(upstreams);
    }

}
