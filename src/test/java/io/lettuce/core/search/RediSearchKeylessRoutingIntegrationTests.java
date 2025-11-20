/*
 * Copyright 2025
 */
package io.lettuce.core.search;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.TestSupport;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.HybridArgs;
import io.lettuce.core.search.arguments.HybridSearchArgs;
import io.lettuce.core.search.arguments.HybridVectorArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.core.search.arguments.VectorFieldArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.core.metrics.CommandLatencyId;
import io.lettuce.core.metrics.CommandMetrics;

import io.lettuce.test.condition.RedisConditions;
import java.nio.ByteBuffer;
import java.util.*;
import javax.inject.Inject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Integration tests verifying that keyless RediSearch commands are routed randomly and respect ReadFrom.
 *
 * We use FT.AGGREGATE WITHCURSOR because AggregationReply carries the executing nodeId so we can observe actual routing
 * decisions end-to-end.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
public class RediSearchKeylessRoutingIntegrationTests extends TestSupport {

    private static final String INDEX = "books-keyless-routing-idx";

    private static final String PREFIX = "book:keyless:routing:";

    private static final String HYBRID_INDEX = "hybrid-keyless-routing-idx";

    private static final String HYBRID_PREFIX = "product:hybrid:routing:";

    private final RedisClusterClient clusterClient;

    private StatefulRedisClusterConnection<String, String> connection;

    private StatefulRedisClusterConnection<byte[], byte[]> binaryConnection;

    private RedisAdvancedClusterAsyncCommands<String, String> async;

    @Inject
    RediSearchKeylessRoutingIntegrationTests(RedisClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    @BeforeEach
    void open() {
        connection = clusterClient.connect();
        binaryConnection = clusterClient.connect(ByteArrayCodec.INSTANCE);
        async = connection.async();
    }

    @AfterEach
    void close() {
        if (connection != null) {
            connection.close();
        }
        if (binaryConnection != null) {
            binaryConnection.close();
        }
    }

    @BeforeEach
    void setUp() {
        // Require Redis 8+ (matches other RediSearch integration tests)
        assumeTrue(RedisConditions.of(connection.sync()).hasVersionGreaterOrEqualsTo("8.0"));
        connection.sync().flushall();

        // Schema for text search tests
        FieldArgs<String> title = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> author = TagFieldArgs.<String> builder().name("author").build();
        FieldArgs<String> year = NumericFieldArgs.<String> builder().name("year").sortable().build();
        FieldArgs<String> rating = NumericFieldArgs.<String> builder().name("rating").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();
        assertThat(connection.sync().ftCreate(INDEX, createArgs, Arrays.asList(title, author, year, rating))).isEqualTo("OK");

        // Data spread across slots
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
            connection.sync().hmset(PREFIX + i, doc);
        }

        // Schema for hybrid search tests
        FieldArgs<String> category = TagFieldArgs.<String> builder().name("category").build();
        FieldArgs<String> price = NumericFieldArgs.<String> builder().name("price").sortable().build();
        FieldArgs<String> embedding = VectorFieldArgs.<String> builder().name("embedding").hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        CreateArgs<String, String> hybridCreateArgs = CreateArgs.<String, String> builder().withPrefix(HYBRID_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();
        assertThat(connection.sync().ftCreate(HYBRID_INDEX, hybridCreateArgs, Arrays.asList(category, price, embedding)))
                .isEqualTo("OK");

        // Add hybrid search data
        String[] categories = { "electronics", "clothing", "electronics", "clothing" };
        String[] prices = { "29.99", "49.99", "39.99", "59.99" };
        float[][] vectors = { { 0.1f, 0.2f, 0.3f, 0.4f }, { 0.5f, 0.6f, 0.7f, 0.8f }, { 0.2f, 0.3f, 0.4f, 0.5f },
                { 0.6f, 0.7f, 0.8f, 0.9f } };

        for (int i = 0; i < categories.length; i++) {
            Map<String, String> doc = new HashMap<>();
            doc.put("category", categories[i]);
            doc.put("price", prices[i]);
            connection.sync().hmset(HYBRID_PREFIX + i, doc);
            binaryConnection.sync().hset((HYBRID_PREFIX + i).getBytes(), "embedding".getBytes(),
                    floatArrayToByteArray(vectors[i]));
        }
    }

    @AfterEach
    void tearDown() {
        try {
            connection.sync().ftDropindex(INDEX);
        } catch (Exception ignore) {
        }
        try {
            connection.sync().ftDropindex(HYBRID_INDEX);
        } catch (Exception ignore) {
        }
        connection.sync().flushall();
    }

    private AggregateArgs<String, String> aggWithCursor(long count) {
        return AggregateArgs.<String, String> builder()
                .groupBy(AggregateArgs.GroupBy.<String, String> of("author")
                        .reduce(AggregateArgs.Reducer.<String, String> avg("@rating").as("avg_rating")))
                .withCursor(AggregateArgs.WithCursor.of(count)).build();
    }

    @Test
    void keylessAggregate_routesRandomly_acrossUpstreams_whenReadFromUpstream() {
        long upstreams = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.UPSTREAM)).count();
        assumeTrue(upstreams >= 2, "requires >= 2 upstream nodes");

