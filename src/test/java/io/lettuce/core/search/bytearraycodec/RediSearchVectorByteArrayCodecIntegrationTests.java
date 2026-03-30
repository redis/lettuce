/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.bytearraycodec;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.core.search.arguments.VectorFieldArgs;
import io.lettuce.test.condition.RedisByteArrayConditions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for Redis vector search functionality using {@link ByteArrayCodec}.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchVectorByteArrayCodecIntegrationTests {

    private static final String DOCUMENTS_INDEX = "documents-idx";

    private static final String MOVIES_INDEX = "movies-idx";

    private static final String PRODUCTS_INDEX = "products-idx";

    private static final byte[] DOCS_PREFIX = "docs:".getBytes();

    private static final byte[] MOVIE_PREFIX = "movie:".getBytes();

    private static final byte[] PRODUCT_PREFIX = "product:".getBytes();

    protected static RedisClient client;

    protected static RedisCommands<byte[], byte[]> redis;

    public RediSearchVectorByteArrayCodecIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(ClientOptions.builder().build());
        redis = client.connect(ByteArrayCodec.INSTANCE).sync();
    }

    @BeforeEach
    public void prepare() {
        redis.flushall();
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    private byte[] getField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] key = fieldName.getBytes();
        return fields.entrySet().stream().filter(e -> Arrays.equals(e.getKey(), key)).map(Map.Entry::getValue).findFirst()
                .orElse(null);
    }

    /**
     * Convert float array to byte[] for vector storage (little-endian FLOAT32).
     */
    private byte[] floatArrayToBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    /**
     * Store a hash document with mixed field types.
     */
    private void storeHashDocument(String key, Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            byte[] fieldKey = entry.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] fieldValue;
            if (entry.getValue() instanceof float[]) {
                fieldValue = floatArrayToBytes((float[]) entry.getValue());
            } else if (entry.getValue() instanceof byte[]) {
                fieldValue = (byte[]) entry.getValue();
            } else {
                fieldValue = entry.getValue().toString().getBytes(StandardCharsets.UTF_8);
            }
            redis.hset(key.getBytes(StandardCharsets.UTF_8), fieldKey, fieldValue);
        }
    }

    @Test
    void testSvsVamanaBasicSearch() {
        assumeTrue(RedisByteArrayConditions.of(redis).hasVersionGreaterOrEqualsTo("8.2"));

        String indexName = "svs-vamana-basic-idx";

        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("embedding".getBytes()).svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("svs:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, nameField))).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("name".getBytes(), "Document 1".getBytes());
        doc1.put("embedding".getBytes(), floatArrayToBytes(new float[] { 1.0f, 0.0f, 0.0f, 0.0f }));
        redis.hmset("svs:doc1".getBytes(), doc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("name".getBytes(), "Document 2".getBytes());
        doc2.put("embedding".getBytes(), floatArrayToBytes(new float[] { 0.0f, 1.0f, 0.0f, 0.0f }));
        redis.hmset("svs:doc2".getBytes(), doc2);

        Map<byte[], byte[]> doc3 = new HashMap<>();
        doc3.put("name".getBytes(), "Document 3".getBytes());
        doc3.put("embedding".getBytes(), floatArrayToBytes(new float[] { 0.7071f, 0.7071f, 0.0f, 0.0f }));
        redis.hmset("svs:doc3".getBytes(), doc3);

        byte[] queryVector = floatArrayToBytes(new float[] { 1.0f, 0.0f, 0.0f, 0.0f });
        SearchArgs<byte[], byte[]> searchArgs = SearchArgs.<byte[], byte[]> builder().param("query_vec".getBytes(), queryVector)
                .build();

        SearchReply<byte[], byte[]> searchResult = redis.ftSearch(indexName, "*=>[KNN 2 @embedding $query_vec]".getBytes(),
                searchArgs);

        assertThat(searchResult.getCount()).isEqualTo(2);
        assertThat(searchResult.getResults()).hasSize(2);

        String firstName = new String(getField(searchResult.getResults().get(0).getFields(), "name"));
        assertThat(firstName).isEqualTo("Document 1");

        redis.ftDropindex(indexName);
    }

    @Test
    void testSvsVamanaWithAdvancedParameters() {
        assumeTrue(RedisByteArrayConditions.of(redis).hasVersionGreaterOrEqualsTo("8.2"));

        String indexName = "svs-vamana-advanced-idx";

        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("embedding".getBytes()).svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(8).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .attribute("CONSTRUCTION_WINDOW_SIZE", 128).attribute("GRAPH_MAX_DEGREE", 32)
                .attribute("SEARCH_WINDOW_SIZE", 64).build();

        FieldArgs<byte[]> categoryField = TagFieldArgs.<byte[]> builder().name("category".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("advanced:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, categoryField))).isEqualTo("OK");

        Map<byte[], byte[]> p1 = new HashMap<>();
        p1.put("category".getBytes(), "electronics".getBytes());
        p1.put("embedding".getBytes(), floatArrayToBytes(new float[] { 1.0f, 0.5f, 0.2f, 0.8f, 0.3f, 0.9f, 0.1f, 0.6f }));
        redis.hmset("advanced:product1".getBytes(), p1);

        Map<byte[], byte[]> p2 = new HashMap<>();
        p2.put("category".getBytes(), "books".getBytes());
        p2.put("embedding".getBytes(), floatArrayToBytes(new float[] { 0.2f, 0.8f, 0.9f, 0.1f, 0.7f, 0.4f, 0.6f, 0.3f }));
        redis.hmset("advanced:product2".getBytes(), p2);

        Map<byte[], byte[]> p3 = new HashMap<>();
        p3.put("category".getBytes(), "electronics".getBytes());
        p3.put("embedding".getBytes(), floatArrayToBytes(new float[] { 0.9f, 0.4f, 0.1f, 0.7f, 0.2f, 0.8f, 0.0f, 0.5f }));
        redis.hmset("advanced:product3".getBytes(), p3);

        byte[] queryVector = floatArrayToBytes(new float[] { 1.0f, 0.5f, 0.2f, 0.8f, 0.3f, 0.9f, 0.1f, 0.6f });
        SearchArgs<byte[], byte[]> searchArgs = SearchArgs.<byte[], byte[]> builder().param("query_vec".getBytes(), queryVector)
                .build();

        SearchReply<byte[], byte[]> searchResult = redis.ftSearch(indexName,
                "(@category:{electronics})=>[KNN 2 @embedding $query_vec]".getBytes(), searchArgs);

        assertThat(searchResult.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<byte[], byte[]> r : searchResult.getResults()) {
            assertThat(new String(getField(r.getFields(), "category"))).isEqualTo("electronics");
        }

        redis.ftDropindex(indexName);
    }

    @Test
    void testSvsVamanaWithAggregation() {
        assumeTrue(RedisByteArrayConditions.of(redis).hasVersionGreaterOrEqualsTo("8.2"));

        String indexName = "svs-vamana-agg-idx";

        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("embedding".getBytes()).svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .attribute("SEARCH_WINDOW_SIZE", 64).build();

        FieldArgs<byte[]> categoryField = TagFieldArgs.<byte[]> builder().name("category".getBytes()).sortable().build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("agg:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, categoryField, priceField)))
                .isEqualTo("OK");

        String[] categories = { "electronics", "books", "electronics", "books", "electronics" };
        float[][] vectors = { { 1.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 1.0f, 0.0f, 0.0f }, { 0.7071f, 0.7071f, 0.0f, 0.0f },
                { 0.0f, 0.0f, 1.0f, 0.0f }, { 0.5f, 0.5f, 0.5f, 0.5f } };
        double[] prices = { 99.99, 19.99, 149.99, 29.99, 199.99 };

        for (int i = 0; i < categories.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", categories[i]);
            item.put("price", String.valueOf(prices[i]));
            item.put("embedding", vectors[i]);
            storeHashDocument("agg:item" + (i + 1), item);
        }

        AggregationReply<byte[], byte[]> aggResult = redis.ftAggregate(indexName, "*".getBytes(), AggregateArgs
                .<byte[], byte[]> builder()
                .groupBy(AggregateArgs.GroupBy.<byte[], byte[]> of("category".getBytes())
                        .reduce(AggregateArgs.Reducer.<byte[], byte[]> count().as("count".getBytes()))
                        .reduce(AggregateArgs.Reducer.<byte[], byte[]> avg("@price".getBytes()).as("avg_price".getBytes())))
                .sortBy(AggregateArgs.SortBy.of("avg_price".getBytes(), AggregateArgs.SortDirection.DESC)).build());

        assertThat(aggResult.getAggregationGroups()).isEqualTo(1);
        assertThat(aggResult.getReplies()).hasSize(1);
        assertThat(aggResult.getReplies().get(0).getResults()).hasSize(2);

        Set<String> foundCategories = new HashSet<>();
        for (SearchReply.SearchResult<byte[], byte[]> r : aggResult.getReplies().get(0).getResults()) {
            foundCategories.add(new String(getField(r.getFields(), "category")));
        }
        assertThat(foundCategories).containsExactlyInAnyOrder("electronics", "books");

        redis.ftDropindex(indexName);
    }

    @Test
    void testSvsVamanaWithDifferentDistanceMetrics() {
        assumeTrue(RedisByteArrayConditions.of(redis).hasVersionGreaterOrEqualsTo("8.2"));

        String[] metrics = { "L2", "COSINE", "IP" };

        for (String metric : metrics) {
            String indexName = "svs-vamana-" + metric.toLowerCase() + "-idx";

            FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("embedding".getBytes()).svsVamana()
                    .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3)
                    .distanceMetric(VectorFieldArgs.DistanceMetric.valueOf(metric)).attribute("CONSTRUCTION_WINDOW_SIZE", 64)
                    .build();

            FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

            CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder()
                    .withPrefix((metric.toLowerCase() + ":").getBytes()).on(CreateArgs.TargetType.HASH).build();

            assertThat(redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, nameField))).isEqualTo("OK");

            redis.hset((metric.toLowerCase() + ":vec1").getBytes(), "name".getBytes(), "Vector 1".getBytes());
            redis.hset((metric.toLowerCase() + ":vec1").getBytes(), "embedding".getBytes(),
                    floatArrayToBytes(new float[] { 1.0f, 0.0f, 0.0f }));

            redis.hset((metric.toLowerCase() + ":vec2").getBytes(), "name".getBytes(), "Vector 2".getBytes());
            redis.hset((metric.toLowerCase() + ":vec2").getBytes(), "embedding".getBytes(),
                    floatArrayToBytes(new float[] { 0.0f, 1.0f, 0.0f }));

            redis.hset((metric.toLowerCase() + ":vec3").getBytes(), "name".getBytes(), "Vector 3".getBytes());
            redis.hset((metric.toLowerCase() + ":vec3").getBytes(), "embedding".getBytes(),
                    floatArrayToBytes(new float[] { 0.5f, 0.5f, 0.0f }));

            byte[] queryVector = floatArrayToBytes(new float[] { 0.9f, 0.1f, 0.0f });
            SearchArgs<byte[], byte[]> searchArgs = SearchArgs.<byte[], byte[]> builder()
                    .param("query_vec".getBytes(), queryVector).build();

            SearchReply<byte[], byte[]> searchResult = redis.ftSearch(indexName, "*=>[KNN 3 @embedding $query_vec]".getBytes(),
                    searchArgs);

            assertThat(searchResult.getCount()).isEqualTo(3);
            assertThat(searchResult.getResults()).hasSize(3);

            for (SearchReply.SearchResult<byte[], byte[]> r : searchResult.getResults()) {
                assertThat(getField(r.getFields(), "name")).isNotNull();
            }

            redis.ftDropindex(indexName);
        }
    }

    @Test
    void testFlatVectorIndexWithKnnSearch() {
        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("doc_embedding".getBytes()).flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> categoryField = TagFieldArgs.<byte[]> builder().name("category".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(DOCS_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(DOCUMENTS_INDEX, createArgs, Arrays.asList(vectorField, titleField, categoryField)))
                .isEqualTo("OK");

        redis.hset("docs:1".getBytes(), "title".getBytes(), "Redis Vector Search Tutorial".getBytes());
        redis.hset("docs:1".getBytes(), "category".getBytes(), "tutorial".getBytes());
        redis.hset("docs:1".getBytes(), "doc_embedding".getBytes(), floatArrayToBytes(new float[] { 0.1f, 0.2f, 0.3f, 0.4f }));

        redis.hset("docs:2".getBytes(), "title".getBytes(), "Advanced Vector Techniques".getBytes());
        redis.hset("docs:2".getBytes(), "category".getBytes(), "advanced".getBytes());
        redis.hset("docs:2".getBytes(), "doc_embedding".getBytes(), floatArrayToBytes(new float[] { 0.2f, 0.3f, 0.4f, 0.5f }));

        redis.hset("docs:3".getBytes(), "title".getBytes(), "Machine Learning Basics".getBytes());
        redis.hset("docs:3".getBytes(), "category".getBytes(), "tutorial".getBytes());
        redis.hset("docs:3".getBytes(), "doc_embedding".getBytes(), floatArrayToBytes(new float[] { 0.9f, 0.8f, 0.7f, 0.6f }));

        byte[] queryVector = floatArrayToBytes(new float[] { 0.15f, 0.25f, 0.35f, 0.45f });
        SearchArgs<byte[], byte[]> knnArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 2).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch(DOCUMENTS_INDEX,
                "*=>[KNN 2 @doc_embedding $BLOB AS vector_score]".getBytes(), knnArgs);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);

        String firstTitle = new String(getField(results.getResults().get(0).getFields(), "title"));
        String secondTitle = new String(getField(results.getResults().get(1).getFields(), "title"));
        assertThat(firstTitle).isIn("Redis Vector Search Tutorial", "Advanced Vector Techniques");
        assertThat(secondTitle).isIn("Redis Vector Search Tutorial", "Advanced Vector Techniques");

        redis.ftDropindex(DOCUMENTS_INDEX);
    }

    @Test
    void testHnswVectorIndexWithFiltering() {
        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("movie_embedding".getBytes()).hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .attribute("M", 40).attribute("EF_CONSTRUCTION", 250).build();

        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> genreField = TagFieldArgs.<byte[]> builder().name("genre".getBytes()).build();
        FieldArgs<byte[]> yearField = NumericFieldArgs.<byte[]> builder().name("year".getBytes()).sortable().build();
        FieldArgs<byte[]> ratingField = NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(MOVIE_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(MOVIES_INDEX, createArgs, Arrays.asList(vectorField, titleField, genreField, yearField, ratingField));

        String[][] movies = { { "movie:1", "The Matrix", "action,sci-fi", "1999", "8.7" },
                { "movie:2", "The Godfather", "drama,crime", "1972", "9.2" },
                { "movie:3", "Blade Runner", "sci-fi,thriller", "1982", "8.1" },
                { "movie:4", "Heat", "action,drama", "1995", "8.3" } };
        float[][] movieVectors = { { 1.0f, 0.1f, 0.1f }, { 0.1f, 1.0f, 0.1f }, { 0.1f, 0.1f, 1.0f }, { 0.7f, 0.7f, 0.1f } };

        for (int i = 0; i < movies.length; i++) {
            redis.hset(movies[i][0].getBytes(), "title".getBytes(), movies[i][1].getBytes());
            redis.hset(movies[i][0].getBytes(), "genre".getBytes(), movies[i][2].getBytes());
            redis.hset(movies[i][0].getBytes(), "year".getBytes(), movies[i][3].getBytes());
            redis.hset(movies[i][0].getBytes(), "rating".getBytes(), movies[i][4].getBytes());
            redis.hset(movies[i][0].getBytes(), "movie_embedding".getBytes(), floatArrayToBytes(movieVectors[i]));
        }

        // Test 1: KNN with genre filter
        byte[] queryVector = floatArrayToBytes(new float[] { 0.8f, 0.6f, 0.2f });
        SearchArgs<byte[], byte[]> filterArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 10).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch(MOVIES_INDEX,
                "(@genre:{action})=>[KNN 3 @movie_embedding $BLOB AS movie_distance]".getBytes(), filterArgs);

        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(new String(getField(result.getFields(), "genre"))).contains("action");
        }

        // Test 2: KNN with year range
        SearchArgs<byte[], byte[]> yearFilterArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 10).build();

        results = redis.ftSearch(MOVIES_INDEX,
                "(@year:[1990 2000])=>[KNN 2 @movie_embedding $BLOB AS movie_distance]".getBytes(), yearFilterArgs);
        assertThat(results.getCount()).isEqualTo(2);

        // Test 3: KNN with runtime EF parameter
        SearchArgs<byte[], byte[]> efArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .param("EF".getBytes(), "150".getBytes()).limit(0, 10).build();

        results = redis.ftSearch(MOVIES_INDEX, "*=>[KNN 3 @movie_embedding $BLOB EF_RUNTIME $EF AS movie_distance]".getBytes(),
                efArgs);
        assertThat(results.getCount()).isEqualTo(3);

        redis.ftDropindex(MOVIES_INDEX);
    }

    @Test
    void testVectorRangeQueries() {
        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("description_vector".getBytes()).flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();
        FieldArgs<byte[]> typeField = TagFieldArgs.<byte[]> builder().name("type".getBytes()).build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(PRODUCT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(PRODUCTS_INDEX, createArgs, Arrays.asList(vectorField, nameField, typeField, priceField));

        Map<String, Object> p1 = new HashMap<>();
        p1.put("name", "Laptop");
        p1.put("type", "electronics");
        p1.put("price", "999.99");
        p1.put("description_vector", new float[] { 1.0f, 0.0f, 0.0f });
        storeHashDocument("product:1", p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("name", "T-Shirt");
        p2.put("type", "clothing");
        p2.put("price", "29.99");
        p2.put("description_vector", new float[] { 0.0f, 1.0f, 0.0f });
        storeHashDocument("product:2", p2);

        Map<String, Object> p3 = new HashMap<>();
        p3.put("name", "Programming Book");
        p3.put("type", "books");
        p3.put("price", "49.99");
        p3.put("description_vector", new float[] { 0.0f, 0.0f, 1.0f });
        storeHashDocument("product:3", p3);

        Map<String, Object> p4 = new HashMap<>();
        p4.put("name", "Smart Watch");
        p4.put("type", "electronics");
        p4.put("price", "299.99");
        p4.put("description_vector", new float[] { 0.5f, 0.5f, 0.0f });
        storeHashDocument("product:4", p4);

        // Test 1: Vector range query within distance 0.5
        byte[] queryVector = floatArrayToBytes(new float[] { 0.9f, 0.1f, 0.0f });
        SearchArgs<byte[], byte[]> rangeArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 100).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch(PRODUCTS_INDEX,
                "@description_vector:[VECTOR_RANGE 0.5 $BLOB]".getBytes(), rangeArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);
        for (SearchReply.SearchResult<byte[], byte[]> r : results.getResults()) {
            assertThat(new String(getField(r.getFields(), "type"))).isIn("electronics");
        }

        // Test 2: Vector range with distance yield
        SearchArgs<byte[], byte[]> sortedRangeArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 100).build();

        results = redis.ftSearch(PRODUCTS_INDEX,
                "@description_vector:[VECTOR_RANGE 1.0 $BLOB]=>{$YIELD_DISTANCE_AS: vector_distance}".getBytes(),
                sortedRangeArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // Test 3: Combined filter
        SearchArgs<byte[], byte[]> combinedArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 100).build();

        results = redis.ftSearch(PRODUCTS_INDEX,
                "(@price:[200 1000]) | @description_vector:[VECTOR_RANGE 0.8 $BLOB]".getBytes(), combinedArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex(PRODUCTS_INDEX);
    }

    @Test
    void testDistanceMetricsAndVectorTypes() {
        String[] metrics = { "L2", "COSINE", "IP" };

        for (String metric : metrics) {
            String indexName = "test-" + metric.toLowerCase() + "-idx";

            FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("embedding".getBytes()).flat()
                    .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(2)
                    .distanceMetric(VectorFieldArgs.DistanceMetric.valueOf(metric)).build();
            FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

            CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("test:".getBytes())
                    .on(CreateArgs.TargetType.HASH).build();

            redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, nameField));

            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("name", "Point A");
            doc1.put("embedding", new float[] { 1.0f, 0.0f });
            storeHashDocument("test:1", doc1);

            Map<String, Object> doc2 = new HashMap<>();
            doc2.put("name", "Point B");
            doc2.put("embedding", new float[] { 0.0f, 1.0f });
            storeHashDocument("test:2", doc2);

            byte[] queryVector = floatArrayToBytes(new float[] { 0.7f, 0.3f });
            SearchArgs<byte[], byte[]> searchArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                    .limit(0, 2).build();

            SearchReply<byte[], byte[]> results = redis.ftSearch(indexName,
                    "*=>[KNN 2 @embedding $BLOB AS distance]".getBytes(), searchArgs);

            assertThat(results.getCount()).isEqualTo(2);
            assertThat(results.getResults()).hasSize(2);

            redis.ftDropindex(indexName);
        }
    }

    @Test
    void testJsonVectorStorage() {
        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("$.vector".getBytes()).as("vector".getBytes())
                .flat().type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3)
                .distanceMetric(VectorFieldArgs.DistanceMetric.COSINE).build();
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("$.title".getBytes()).as("title".getBytes())
                .build();
        FieldArgs<byte[]> categoryField = TagFieldArgs.<byte[]> builder().name("$.category".getBytes())
                .as("category".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("json:".getBytes())
                .on(CreateArgs.TargetType.JSON).build();

        redis.ftCreate("json-vector-idx", createArgs, Arrays.asList(vectorField, titleField, categoryField));

        JsonParser parser = redis.getJsonParser();
        redis.jsonSet("json:1".getBytes(), JsonPath.ROOT_PATH,
                parser.createJsonValue("{\"title\":\"Document 1\",\"category\":\"tech\",\"vector\":[0.1,0.2,0.3]}"));
        redis.jsonSet("json:2".getBytes(), JsonPath.ROOT_PATH,
                parser.createJsonValue("{\"title\":\"Document 2\",\"category\":\"science\",\"vector\":[0.4,0.5,0.6]}"));
        redis.jsonSet("json:3".getBytes(), JsonPath.ROOT_PATH,
                parser.createJsonValue("{\"title\":\"Document 3\",\"category\":\"tech\",\"vector\":[0.7,0.8,0.9]}"));

        byte[] queryVector = floatArrayToBytes(new float[] { 0.2f, 0.3f, 0.4f });
        SearchArgs<byte[], byte[]> searchArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 3).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch("json-vector-idx", "*=>[KNN 3 @vector $BLOB]".getBytes(),
                searchArgs);
        assertThat(results.getCount()).isEqualTo(3);

        // Filter by category
        SearchArgs<byte[], byte[]> filterArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 10).build();

        results = redis.ftSearch("json-vector-idx", "(@category:{tech})=>[KNN 2 @vector $BLOB]".getBytes(), filterArgs);
        assertThat(results.getCount()).isEqualTo(2);

        redis.ftDropindex("json-vector-idx");
        redis.del("json:1".getBytes(), "json:2".getBytes(), "json:3".getBytes());
    }

    @Test
    void testAdvancedVectorSearchFeatures() {
        VectorFieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("content_vector".getBytes()).hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .attribute("M", 16).attribute("EF_CONSTRUCTION", 200).build();

        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> statusField = TagFieldArgs.<byte[]> builder().name("status".getBytes()).build();
        FieldArgs<byte[]> priorityField = NumericFieldArgs.<byte[]> builder().name("priority".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("task:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("tasks-idx", createArgs, Arrays.asList(vectorField, titleField, statusField, priorityField));

        for (int i = 1; i <= 10; i++) {
            float[] vector = { (float) Math.random(), (float) Math.random(), (float) Math.random(), (float) Math.random() };
            Map<String, Object> task = new HashMap<>();
            task.put("title", "Task " + i);
            task.put("status", i % 2 == 0 ? "active" : "completed");
            task.put("priority", String.valueOf(i % 5 + 1));
            task.put("content_vector", vector);
            storeHashDocument("task:" + i, task);
        }

        byte[] queryVector = floatArrayToBytes(new float[] { 0.5f, 0.5f, 0.5f, 0.5f });

        // Test 1: ADHOC_BF hybrid policy
        SearchArgs<byte[], byte[]> adhocArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 5).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch("tasks-idx",
                "(@status:{active})=>[KNN 5 @content_vector $BLOB HYBRID_POLICY ADHOC_BF AS task_score]".getBytes(), adhocArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Test 2: BATCHES hybrid policy
        SearchArgs<byte[], byte[]> batchArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .param("BATCH_SIZE".getBytes(), "3".getBytes()).limit(0, 5).build();

        results = redis.ftSearch("tasks-idx",
                "(@status:{active})=>[KNN 5 @content_vector $BLOB HYBRID_POLICY BATCHES BATCH_SIZE $BATCH_SIZE AS task_score]"
                        .getBytes(),
                batchArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Test 3: EF_RUNTIME parameter
        SearchArgs<byte[], byte[]> efArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .param("EF".getBytes(), "50".getBytes()).limit(0, 3).build();

        results = redis.ftSearch("tasks-idx", "*=>[KNN 3 @content_vector $BLOB EF_RUNTIME $EF AS task_score]".getBytes(),
                efArgs);
        assertThat(results.getCount()).isEqualTo(3);

        // Test 4: Complex filter + vector search
        SearchArgs<byte[], byte[]> complexArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 10).build();

        results = redis.ftSearch("tasks-idx",
                "((@status:{active}) (@priority:[3 5]))=>[KNN 5 @content_vector $BLOB AS task_score]".getBytes(), complexArgs);

        for (SearchReply.SearchResult<byte[], byte[]> r : results.getResults()) {
            assertThat(new String(getField(r.getFields(), "status"))).isEqualTo("active");
            int priority = Integer.parseInt(new String(getField(r.getFields(), "priority")));
            assertThat(priority).isBetween(3, 5);
        }

        // Test 5: Timeout
        SearchArgs<byte[], byte[]> timeoutArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .timeout(Duration.ofSeconds(5)).limit(0, 5).build();

        results = redis.ftSearch("tasks-idx", "*=>[KNN 5 @content_vector $BLOB AS task_score]".getBytes(), timeoutArgs);
        assertThat(results.getCount()).isEqualTo(5);

        redis.ftDropindex("tasks-idx");
    }

    @Test
    void testVectorTypesAndPrecision() {
        FieldArgs<byte[]> float64Field = VectorFieldArgs.<byte[]> builder().name("embedding_f64".getBytes()).flat()
                .type(VectorFieldArgs.VectorType.FLOAT64).dimensions(2).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("precision:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("precision-idx", createArgs, Arrays.asList(float64Field, nameField));

        double[] preciseVector1 = { 1.123456789012345, 2.987654321098765 };
        double[] preciseVector2 = { 3.141592653589793, 2.718281828459045 };

        ByteBuffer buf1 = ByteBuffer.allocate(preciseVector1.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : preciseVector1)
            buf1.putDouble(v);

        ByteBuffer buf2 = ByteBuffer.allocate(preciseVector2.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : preciseVector2)
            buf2.putDouble(v);

        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("name", "High Precision Vector 1");
        doc1.put("embedding_f64", buf1.array());
        storeHashDocument("precision:1", doc1);

        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("name", "High Precision Vector 2");
        doc2.put("embedding_f64", buf2.array());
        storeHashDocument("precision:2", doc2);

        double[] queryVector = { 1.5, 2.5 };
        ByteBuffer queryBuf = ByteBuffer.allocate(queryVector.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : queryVector)
            queryBuf.putDouble(v);

        SearchArgs<byte[], byte[]> args = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryBuf.array())
                .limit(0, 2).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch("precision-idx",
                "*=>[KNN 2 @embedding_f64 $BLOB AS distance]".getBytes(), args);

        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<byte[], byte[]> r : results.getResults()) {
            assertThat(new String(getField(r.getFields(), "name"))).contains("High Precision Vector");
        }

        redis.ftDropindex("precision-idx");
    }

    @Test
    void testVectorSearchErrorHandling() {
        assumeTrue(RedisByteArrayConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));

        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("test_vector".getBytes()).flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("error:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("error-test-idx", createArgs, Collections.singletonList(vectorField));

        Map<String, Object> doc = new HashMap<>();
        doc.put("test_vector", new float[] { 1.0f, 0.0f, 0.0f });
        storeHashDocument("error:1", doc);

        // Valid search
        byte[] queryVector = floatArrayToBytes(new float[] { 0.9f, 0.1f, 0.0f });
        SearchArgs<byte[], byte[]> validArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 1).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch("error-test-idx", "*=>[KNN 1 @test_vector $BLOB]".getBytes(),
                validArgs);
        assertThat(results.getCount()).isEqualTo(1);

        // Invalid field should throw
        SearchArgs<byte[], byte[]> noResultsArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 10).build();

        assertThatThrownBy(() -> redis.ftSearch("error-test-idx",
                "(@nonexistent_field:value)=>[KNN 5 @test_vector $BLOB]".getBytes(), noResultsArgs))
                        .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("Unknown field");

        redis.ftDropindex("error-test-idx");
    }

    static Stream<VectorFieldArgs.VectorType> quantizedVectorTypes() {
        return Stream.of(VectorFieldArgs.VectorType.INT8, VectorFieldArgs.VectorType.UINT8);
    }

    @ParameterizedTest
    @MethodSource("quantizedVectorTypes")
    void testQuantizedVectorTypes(VectorFieldArgs.VectorType vectorType) {
        assumeTrue(RedisByteArrayConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));

        String typeName = vectorType.name();
        String indexName = typeName.toLowerCase() + "-idx";
        String prefix = typeName.toLowerCase() + ":";
        String fieldName = "embedding_" + typeName.toLowerCase();

        FieldArgs<byte[]> vectorField;
        if (vectorType == VectorFieldArgs.VectorType.INT8) {
            vectorField = VectorFieldArgs.<byte[]> builder().name(fieldName.getBytes()).flat().type(vectorType).dimensions(4)
                    .distanceMetric(VectorFieldArgs.DistanceMetric.L2).build();
        } else {
            vectorField = VectorFieldArgs.<byte[]> builder().name(fieldName.getBytes()).hnsw().type(vectorType).dimensions(4)
                    .distanceMetric(VectorFieldArgs.DistanceMetric.COSINE).attribute("M", 16).attribute("EF_CONSTRUCTION", 200)
                    .build();
        }

        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(prefix.getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, nameField));

        byte[] vector1, vector2, vector3, queryVector;
        if (vectorType == VectorFieldArgs.VectorType.INT8) {
            vector1 = new byte[] { 10, 20, 30, 40 };
            vector2 = new byte[] { -50, 60, -70, 80 };
            vector3 = new byte[] { 15, 25, 35, 45 };
            queryVector = new byte[] { 12, 22, 32, 42 };
        } else {
            vector1 = new byte[] { (byte) 100, (byte) 150, (byte) 200, (byte) 250 };
            vector2 = new byte[] { (byte) 50, (byte) 100, (byte) 150, (byte) 200 };
            vector3 = new byte[] { (byte) 110, (byte) 160, (byte) 210, (byte) 240 };
            queryVector = new byte[] { (byte) 105, (byte) 155, (byte) 205, (byte) 245 };
        }

        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("name", typeName + " Vector 1");
        doc1.put(fieldName, vector1);
        storeHashDocument(prefix + "1", doc1);

        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("name", typeName + " Vector 2");
        doc2.put(fieldName, vector2);
        storeHashDocument(prefix + "2", doc2);

        Map<String, Object> doc3 = new HashMap<>();
        doc3.put("name", typeName + " Vector 3");
        doc3.put(fieldName, vector3);
        storeHashDocument(prefix + "3", doc3);

        SearchArgs<byte[], byte[]> searchArgs = SearchArgs.<byte[], byte[]> builder().param("BLOB".getBytes(), queryVector)
                .limit(0, 2).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch(indexName,
                ("*=>[KNN 2 @" + fieldName + " $BLOB AS distance]").getBytes(), searchArgs);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> r : results.getResults()) {
            assertThat(new String(getField(r.getFields(), "name"))).contains(typeName + " Vector");
        }

        redis.ftDropindex(indexName);
    }

}
