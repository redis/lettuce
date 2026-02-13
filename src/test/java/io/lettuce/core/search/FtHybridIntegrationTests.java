/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.search.arguments.Combiners;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.GroupBy;
import io.lettuce.core.search.arguments.HybridArgs;
import io.lettuce.core.search.arguments.HybridSearchArgs;
import io.lettuce.core.search.arguments.HybridVectorArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.PostProcessingArgs;
import io.lettuce.core.search.arguments.Reducers;
import io.lettuce.core.search.arguments.Scorers;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.core.search.arguments.VectorFieldArgs;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
@EnabledOnCommand("FT.HYBRID")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FtHybridIntegrationTests {

    private static final String INDEX = "hybrid-test-idx";

    private static final String PREFIX = "htest:";

    private static RedisClient client;

    private static RedisCommands<String, String> redis;

    private static RedisCommands<byte[], byte[]> redisBinary;

    // Query vectors for different test scenarios
    private static byte[] queryVectorClose; // Close to products 1,4,10 (Apple)

    private static byte[] queryVectorMid; // Mid-range distance

    @BeforeAll
    static void setupOnce() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(ClientOptions.builder().build());
        redis = client.connect().sync();
        redisBinary = client.connect(ByteArrayCodec.INSTANCE).sync();

        // Clean slate
        redis.flushall();

        // Create index with all needed fields
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> categoryField = TagFieldArgs.<String> builder().name("category").build();
        FieldArgs<String> brandField = TagFieldArgs.<String> builder().name("brand").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("price").sortable().build();
        FieldArgs<String> ratingField = NumericFieldArgs.<String> builder().name("rating").sortable().build();
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("embedding").hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(8).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(INDEX, createArgs,
                Arrays.asList(titleField, categoryField, brandField, priceField, ratingField, vectorField))).isEqualTo("OK");

        // Seed test data - products with varied prices for reducer tests
        seedProducts();

        // Prepare query vectors
        queryVectorClose = floatArrayToByteArray(new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f });
        queryVectorMid = floatArrayToByteArray(new float[] { 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
    }

    private static void seedProducts() {
        // Electronics - Apple (high price)
        createProduct("1", "Apple iPhone 15 Pro smartphone camera", "electronics", "apple", "999", "4.8",
                new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f });
        createProduct("2", "Apple MacBook Pro laptop", "electronics", "apple", "2499", "4.9",
                new float[] { 0.12f, 0.22f, 0.32f, 0.42f, 0.52f, 0.62f, 0.72f, 0.82f });

        // Electronics - Samsung (mid price)
        createProduct("3", "Samsung Galaxy S24 smartphone camera", "electronics", "samsung", "799", "4.6",
                new float[] { 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f });
        createProduct("4", "Samsung TV 65 inch display", "electronics", "samsung", "1299", "4.5",
                new float[] { 0.18f, 0.28f, 0.38f, 0.48f, 0.58f, 0.68f, 0.78f, 0.88f });

        // Electronics - Google (lower price)
        createProduct("5", "Google Pixel 8 Pro camera smartphone", "electronics", "google", "699", "4.5",
                new float[] { 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f });

        // Apparel
        createProduct("6", "Nike Air Max running shoes", "apparel", "nike", "150", "4.3",
                new float[] { 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f });
        createProduct("7", "Adidas Ultraboost shoes", "apparel", "adidas", "180", "4.4",
                new float[] { 0.55f, 0.55f, 0.55f, 0.55f, 0.55f, 0.55f, 0.55f, 0.55f });

        // Sports
        createProduct("8", "Wilson tennis racket pro", "sports", "wilson", "250", "4.2",
                new float[] { 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f });
    }

    @AfterAll
    static void teardownOnce() {
        if (redis != null) {
            try {
                redis.ftDropindex(INDEX);
            } catch (Exception ignored) {
            }
        }
        if (client != null) {
            client.shutdown();
        }
    }

    private static void createProduct(String id, String title, String category, String brand, String price, String rating,
            float[] embedding) {
        redis.hset(PREFIX + id, "title", title);
        redis.hset(PREFIX + id, "category", category);
        redis.hset(PREFIX + id, "brand", brand);
        redis.hset(PREFIX + id, "price", price);
        redis.hset(PREFIX + id, "rating", rating);
        redisBinary.hset((PREFIX + id).getBytes(), "embedding".getBytes(), floatArrayToByteArray(embedding));
    }

    private static byte[] floatArrayToByteArray(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    // ==================== TEST 1: RRF Combiner ====================

    @Test
    @Order(1)
    void hybridWithRrfCombiner() {
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("smartphone camera").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(10)).build())
                .combine(Combiners.<String> rrf().window(20).constant(60))
                .postProcessing(PostProcessingArgs.<String, String> builder().load("@title", "@brand").build())
                .param("vec", queryVectorClose).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getTotalResults()).isGreaterThan(0);

        // Verify we get electronics products with "smartphone camera" in title
        boolean hasSmartphone = reply.getResults().stream()
                .anyMatch(r -> r.get("title") != null && r.get("title").toLowerCase().contains("smartphone"));
        assertThat(hasSmartphone).isTrue();
    }

    // ==================== TEST 2: RANGE Vector Search ====================

    @Test
    @Order(2)
    void hybridWithRangeVectorSearch() {
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("*").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Range.of(0.5)).build())
                .combine(Combiners.<String> rrf().window(20))
                .postProcessing(PostProcessingArgs.<String, String> builder().load("@title").build())
                .param("vec", queryVectorClose).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        // RANGE returns all vectors within radius 0.5 - should include close vectors
        assertThat(reply.getResults()).isNotEmpty();
    }

    // ==================== TEST 3: Explicit Scorer (BM25) ====================

    @Test
    @Order(3)
    void hybridWithExplicitScorer() {
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("smartphone camera").scorer(Scorers.bm25())
                        .scoreAlias("bm25_score").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(10)).build())
                .combine(Combiners.<String> linear().alpha(0.5).beta(0.5))
                .postProcessing(PostProcessingArgs.<String, String> builder().load("@title", "@brand").build())
                .param("vec", queryVectorClose).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();
        // Products with "smartphone camera" should rank higher with BM25
        String firstTitle = reply.getResults().get(0).get("title");
        assertThat(firstTitle).isNotNull();
        assertThat(firstTitle.toLowerCase()).containsAnyOf("smartphone", "camera");
    }

    // ==================== TEST 4: LOAD * ====================

    @Test
    @Order(4)
    void hybridWithLoadAll() {
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("@category:{electronics}").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(5)).build())
                .combine(Combiners.<String> rrf().window(20))
                .postProcessing(PostProcessingArgs.<String, String> builder().loadAll().build()).param("vec", queryVectorClose)
                .build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();

        // With LOAD *, all fields should be present
        Map<String, String> firstResult = reply.getResults().get(0);
        assertThat(firstResult).containsKeys("title", "category", "brand", "price", "rating");
    }

    // ==================== TEST 5: TIMEOUT ====================

    @Test
    @Order(5)
    void hybridWithTimeout() {
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("smartphone").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(10)).build())
                .combine(Combiners.<String> rrf().window(20)).timeout(Duration.ofSeconds(30))
                .postProcessing(PostProcessingArgs.<String, String> builder().load("@title").build())
                .param("vec", queryVectorClose).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();
        // Query should complete within timeout
        assertThat(reply.getExecutionTime()).isGreaterThan(0L);
    }

    // ==================== TEST 6: Score Aliases (SEARCH and VSIM) ====================

    @Test
    @Order(6)
    void hybridWithScoreAliases() {
        // Test YIELD_SCORE_AS for SEARCH and VSIM
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("smartphone").scoreAlias("text_score").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(10)).scoreAlias("vector_score").build())
                .combine(Combiners.<String> rrf().window(20).constant(60))
                .postProcessing(PostProcessingArgs.<String, String> builder().load("@title", "@brand").build())
                .param("vec", queryVectorClose).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();
        // Results should be returned with the aliased scores available
        assertThat(reply.getTotalResults()).isGreaterThan(0);
    }

    // ==================== TEST 7: Reducers AVG, MIN, MAX ====================

    @Test
    @Order(7)
    void hybridWithReducerAvgMinMax() {
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("@category:{electronics}").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(10)).build())
                .combine(Combiners.<String> rrf().window(20))
                .postProcessing(PostProcessingArgs.<String, String> builder()
                        .groupBy(GroupBy.<String, String> of("@brand").reduce(Reducers.<String> avg("@price").as("avg_price"))
                                .reduce(Reducers.min("@price").as("min_price")).reduce(Reducers.max("@price").as("max_price")))
                        .build())
                .param("vec", queryVectorClose).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();

        // Find Apple result and verify aggregations
        // Apple has products at $999 and $2499, so avg=1749, min=999, max=2499
        for (Map<String, String> result : reply.getResults()) {
            if ("apple".equals(result.get("brand"))) {
                assertThat(result.get("avg_price")).isEqualTo("1749");
                assertThat(result.get("min_price")).isEqualTo("999");
                assertThat(result.get("max_price")).isEqualTo("2499");
            } else if ("samsung".equals(result.get("brand"))) {
                // Samsung: $799 and $1299, avg=1049, min=799, max=1299
                assertThat(result.get("avg_price")).isEqualTo("1049");
                assertThat(result.get("min_price")).isEqualTo("799");
                assertThat(result.get("max_price")).isEqualTo("1299");
            }
        }
    }

    // ==================== TEST 8: Reducer QUANTILE ====================

    @Test
    @Order(8)
    void hybridWithReducerQuantile() {
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("@category:{electronics}").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(10)).build())
                .combine(Combiners.<String> rrf().window(20))
                .postProcessing(PostProcessingArgs.<String, String> builder()
                        .groupBy(GroupBy.<String, String> of("@category")
                                .reduce(Reducers.quantile("@price", 0.5).as("median_price"))
                                .reduce(Reducers.<String> count().as("count")))
                        .build())
                .param("vec", queryVectorClose).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();

        // Find electronics category and verify quantile was computed
        boolean foundElectronics = false;
        for (Map<String, String> result : reply.getResults()) {
            if ("electronics".equals(result.get("category"))) {
                foundElectronics = true;
                assertThat(result.get("median_price")).isNotNull();
                assertThat(result.get("count")).isEqualTo("5"); // 5 electronics products
            }
        }
        assertThat(foundElectronics).isTrue();
    }

    // ==================== TEST 9: Mid-distance Vector with Text Dominance ====================

    @Test
    @Order(9)
    void hybridWithMidDistanceVectorTextDominates() {
        // Using queryVectorMid (equidistant from all products), text search should dominate ranking
        // With LINEAR combiner alpha=0.8 (text weight) and beta=0.2 (vector weight),
        // the text match "Pro" should determine the ranking
        HybridArgs<String, String> args = HybridArgs.<String, String> builder()
                .search(HybridSearchArgs.<String, String> builder().query("Pro").build())
                .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector("$vec")
                        .method(HybridVectorArgs.Knn.of(10)).build())
                .combine(Combiners.<String> linear().alpha(0.8).beta(0.2))
                .postProcessing(PostProcessingArgs.<String, String> builder().load("@title").build())
                .param("vec", queryVectorMid).build();

        HybridReply<String, String> reply = redis.ftHybrid(INDEX, args);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();

        // With mid-distance vector and high text weight, products with "Pro" in title should rank high
        // Our dataset has: "iPhone 15 Pro", "MacBook Pro"
        String firstTitle = reply.getResults().get(0).get("title");
        assertThat(firstTitle).containsIgnoringCase("Pro");
    }

}