        connection.setReadFrom(ReadFrom.UPSTREAM);

        Set<String> nodeIds = new HashSet<>();
        int observedCursors = 0;
        AggregateArgs<String, String> args = aggWithCursor(1L);
        for (int i = 0; i < 40 && nodeIds.size() < upstreams; i++) {
            AggregationReply<String, String> first = async.ftAggregate(INDEX, "*", args).toCompletableFuture().join();
            assertThat(first).isNotNull();
            if (first.getCursor().isPresent() && first.getCursor().get().getCursorId() > 0) {
                observedCursors++;
                first.getCursor().get().getNodeId().ifPresent(nodeId -> {
                    // Node must be an upstream per ReadFrom
                    boolean isUpstream = connection.getPartitions().getPartitionByNodeId(nodeId)
                            .is(RedisClusterNode.NodeFlag.UPSTREAM);
                    assertThat(isUpstream).isTrue();
                    nodeIds.add(nodeId);
                    async.ftCursordel(INDEX, first.getCursor().get()).toCompletableFuture().join();
                });
            }
        }
        assumeTrue(observedCursors > 0, "no cursors were created; cannot validate rotation");
        assertThat(nodeIds.size()).isEqualTo(upstreams);
    }

    @Test
    void keylessAggregate_routesRandomly_acrossReplicas_whenReadFromAnyReplica() {
        long replicas = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.REPLICA)).count();
        assumeTrue(replicas >= 2, "requires >= 2 replica node");

        connection.setReadFrom(ReadFrom.ANY_REPLICA);

        Set<String> nodeIds = new HashSet<>();
        int observedCursors = 0;
        AggregateArgs<String, String> args = aggWithCursor(1L);
        for (int i = 0; i < 60 && nodeIds.size() < replicas; i++) {
            AggregationReply<String, String> first = async.ftAggregate(INDEX, "*", args).toCompletableFuture().join();
            assertThat(first).isNotNull();
            if (first.getCursor().isPresent() && first.getCursor().get().getCursorId() > 0) {
                observedCursors++;
                first.getCursor().get().getNodeId().ifPresent(nodeId -> {
                    // Node must be a replica per ReadFrom
                    boolean isReplica = connection.getPartitions().getPartitionByNodeId(nodeId)
                            .is(RedisClusterNode.NodeFlag.REPLICA);
                    assertThat(isReplica).isTrue();
                    nodeIds.add(nodeId);
                    async.ftCursordel(INDEX, first.getCursor().get()).toCompletableFuture().join();
                });
            }
        }
        assumeTrue(observedCursors > 0, "no cursors were created; cannot validate rotation");
        // We expect to see at least one replica (and up to all replicas depending on randomness and count)
        assertThat(nodeIds).isNotEmpty();
        assertThat(nodeIds.size()).isLessThanOrEqualTo((int) replicas);
    }

    @Test
    void ftCreate_routesToUpstream_evenWhenReadFromReplica() {
        Partitions partitions = connection.getPartitions();
        List<RedisClusterNode> upstreams = partitions.stream().filter(n -> n.is(RedisClusterNode.NodeFlag.UPSTREAM))
                .collect(Collectors.toList());
        List<RedisClusterNode> replicas = partitions.stream().filter(n -> n.is(RedisClusterNode.NodeFlag.REPLICA))
                .collect(Collectors.toList());
        assumeTrue(!upstreams.isEmpty(), "requires >= 1 upstream node");
        assumeTrue(!replicas.isEmpty(), "requires >= 1 replica node");

        connection.setReadFrom(ReadFrom.REPLICA);
        clearLatencyMetrics();

        String tmpIndex = INDEX + ":create:" + UUID.randomUUID();
        FieldArgs<String> title = TextFieldArgs.<String> builder().name("title").build();
        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();
        assertThat(connection.sync().ftCreate(tmpIndex, createArgs, Arrays.asList(title))).isEqualTo("OK");

        Set<String> nodes = observedNodeIdsFor(CommandType.FT_CREATE);
        assertThat(nodes).isNotEmpty();
        nodes.forEach(
                id -> assertThat(connection.getPartitions().getPartitionByNodeId(id).is(RedisClusterNode.NodeFlag.UPSTREAM))
                        .isTrue());
        assertThat(nodes.size()).isEqualTo(1);

        // cleanup
        connection.sync().ftDropindex(tmpIndex);
    }

    @Test
    void list_routesRandomly_acrossUpstreams_whenReadFromUpstream() {
        long upstreams = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.UPSTREAM)).count();
        assumeTrue(upstreams >= 2, "requires >= 2 upstream nodes");

        connection.setReadFrom(ReadFrom.UPSTREAM);
        clearLatencyMetrics();

        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 40 && nodes.size() < (int) upstreams; i++) {
            connection.sync().ftList();
            nodes.addAll(observedNodeIdsFor(CommandType.FT_LIST));
        }

        assertThat(nodes).isNotEmpty();
        nodes.forEach(
                id -> assertThat(connection.getPartitions().getPartitionByNodeId(id).is(RedisClusterNode.NodeFlag.UPSTREAM))
                        .isTrue());
        assertThat(nodes.size()).isEqualTo((int) upstreams);
    }

    @Test
    void list_routesRandomly_acrossReplicas_whenReadFromAnyReplica() {
        long replicas = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.REPLICA)).count();
        assumeTrue(replicas >= 2, "requires >= 2 replica node");

        connection.setReadFrom(ReadFrom.ANY_REPLICA);
        clearLatencyMetrics();

        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 60 && nodes.size() < replicas; i++) {
            connection.sync().ftList();
            nodes.addAll(observedNodeIdsFor(CommandType.FT_LIST));
        }

        assertThat(nodes).isNotEmpty();
        nodes.forEach(
                id -> assertThat(connection.getPartitions().getPartitionByNodeId(id).is(RedisClusterNode.NodeFlag.REPLICA))
                        .isTrue());
        assertThat(nodes.size()).isLessThanOrEqualTo((int) replicas);
    }

    @Test
    void search_routesRandomly_acrossUpstreams_whenReadFromUpstream() {
        long upstreams = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.UPSTREAM)).count();
        assumeTrue(upstreams >= 2, "requires >= 2 upstream nodes");

        connection.setReadFrom(ReadFrom.UPSTREAM);
        clearLatencyMetrics();

        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 40 && nodes.size() < (int) upstreams; i++) {
            connection.sync().ftSearch(INDEX, "*");
            nodes.addAll(observedNodeIdsFor(CommandType.FT_SEARCH));
        }
        assertThat(nodes).isNotEmpty();
        nodes.forEach(
                id -> assertThat(connection.getPartitions().getPartitionByNodeId(id).is(RedisClusterNode.NodeFlag.UPSTREAM))
                        .isTrue());
        assertThat(nodes.size()).isEqualTo((int) upstreams);
    }

    @Test
    void search_routesRandomly_acrossReplicas_whenReadFromAnyReplica() {
        long replicas = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.REPLICA)).count();
        assumeTrue(replicas >= 1, "requires >= 1 replica node");

        connection.setReadFrom(ReadFrom.ANY_REPLICA);

        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 60 && nodes.size() < replicas; i++) {
            connection.sync().ftSearch(INDEX, "*");
            nodes.addAll(observedNodeIdsFor(CommandType.FT_SEARCH));
        }
        assertThat(nodes).isNotEmpty();
        nodes.forEach(
                id -> assertThat(connection.getPartitions().getPartitionByNodeId(id).is(RedisClusterNode.NodeFlag.REPLICA))
                        .isTrue());
        assertThat(nodes.size()).isLessThanOrEqualTo((int) replicas);
    }

    private Set<String> observedNodeIdsFor(ProtocolKeyword type) {
        Set<String> nodeIds = new HashSet<>();
        if (clusterClient.getResources().commandLatencyRecorder() instanceof CommandLatencyCollector) {
            CommandLatencyCollector c = (CommandLatencyCollector) clusterClient.getResources().commandLatencyRecorder();
            Map<CommandLatencyId, CommandMetrics> metrics = c.retrieveMetrics();
            metrics.forEach((id, m) -> {
                if (id.commandType().toString().equals(type.toString())) {
                    SocketAddress remote = id.remoteAddress();
                    if (remote instanceof InetSocketAddress) {
                        InetSocketAddress isa = (InetSocketAddress) remote;
                        String host = isa.getHostString();
                        int port = isa.getPort();
                        connection.getPartitions().forEach(n -> {
                            if (n.getUri().getPort() == port
                                    && (n.getUri().getHost().equals(host) || "localhost".equals(host))) {
                                nodeIds.add(n.getNodeId());
                            }
                        });
                    }
                }
            });
        }
        return nodeIds;
    }

    private void clearLatencyMetrics() {
        if (clusterClient.getResources().commandLatencyRecorder() instanceof CommandLatencyCollector) {
            CommandLatencyCollector c = (CommandLatencyCollector) clusterClient.getResources().commandLatencyRecorder();
            // Default options reset on retrieve; calling once clears any previous run's samples
            c.retrieveMetrics();
        }
    }

    private byte[] floatArrayToByteArray(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    private HybridArgs<String, String> hybridArgs() {
        float[] queryVector = { 0.15f, 0.25f, 0.35f, 0.45f };
        return HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("@category:{electronics}").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding")
                        .vector(floatArrayToByteArray(queryVector)).method(VectorSearchMethod.knn(5)).build())
                .build();
    }

    @Test
    void hybrid_routesRandomly_acrossUpstreams_whenReadFromUpstream() {
        long upstreams = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.UPSTREAM)).count();
        assumeTrue(upstreams >= 2, "requires >= 2 upstream nodes");

        connection.setReadFrom(ReadFrom.UPSTREAM);
        clearLatencyMetrics();

        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 40 && nodes.size() < (int) upstreams; i++) {
            connection.sync().ftHybrid(HYBRID_INDEX, hybridArgs());
            nodes.addAll(observedNodeIdsFor(CommandType.FT_HYBRID));
        }

        assertThat(nodes).isNotEmpty();
        nodes.forEach(
                id -> assertThat(connection.getPartitions().getPartitionByNodeId(id).is(RedisClusterNode.NodeFlag.UPSTREAM))
                        .isTrue());
        assertThat(nodes.size()).isEqualTo((int) upstreams);
    }

    @Test
    void hybrid_routesRandomly_acrossReplicas_whenReadFromAnyReplica() {
        long replicas = connection.getPartitions().stream().filter(n -> n.is(RedisClusterNode.NodeFlag.REPLICA)).count();
        assumeTrue(replicas >= 1, "requires >= 1 replica node");

        connection.setReadFrom(ReadFrom.ANY_REPLICA);
        clearLatencyMetrics();

        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 60 && nodes.size() < replicas; i++) {
            connection.sync().ftHybrid(HYBRID_INDEX, hybridArgs());
            nodes.addAll(observedNodeIdsFor(CommandType.FT_HYBRID));
        }

        assertThat(nodes).isNotEmpty();
        nodes.forEach(
                id -> assertThat(connection.getPartitions().getPartitionByNodeId(id).is(RedisClusterNode.NodeFlag.REPLICA))
                        .isTrue());
        assertThat(nodes.size()).isLessThanOrEqualTo((int) replicas);
    }

}
