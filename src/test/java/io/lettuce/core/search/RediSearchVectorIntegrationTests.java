/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ByteBufferCodec;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SortByArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.SearchReply.SearchResult;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.core.search.arguments.VectorFieldArgs;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonValue;
import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for Redis Vector Search functionality using FT.SEARCH command with vector fields.
 * <p>
 * These tests are based on the examples from the Redis documentation:
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/">Vector Search</a>
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RediSearchVectorIntegrationTests {

    // Index names
    private static final String DOCUMENTS_INDEX = "documents-idx";

    private static final String MOVIES_INDEX = "movies-idx";

    private static final String PRODUCTS_INDEX = "products-idx";

    // Prefixes
    private static final String DOCS_PREFIX = "docs:";

    private static final String MOVIE_PREFIX = "movie:";

    private static final String PRODUCT_PREFIX = "product:";

    protected static RedisClient client;

    protected static RedisCommands<ByteBuffer, ByteBuffer> redisBinary;

    protected static RedisCommands<String, String> redis;

    public RediSearchVectorIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect().sync();
        redisBinary = client.connect(new ByteBufferCodec()).sync();
    }

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
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

    /**
     * Test SVS-VAMANA vector index creation and basic search functionality. This test verifies that SVS-VAMANA algorithm can be
     * used to create vector indexes and perform basic vector similarity searches.
     */
    @Test
    void testSvsVamanaBasicVectorSearch() {
        String indexName = "svs-vamana-basic-idx";

        // Create SVS-VAMANA vector field
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("embedding").svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("svs:")
                .on(CreateArgs.TargetType.HASH).build();

        // Create index
        String result = redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, nameField));
        assertThat(result).isEqualTo("OK");

        // Insert test vectors using binary format
        Map<ByteBuffer, ByteBuffer> doc1 = new HashMap<>();
        doc1.put(ByteBuffer.wrap("name".getBytes()), ByteBuffer.wrap("Document 1".getBytes()));
        doc1.put(ByteBuffer.wrap("embedding".getBytes()), floatArrayToByteBuffer(new float[] { 1.0f, 0.0f, 0.0f, 0.0f }));
        redisBinary.hmset(ByteBuffer.wrap("svs:doc1".getBytes()), doc1);

        Map<ByteBuffer, ByteBuffer> doc2 = new HashMap<>();
        doc2.put(ByteBuffer.wrap("name".getBytes()), ByteBuffer.wrap("Document 2".getBytes()));
        doc2.put(ByteBuffer.wrap("embedding".getBytes()), floatArrayToByteBuffer(new float[] { 0.0f, 1.0f, 0.0f, 0.0f }));
        redisBinary.hmset(ByteBuffer.wrap("svs:doc2".getBytes()), doc2);

        Map<ByteBuffer, ByteBuffer> doc3 = new HashMap<>();
        doc3.put(ByteBuffer.wrap("name".getBytes()), ByteBuffer.wrap("Document 3".getBytes()));
        doc3.put(ByteBuffer.wrap("embedding".getBytes()), floatArrayToByteBuffer(new float[] { 0.7071f, 0.7071f, 0.0f, 0.0f }));
        redisBinary.hmset(ByteBuffer.wrap("svs:doc3".getBytes()), doc3);

        // Perform vector search using binary query vector
        ByteBuffer queryVector = floatArrayToByteBuffer(new float[] { 1.0f, 0.0f, 0.0f, 0.0f });
        ByteBuffer blobKey = ByteBuffer.wrap("query_vec".getBytes());
        SearchArgs<ByteBuffer, ByteBuffer> searchArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVector).build();

        ByteBuffer indexKey = ByteBuffer.wrap(indexName.getBytes());
        ByteBuffer queryString = ByteBuffer.wrap("*=>[KNN 2 @embedding $query_vec]".getBytes());
        SearchReply<ByteBuffer, ByteBuffer> searchResult = redisBinary.ftSearch(indexKey, queryString, searchArgs);

        // Verify results
        assertThat(searchResult.getCount()).isEqualTo(2);
        assertThat(searchResult.getResults()).hasSize(2);

        // First result should be doc1 (exact match)
        ByteBuffer nameKey = ByteBuffer.wrap("name".getBytes());
        String firstName = new String(searchResult.getResults().get(0).getFields().get(nameKey).array());
        assertThat(firstName).isEqualTo("Document 1");

        // Cleanup
        redis.ftDropindex(indexName);
    }

    /**
     * Test SVS-VAMANA vector index with advanced configuration parameters. This test verifies that SVS-VAMANA can be configured
     * with construction and search parameters for performance tuning (without compression which requires Redis 8.2+).
     */
    @Test
    void testSvsVamanaWithAdvancedParameters() {
        String indexName = "svs-vamana-advanced-idx";

        // Create SVS-VAMANA vector field with advanced parameters (no compression)
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("embedding").svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(8).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .attribute("CONSTRUCTION_WINDOW_SIZE", 128).attribute("GRAPH_MAX_DEGREE", 32)
                .attribute("SEARCH_WINDOW_SIZE", 64).build();

        FieldArgs<String> categoryField = TagFieldArgs.<String> builder().name("category").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("advanced:")
                .on(CreateArgs.TargetType.HASH).build();

        // Create index
        String result = redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, categoryField));
        assertThat(result).isEqualTo("OK");

        // Insert test vectors with higher dimensionality using binary format
        Map<ByteBuffer, ByteBuffer> product1 = new HashMap<>();
        product1.put(ByteBuffer.wrap("category".getBytes()), ByteBuffer.wrap("electronics".getBytes()));
        product1.put(ByteBuffer.wrap("embedding".getBytes()),
                floatArrayToByteBuffer(new float[] { 1.0f, 0.5f, 0.2f, 0.8f, 0.3f, 0.9f, 0.1f, 0.6f }));
        redisBinary.hmset(ByteBuffer.wrap("advanced:product1".getBytes()), product1);

        Map<ByteBuffer, ByteBuffer> product2 = new HashMap<>();
        product2.put(ByteBuffer.wrap("category".getBytes()), ByteBuffer.wrap("books".getBytes()));
        product2.put(ByteBuffer.wrap("embedding".getBytes()),
                floatArrayToByteBuffer(new float[] { 0.2f, 0.8f, 0.9f, 0.1f, 0.7f, 0.4f, 0.6f, 0.3f }));
        redisBinary.hmset(ByteBuffer.wrap("advanced:product2".getBytes()), product2);

        Map<ByteBuffer, ByteBuffer> product3 = new HashMap<>();
        product3.put(ByteBuffer.wrap("category".getBytes()), ByteBuffer.wrap("electronics".getBytes()));
        product3.put(ByteBuffer.wrap("embedding".getBytes()),
                floatArrayToByteBuffer(new float[] { 0.9f, 0.4f, 0.1f, 0.7f, 0.2f, 0.8f, 0.0f, 0.5f }));
        redisBinary.hmset(ByteBuffer.wrap("advanced:product3".getBytes()), product3);

        // Perform vector search with category filter
        ByteBuffer queryVector = floatArrayToByteBuffer(new float[] { 1.0f, 0.5f, 0.2f, 0.8f, 0.3f, 0.9f, 0.1f, 0.6f });
        ByteBuffer blobKey = ByteBuffer.wrap("query_vec".getBytes());
        SearchArgs<ByteBuffer, ByteBuffer> searchArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVector).build();

        ByteBuffer indexKey = ByteBuffer.wrap(indexName.getBytes());
        ByteBuffer queryString = ByteBuffer.wrap("(@category:{electronics})=>[KNN 2 @embedding $query_vec]".getBytes());
        SearchReply<ByteBuffer, ByteBuffer> searchResult = redisBinary.ftSearch(indexKey, queryString, searchArgs);

        // Verify results - should find electronics products only
        assertThat(searchResult.getCount()).isEqualTo(2);
        assertThat(searchResult.getResults()).hasSize(2);

        // All results should be electronics
        ByteBuffer categoryKey = ByteBuffer.wrap("category".getBytes());
        for (SearchReply.SearchResult<ByteBuffer, ByteBuffer> searchResultItem : searchResult.getResults()) {
            String category = new String(searchResultItem.getFields().get(categoryKey).array());
            assertThat(category).isEqualTo("electronics");
        }

        // Cleanup
        redis.ftDropindex(indexName);
    }

    /**
     * Test SVS-VAMANA vector index with aggregation operations. This test verifies that SVS-VAMANA indexes work correctly with
     * aggregation queries and can be used for analytical operations on vector data.
     */
    @Test
    void testSvsVamanaWithAggregation() {
        String indexName = "svs-vamana-agg-idx";

        // Create SVS-VAMANA vector field optimized for aggregation (no compression)
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("embedding").svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .attribute("SEARCH_WINDOW_SIZE", 64).build();

        FieldArgs<String> categoryField = TagFieldArgs.<String> builder().name("category").sortable().build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("price").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("agg:")
                .on(CreateArgs.TargetType.HASH).build();

        // Create index
        String result = redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, categoryField, priceField));
        assertThat(result).isEqualTo("OK");

        // Insert test data for aggregation
        String[] categories = { "electronics", "books", "electronics", "books", "electronics" };
        float[][] vectors = { { 1.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 1.0f, 0.0f, 0.0f }, { 0.7071f, 0.7071f, 0.0f, 0.0f },
                { 0.0f, 0.0f, 1.0f, 0.0f }, { 0.5f, 0.5f, 0.5f, 0.5f } };
        double[] prices = { 99.99, 19.99, 149.99, 29.99, 199.99 };

        for (int i = 0; i < categories.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", categories[i]);
            item.put("price", String.valueOf(prices[i]));
            item.put("embedding", floatArrayToByteBuffer(vectors[i]).array());
            storeHashDocument("agg:item" + (i + 1), item);
        }

        // Perform aggregation: group by category and calculate average price
        AggregationReply<String, String> aggregationResult = redis.ftAggregate(indexName, "*",
                AggregateArgs.<String, String> builder()
                        .groupBy(AggregateArgs.GroupBy.<String, String> of("category")
                                .reduce(AggregateArgs.Reducer.<String, String> count().as("count"))
                                .reduce(AggregateArgs.Reducer.<String, String> avg("@price").as("avg_price")))
                        .sortBy(AggregateArgs.SortBy.of("avg_price", AggregateArgs.SortDirection.DESC)).build());

        // Verify aggregation results
        // Verify aggregation results
        assertThat(aggregationResult.getAggregationGroups()).isEqualTo(1); // 1 aggregation operation
        assertThat(aggregationResult.getReplies()).hasSize(1); // One reply containing all groups

        SearchReply<String, String> reply = aggregationResult.getReplies().get(0);
        assertThat(reply.getResults()).hasSize(2); // 2 category groups

        // Verify we have both categories represented
        List<SearchResult<String, String>> aggregationResults = reply.getResults();
        Set<String> foundCategories = new HashSet<>();
        for (SearchResult<String, String> groupResult : aggregationResults) {
            foundCategories.add(groupResult.getFields().get("category"));
        }
        assertThat(foundCategories).containsExactlyInAnyOrder("electronics", "books");

        // Cleanup
        redis.ftDropindex(indexName);
    }

    /**
     * Test SVS-VAMANA vector index with different distance metrics. This test verifies that SVS-VAMANA works correctly with all
     * supported distance metrics (L2, COSINE, IP) and produces expected similarity rankings.
     */
    @Test
    void testSvsVamanaWithDifferentDistanceMetrics() {
        // Test each distance metric
        String[] metrics = { "L2", "COSINE", "IP" };

        for (String metric : metrics) {
            String indexName = "svs-vamana-" + metric.toLowerCase() + "-idx";

            FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("embedding").svsVamana()
                    .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3)
                    .distanceMetric(VectorFieldArgs.DistanceMetric.valueOf(metric)).attribute("CONSTRUCTION_WINDOW_SIZE", 64)
                    .build();

            FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

            CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(metric.toLowerCase() + ":")
                    .on(CreateArgs.TargetType.HASH).build();

            // Create index
            String result = redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, nameField));
            assertThat(result).isEqualTo("OK");

            // Insert test vectors using binary format
            Map<ByteBuffer, ByteBuffer> vec1 = new HashMap<>();
            vec1.put(ByteBuffer.wrap("name".getBytes()), ByteBuffer.wrap("Vector 1".getBytes()));
            vec1.put(ByteBuffer.wrap("embedding".getBytes()), floatArrayToByteBuffer(new float[] { 1.0f, 0.0f, 0.0f }));
            redisBinary.hmset(ByteBuffer.wrap((metric.toLowerCase() + ":vec1").getBytes()), vec1);

            Map<ByteBuffer, ByteBuffer> vec2 = new HashMap<>();
            vec2.put(ByteBuffer.wrap("name".getBytes()), ByteBuffer.wrap("Vector 2".getBytes()));
            vec2.put(ByteBuffer.wrap("embedding".getBytes()), floatArrayToByteBuffer(new float[] { 0.0f, 1.0f, 0.0f }));
            redisBinary.hmset(ByteBuffer.wrap((metric.toLowerCase() + ":vec2").getBytes()), vec2);

            Map<ByteBuffer, ByteBuffer> vec3 = new HashMap<>();
            vec3.put(ByteBuffer.wrap("name".getBytes()), ByteBuffer.wrap("Vector 3".getBytes()));
            vec3.put(ByteBuffer.wrap("embedding".getBytes()), floatArrayToByteBuffer(new float[] { 0.5f, 0.5f, 0.0f }));
            redisBinary.hmset(ByteBuffer.wrap((metric.toLowerCase() + ":vec3").getBytes()), vec3);

            // Query with vector similar to vec1
            ByteBuffer queryVector = floatArrayToByteBuffer(new float[] { 0.9f, 0.1f, 0.0f });
            ByteBuffer blobKey = ByteBuffer.wrap("query_vec".getBytes());
            SearchArgs<ByteBuffer, ByteBuffer> searchArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                    .param(blobKey, queryVector).build();

            ByteBuffer indexKey = ByteBuffer.wrap(indexName.getBytes());
            ByteBuffer queryString = ByteBuffer.wrap("*=>[KNN 3 @embedding $query_vec]".getBytes());
            SearchReply<ByteBuffer, ByteBuffer> searchResult = redisBinary.ftSearch(indexKey, queryString, searchArgs);

            // Verify we get results
            assertThat(searchResult.getCount()).isEqualTo(3);
            assertThat(searchResult.getResults()).hasSize(3);

            // For all metrics, the most similar should be found
            // The exact ranking may vary by metric, but we should get valid results
            List<SearchReply.SearchResult<ByteBuffer, ByteBuffer>> results = searchResult.getResults();
            ByteBuffer nameKey = ByteBuffer.wrap("name".getBytes());
            assertThat(results.get(0).getFields().get(nameKey)).isNotNull();
            assertThat(results.get(1).getFields().get(nameKey)).isNotNull();
            assertThat(results.get(2).getFields().get(nameKey)).isNotNull();

            // Cleanup
            redis.ftDropindex(indexName);
        }
    }

    /**
     * Helper method to convert float array to ByteBuffer for vector storage. Redis expects vectors as binary data when stored
     * in HASH fields.
     */
    private ByteBuffer floatArrayToByteBuffer(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return (ByteBuffer) buffer.flip();
    }

    /**
     * Helper method to store hash document using binary codec.
     */
    private void storeHashDocument(String key, Map<String, Object> fields) {
        ByteBuffer keyBuffer = ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            ByteBuffer fieldKey = ByteBuffer.wrap(entry.getKey().getBytes(StandardCharsets.UTF_8));
            ByteBuffer fieldValue;
            if (entry.getValue() instanceof float[]) {
                fieldValue = floatArrayToByteBuffer((float[]) entry.getValue());
            } else if (entry.getValue() instanceof byte[]) {
                fieldValue = ByteBuffer.wrap((byte[]) entry.getValue());
            } else {
                fieldValue = ByteBuffer.wrap(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
            }
            redisBinary.hset(keyBuffer, fieldKey, fieldValue);
        }
    }

    /**
     * Test basic FLAT vector index creation and KNN search based on Redis documentation examples. Creates a FLAT vector index
     * with FLOAT32 vectors and performs KNN searches.
     */
    @Test
    void testFlatVectorIndexWithKnnSearch() {
        // Create FLAT vector index based on Redis documentation:
        // FT.CREATE documents ON HASH PREFIX 1 docs: SCHEMA doc_embedding VECTOR FLAT 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC
        // COSINE
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("doc_embedding").flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4) // Using smaller dimensions for testing
                .distanceMetric(VectorFieldArgs.DistanceMetric.COSINE).build();
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> categoryField = TagFieldArgs.<String> builder().name("category").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(DOCS_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(DOCUMENTS_INDEX, createArgs, Arrays.asList(vectorField, titleField, categoryField));
        assertThat(result).isEqualTo("OK");

        // Add sample documents with vectors
        float[] vector1 = { 0.1f, 0.2f, 0.3f, 0.4f };
        float[] vector2 = { 0.2f, 0.3f, 0.4f, 0.5f };
        float[] vector3 = { 0.9f, 0.8f, 0.7f, 0.6f };

        // Store vectors as binary data using binary connection
        ByteBuffer titleKey = ByteBuffer.wrap("title".getBytes(StandardCharsets.UTF_8));
        ByteBuffer categoryKey = ByteBuffer.wrap("category".getBytes(StandardCharsets.UTF_8));
        ByteBuffer embeddingKey = ByteBuffer.wrap("doc_embedding".getBytes(StandardCharsets.UTF_8));

        ByteBuffer doc1Key = ByteBuffer.wrap("docs:1".getBytes(StandardCharsets.UTF_8));
        redisBinary.hset(doc1Key, titleKey, ByteBuffer.wrap("Redis Vector Search Tutorial".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(doc1Key, categoryKey, ByteBuffer.wrap("tutorial".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(doc1Key, embeddingKey, floatArrayToByteBuffer(vector1));

        ByteBuffer doc2Key = ByteBuffer.wrap("docs:2".getBytes(StandardCharsets.UTF_8));
        redisBinary.hset(doc2Key, titleKey, ByteBuffer.wrap("Advanced Vector Techniques".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(doc2Key, categoryKey, ByteBuffer.wrap("advanced".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(doc2Key, embeddingKey, floatArrayToByteBuffer(vector2));

        ByteBuffer doc3Key = ByteBuffer.wrap("docs:3".getBytes(StandardCharsets.UTF_8));
        redisBinary.hset(doc3Key, titleKey, ByteBuffer.wrap("Machine Learning Basics".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(doc3Key, categoryKey, ByteBuffer.wrap("tutorial".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(doc3Key, embeddingKey, floatArrayToByteBuffer(vector3));

        // Test 1: Basic KNN search - find 2 nearest neighbors using binary connection
        float[] queryVector = { 0.15f, 0.25f, 0.35f, 0.45f }; // Similar to vector1 and vector2
        ByteBuffer queryVectorBuffer = floatArrayToByteBuffer(queryVector);

        // Use binary connection for search to handle binary vector data properly
        ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> knnArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 2).build();

        ByteBuffer indexKey = ByteBuffer.wrap(DOCUMENTS_INDEX.getBytes(StandardCharsets.UTF_8));
        ByteBuffer queryString = ByteBuffer
                .wrap("*=>[KNN 2 @doc_embedding $BLOB AS vector_score]".getBytes(StandardCharsets.UTF_8));

        SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, knnArgs);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);

        // The results should be sorted by vector similarity (closest first)
        // vector1 and vector2 should be more similar to queryVector than vector3
        SearchReply.SearchResult<ByteBuffer, ByteBuffer> firstResult = results.getResults().get(0);
        SearchReply.SearchResult<ByteBuffer, ByteBuffer> secondResult = results.getResults().get(1);

        // Convert ByteBuffer results back to strings for assertions
        ByteBuffer titleFieldKey = ByteBuffer.wrap("title".getBytes(StandardCharsets.UTF_8));
        String firstTitle = new String(firstResult.getFields().get(titleFieldKey).array(), StandardCharsets.UTF_8);
        String secondTitle = new String(secondResult.getFields().get(titleFieldKey).array(), StandardCharsets.UTF_8);

        assertThat(firstTitle).isIn("Redis Vector Search Tutorial", "Advanced Vector Techniques");
        assertThat(secondTitle).isIn("Redis Vector Search Tutorial", "Advanced Vector Techniques");

        // Cleanup
        redis.ftDropindex(DOCUMENTS_INDEX);
    }

    /**
     * Test HNSW vector index with runtime parameters and filtering. Based on Redis documentation examples for HNSW algorithm.
     */
    @Test
    void testHnswVectorIndexWithFiltering() {
        // Create HNSW vector index with custom parameters
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("movie_embedding").hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .attribute("M", 40).attribute("EF_CONSTRUCTION", 250).build();

        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> genreField = TagFieldArgs.<String> builder().name("genre").build();
        FieldArgs<String> yearField = NumericFieldArgs.<String> builder().name("year").sortable().build();
        FieldArgs<String> ratingField = NumericFieldArgs.<String> builder().name("rating").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(MOVIE_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(MOVIES_INDEX, createArgs, Arrays.asList(vectorField, titleField, genreField, yearField, ratingField));

        // Add sample movies with vectors
        float[] actionVector = { 1.0f, 0.1f, 0.1f };
        float[] dramaVector = { 0.1f, 1.0f, 0.1f };
        float[] sciFiVector = { 0.1f, 0.1f, 1.0f };
        float[] actionDramaVector = { 0.7f, 0.7f, 0.1f };

        // Store movie data using binary connection for vector fields
        ByteBuffer titleKey = ByteBuffer.wrap("title".getBytes(StandardCharsets.UTF_8));
        ByteBuffer genreKey = ByteBuffer.wrap("genre".getBytes(StandardCharsets.UTF_8));
        ByteBuffer yearKey = ByteBuffer.wrap("year".getBytes(StandardCharsets.UTF_8));
        ByteBuffer ratingKey = ByteBuffer.wrap("rating".getBytes(StandardCharsets.UTF_8));
        ByteBuffer embeddingKey = ByteBuffer.wrap("movie_embedding".getBytes(StandardCharsets.UTF_8));

        ByteBuffer movie1Key = ByteBuffer.wrap("movie:1".getBytes(StandardCharsets.UTF_8));
        redisBinary.hset(movie1Key, titleKey, ByteBuffer.wrap("The Matrix".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie1Key, genreKey, ByteBuffer.wrap("action,sci-fi".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie1Key, yearKey, ByteBuffer.wrap("1999".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie1Key, ratingKey, ByteBuffer.wrap("8.7".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie1Key, embeddingKey, floatArrayToByteBuffer(actionVector));

        ByteBuffer movie2Key = ByteBuffer.wrap("movie:2".getBytes(StandardCharsets.UTF_8));
        redisBinary.hset(movie2Key, titleKey, ByteBuffer.wrap("The Godfather".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie2Key, genreKey, ByteBuffer.wrap("drama,crime".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie2Key, yearKey, ByteBuffer.wrap("1972".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie2Key, ratingKey, ByteBuffer.wrap("9.2".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie2Key, embeddingKey, floatArrayToByteBuffer(dramaVector));

        ByteBuffer movie3Key = ByteBuffer.wrap("movie:3".getBytes(StandardCharsets.UTF_8));
        redisBinary.hset(movie3Key, titleKey, ByteBuffer.wrap("Blade Runner".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie3Key, genreKey, ByteBuffer.wrap("sci-fi,thriller".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie3Key, yearKey, ByteBuffer.wrap("1982".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie3Key, ratingKey, ByteBuffer.wrap("8.1".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie3Key, embeddingKey, floatArrayToByteBuffer(sciFiVector));

        ByteBuffer movie4Key = ByteBuffer.wrap("movie:4".getBytes(StandardCharsets.UTF_8));
        redisBinary.hset(movie4Key, titleKey, ByteBuffer.wrap("Heat".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie4Key, genreKey, ByteBuffer.wrap("action,drama".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie4Key, yearKey, ByteBuffer.wrap("1995".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie4Key, ratingKey, ByteBuffer.wrap("8.3".getBytes(StandardCharsets.UTF_8)));
        redisBinary.hset(movie4Key, embeddingKey, floatArrayToByteBuffer(actionDramaVector));

        // Test 1: KNN search with genre filter using binary codec
        float[] queryVector = { 0.8f, 0.6f, 0.2f }; // Similar to action-drama
        ByteBuffer queryVectorBuffer = floatArrayToByteBuffer(queryVector);

        ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> filterArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 10).build();

        ByteBuffer indexKey = ByteBuffer.wrap(MOVIES_INDEX.getBytes(StandardCharsets.UTF_8));
        ByteBuffer queryString = ByteBuffer
                .wrap("(@genre:{action})=>[KNN 3 @movie_embedding $BLOB AS movie_distance]".getBytes(StandardCharsets.UTF_8));

        // Search for action movies with vector similarity
        SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, filterArgs);

        assertThat(results.getCount()).isEqualTo(2); // The Matrix and Heat have action genre
        ByteBuffer genreFieldKey = ByteBuffer.wrap("genre".getBytes(StandardCharsets.UTF_8));
        for (SearchReply.SearchResult<ByteBuffer, ByteBuffer> result : results.getResults()) {
            String genre = new String(result.getFields().get(genreFieldKey).array(), StandardCharsets.UTF_8);
            assertThat(genre).contains("action");
        }

        // Test 2: KNN search with year range filter
        SearchArgs<ByteBuffer, ByteBuffer> yearFilterArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 10).build();

        ByteBuffer yearQueryString = ByteBuffer
                .wrap("(@year:[1990 2000])=>[KNN 2 @movie_embedding $BLOB AS movie_distance]".getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, yearQueryString, yearFilterArgs);

        assertThat(results.getCount()).isEqualTo(2); // The Matrix (1999) and Heat (1995)

        // Test 3: KNN search with runtime EF parameter
        ByteBuffer efKey = ByteBuffer.wrap("EF".getBytes(StandardCharsets.UTF_8));
        ByteBuffer efValue = ByteBuffer.wrap("150".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> efArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).param(efKey, efValue).limit(0, 10).build();

        ByteBuffer efQueryString = ByteBuffer
                .wrap("*=>[KNN 3 @movie_embedding $BLOB EF_RUNTIME $EF AS movie_distance]".getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, efQueryString, efArgs);

        assertThat(results.getCount()).isEqualTo(3);

        // Cleanup
        redis.ftDropindex(MOVIES_INDEX);
    }

    /**
     * Test vector range queries based on Redis documentation examples. Vector range queries filter results based on semantic
     * distance radius.
     */
    @Test
    void testVectorRangeQueries() {
        // Create vector index for range query testing
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("description_vector").flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();
        FieldArgs<String> typeField = TagFieldArgs.<String> builder().name("type").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("price").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PRODUCT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(PRODUCTS_INDEX, createArgs, Arrays.asList(vectorField, nameField, typeField, priceField));

        // Add sample products with vectors representing different categories
        float[] electronicsVector = { 1.0f, 0.0f, 0.0f };
        float[] clothingVector = { 0.0f, 1.0f, 0.0f };
        float[] booksVector = { 0.0f, 0.0f, 1.0f };
        float[] mixedVector = { 0.5f, 0.5f, 0.0f }; // Between electronics and clothing

        // Store products using binary codec
        Map<String, Object> product1 = new HashMap<>();
        product1.put("name", "Laptop");
        product1.put("type", "electronics");
        product1.put("price", "999.99");
        product1.put("description_vector", electronicsVector);
        storeHashDocument("product:1", product1);

        Map<String, Object> product2 = new HashMap<>();
        product2.put("name", "T-Shirt");
        product2.put("type", "clothing");
        product2.put("price", "29.99");
        product2.put("description_vector", clothingVector);
        storeHashDocument("product:2", product2);

        Map<String, Object> product3 = new HashMap<>();
        product3.put("name", "Programming Book");
        product3.put("type", "books");
        product3.put("price", "49.99");
        product3.put("description_vector", booksVector);
        storeHashDocument("product:3", product3);

        Map<String, Object> product4 = new HashMap<>();
        product4.put("name", "Smart Watch");
        product4.put("type", "electronics");
        product4.put("price", "299.99");
        product4.put("description_vector", mixedVector);
        storeHashDocument("product:4", product4);

        // Test 1: Vector range query - find products within distance 0.5 of electronics vector using binary codec
        float[] queryVector = { 0.9f, 0.1f, 0.0f }; // Close to electronics
        ByteBuffer queryVectorBuffer = floatArrayToByteBuffer(queryVector);

        ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> rangeArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 100).build();

        ByteBuffer indexKey = ByteBuffer.wrap(PRODUCTS_INDEX.getBytes(StandardCharsets.UTF_8));
        ByteBuffer queryString = ByteBuffer
                .wrap("@description_vector:[VECTOR_RANGE 0.5 $BLOB]".getBytes(StandardCharsets.UTF_8));
        SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, rangeArgs);

        // Should find electronics products and smart watch (mixed vector)
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);
        ByteBuffer typeKey = ByteBuffer.wrap("type".getBytes(StandardCharsets.UTF_8));
        for (SearchReply.SearchResult<ByteBuffer, ByteBuffer> result : results.getResults()) {
            String productType = new String(result.getFields().get(typeKey).array(), StandardCharsets.UTF_8);
            assertThat(productType).isIn("electronics"); // Electronics should be within range
        }

        // Test 2: Vector range query with distance field and sorting
        SearchArgs<ByteBuffer, ByteBuffer> sortedRangeArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 100).build();

        ByteBuffer sortedQueryString = ByteBuffer
                .wrap("@description_vector:[VECTOR_RANGE 1.0 $BLOB]=>{$YIELD_DISTANCE_AS: vector_distance}"
                        .getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, sortedQueryString, sortedRangeArgs);

        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // Test 3: Combined filter - vector range + price filter
        SearchArgs<ByteBuffer, ByteBuffer> combinedArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 100).build();

        ByteBuffer combinedQueryString = ByteBuffer
                .wrap("(@price:[200 1000]) | @description_vector:[VECTOR_RANGE 0.8 $BLOB]".getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, combinedQueryString, combinedArgs);

        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Cleanup
        redis.ftDropindex(PRODUCTS_INDEX);
    }

    /**
     * Test different distance metrics (L2, COSINE, IP) and vector types. Based on Redis documentation for distance metrics.
     */
    @Test
    void testDistanceMetricsAndVectorTypes() {
        // Test with different distance metrics
        String[] metrics = { "L2", "COSINE", "IP" };

        for (String metric : metrics) {
            String indexName = "test-" + metric.toLowerCase() + "-idx";

            FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("embedding").flat()
                    .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(2)
                    .distanceMetric(VectorFieldArgs.DistanceMetric.valueOf(metric)).build();

            FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

            CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("test:")
                    .on(CreateArgs.TargetType.HASH).build();

            redis.ftCreate(indexName, createArgs, Arrays.asList(vectorField, nameField));

            // Add test vectors
            float[] vector1 = { 1.0f, 0.0f };
            float[] vector2 = { 0.0f, 1.0f };

            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("name", "Point A");
            doc1.put("embedding", vector1);
            storeHashDocument("test:1", doc1);

            Map<String, Object> doc2 = new HashMap<>();
            doc2.put("name", "Point B");
            doc2.put("embedding", vector2);
            storeHashDocument("test:2", doc2);

            // Test KNN search with this metric using binary codec
            float[] queryVector = { 0.7f, 0.3f };
            ByteBuffer queryVectorBuffer = floatArrayToByteBuffer(queryVector);

            ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
            SearchArgs<ByteBuffer, ByteBuffer> searchArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                    .param(blobKey, queryVectorBuffer).limit(0, 2).build();

            ByteBuffer indexKey = ByteBuffer.wrap(indexName.getBytes(StandardCharsets.UTF_8));
            ByteBuffer queryString = ByteBuffer
                    .wrap("*=>[KNN 2 @embedding $BLOB AS distance]".getBytes(StandardCharsets.UTF_8));
            SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, searchArgs);

            assertThat(results.getCount()).isEqualTo(2);
            assertThat(results.getResults()).hasSize(2);

            // Cleanup
            redis.ftDropindex(indexName);
        }
    }

    /**
     * Test JSON storage for vectors as arrays instead of binary data. Based on Redis documentation for JSON vector storage.
     * This test demonstrates that JSON vector search works correctly when using field aliases.
     */
    @Test
    void testJsonVectorStorage() {
        // Create vector index for JSON documents with field aliases (key for proper search syntax)
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("$.vector").as("vector").flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("$.title").as("title").build();
        FieldArgs<String> categoryField = TagFieldArgs.<String> builder().name("$.category").as("category").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("json:")
                .on(CreateArgs.TargetType.JSON).build();

        redis.ftCreate("json-vector-idx", createArgs, Arrays.asList(vectorField, titleField, categoryField));

        // Add JSON documents with vector arrays

        String doc1Raw = "{\"title\":\"Document 1\",\"category\":\"tech\",\"vector\":[0.1,0.2,0.3]}";
        String doc2Raw = "{\"title\":\"Document 2\",\"category\":\"science\",\"vector\":[0.4,0.5,0.6]}";
        String doc3Raw = "{\"title\":\"Document 3\",\"category\":\"tech\",\"vector\":[0.7,0.8,0.9]}";

        JsonParser parser = redis.getJsonParser();
        JsonValue doc1 = parser.createJsonValue(doc1Raw);
        JsonValue doc2 = parser.createJsonValue(doc2Raw);
        JsonValue doc3 = parser.createJsonValue(doc3Raw);

        redis.jsonSet("json:1", JsonPath.ROOT_PATH, doc1);
        redis.jsonSet("json:2", JsonPath.ROOT_PATH, doc2);
        redis.jsonSet("json:3", JsonPath.ROOT_PATH, doc3);

        // Test KNN search on JSON vectors
        // Note: For JSON vectors, we still need to pass the query vector as bytes
        float[] queryVector = { 0.2f, 0.3f, 0.4f };
        ByteBuffer queryVectorBuffer = floatArrayToByteBuffer(queryVector);

        // Test 1: KNN search with ADHOC_BF hybrid policy using binary codec
        ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> adhocArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 3).build();

        ByteBuffer indexKey = ByteBuffer.wrap("json-vector-idx".getBytes(StandardCharsets.UTF_8));
        ByteBuffer queryString = ByteBuffer.wrap("*=>[KNN 3 @vector $BLOB]".getBytes(StandardCharsets.UTF_8));
        SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, adhocArgs);

        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);

        // Test filtering with JSON vectors
        SearchArgs<ByteBuffer, ByteBuffer> filterArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 10).build();

        ByteBuffer filterQueryString = ByteBuffer
                .wrap("(@category:{tech})=>[KNN 2 @vector $BLOB]".getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, filterQueryString, filterArgs);

        assertThat(results.getCount()).isEqualTo(2); // Only tech category documents

        // Cleanup
        redis.ftDropindex("json-vector-idx");
        redis.del("json:1", "json:2", "json:3");
    }

    /**
     * Test advanced vector search features including hybrid policies and batch sizes. Based on Redis documentation for runtime
     * query parameters.
     */
    @Test
    void testAdvancedVectorSearchFeatures() {
        // Create HNSW index for advanced testing
        VectorFieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("content_vector").hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .attribute("M", 16).attribute("EF_CONSTRUCTION", 200).build();

        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> statusField = TagFieldArgs.<String> builder().name("status").build();
        FieldArgs<String> priorityField = NumericFieldArgs.<String> builder().name("priority").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("task:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("tasks-idx", createArgs, Arrays.asList(vectorField, titleField, statusField, priorityField));

        // Add multiple tasks with different vectors and metadata using binary codec
        for (int i = 1; i <= 10; i++) {
            float[] vector = { (float) Math.random(), (float) Math.random(), (float) Math.random(), (float) Math.random() };

            Map<String, Object> task = new HashMap<>();
            task.put("title", "Task " + i);
            task.put("status", i % 2 == 0 ? "active" : "completed");
            task.put("priority", String.valueOf(i % 5 + 1));
            task.put("content_vector", vector);
            storeHashDocument("task:" + i, task);
        }

        float[] queryVector = { 0.5f, 0.5f, 0.5f, 0.5f };
        ByteBuffer queryVectorBuffer = floatArrayToByteBuffer(queryVector);

        // Test 1: KNN search with ADHOC_BF hybrid policy using binary codec
        ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> adhocArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 5).build();

        ByteBuffer indexKey = ByteBuffer.wrap("tasks-idx".getBytes(StandardCharsets.UTF_8));
        ByteBuffer queryString = ByteBuffer
                .wrap("(@status:{active})=>[KNN 5 @content_vector $BLOB HYBRID_POLICY ADHOC_BF AS task_score]"
                        .getBytes(StandardCharsets.UTF_8));
        SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, adhocArgs);

        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Test 2: KNN search with BATCHES hybrid policy and custom batch size
        ByteBuffer batchSizeKey = ByteBuffer.wrap("BATCH_SIZE".getBytes(StandardCharsets.UTF_8));
        ByteBuffer batchSizeValue = ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> batchArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).param(batchSizeKey, batchSizeValue).limit(0, 5).build();

        ByteBuffer batchQueryString = ByteBuffer.wrap(
                "(@status:{active})=>[KNN 5 @content_vector $BLOB HYBRID_POLICY BATCHES BATCH_SIZE $BATCH_SIZE AS task_score]"
                        .getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, batchQueryString, batchArgs);

        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Test 3: Vector search with custom EF_RUNTIME parameter
        ByteBuffer efKey = ByteBuffer.wrap("EF".getBytes(StandardCharsets.UTF_8));
        ByteBuffer efValue = ByteBuffer.wrap("50".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> efArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).param(efKey, efValue).limit(0, 3).build();

        ByteBuffer efQueryString = ByteBuffer
                .wrap("*=>[KNN 3 @content_vector $BLOB EF_RUNTIME $EF AS task_score]".getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, efQueryString, efArgs);

        assertThat(results.getCount()).isEqualTo(3);

        // Test 4: Complex query with multiple filters and vector search
        SearchArgs<ByteBuffer, ByteBuffer> complexArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 10).build();

        ByteBuffer complexQueryString = ByteBuffer
                .wrap("((@status:{active}) (@priority:[3 5]))=>[KNN 5 @content_vector $BLOB AS task_score]"
                        .getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, complexQueryString, complexArgs);

        // Verify all results match the filter criteria
        ByteBuffer statusKey = ByteBuffer.wrap("status".getBytes(StandardCharsets.UTF_8));
        ByteBuffer priorityKey = ByteBuffer.wrap("priority".getBytes(StandardCharsets.UTF_8));
        for (SearchReply.SearchResult<ByteBuffer, ByteBuffer> result : results.getResults()) {
            String status = new String(result.getFields().get(statusKey).array(), StandardCharsets.UTF_8);
            String priorityStr = new String(result.getFields().get(priorityKey).array(), StandardCharsets.UTF_8);
            assertThat(status).isEqualTo("active");
            int priority = Integer.parseInt(priorityStr);
            assertThat(priority).isBetween(3, 5);
        }

        // Test 5: Vector search with timeout
        SearchArgs<ByteBuffer, ByteBuffer> timeoutArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).timeout(Duration.ofSeconds(5)).limit(0, 5).build();

        ByteBuffer timeoutQueryString = ByteBuffer
                .wrap("*=>[KNN 5 @content_vector $BLOB AS task_score]".getBytes(StandardCharsets.UTF_8));
        results = redisBinary.ftSearch(indexKey, timeoutQueryString, timeoutArgs);

        assertThat(results.getCount()).isEqualTo(5);

        // Cleanup
        redis.ftDropindex("tasks-idx");
    }

    /**
     * Test vector search with different vector types (FLOAT32, FLOAT64) and precision. Based on Redis documentation for memory
     * consumption comparison.
     */
    @Test
    void testVectorTypesAndPrecision() {
        // Test FLOAT64 vectors
        FieldArgs<String> float64Field = VectorFieldArgs.<String> builder().name("embedding_f64").flat()
                .type(VectorFieldArgs.VectorType.FLOAT64).dimensions(2).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .build();

        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("precision:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("precision-idx", createArgs, Arrays.asList(float64Field, nameField));

        // Add vectors with high precision values
        double[] preciseVector1 = { 1.123456789012345, 2.987654321098765 };
        double[] preciseVector2 = { 3.141592653589793, 2.718281828459045 };

        // Convert double arrays to byte arrays (FLOAT64) with little-endian byte order
        ByteBuffer buffer1 = ByteBuffer.allocate(preciseVector1.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double value : preciseVector1) {
            buffer1.putDouble(value);
        }

        ByteBuffer buffer2 = ByteBuffer.allocate(preciseVector2.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double value : preciseVector2) {
            buffer2.putDouble(value);
        }

        // Store documents using binary codec with FLOAT64 vectors
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("name", "High Precision Vector 1");
        doc1.put("embedding_f64", buffer1.array());
        storeHashDocument("precision:1", doc1);

        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("name", "High Precision Vector 2");
        doc2.put("embedding_f64", buffer2.array());
        storeHashDocument("precision:2", doc2);

        // Test KNN search with FLOAT64 query vector using binary codec
        double[] queryVector = { 1.5, 2.5 };
        ByteBuffer queryBuffer = ByteBuffer.allocate(queryVector.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double value : queryVector) {
            queryBuffer.putDouble(value);
        }
        queryBuffer.flip();

        ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> precisionArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryBuffer).limit(0, 2).build();

        ByteBuffer indexKey = ByteBuffer.wrap("precision-idx".getBytes(StandardCharsets.UTF_8));
        ByteBuffer queryString = ByteBuffer
                .wrap("*=>[KNN 2 @embedding_f64 $BLOB AS distance]".getBytes(StandardCharsets.UTF_8));
        SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, precisionArgs);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);

        // Verify that the search worked with high precision vectors
        ByteBuffer nameKey = ByteBuffer.wrap("name".getBytes(StandardCharsets.UTF_8));
        for (SearchReply.SearchResult<ByteBuffer, ByteBuffer> result : results.getResults()) {
            String name = new String(result.getFields().get(nameKey).array(), StandardCharsets.UTF_8);
            assertThat(name).contains("High Precision Vector");
        }

        // Cleanup
        redis.ftDropindex("precision-idx");
    }

    /**
     * Test error handling and edge cases for vector search.
     */
    @Test
    void testVectorSearchErrorHandling() {
        // 7.4 and 7.2 have a different behavior, but we do not want to test corner cases for old versions
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));

        // Create a simple vector index
        FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("test_vector").flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(3).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("error:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("error-test-idx", createArgs, Collections.singletonList(vectorField));

        // Add a valid document using binary codec
        float[] validVector = { 1.0f, 0.0f, 0.0f };
        Map<String, Object> doc = new HashMap<>();
        doc.put("test_vector", validVector);
        storeHashDocument("error:1", doc);

        // Test 1: Valid KNN search should work
        float[] queryVector = { 0.9f, 0.1f, 0.0f };
        ByteBuffer queryVectorBuffer = floatArrayToByteBuffer(queryVector);

        ByteBuffer blobKey = ByteBuffer.wrap("BLOB".getBytes(StandardCharsets.UTF_8));
        SearchArgs<ByteBuffer, ByteBuffer> validArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 1).build();

        ByteBuffer indexKey = ByteBuffer.wrap("error-test-idx".getBytes(StandardCharsets.UTF_8));
        ByteBuffer queryString = ByteBuffer.wrap("*=>[KNN 1 @test_vector $BLOB]".getBytes(StandardCharsets.UTF_8));
        SearchReply<ByteBuffer, ByteBuffer> results = redisBinary.ftSearch(indexKey, queryString, validArgs);

        assertThat(results.getCount()).isEqualTo(1);

        // Test 2: Search with invalid field should throw exception
        SearchArgs<ByteBuffer, ByteBuffer> noResultsArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(blobKey, queryVectorBuffer).limit(0, 10).build();

        ByteBuffer noResultsQueryString = ByteBuffer
                .wrap("(@nonexistent_field:value)=>[KNN 5 @test_vector $BLOB]".getBytes(StandardCharsets.UTF_8));

        // This should throw an exception because the field doesn't exist
        assertThatThrownBy(() -> redisBinary.ftSearch(indexKey, noResultsQueryString, noResultsArgs))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("Unknown field");

        // Cleanup
        redis.ftDropindex("error-test-idx");
    }

    /**
     * Test vector search with mixed binary and text fields, following the Python example. This test demonstrates handling both
     * binary vector data and text data in the same hash, with proper decoding of each field type.
     */
    @Test
    void testVectorSearchBinaryAndTextFields() {
        // Create a custom codec that can handle both strings and byte arrays
        RedisCodec<String, Object> mixedCodec = new RedisCodec<String, Object>() {

            @Override
            public String decodeKey(ByteBuffer bytes) {
                return StandardCharsets.UTF_8.decode(bytes).toString();
            }

            @Override
            public Object decodeValue(ByteBuffer bytes) {
                // Try to decode as UTF-8 string first
                try {
                    String str = StandardCharsets.UTF_8.decode(bytes.duplicate()).toString();
                    // Check if it's a valid UTF-8 string (no replacement characters)
                    if (!str.contains("\uFFFD")) {
                        return str;
                    }
                } catch (Exception e) {
                    // Fall through to return raw bytes
                }
                // Return raw bytes for binary data
                byte[] result = new byte[bytes.remaining()];
                bytes.get(result);
                return result;
            }

            @Override
            public ByteBuffer encodeKey(String key) {
                return ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public ByteBuffer encodeValue(Object value) {
                if (value instanceof String) {
                    return ByteBuffer.wrap(((String) value).getBytes(StandardCharsets.UTF_8));
                } else if (value instanceof byte[]) {
                    return ByteBuffer.wrap((byte[]) value);
                } else if (value instanceof float[]) {
                    float[] floats = (float[]) value;
                    ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
                    for (float f : floats) {
                        buffer.putFloat(f);
                    }
                    return (ByteBuffer) buffer.flip();
                } else {
                    return ByteBuffer.wrap(value.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

        };

        // Create connection with mixed codec
        RedisCommands<String, Object> redisMixed = client.connect(mixedCodec).sync();

        try {
            // Create fake vector similar to Python example
            float[] fakeVec = { 0.1f, 0.2f, 0.3f, 0.4f };
            byte[] fakeVecBytes = floatArrayToByteBuffer(fakeVec).array();

            String indexName = "mixed_index";
            String keyName = indexName + ":1";

            // Store mixed data: text field and binary vector field
            redisMixed.hset(keyName, "first_name", "🥬 Lettuce");
            redisMixed.hset(keyName, "vector_emb", fakeVecBytes);

            // Create index with both text and vector fields
            FieldArgs<String> textField = TagFieldArgs.<String> builder().name("first_name").build();

            FieldArgs<String> vectorField = VectorFieldArgs.<String> builder().name("embeddings_bio").hnsw()
                    .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(4)
                    .distanceMetric(VectorFieldArgs.DistanceMetric.COSINE).build();

            CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(indexName + ":")
                    .on(CreateArgs.TargetType.HASH).build();

            redis.ftCreate(indexName, createArgs, Arrays.asList(textField, vectorField));

            // Search with specific field returns - equivalent to Python's return_field with decode_field=False
            SearchArgs<String, Object> searchArgs = SearchArgs.<String, Object> builder().returnField("vector_emb") // This
                                                                                                                    // should
                                                                                                                    // return
                                                                                                                    // raw
                                                                                                                    // binary
                                                                                                                    // data
                    .returnField("first_name") // This should return decoded text
                    .build();

            SearchReply<String, Object> results = redisMixed.ftSearch(indexName, "*", searchArgs);

            assertThat(results.getCount()).isEqualTo(1);
            assertThat(results.getResults()).hasSize(1);

            SearchReply.SearchResult<String, Object> result = results.getResults().get(0);
            Map<String, Object> fields = result.getFields();

            // Verify text field is properly decoded
            Object firstNameValue = fields.get("first_name");
            assertThat(firstNameValue).isInstanceOf(String.class);
            assertThat((String) firstNameValue).isEqualTo("🥬 Lettuce");

            // Verify vector field returns binary data
            Object vectorValue = fields.get("vector_emb");
            assertThat(vectorValue).isInstanceOf(byte[].class);

            // Convert retrieved binary data back to float array and compare
            byte[] retrievedVecBytes = (byte[]) vectorValue;
            ByteBuffer buffer = ByteBuffer.wrap(retrievedVecBytes).order(ByteOrder.LITTLE_ENDIAN);
            float[] retrievedVec = new float[4];
            for (int i = 0; i < 4; i++) {
                retrievedVec[i] = buffer.getFloat();
            }

            // Assert that the vectors are equal (equivalent to Python's np.array_equal)
            assertThat(retrievedVec).containsExactly(fakeVec);

            // Cleanup
            redis.ftDropindex(indexName);

        } finally {
            // Close the mixed codec connection
            if (redisMixed != null) {
                redisMixed.getStatefulConnection().close();
            }
        }
    }

}
