/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.bytearraycodec;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.protocol.DecodeBufferPolicies;
import io.lettuce.core.search.HybridReply;
import io.lettuce.core.search.Suggestion;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.ExplainArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.QueryDialects;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SortByArgs;
import io.lettuce.core.search.arguments.SugAddArgs;
import io.lettuce.core.search.arguments.SugGetArgs;
import io.lettuce.core.search.arguments.SynUpdateArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.core.search.arguments.VectorFieldArgs;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.arguments.hybrid.Combiners;
import io.lettuce.core.search.arguments.hybrid.HybridArgs;
import io.lettuce.core.search.arguments.hybrid.HybridSearchArgs;
import io.lettuce.core.search.arguments.hybrid.HybridVectorArgs;
import io.lettuce.core.search.arguments.hybrid.PostProcessingArgs;
import io.lettuce.core.search.aggregateutils.Apply;
import io.lettuce.core.search.aggregateutils.Filter;
import io.lettuce.core.search.aggregateutils.GroupBy;
import io.lettuce.core.search.aggregateutils.Limit;
import io.lettuce.core.search.aggregateutils.Reducers;
import io.lettuce.core.search.aggregateutils.SortBy;
import io.lettuce.core.search.aggregateutils.SortDirection;
import io.lettuce.core.search.aggregateutils.SortProperty;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for Redis Search functionality using {@link ByteArrayCodec}.
 * <p>
 * These tests are based on the examples from the Redis documentation: -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/schema-definition/">...</a> -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">...</a>
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchByteArrayCodecIntegrationTests {

    // Index names
    private static final String BLOG_INDEX = "blog-idx";

    private static final String BOOKS_INDEX = "books-idx";

    private static final String PRODUCTS_INDEX = "products-idx";

    private static final String MOVIES_INDEX = "movies-idx";

    // Prefixes
    private static final byte[] BLOG_PREFIX = "blog:post:".getBytes();

    private static final byte[] BOOK_PREFIX = "book:details:".getBytes();

    private static final byte[] PRODUCT_PREFIX = "product:".getBytes();

    private static final byte[] MOVIE_PREFIX = "movie:".getBytes();

    protected static RedisClient client;

    protected static RedisCommands<byte[], byte[]> redis;

    public RediSearchByteArrayCodecIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect(ByteArrayCodec.INSTANCE).sync();
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
     * Test basic text search functionality based on the blog post example from Redis documentation. Creates an index with TEXT,
     * NUMERIC, and TAG fields and performs various search operations.
     */
    @Test
    void testBasicTextSearchWithBlogPosts() {
        // Create index based on Redis documentation example:
        // FT.CREATE idx ON HASH PREFIX 1 blog:post: SCHEMA title TEXT WEIGHT 5.0 content TEXT author TAG created_date NUMERIC
        // SORTABLE views NUMERIC
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).weight(5).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
        FieldArgs<byte[]> authorField = TagFieldArgs.<byte[]> builder().name("author".getBytes()).build();
        FieldArgs<byte[]> createdDateField = NumericFieldArgs.<byte[]> builder().name("created_date".getBytes()).sortable()
                .build();
        FieldArgs<byte[]> viewsField = NumericFieldArgs.<byte[]> builder().name("views".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(BLOG_INDEX, createArgs,
                Arrays.asList(titleField, contentField, authorField, createdDateField, viewsField));
        assertThat(result).isEqualTo("OK");

        // Add sample blog posts
        Map<byte[], byte[]> post1 = new HashMap<>();
        post1.put("title".getBytes(), "Redis Search Tutorial".getBytes());
        post1.put("content".getBytes(), "Learn how to use Redis Search for full-text search capabilities".getBytes());
        post1.put("author".getBytes(), "john_doe".getBytes());
        post1.put("created_date".getBytes(), "1640995200".getBytes());
        post1.put("views".getBytes(), "150".getBytes());
        assertThat(redis.hmset("blog:post:1".getBytes(), post1)).isEqualTo("OK");

        Map<byte[], byte[]> post2 = new HashMap<>();
        post2.put("title".getBytes(), "Advanced Redis Techniques".getBytes());
        post2.put("content".getBytes(), "Explore advanced Redis features and optimization techniques".getBytes());
        post2.put("author".getBytes(), "jane_smith".getBytes());
        post2.put("created_date".getBytes(), "1641081600".getBytes());
        post2.put("views".getBytes(), "200".getBytes());
        assertThat(redis.hmset("blog:post:2".getBytes(), post2)).isEqualTo("OK");

        Map<byte[], byte[]> post3 = new HashMap<>();
        post3.put("title".getBytes(), "Database Performance".getBytes());
        post3.put("content".getBytes(), "Tips for improving database performance and scalability".getBytes());
        post3.put("author".getBytes(), "john_doe".getBytes());
        post3.put("created_date".getBytes(), "1641168000".getBytes());
        post3.put("views".getBytes(), "75".getBytes());
        assertThat(redis.hmset("blog:post:3".getBytes(), post3)).isEqualTo("OK");

        // Test 1: Basic text search
        SearchReply<byte[], byte[]> searchReply = redis.ftSearch(BLOG_INDEX, "@title:(Redis)".getBytes());
        assertThat(searchReply.getCount()).isEqualTo(2);
        assertThat(searchReply.getResults()).hasSize(2);
        assertThat(new String(getField(searchReply.getResults().get(1).getFields(), "title")))
                .isEqualTo("Redis Search Tutorial");
        assertThat(new String(getField(searchReply.getResults().get(0).getFields(), "title")))
                .isEqualTo("Advanced Redis Techniques");
        assertThat(new String(getField(searchReply.getResults().get(1).getFields(), "author"))).isEqualTo("john_doe");
        assertThat(new String(getField(searchReply.getResults().get(0).getFields(), "author"))).isEqualTo("jane_smith");

        // Test 2: Search with field-specific query
        SearchArgs<byte[], byte[]> titleSearchArgs = SearchArgs.<byte[], byte[]> builder().build();
        searchReply = redis.ftSearch(BLOG_INDEX, "@title:Redis".getBytes(), titleSearchArgs);
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Test 3: Tag search
        searchReply = redis.ftSearch(BLOG_INDEX, "@author:{john_doe}".getBytes());
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Test 4: Numeric range search
        searchReply = redis.ftSearch(BLOG_INDEX, "@views:[100 300]".getBytes());
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Cleanup
        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test search options like INFIELDS, RETURN, LIMIT.
     */
    @Test
    void testSearchArgsWithByteArrayParams() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
        FieldArgs<byte[]> authorField = TagFieldArgs.<byte[]> builder().name("author".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, contentField, authorField));

        // Add documents
        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("title".getBytes(), "Redis Guide".getBytes());
        doc1.put("content".getBytes(), "Complete Redis guide".getBytes());
        doc1.put("author".getBytes(), "alice".getBytes());
        redis.hmset("blog:post:1".getBytes(), doc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "Database Book".getBytes());
        doc2.put("content".getBytes(), "Redis and databases".getBytes());
        doc2.put("author".getBytes(), "bob".getBytes());
        redis.hmset("blog:post:2".getBytes(), doc2);

        // Test inField - search only in title field
        SearchArgs<byte[], byte[]> inFieldArgs = SearchArgs.<byte[], byte[]> builder().inField("title".getBytes()).build();
        SearchReply<byte[], byte[]> inFieldResults = redis.ftSearch(BLOG_INDEX, "Redis".getBytes(), inFieldArgs);
        assertThat(inFieldResults.getCount()).isEqualTo(1); // Only doc1 has "Redis" in title

        // Test returnField - return only specific fields
        SearchArgs<byte[], byte[]> returnFieldArgs = SearchArgs.<byte[], byte[]> builder().returnField("title".getBytes())
                .returnField("author".getBytes()).build();
        SearchReply<byte[], byte[]> returnResults = redis.ftSearch(BLOG_INDEX, "*".getBytes(), returnFieldArgs);
        assertThat(returnResults.getCount()).isEqualTo(2);
        // Content field should not be returned
        Map<byte[], byte[]> fields = returnResults.getResults().get(0).getFields();
        assertThat(getField(fields, "content")).isNull();
        assertThat(getField(fields, "title")).isNotNull();

        // Test limit
        SearchArgs<byte[], byte[]> limitArgs = SearchArgs.<byte[], byte[]> builder().limit(0, 1).build();
        SearchReply<byte[], byte[]> limitResults = redis.ftSearch(BLOG_INDEX, "*".getBytes(), limitArgs);
        assertThat(limitResults.getCount()).isEqualTo(2); // Total count is still 2
        assertThat(limitResults.getResults()).hasSize(1); // But only 1 result returned

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test search with PARAMS option using byte[] keys and values.
     */
    @Test
    void testSearchArgsWithParams() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, priceField));

        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("title".getBytes(), "Cheap Item".getBytes());
        doc1.put("price".getBytes(), "50".getBytes());
        redis.hmset("blog:post:1".getBytes(), doc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "Expensive Item".getBytes());
        doc2.put("price".getBytes(), "500".getBytes());
        redis.hmset("blog:post:2".getBytes(), doc2);

        // Search using params with byte[] key and value
        SearchArgs<byte[], byte[]> paramsArgs = SearchArgs.<byte[], byte[]> builder()
                .param("maxprice".getBytes(), "100".getBytes()).build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(BLOG_INDEX, "@price:[0 $maxprice]".getBytes(), paramsArgs);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "title"))).isEqualTo("Cheap Item");

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test FT.DROPINDEX command.
     */
    @Test
    void testDropIndex() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField));

        String result = redis.ftDropindex(BLOG_INDEX);
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Test search options like WITHSCORES, NOCONTENT, LIMIT, SORTBY, RETURN fields.
     */
    @Test
    void testSearchOptionsAndModifiers() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).sortable().build();
        FieldArgs<byte[]> ratingField = NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(MOVIE_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(MOVIES_INDEX, createArgs, Arrays.asList(titleField, ratingField));

        Map<byte[], byte[]> movie1 = new HashMap<>();
        movie1.put("title".getBytes(), "The Matrix".getBytes());
        movie1.put("rating".getBytes(), "8.7".getBytes());
        redis.hmset("movie:1".getBytes(), movie1);

        Map<byte[], byte[]> movie2 = new HashMap<>();
        movie2.put("title".getBytes(), "Matrix Reloaded".getBytes());
        movie2.put("rating".getBytes(), "7.2".getBytes());
        redis.hmset("movie:2".getBytes(), movie2);

        Map<byte[], byte[]> movie3 = new HashMap<>();
        movie3.put("title".getBytes(), "Matrix Revolutions".getBytes());
        movie3.put("rating".getBytes(), "6.8".getBytes());
        redis.hmset("movie:3".getBytes(), movie3);

        // WITHSCORES
        SearchArgs<byte[], byte[]> withScoresArgs = SearchArgs.<byte[], byte[]> builder().withScores().build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(MOVIES_INDEX, "Matrix".getBytes(), withScoresArgs);
        assertThat(results.getCount()).isEqualTo(3);
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(result.getScore()).isNotNull();
            assertThat(result.getScore()).isGreaterThan(0.0);
        }

        // NOCONTENT
        SearchArgs<byte[], byte[]> noContentArgs = SearchArgs.<byte[], byte[]> builder().noContent().build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix".getBytes(), noContentArgs);
        assertThat(results.getCount()).isEqualTo(3);
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(result.getFields()).isEmpty();
        }

        // LIMIT
        SearchArgs<byte[], byte[]> limitArgs = SearchArgs.<byte[], byte[]> builder().limit(0, 2).build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix".getBytes(), limitArgs);
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(2);

        // SORTBY
        SortByArgs<byte[]> sortByArgs = SortByArgs.<byte[]> builder().attribute("rating".getBytes()).descending().build();
        SearchArgs<byte[], byte[]> sortArgs = SearchArgs.<byte[], byte[]> builder().sortBy(sortByArgs).build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix".getBytes(), sortArgs);
        assertThat(results.getCount()).isEqualTo(3);
        double previousRating = Double.MAX_VALUE;
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            double currentRating = Double.parseDouble(new String(getField(result.getFields(), "rating")));
            assertThat(currentRating).isLessThanOrEqualTo(previousRating);
            previousRating = currentRating;
        }

        // RETURN fields
        SearchArgs<byte[], byte[]> returnArgs = SearchArgs.<byte[], byte[]> builder().returnField("title".getBytes()).build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix".getBytes(), returnArgs);
        assertThat(results.getCount()).isEqualTo(3);
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(getField(result.getFields(), "title")).isNotNull();
            assertThat(getField(result.getFields(), "rating")).isNull();
        }

        redis.ftDropindex(MOVIES_INDEX);
    }

    /**
     * Test TAG fields with custom separators.
     */
    @Test
    void testTagFieldsWithCustomSeparator() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> categoriesField = TagFieldArgs.<byte[]> builder().name("categories".getBytes()).separator(";")
                .build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BOOK_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BOOKS_INDEX, createArgs, Arrays.asList(titleField, categoriesField));

        Map<byte[], byte[]> book1 = new HashMap<>();
        book1.put("title".getBytes(), "Redis in Action".getBytes());
        book1.put("categories".getBytes(), "programming;databases;nosql".getBytes());
        redis.hmset("book:details:1".getBytes(), book1);

        Map<byte[], byte[]> book2 = new HashMap<>();
        book2.put("title".getBytes(), "Database Design Patterns".getBytes());
        book2.put("categories".getBytes(), "databases;design;architecture".getBytes());
        redis.hmset("book:details:2".getBytes(), book2);

        Map<byte[], byte[]> book3 = new HashMap<>();
        book3.put("title".getBytes(), "NoSQL Distilled".getBytes());
        book3.put("categories".getBytes(), "nosql;databases;theory".getBytes());
        redis.hmset("book:details:3".getBytes(), book3);

        SearchReply<byte[], byte[]> results = redis.ftSearch(BOOKS_INDEX, "@categories:{databases}".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        results = redis.ftSearch(BOOKS_INDEX, "@categories:{nosql}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(BOOKS_INDEX, "@categories:{programming}".getBytes());
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "title"))).isEqualTo("Redis in Action");

        results = redis.ftSearch(BOOKS_INDEX, "@categories:{programming|design}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        redis.ftDropindex(BOOKS_INDEX);
    }

    /**
     * Test numeric field operations and range queries.
     */
    @Test
    void testNumericFieldOperations() {
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build();
        FieldArgs<byte[]> stockField = NumericFieldArgs.<byte[]> builder().name("stock".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(PRODUCT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(PRODUCTS_INDEX, createArgs, Arrays.asList(nameField, priceField, stockField));

        Map<byte[], byte[]> product1 = new HashMap<>();
        product1.put("name".getBytes(), "Laptop".getBytes());
        product1.put("price".getBytes(), "999.99".getBytes());
        product1.put("stock".getBytes(), "15".getBytes());
        redis.hmset("product:1".getBytes(), product1);

        Map<byte[], byte[]> product2 = new HashMap<>();
        product2.put("name".getBytes(), "Mouse".getBytes());
        product2.put("price".getBytes(), "29.99".getBytes());
        product2.put("stock".getBytes(), "100".getBytes());
        redis.hmset("product:2".getBytes(), product2);

        Map<byte[], byte[]> product3 = new HashMap<>();
        product3.put("name".getBytes(), "Keyboard".getBytes());
        product3.put("price".getBytes(), "79.99".getBytes());
        product3.put("stock".getBytes(), "50".getBytes());
        redis.hmset("product:3".getBytes(), product3);

        Map<byte[], byte[]> product4 = new HashMap<>();
        product4.put("name".getBytes(), "Monitor".getBytes());
        product4.put("price".getBytes(), "299.99".getBytes());
        product4.put("stock".getBytes(), "25".getBytes());
        redis.hmset("product:4".getBytes(), product4);

        SearchReply<byte[], byte[]> results = redis.ftSearch(PRODUCTS_INDEX, "@price:[50 500]".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[100 +inf]".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[-inf 100]".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[29.99 29.99]".getBytes());
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "name"))).isEqualTo("Mouse");

        results = redis.ftSearch(PRODUCTS_INDEX, "@stock:[20 60]".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[50 +inf] @stock:[20 +inf]".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        redis.ftDropindex(PRODUCTS_INDEX);
    }

    /**
     * Test advanced search features like INKEYS, INFIELDS, TIMEOUT, and PARAMS.
     */
    @Test
    void testAdvancedSearchFeatures() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
        FieldArgs<byte[]> categoryField = TagFieldArgs.<byte[]> builder().name("category".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, contentField, categoryField));

        Map<byte[], byte[]> post1 = new HashMap<>();
        post1.put("title".getBytes(), "Redis Tutorial".getBytes());
        post1.put("content".getBytes(), "Learn Redis basics".getBytes());
        post1.put("category".getBytes(), "tutorial".getBytes());
        redis.hmset("blog:post:1".getBytes(), post1);

        Map<byte[], byte[]> post2 = new HashMap<>();
        post2.put("title".getBytes(), "Advanced Redis".getBytes());
        post2.put("content".getBytes(), "Advanced Redis techniques".getBytes());
        post2.put("category".getBytes(), "advanced".getBytes());
        redis.hmset("blog:post:2".getBytes(), post2);

        Map<byte[], byte[]> post3 = new HashMap<>();
        post3.put("title".getBytes(), "Database Guide".getBytes());
        post3.put("content".getBytes(), "Database best practices".getBytes());
        post3.put("category".getBytes(), "tutorial".getBytes());
        redis.hmset("blog:post:3".getBytes(), post3);

        // INKEYS
        SearchArgs<byte[], byte[]> inKeysArgs = SearchArgs.<byte[], byte[]> builder().inKey("blog:post:1".getBytes())
                .inKey("blog:post:2".getBytes()).build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(BLOG_INDEX, "Redis".getBytes(), inKeysArgs);
        assertThat(results.getCount()).isEqualTo(2);

        // INFIELDS
        SearchArgs<byte[], byte[]> inFieldsArgs = SearchArgs.<byte[], byte[]> builder().inField("title".getBytes()).build();
        results = redis.ftSearch(BLOG_INDEX, "Redis".getBytes(), inFieldsArgs);
        assertThat(results.getCount()).isEqualTo(2);

        // TIMEOUT
        SearchArgs<byte[], byte[]> timeoutArgs = SearchArgs.<byte[], byte[]> builder().timeout(Duration.ofSeconds(5)).build();
        results = redis.ftSearch(BLOG_INDEX, "Redis".getBytes(), timeoutArgs);
        assertThat(results.getCount()).isEqualTo(2);

        // PARAMS
        SearchArgs<byte[], byte[]> paramsArgs = SearchArgs.<byte[], byte[]> builder()
                .param("category_param".getBytes(), "tutorial".getBytes()).build();
        results = redis.ftSearch(BLOG_INDEX, "@category:{$category_param}".getBytes(), paramsArgs);
        assertThat(results.getCount()).isEqualTo(2);

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test complex queries with boolean operations, wildcards, and phrase matching.
     */
    @Test
    void testComplexQueriesAndBooleanOperations() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> descriptionField = TextFieldArgs.<byte[]> builder().name("description".getBytes()).build();
        FieldArgs<byte[]> tagsField = TagFieldArgs.<byte[]> builder().name("tags".getBytes()).build();
        FieldArgs<byte[]> ratingField = NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(MOVIE_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(MOVIES_INDEX, createArgs, Arrays.asList(titleField, descriptionField, tagsField, ratingField));

        Map<byte[], byte[]> movie1 = new HashMap<>();
        movie1.put("title".getBytes(), "The Matrix".getBytes());
        movie1.put("description".getBytes(), "A computer hacker learns about the true nature of reality".getBytes());
        movie1.put("tags".getBytes(), "sci-fi,action,thriller".getBytes());
        movie1.put("rating".getBytes(), "8.7".getBytes());
        redis.hmset("movie:1".getBytes(), movie1);

        Map<byte[], byte[]> movie2 = new HashMap<>();
        movie2.put("title".getBytes(), "Matrix Reloaded".getBytes());
        movie2.put("description".getBytes(), "Neo and the rebel leaders estimate they have 72 hours".getBytes());
        movie2.put("tags".getBytes(), "sci-fi,action".getBytes());
        movie2.put("rating".getBytes(), "7.2".getBytes());
        redis.hmset("movie:2".getBytes(), movie2);

        Map<byte[], byte[]> movie3 = new HashMap<>();
        movie3.put("title".getBytes(), "Inception".getBytes());
        movie3.put("description".getBytes(),
                "A thief who steals corporate secrets through dream-sharing technology".getBytes());
        movie3.put("tags".getBytes(), "sci-fi,thriller,drama".getBytes());
        movie3.put("rating".getBytes(), "8.8".getBytes());
        redis.hmset("movie:3".getBytes(), movie3);

        Map<byte[], byte[]> movie4 = new HashMap<>();
        movie4.put("title".getBytes(), "The Dark Knight".getBytes());
        movie4.put("description".getBytes(), "Batman faces the Joker in Gotham City".getBytes());
        movie4.put("tags".getBytes(), "action,crime,drama".getBytes());
        movie4.put("rating".getBytes(), "9.0".getBytes());
        redis.hmset("movie:4".getBytes(), movie4);

        // Boolean AND
        SearchReply<byte[], byte[]> results = redis.ftSearch(MOVIES_INDEX, "((@tags:{thriller}) (@tags:{action}))".getBytes());
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "title"))).isEqualTo("The Matrix");

        // Boolean OR
        results = redis.ftSearch(MOVIES_INDEX, "((@tags:{thriller}) | (@tags:{crime}))".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        // Boolean NOT
        results = redis.ftSearch(MOVIES_INDEX, "((@tags:{action}) (-@tags:{thriller}))".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        // Phrase matching
        results = redis.ftSearch(MOVIES_INDEX, "@title:\"Inception\"".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        // Wildcard
        results = redis.ftSearch(MOVIES_INDEX, "Matrix*".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        // Numeric range + text
        results = redis.ftSearch(MOVIES_INDEX, "@rating:[8.0 9.5] @tags:{action}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        // Field-specific OR
        results = redis.ftSearch(MOVIES_INDEX, "@title:(Matrix | Inception)".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        redis.ftDropindex(MOVIES_INDEX);
    }

    /**
     * Test empty search results and edge cases.
     */
    @Test
    void testEmptyResultsAndEdgeCases() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Collections.singletonList(titleField));

        Map<byte[], byte[]> post1 = new HashMap<>();
        post1.put("title".getBytes(), "Redis Tutorial".getBytes());
        redis.hmset("blog:post:1".getBytes(), post1);

        // Non-existent term
        SearchReply<byte[], byte[]> results = redis.ftSearch(BLOG_INDEX, "nonexistent".getBytes());
        assertThat(results.getCount()).isEqualTo(0);
        assertThat(results.getResults()).isEmpty();

        // LIMIT beyond available results
        SearchArgs<byte[], byte[]> limitArgs = SearchArgs.<byte[], byte[]> builder().limit(10, 20).build();
        results = redis.ftSearch(BLOG_INDEX, "Redis".getBytes(), limitArgs);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults()).isEmpty();

        // NOCONTENT + WITHSCORES combined
        SearchArgs<byte[], byte[]> combinedArgs = SearchArgs.<byte[], byte[]> builder().noContent().withScores().build();
        results = redis.ftSearch(BLOG_INDEX, "Redis".getBytes(), combinedArgs);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults()).hasSize(1);
        assertThat(results.getResults().get(0).getFields()).isEmpty();
        assertThat(results.getResults().get(0).getScore()).isNotNull();

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test FT.ALTER command to add new fields to an existing index.
     */
    @Test
    void testFtAlterAddingNewFields() {
        String testIndex = "alter-test-idx";

        List<FieldArgs<byte[]>> initialFields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build());

        assertThat(redis.ftCreate(testIndex, initialFields)).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("title".getBytes(), "Test Document".getBytes());
        redis.hset("doc:1".getBytes(), doc1);

        SearchReply<byte[], byte[]> initialSearch = redis.ftSearch(testIndex, "Test".getBytes());
        assertThat(initialSearch.getCount()).isEqualTo(1);

        List<FieldArgs<byte[]>> newFields = Arrays.asList(
                NumericFieldArgs.<byte[]> builder().name("published_at".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("author".getBytes()).build());

        assertThat(redis.ftAlter(testIndex, false, newFields)).isEqualTo("OK");

        Map<byte[], byte[]> updateDoc1 = new HashMap<>();
        updateDoc1.put("published_at".getBytes(), "1640995200".getBytes());
        updateDoc1.put("author".getBytes(), "John Doe".getBytes());
        redis.hset("doc:1".getBytes(), updateDoc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "Another Document".getBytes());
        doc2.put("published_at".getBytes(), "1641081600".getBytes());
        doc2.put("author".getBytes(), "Jane Smith".getBytes());
        redis.hset("doc:2".getBytes(), doc2);

        SearchReply<byte[], byte[]> searchAfterAlter = redis.ftSearch(testIndex, "Document".getBytes());
        assertThat(searchAfterAlter.getCount()).isEqualTo(2);

        SearchReply<byte[], byte[]> authorSearch = redis.ftSearch(testIndex, "@author:John".getBytes());
        assertThat(authorSearch.getCount()).isEqualTo(1);
        assertThat(new String(authorSearch.getResults().get(0).getId())).isEqualTo("doc:1");

        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.ALTER command with SKIPINITIALSCAN option.
     */
    @Test
    void testFtAlterWithSkipInitialScan() {
        String testIndex = "alter-skip-test-idx";

        List<FieldArgs<byte[]>> initialFields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build());

        assertThat(redis.ftCreate(testIndex, initialFields)).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("title".getBytes(), "Existing Document".getBytes());
        doc1.put("category".getBytes(), "Technology".getBytes());
        redis.hset("doc:1".getBytes(), doc1);

        List<FieldArgs<byte[]>> newFields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        assertThat(redis.ftAlter(testIndex, true, newFields)).isEqualTo("OK");

        SearchReply<byte[], byte[]> categorySearch = redis.ftSearch(testIndex, "@category:Technology".getBytes());
        assertThat(categorySearch.getCount()).isEqualTo(0);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "New Document".getBytes());
        doc2.put("category".getBytes(), "Science".getBytes());
        redis.hset("doc:2".getBytes(), doc2);

        SearchReply<byte[], byte[]> newCategorySearch = redis.ftSearch(testIndex, "@category:Science".getBytes());
        assertThat(newCategorySearch.getCount()).isEqualTo(1);
        assertThat(new String(newCategorySearch.getResults().get(0).getId())).isEqualTo("doc:2");

        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.ALIASADD, FT.ALIASUPDATE, and FT.ALIASDEL commands.
     */
    @Test
    void testFtAliasCommands() {
        String testIndex = "alias-test-idx";
        String testIndex2 = "alias-test-idx2";
        String alias = "test-alias";

        List<FieldArgs<byte[]>> fields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build());

        assertThat(redis.ftCreate(testIndex, fields)).isEqualTo("OK");
        assertThat(redis.ftCreate(testIndex2, fields)).isEqualTo("OK");

        assertThat(redis.ftAliasadd(alias, testIndex)).isEqualTo("OK");

        Map<byte[], byte[]> doc = new HashMap<>();
        doc.put("title".getBytes(), "Test Document".getBytes());
        redis.hset("doc:1".getBytes(), doc);

        SearchReply<byte[], byte[]> aliasSearch = redis.ftSearch(alias, "Test".getBytes());
        assertThat(aliasSearch.getCount()).isEqualTo(1);

        assertThat(redis.ftAliasupdate(alias, testIndex2)).isEqualTo("OK");

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "Different Document".getBytes());
        redis.hset("doc:2".getBytes(), doc2);

        SearchReply<byte[], byte[]> updatedAliasSearch = redis.ftSearch(alias, "Different".getBytes());
        assertThat(updatedAliasSearch.getCount()).isEqualTo(1);
        assertThat(new String(updatedAliasSearch.getResults().get(0).getId())).isEqualTo("doc:2");

        assertThat(redis.ftAliasdel(alias)).isEqualTo("OK");

        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
        assertThat(redis.ftDropindex(testIndex2)).isEqualTo("OK");
    }

    /**
     * Test FT.TAGVALS command to retrieve distinct values from a tag field.
     */
    @Test
    void testFtTagvals() {
        String testIndex = "tagvals-test-idx";

        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TagFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        assertThat(redis.ftCreate(testIndex, fields)).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("title".getBytes(), "Document 1".getBytes());
        doc1.put("category".getBytes(), "Technology".getBytes());
        redis.hset("doc:1".getBytes(), doc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "Document 2".getBytes());
        doc2.put("category".getBytes(), "Science".getBytes());
        redis.hset("doc:2".getBytes(), doc2);

        Map<byte[], byte[]> doc3 = new HashMap<>();
        doc3.put("title".getBytes(), "Document 3".getBytes());
        doc3.put("category".getBytes(), "Technology".getBytes());
        redis.hset("doc:3".getBytes(), doc3);

        Map<byte[], byte[]> doc4 = new HashMap<>();
        doc4.put("title".getBytes(), "Document 4".getBytes());
        doc4.put("category".getBytes(), "Arts".getBytes());
        redis.hset("doc:4".getBytes(), doc4);

        List<byte[]> tagValues = redis.ftTagvals(testIndex, "category");
        List<String> tagStrings = tagValues.stream().map(String::new).collect(Collectors.toList());

        assertThat(tagStrings).hasSize(3);
        assertThat(tagStrings).containsExactlyInAnyOrder("technology", "science", "arts");

        assertThrows(RedisCommandExecutionException.class, () -> redis.ftTagvals(testIndex, "nonexistent"));
        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.SUGADD, FT.SUGGET, FT.SUGDEL, and FT.SUGLEN commands.
     */
    @Test
    void testFtSuggestionCommands() {
        byte[] suggestionKey = "autocomplete:cities".getBytes();

        assertThat(redis.ftSugadd(suggestionKey, "New York".getBytes(), 1.0)).isEqualTo(1L);
        assertThat(redis.ftSugadd(suggestionKey, "New Orleans".getBytes(), 0.8)).isEqualTo(2L);
        assertThat(redis.ftSugadd(suggestionKey, "Newark".getBytes(), 0.6)).isEqualTo(3L);
        assertThat(redis.ftSugadd(suggestionKey, "Boston".getBytes(), 0.9)).isEqualTo(4L);
        assertThat(redis.ftSugadd(suggestionKey, "Barcelona".getBytes(), 0.7)).isEqualTo(5L);

        assertThat(redis.ftSuglen(suggestionKey)).isEqualTo(5L);

        List<Suggestion<byte[]>> suggestions = redis.ftSugget(suggestionKey, "New".getBytes());
        assertThat(suggestions).hasSize(3);
        List<String> values = suggestions.stream().map(s -> new String(s.getValue())).collect(Collectors.toList());
        assertThat(values).containsExactlyInAnyOrder("New York", "New Orleans", "Newark");

        SugGetArgs<byte[], byte[]> maxArgs = SugGetArgs.Builder.max(2);
        List<Suggestion<byte[]>> limitedSuggestions = redis.ftSugget(suggestionKey, "New".getBytes(), maxArgs);
        assertThat(limitedSuggestions).hasSize(2);

        SugGetArgs<byte[], byte[]> fuzzyArgs = SugGetArgs.Builder.fuzzy();
        List<Suggestion<byte[]>> fuzzySuggestions = redis.ftSugget(suggestionKey, "Bost".getBytes(), fuzzyArgs);
        List<String> fuzzyValues = fuzzySuggestions.stream().map(s -> new String(s.getValue())).collect(Collectors.toList());
        assertThat(fuzzyValues).contains("Boston");

        assertThat(redis.ftSugdel(suggestionKey, "Newark".getBytes())).isTrue();
        assertThat(redis.ftSuglen(suggestionKey)).isEqualTo(4L);

        List<Suggestion<byte[]>> afterDeletion = redis.ftSugget(suggestionKey, "New".getBytes());
        assertThat(afterDeletion).hasSize(2);
        List<String> afterValues = afterDeletion.stream().map(s -> new String(s.getValue())).collect(Collectors.toList());
        assertThat(afterValues).containsExactlyInAnyOrder("New York", "New Orleans");

        assertThat(redis.ftSugdel(suggestionKey, "NonExistent".getBytes())).isFalse();

        SugAddArgs<byte[], byte[]> incrArgs = SugAddArgs.Builder.<byte[], byte[]> incr().payload("US-East".getBytes());
        assertThat(redis.ftSugadd(suggestionKey, "New York".getBytes(), 0.5, incrArgs)).isEqualTo(4L);

        SugGetArgs<byte[], byte[]> withExtrasArgs = SugGetArgs.Builder.<byte[], byte[]> withScores().withPayloads();
        List<Suggestion<byte[]>> detailedSuggestions = redis.ftSugget(suggestionKey, "New".getBytes(), withExtrasArgs);
        assertThat(detailedSuggestions).isNotEmpty();

        for (Suggestion<byte[]> suggestion : detailedSuggestions) {
            assertThat(suggestion.getValue()).isNotNull();
            if ("New York".equals(new String(suggestion.getValue()))) {
                assertThat(suggestion.hasScore()).isTrue();
                assertThat(suggestion.hasPayload()).isTrue();
                assertThat(new String(suggestion.getPayload())).isEqualTo("US-East");
            }
        }

        redis.ftSugdel(suggestionKey, "New York".getBytes());
        redis.ftSugdel(suggestionKey, "New Orleans".getBytes());
        redis.ftSugdel(suggestionKey, "Boston".getBytes());
        redis.ftSugdel(suggestionKey, "Barcelona".getBytes());
        assertThat(redis.ftSuglen(suggestionKey)).isEqualTo(0L);
    }

    /**
     * Test FT.DICTADD, FT.DICTDEL, and FT.DICTDUMP commands.
     */
    @Test
    void testFtDictionaryCommands() {
        String dictKey = "stopwords:english";

        assertThat(redis.ftDictadd(dictKey, "the".getBytes(), "and".getBytes(), "or".getBytes())).isEqualTo(3L);
        assertThat(redis.ftDictadd(dictKey, "but".getBytes(), "not".getBytes())).isEqualTo(2L);
        assertThat(redis.ftDictadd(dictKey, "the".getBytes(), "and".getBytes())).isEqualTo(0L);

        List<byte[]> allTerms = redis.ftDictdump(dictKey);
        List<String> allTermStrings = allTerms.stream().map(String::new).collect(Collectors.toList());
        assertThat(allTermStrings).hasSize(5);
        assertThat(allTermStrings).containsExactlyInAnyOrder("the", "and", "or", "but", "not");

        assertThat(redis.ftDictdel(dictKey, "or".getBytes(), "not".getBytes())).isEqualTo(2L);
        assertThat(redis.ftDictdel(dictKey, "nonexistent".getBytes())).isEqualTo(0L);

        List<byte[]> remainingTerms = redis.ftDictdump(dictKey);
        List<String> remainingStrings = remainingTerms.stream().map(String::new).collect(Collectors.toList());
        assertThat(remainingStrings).hasSize(3);
        assertThat(remainingStrings).containsExactlyInAnyOrder("the", "and", "but");

        assertThat(redis.ftDictadd(dictKey, "with".getBytes(), "from".getBytes(), "by".getBytes())).isEqualTo(3L);

        List<byte[]> finalTerms = redis.ftDictdump(dictKey);
        List<String> finalStrings = finalTerms.stream().map(String::new).collect(Collectors.toList());
        assertThat(finalStrings).hasSize(6);
        assertThat(finalStrings).containsExactlyInAnyOrder("the", "and", "but", "with", "from", "by");

        redis.ftDictdel(dictKey, "the".getBytes(), "and".getBytes(), "but".getBytes(), "with".getBytes(), "from".getBytes(),
                "by".getBytes());
        List<byte[]> emptyDict = redis.ftDictdump(dictKey);
        assertThat(emptyDict).isEmpty();
    }

    /**
     * Test FT.SPELLCHECK command for spelling correction.
     */
    // @Test
    // // Spell check loads endlessly
    // void testFtSpellcheckCommand() {
    // String testIndex = "spellcheck-idx";
    //
    // FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
    // FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
    //
    // CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("doc:".getBytes())
    // .on(CreateArgs.TargetType.HASH).build();
    //
    // assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, contentField))).isEqualTo("OK");
    //
    // Map<byte[], byte[]> doc1 = new HashMap<>();
    // doc1.put("title".getBytes(), "Redis Search".getBytes());
    // doc1.put("content".getBytes(), "Redis is a fast in-memory database".getBytes());
    // redis.hmset("doc:1".getBytes(), doc1);
    //
    // Map<byte[], byte[]> doc2 = new HashMap<>();
    // doc2.put("title".getBytes(), "Database Performance".getBytes());
    // doc2.put("content".getBytes(), "Performance optimization techniques".getBytes());
    // redis.hmset("doc:2".getBytes(), doc2);
    //
    // Map<byte[], byte[]> doc3 = new HashMap<>();
    // doc3.put("title".getBytes(), "Memory Management".getBytes());
    // doc3.put("content".getBytes(), "Efficient memory usage patterns".getBytes());
    // redis.hmset("doc:3".getBytes(), doc3);
    //
    // Map<byte[], byte[]> doc4 = new HashMap<>();
    // doc4.put("title".getBytes(), "Search Engine".getBytes());
    // doc4.put("content".getBytes(), "Full text search capabilities".getBytes());
    // redis.hmset("doc:4".getBytes(), doc4);
    //
    // SpellCheckResult<byte[]> result = redis.ftSpellcheck(testIndex, "reids serch".getBytes());
    // assertThat(result.hasMisspelledTerms()).isTrue();
    // assertThat(result.getMisspelledTermCount()).isEqualTo(2);
    //
    // SpellCheckResult.MisspelledTerm<byte[]> firstTerm = result.getMisspelledTerms().get(0);
    // assertThat(new String(firstTerm.getTerm())).isEqualTo("reids");
    // assertThat(firstTerm.hasSuggestions()).isFalse();
    //
    // SpellCheckResult.MisspelledTerm<byte[]> secondTerm = result.getMisspelledTerms().get(1);
    // assertThat(new String(secondTerm.getTerm())).isEqualTo("serch");
    // assertThat(secondTerm.hasSuggestions()).isTrue();
    //
    // boolean hasSearchSuggestion = secondTerm.getSuggestions().stream()
    // .anyMatch(suggestion -> "search".equalsIgnoreCase(new String(suggestion.getSuggestion())));
    // assertThat(hasSearchSuggestion).isTrue();
    //
    // SpellCheckArgs<byte[], byte[]> distanceArgs = SpellCheckArgs.Builder.distance(2);
    // SpellCheckResult<byte[]> distanceResult = redis.ftSpellcheck(testIndex, "databse".getBytes(), distanceArgs);
    // assertThat(distanceResult.hasMisspelledTerms()).isTrue();
    //
    // String dictKey = "custom-dict";
    // redis.ftDictadd(dictKey, "elasticsearch".getBytes(), "solr".getBytes(), "lucene".getBytes());
    //
    // SpellCheckArgs<byte[], byte[]> includeArgs = SpellCheckArgs.Builder.termsInclude(dictKey.getBytes());
    // SpellCheckResult<byte[]> includeResult = redis.ftSpellcheck(testIndex, "elasticsearh".getBytes(), includeArgs);
    // assertThat(includeResult.hasMisspelledTerms()).isTrue();
    //
    // SpellCheckArgs<byte[], byte[]> excludeArgs = SpellCheckArgs.Builder.termsExclude(dictKey.getBytes());
    // SpellCheckResult<byte[]> excludeResult = redis.ftSpellcheck(testIndex, "elasticsearh".getBytes(), excludeArgs);
    // assertThat(excludeResult.hasMisspelledTerms()).isTrue();
    //
    // SpellCheckResult<byte[]> correctResult = redis.ftSpellcheck(testIndex, "redis search".getBytes());
    // assertThat(correctResult.hasMisspelledTerms()).isFalse();
    // assertThat(correctResult.getMisspelledTermCount()).isEqualTo(0);
    //
    // redis.ftDictdel(dictKey, "elasticsearch".getBytes(), "solr".getBytes(), "lucene".getBytes());
    // assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    // }

    /**
     * Test FT.EXPLAIN command for query execution plan analysis.
     */
    @Test
    void testFtExplainCommand() {
        String testIndex = "explain-idx";

        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("doc:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, contentField))).isEqualTo("OK");

        String basicExplain = redis.ftExplain(testIndex, "hello world".getBytes());
        assertThat(basicExplain).isNotNull();
        assertThat(basicExplain).isNotEmpty();
        assertThat(basicExplain).contains("INTERSECT", "UNION", "hello", "world");

        ExplainArgs<byte[], byte[]> dialectArgs = ExplainArgs.Builder.dialect(QueryDialects.DIALECT1);
        String dialectExplain = redis.ftExplain(testIndex, "hello world".getBytes(), dialectArgs);
        assertThat(dialectExplain).isNotNull();
        assertThat(dialectExplain).contains("INTERSECT", "UNION", "hello", "world");

        String complexExplain = redis.ftExplain(testIndex, "@title:hello @content:world".getBytes());
        assertThat(complexExplain).isNotNull();
        assertThat(complexExplain).contains("INTERSECT", "@title:UNION", "@title:hello", "@content:UNION", "@content:world");

        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT._LIST command for listing all indexes.
     */
    @Test
    void testFtListCommand() {
        String testIndex1 = "list-idx-1";
        String testIndex2 = "list-idx-2";

        List<byte[]> initialIndexes = redis.ftList();

        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs1 = CreateArgs.<byte[], byte[]> builder().withPrefix("doc1:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();
        assertThat(redis.ftCreate(testIndex1, createArgs1, Collections.singletonList(titleField))).isEqualTo("OK");

        CreateArgs<byte[], byte[]> createArgs2 = CreateArgs.<byte[], byte[]> builder().withPrefix("doc2:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();
        assertThat(redis.ftCreate(testIndex2, createArgs2, Collections.singletonList(titleField))).isEqualTo("OK");

        List<byte[]> updatedIndexes = redis.ftList();
        List<String> updatedNames = updatedIndexes.stream().map(String::new).collect(Collectors.toList());
        assertThat(updatedNames).contains(testIndex1, testIndex2);
        assertThat(updatedIndexes.size()).isEqualTo(initialIndexes.size() + 2);

        assertThat(redis.ftDropindex(testIndex1)).isEqualTo("OK");
        assertThat(redis.ftDropindex(testIndex2)).isEqualTo("OK");

        List<byte[]> finalIndexes = redis.ftList();
        List<String> finalNames = finalIndexes.stream().map(String::new).collect(Collectors.toList());
        assertThat(finalNames).doesNotContain(testIndex1, testIndex2);
        assertThat(finalIndexes.size()).isEqualTo(initialIndexes.size());
    }

    /**
     * Test field aliases in RETURN clause to rename fields in search results.
     */
    @Test
    void testSearchWithFieldAliases() {
        String testIndex = "alias-field-idx";

        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> authorField = TextFieldArgs.<byte[]> builder().name("author".getBytes()).build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("book:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, authorField, priceField))).isEqualTo("OK");

        Map<byte[], byte[]> book1 = new HashMap<>();
        book1.put("title".getBytes(), "Redis in Action".getBytes());
        book1.put("author".getBytes(), "Josiah Carlson".getBytes());
        book1.put("price".getBytes(), "39.99".getBytes());
        redis.hmset("book:1".getBytes(), book1);

        Map<byte[], byte[]> book2 = new HashMap<>();
        book2.put("title".getBytes(), "Redis Essentials".getBytes());
        book2.put("author".getBytes(), "Maxwell Dayvson".getBytes());
        book2.put("price".getBytes(), "29.99".getBytes());
        redis.hmset("book:2".getBytes(), book2);

        // Single field alias
        SearchArgs<byte[], byte[]> aliasArgs = SearchArgs.<byte[], byte[]> builder()
                .returnField("title".getBytes(), "book_title".getBytes()).build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(testIndex, "Redis".getBytes(), aliasArgs);
        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(getField(result.getFields(), "book_title")).isNotNull();
            assertThat(getField(result.getFields(), "title")).isNull();
        }

        // Multiple field aliases
        SearchArgs<byte[], byte[]> multiAliasArgs = SearchArgs.<byte[], byte[]> builder()
                .returnField("title".getBytes(), "book_title".getBytes()).returnField("author".getBytes(), "writer".getBytes())
                .returnField("price".getBytes(), "cost".getBytes()).build();
        results = redis.ftSearch(testIndex, "Redis".getBytes(), multiAliasArgs);
        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(getField(result.getFields(), "book_title")).isNotNull();
            assertThat(getField(result.getFields(), "writer")).isNotNull();
            assertThat(getField(result.getFields(), "cost")).isNotNull();
            assertThat(getField(result.getFields(), "title")).isNull();
            assertThat(getField(result.getFields(), "author")).isNull();
            assertThat(getField(result.getFields(), "price")).isNull();
        }

        // Mix of aliased and non-aliased
        SearchArgs<byte[], byte[]> mixedArgs = SearchArgs.<byte[], byte[]> builder()
                .returnField("title".getBytes(), "book_title".getBytes()).returnField("author".getBytes()).build();
        results = redis.ftSearch(testIndex, "Redis".getBytes(), mixedArgs);
        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(getField(result.getFields(), "book_title")).isNotNull();
            assertThat(getField(result.getFields(), "title")).isNull();
            assertThat(getField(result.getFields(), "author")).isNotNull();
            assertThat(getField(result.getFields(), "price")).isNull();
        }

        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.SYNDUMP and FT.SYNUPDATE commands for synonym management.
     */
    @Test
    void testFtSynonymCommands() {
        String testIndex = "synonym-idx";

        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("doc:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, contentField))).isEqualTo("OK");

        Map<byte[], List<byte[]>> initialSynonyms = redis.ftSyndump(testIndex);
        assertThat(initialSynonyms).isEmpty();

        String result1 = redis.ftSynupdate(testIndex, "group1".getBytes(), "car".getBytes(), "automobile".getBytes(),
                "vehicle".getBytes());
        assertThat(result1).isEqualTo("OK");

        Map<byte[], List<byte[]>> synonymsAfterUpdate = redis.ftSyndump(testIndex);
        assertThat(synonymsAfterUpdate).isNotEmpty();
        assertThat(synonymsAfterUpdate).hasSize(3);

        assertThat(new String(getByteMapValue(synonymsAfterUpdate, "car").get(0))).isEqualTo("group1");
        assertThat(new String(getByteMapValue(synonymsAfterUpdate, "automobile").get(0))).isEqualTo("group1");
        assertThat(new String(getByteMapValue(synonymsAfterUpdate, "vehicle").get(0))).isEqualTo("group1");

        SynUpdateArgs<byte[], byte[]> skipArgs = SynUpdateArgs.Builder.skipInitialScan();
        String result2 = redis.ftSynupdate(testIndex, "group2".getBytes(), skipArgs, "fast".getBytes(), "quick".getBytes(),
                "rapid".getBytes());
        assertThat(result2).isEqualTo("OK");

        Map<byte[], List<byte[]>> finalSynonyms = redis.ftSyndump(testIndex);
        assertThat(finalSynonyms).isNotEmpty();
        assertThat(finalSynonyms.size()).isGreaterThan(synonymsAfterUpdate.size());

        assertThat(new String(getByteMapValue(finalSynonyms, "fast").get(0))).isEqualTo("group2");
        assertThat(new String(getByteMapValue(finalSynonyms, "quick").get(0))).isEqualTo("group2");
        assertThat(new String(getByteMapValue(finalSynonyms, "rapid").get(0))).isEqualTo("group2");

        String result3 = redis.ftSynupdate(testIndex, "group1".getBytes(), "car".getBytes(), "automobile".getBytes(),
                "vehicle".getBytes(), "auto".getBytes());
        assertThat(result3).isEqualTo("OK");

        Map<byte[], List<byte[]>> updatedSynonyms = redis.ftSyndump(testIndex);
        assertThat(new String(getByteMapValue(updatedSynonyms, "auto").get(0))).isEqualTo("group1");

        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    @Test
    @EnabledOnCommand("FT.HYBRID")
    // this one failing
    void ftHybridAdvancedMultiQueryWithPostProcessing() {
        String indexName = "idx:ecommerce";

        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> categoryField = TagFieldArgs.<byte[]> builder().name("category".getBytes()).build();
        FieldArgs<byte[]> brandField = TagFieldArgs.<byte[]> builder().name("brand".getBytes()).build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build();
        FieldArgs<byte[]> ratingField = NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).build();
        FieldArgs<byte[]> vectorField = VectorFieldArgs.<byte[]> builder().name("image_embedding".getBytes()).hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(10).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("product:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(indexName, createArgs,
                Arrays.asList(titleField, categoryField, brandField, priceField, ratingField, vectorField))).isEqualTo("OK");

        createProduct("1", "Apple iPhone 15 Pro smartphone with advanced camera", "electronics", "apple", "999", "4.8",
                new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
        createProduct("2", "Samsung Galaxy S24 smartphone camera", "electronics", "samsung", "799", "4.6",
                new float[] { 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f, 0.9f });
        createProduct("3", "Google Pixel 8 Pro camera smartphone", "electronics", "google", "699", "4.5",
                new float[] { 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 0.8f });
        createProduct("4", "Apple iPhone 15 Pro smartphone camera", "electronics", "apple", "999", "4.8",
                new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
        createProduct("5", "Samsung Galaxy S24", "electronics", "samsung", "799", "4.6",
                new float[] { 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f, 0.9f });
        createProduct("6", "Google Pixel 8 Pro", "electronics", "google", "699", "4.5",
                new float[] { 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 0.8f });
        createProduct("7", "Best T-shirt", "apparel", "denim", "255", "4.2",
                new float[] { 0.12f, 0.22f, 0.32f, 0.42f, 0.52f, 0.62f, 0.72f, 0.82f, 0.92f, 0.85f });
        createProduct("8", "Best makeup", "beauty", "loreal", "155", "4.4",
                new float[] { 0.18f, 0.28f, 0.38f, 0.48f, 0.58f, 0.68f, 0.78f, 0.88f, 0.98f, 0.75f });
        createProduct("9", "Best punching bag", "sports", "lonsdale", "733", "4.6",
                new float[] { 0.11f, 0.21f, 0.31f, 0.41f, 0.51f, 0.61f, 0.71f, 0.81f, 0.91f, 0.95f });
        createProduct("10", "Apple iPhone 15 Pro smartphone camera", "electronics", "apple", "999", "4.8",
                new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });

        byte[] queryVector = floatArrayToByteArray(new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });

        // Build HybridArgs — use raw type for param() to avoid ambiguity when K=V=byte[]
        HybridArgs.Builder<byte[], byte[]> hybridBuilder = HybridArgs.<byte[], byte[]> builder()
                .search(HybridSearchArgs.<byte[], byte[]> builder()
                        .query("@category:{electronics} smartphone camera".getBytes()).scoreAlias("text_score".getBytes())
                        .build())
                .vectorSearch(HybridVectorArgs.<byte[], byte[]> builder().field("@image_embedding".getBytes())
                        .vector("$vec".getBytes()).method(HybridVectorArgs.Knn.of(20).efRuntime(150))
                        .filter("@brand:{apple|samsung|google}").scoreAlias("vector_score".getBytes()).build())
                .combine(Combiners.<byte[]> linear().alpha(0.7).beta(0.3).window(26))
                .postProcessing(PostProcessingArgs.<byte[], byte[]> builder()
                        .load("@price".getBytes(), "@brand".getBytes(), "@category".getBytes())
                        .groupBy(GroupBy.<byte[], byte[]> of("@brand".getBytes())
                                .reduce(Reducers.sum("@price".getBytes()).as("sum".getBytes()))
                                .reduce(Reducers.<byte[]> count().as("count".getBytes())))
                        .sortBy(SortBy.of(new SortProperty<>("@sum".getBytes(), SortDirection.ASC),
                                new SortProperty<>("@count".getBytes(), SortDirection.DESC)))
                        .apply(Apply.of("@sum * 0.9".getBytes(), "discounted_price".getBytes()))
                        .filter(Filter.of("@sum > 700".getBytes())).limit(Limit.of(0, 20)).build());

        @SuppressWarnings("rawtypes")
        HybridArgs.Builder rawBuilder = hybridBuilder;
        rawBuilder.param("vec".getBytes(), queryVector);
        rawBuilder.param("discount_rate".getBytes(), "0.9".getBytes());

        @SuppressWarnings("unchecked")
        HybridArgs<byte[], byte[]> hybridArgs = (HybridArgs<byte[], byte[]>) rawBuilder.build();

        HybridReply<byte[], byte[]> reply = redis.ftHybrid(indexName, hybridArgs);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getTotalResults()).isEqualTo(3);
        assertThat(reply.getWarnings().size()).isGreaterThanOrEqualTo(0);
        assertThat(reply.getExecutionTime()).isGreaterThan(0L);

        Map<byte[], byte[]> r1 = reply.getResults().get(0);
        assertThat(new String(getField(r1, "brand"))).isEqualTo("google");
        assertThat(new String(getField(r1, "count"))).isEqualTo("2");
        assertThat(new String(getField(r1, "sum"))).isEqualTo("1398");
        assertThat(new String(getField(r1, "discounted_price"))).isEqualTo("1258.2");

        Map<byte[], byte[]> r2 = reply.getResults().get(1);
        assertThat(new String(getField(r2, "brand"))).isEqualTo("samsung");
        assertThat(new String(getField(r2, "count"))).isEqualTo("2");
        assertThat(new String(getField(r2, "sum"))).isEqualTo("1598");
        assertThat(new String(getField(r2, "discounted_price"))).isEqualTo("1438.2");

        Map<byte[], byte[]> r3 = reply.getResults().get(2);
        assertThat(new String(getField(r3, "brand"))).isEqualTo("apple");
        assertThat(new String(getField(r3, "count"))).isEqualTo("3");
        assertThat(new String(getField(r3, "sum"))).isEqualTo("2997");
        assertThat(new String(getField(r3, "discounted_price"))).isEqualTo("2697.3");

        redis.ftDropindex(indexName);
    }

    @Test
    void testSearchWithLargeJsonPayloads() {
        String testIndex = "idx-large-json";
        byte[] prefix = "large-json:".getBytes();

        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).withTimeout(Duration.ofSeconds(30)).build();
        RedisClient testClient = RedisClient.create(redisURI);
        testClient.setOptions(
                ClientOptions.builder().decodeBufferPolicy(DecodeBufferPolicies.ratio(Integer.MAX_VALUE / 2.0f)).build());

        try (StatefulRedisConnection<byte[], byte[]> connection = testClient.connect(ByteArrayCodec.INSTANCE)) {
            RedisCommands<byte[], byte[]> testRedis = connection.sync();

            testRedis.ftCreate(testIndex,
                    CreateArgs.<byte[], byte[]> builder().on(CreateArgs.TargetType.JSON).withPrefix(prefix).build(),
                    Collections.singletonList(
                            NumericFieldArgs.<byte[]> builder().name("$.pos".getBytes()).as("pos".getBytes()).build()));

            SearchArgs<byte[], byte[]> searchArgs = SearchArgs.<byte[], byte[]> builder()
                    .sortBy(SortByArgs.<byte[]> builder().attribute("pos".getBytes()).build()).limit(0, 10_000).build();

            ArrayList<String> expected = new ArrayList<>();

            for (int i = 1; i <= 1000; i++) {
                String json = String.format(
                        "{\"pos\":%d,\"ts\":%d,\"large\":\"just here to make the response larger to some great extend and overflow the buffers\"}",
                        i, System.currentTimeMillis());

                testRedis.jsonSet(("large-json:" + i).getBytes(), JsonPath.ROOT_PATH, json);
                expected.add(json);

                if (i >= 924) {
                    SearchReply<byte[], byte[]> reply = testRedis.ftSearch(testIndex, "*".getBytes(), searchArgs);
                    assertThat(reply.getCount()).isEqualTo(i);

                    for (int t = 0; t < expected.size(); t++) {
                        String actualBody = new String(getField(reply.getResults().get(t).getFields(), "$"));
                        assertThat(actualBody).as("Mismatch at position %d on loop %d", t, i).isEqualTo(expected.get(t));
                    }
                }
            }

            testRedis.ftDropindex(testIndex);
        } finally {
            testClient.shutdown();
        }
    }

    /**
     * Helper method to find a field value in a byte[] keyed map. Since arrays in Java use reference equality rather than value
     * equality, Map.get() won't work with byte[] keys. We need to iterate and compare using Arrays.equals().
     */
    private byte[] getField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] key = fieldName.getBytes();
        return fields.entrySet().stream().filter(e -> Arrays.equals(e.getKey(), key)).map(Map.Entry::getValue).findFirst()
                .orElse(null);
    }

    /**
     * Helper method to find a value in a Map with byte[] keys.
     */
    private List<byte[]> getByteMapValue(Map<byte[], List<byte[]>> map, String keyName) {
        byte[] key = keyName.getBytes();
        return map.entrySet().stream().filter(e -> Arrays.equals(e.getKey(), key)).map(Map.Entry::getValue).findFirst()
                .orElse(null);
    }

    private void createProduct(String id, String title, String category, String brand, String price, String rating,
            float[] embedding) {
        byte[] keyBytes = ("product:" + id).getBytes();
        redis.hset(keyBytes, "title".getBytes(), title.getBytes());
        redis.hset(keyBytes, "category".getBytes(), category.getBytes());
        redis.hset(keyBytes, "brand".getBytes(), brand.getBytes());
        redis.hset(keyBytes, "price".getBytes(), price.getBytes());
        redis.hset(keyBytes, "rating".getBytes(), rating.getBytes());
        redis.hset(keyBytes, "image_embedding".getBytes(), floatArrayToByteArray(embedding));
    }

    private byte[] floatArrayToByteArray(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

}
