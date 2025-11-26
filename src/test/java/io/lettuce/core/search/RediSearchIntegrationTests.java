/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.ExplainArgs;

import io.lettuce.core.search.arguments.QueryDialects;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SortByArgs;
import io.lettuce.core.search.arguments.SpellCheckArgs;
import io.lettuce.core.search.arguments.SugAddArgs;
import io.lettuce.core.search.arguments.SugGetArgs;
import io.lettuce.core.search.arguments.SynUpdateArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for Redis Search functionality using FT.SEARCH command.
 * <p>
 * These tests are based on the examples from the Redis documentation: -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/schema-definition/">...</a> -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">...</a>
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RediSearchIntegrationTests {

    // Index names
    private static final String BLOG_INDEX = "blog-idx";

    private static final String BOOKS_INDEX = "books-idx";

    private static final String PRODUCTS_INDEX = "products-idx";

    private static final String MOVIES_INDEX = "movies-idx";

    // Prefixes
    private static final String BLOG_PREFIX = "blog:post:";

    private static final String BOOK_PREFIX = "book:details:";

    private static final String PRODUCT_PREFIX = "product:";

    private static final String MOVIE_PREFIX = "movie:";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RediSearchIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect().sync();
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
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").weight(5).build();
        FieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();
        FieldArgs<String> authorField = TagFieldArgs.<String> builder().name("author").build();
        FieldArgs<String> createdDateField = NumericFieldArgs.<String> builder().name("created_date").sortable().build();
        FieldArgs<String> viewsField = NumericFieldArgs.<String> builder().name("views").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(BLOG_INDEX, createArgs,
                Arrays.asList(titleField, contentField, authorField, createdDateField, viewsField));
        assertThat(result).isEqualTo("OK");

        // Add sample blog posts
        Map<String, String> post1 = new HashMap<>();
        post1.put("title", "Redis Search Tutorial");
        post1.put("content", "Learn how to use Redis Search for full-text search capabilities");
        post1.put("author", "john_doe");
        post1.put("created_date", "1640995200"); // 2022-01-01
        post1.put("views", "150");
        assertThat(redis.hmset("blog:post:1", post1)).isEqualTo("OK");

        Map<String, String> post2 = new HashMap<>();
        post2.put("title", "Advanced Redis Techniques");
        post2.put("content", "Explore advanced Redis features and optimization techniques");
        post2.put("author", "jane_smith");
        post2.put("created_date", "1641081600"); // 2022-01-02
        post2.put("views", "200");
        assertThat(redis.hmset("blog:post:2", post2)).isEqualTo("OK");

        Map<String, String> post3 = new HashMap<>();
        post3.put("title", "Database Performance");
        post3.put("content", "Tips for improving database performance and scalability");
        post3.put("author", "john_doe");
        post3.put("created_date", "1641168000"); // 2022-01-03
        post3.put("views", "75");
        assertThat(redis.hmset("blog:post:3", post3)).isEqualTo("OK");

        // Test 1: Basic text search
        SearchReply<String, String> searchReply = redis.ftSearch(BLOG_INDEX, "@title:(Redis)");
        assertThat(searchReply.getCount()).isEqualTo(2);
        assertThat(searchReply.getResults()).hasSize(2);
        assertThat(searchReply.getResults().get(1).getFields().get("title")).isEqualTo("Redis Search Tutorial");
        assertThat(searchReply.getResults().get(0).getFields().get("title")).isEqualTo("Advanced Redis Techniques");
        assertThat(searchReply.getResults().get(1).getFields().get("author")).isEqualTo("john_doe");
        assertThat(searchReply.getResults().get(0).getFields().get("author")).isEqualTo("jane_smith");

        // Test 2: Search with field-specific query
        SearchArgs<String, String> titleSearchArgs = SearchArgs.<String, String> builder().build();
        searchReply = redis.ftSearch(BLOG_INDEX, "@title:Redis", titleSearchArgs);
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Test 3: Tag search
        searchReply = redis.ftSearch(BLOG_INDEX, "@author:{john_doe}");
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Test 4: Numeric range search
        searchReply = redis.ftSearch(BLOG_INDEX, "@views:[100 300]");
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Cleanup
        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test search options like WITHSCORES, WITHPAYLOADS, NOCONTENT, LIMIT, SORTBY.
     */
    @Test
    void testSearchOptionsAndModifiers() {
        // Create a simple index for testing search options
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").sortable().build();
        FieldArgs<String> ratingField = NumericFieldArgs.<String> builder().name("rating").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(MOVIE_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(MOVIES_INDEX, createArgs, Arrays.asList(titleField, ratingField));

        // Add sample movies with payloads
        Map<String, String> movie1 = new HashMap<>();
        movie1.put("title", "The Matrix");
        movie1.put("rating", "8.7");
        redis.hmset("movie:1", movie1);

        Map<String, String> movie2 = new HashMap<>();
        movie2.put("title", "Matrix Reloaded");
        movie2.put("rating", "7.2");
        redis.hmset("movie:2", movie2);

        Map<String, String> movie3 = new HashMap<>();
        movie3.put("title", "Matrix Revolutions");
        movie3.put("rating", "6.8");
        redis.hmset("movie:3", movie3);

        // Test 1: Search with WITHSCORES
        SearchArgs<String, String> withScoresArgs = SearchArgs.<String, String> builder().withScores().build();
        SearchReply<String, String> results = redis.ftSearch(MOVIES_INDEX, "Matrix", withScoresArgs);
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);
        // Verify that scores are present
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getScore()).isNotNull();
            assertThat(result.getScore()).isGreaterThan(0.0);
        }

        // Test 2: Search with NOCONTENT
        SearchArgs<String, String> noContentArgs = SearchArgs.<String, String> builder().noContent().build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix", noContentArgs);
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);
        // Verify that fields are not present
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getFields()).isEmpty();
        }

        // Test 3: Search with LIMIT
        SearchArgs<String, String> limitArgs = SearchArgs.<String, String> builder().limit(0, 2).build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix", limitArgs);
        assertThat(results.getCount()).isEqualTo(3); // Total count should still be 3
        assertThat(results.getResults()).hasSize(2); // But only 2 results returned

        // Test 4: Search with SORTBY
        SortByArgs<String> sortByArgs = SortByArgs.<String> builder().attribute("rating").descending().build();
        SearchArgs<String, String> sortArgs = SearchArgs.<String, String> builder().sortBy(sortByArgs).build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix", sortArgs);
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);
        // Verify sorting order (highest rating first)
        double previousRating = Double.MAX_VALUE;
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            double currentRating = Double.parseDouble(result.getFields().get("rating"));
            assertThat(currentRating).isLessThanOrEqualTo(previousRating);
            previousRating = currentRating;
        }

        // Test 5: Search with RETURN fields
        SearchArgs<String, String> returnArgs = SearchArgs.<String, String> builder().returnField("title").build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix", returnArgs);
        assertThat(results.getCount()).isEqualTo(3);
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getFields()).containsKey("title");
            assertThat(result.getFields()).doesNotContainKey("rating");
        }

        // Cleanup
        redis.ftDropindex(MOVIES_INDEX);
    }

    /**
     * Test TAG fields with custom separators based on Redis documentation example. Example: Index books that have a categories
     * attribute, where each category is separated by a ';' character.
     */
    @Test
    void testTagFieldsWithCustomSeparator() {
        // Create index with TAG field using custom separator
        // FT.CREATE books-idx ON HASH PREFIX 1 book:details SCHEMA title TEXT categories TAG SEPARATOR ";"
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> categoriesField = TagFieldArgs.<String> builder().name("categories").separator(";").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(BOOK_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BOOKS_INDEX, createArgs, Arrays.asList(titleField, categoriesField));

        // Add sample books with categories
        Map<String, String> book1 = new HashMap<>();
        book1.put("title", "Redis in Action");
        book1.put("categories", "programming;databases;nosql");
        redis.hmset("book:details:1", book1);

        Map<String, String> book2 = new HashMap<>();
        book2.put("title", "Database Design Patterns");
        book2.put("categories", "databases;design;architecture");
        redis.hmset("book:details:2", book2);

        Map<String, String> book3 = new HashMap<>();
        book3.put("title", "NoSQL Distilled");
        book3.put("categories", "nosql;databases;theory");
        redis.hmset("book:details:3", book3);

        // Test 1: Search for books with "databases" category
        SearchReply<String, String> results = redis.ftSearch(BOOKS_INDEX, "@categories:{databases}");
        assertThat(results.getCount()).isEqualTo(3);

        // Test 2: Search for books with "nosql" category
        results = redis.ftSearch(BOOKS_INDEX, "@categories:{nosql}");
        assertThat(results.getCount()).isEqualTo(2);

        // Test 3: Search for books with "programming" category
        results = redis.ftSearch(BOOKS_INDEX, "@categories:{programming}");
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("title")).isEqualTo("Redis in Action");

        // Test 4: Search for books with multiple categories (OR)
        results = redis.ftSearch(BOOKS_INDEX, "@categories:{programming|design}");
        assertThat(results.getCount()).isEqualTo(2);

        // Cleanup
        redis.ftDropindex(BOOKS_INDEX);
    }

    /**
     * Test numeric field operations and range queries based on Redis documentation examples.
     */
    @Test
    void testNumericFieldOperations() {
        // Create index with numeric fields for testing range queries
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("price").sortable().build();
        FieldArgs<String> stockField = NumericFieldArgs.<String> builder().name("stock").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PRODUCT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(PRODUCTS_INDEX, createArgs, Arrays.asList(nameField, priceField, stockField));

        // Add sample products with numeric values
        Map<String, String> product1 = new HashMap<>();
        product1.put("name", "Laptop");
        product1.put("price", "999.99");
        product1.put("stock", "15");
        redis.hmset("product:1", product1);

        Map<String, String> product2 = new HashMap<>();
        product2.put("name", "Mouse");
        product2.put("price", "29.99");
        product2.put("stock", "100");
        redis.hmset("product:2", product2);

        Map<String, String> product3 = new HashMap<>();
        product3.put("name", "Keyboard");
        product3.put("price", "79.99");
        product3.put("stock", "50");
        redis.hmset("product:3", product3);

        Map<String, String> product4 = new HashMap<>();
        product4.put("name", "Monitor");
        product4.put("price", "299.99");
        product4.put("stock", "25");
        redis.hmset("product:4", product4);

        // Test 1: Range query - products between $50 and $500
        SearchReply<String, String> results = redis.ftSearch(PRODUCTS_INDEX, "@price:[50 500]");
        assertThat(results.getCount()).isEqualTo(2); // Keyboard and Monitor

        // Test 2: Open range query - products over $100
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[100 +inf]");
        assertThat(results.getCount()).isEqualTo(2); // Laptop and Monitor

        // Test 3: Open range query - products under $100
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[-inf 100]");
        assertThat(results.getCount()).isEqualTo(2); // Mouse and Keyboard

        // Test 4: Exact numeric value
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[29.99 29.99]");
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("name")).isEqualTo("Mouse");

        // Test 5: Stock range query
        results = redis.ftSearch(PRODUCTS_INDEX, "@stock:[20 60]");
        assertThat(results.getCount()).isEqualTo(2); // Monitor and Keyboard

        // Test 6: Combined query - products with price > 50 AND stock > 20
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[50 +inf] @stock:[20 +inf]");
        assertThat(results.getCount()).isEqualTo(2); // Keyboard and Monitor

        // Cleanup
        redis.ftDropindex(PRODUCTS_INDEX);
    }

    /**
     * Test advanced search features like INKEYS, INFIELDS, TIMEOUT, and PARAMS.
     */
    @Test
    void testAdvancedSearchFeatures() {
        // Create a simple index for testing advanced features
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();
        FieldArgs<String> categoryField = TagFieldArgs.<String> builder().name("category").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, contentField, categoryField));

        // Add sample documents
        Map<String, String> post1 = new HashMap<>();
        post1.put("title", "Redis Tutorial");
        post1.put("content", "Learn Redis basics");
        post1.put("category", "tutorial");
        redis.hmset("blog:post:1", post1);

        Map<String, String> post2 = new HashMap<>();
        post2.put("title", "Advanced Redis");
        post2.put("content", "Advanced Redis techniques");
        post2.put("category", "advanced");
        redis.hmset("blog:post:2", post2);

        Map<String, String> post3 = new HashMap<>();
        post3.put("title", "Database Guide");
        post3.put("content", "Database best practices");
        post3.put("category", "tutorial");
        redis.hmset("blog:post:3", post3);

        // Test 1: Search with INKEYS (limit search to specific keys)
        SearchArgs<String, String> inKeysArgs = SearchArgs.<String, String> builder().inKey("blog:post:1").inKey("blog:post:2")
                .build();
        SearchReply<String, String> results = redis.ftSearch(BLOG_INDEX, "Redis", inKeysArgs);
        assertThat(results.getCount()).isEqualTo(2); // Only posts 1 and 2

        // Test 2: Search with INFIELDS (limit search to specific fields)
        SearchArgs<String, String> inFieldsArgs = SearchArgs.<String, String> builder().inField("title").build();
        results = redis.ftSearch(BLOG_INDEX, "Redis", inFieldsArgs);
        assertThat(results.getCount()).isEqualTo(2); // Only matches in title field

        // Test 3: Search with TIMEOUT
        SearchArgs<String, String> timeoutArgs = SearchArgs.<String, String> builder().timeout(Duration.ofSeconds(5)).build();
        results = redis.ftSearch(BLOG_INDEX, "Redis", timeoutArgs);
        assertThat(results.getCount()).isEqualTo(2);

        // Test 4: Search with PARAMS (parameterized query)
        SearchArgs<String, String> paramsArgs = SearchArgs.<String, String> builder().param("category_param", "tutorial")
                .build();
        results = redis.ftSearch(BLOG_INDEX, "@category:{$category_param}", paramsArgs);
        assertThat(results.getCount()).isEqualTo(2); // Posts with tutorial category

        // Cleanup
        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test complex queries with boolean operations, wildcards, and phrase matching.
     */
    @Test
    void testComplexQueriesAndBooleanOperations() {
        // Create index for testing complex queries
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> descriptionField = TextFieldArgs.<String> builder().name("description").build();
        FieldArgs<String> tagsField = TagFieldArgs.<String> builder().name("tags").build();
        FieldArgs<String> ratingField = NumericFieldArgs.<String> builder().name("rating").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(MOVIE_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(MOVIES_INDEX, createArgs, Arrays.asList(titleField, descriptionField, tagsField, ratingField));

        // Add sample movies
        Map<String, String> movie1 = new HashMap<>();
        movie1.put("title", "The Matrix");
        movie1.put("description", "A computer hacker learns about the true nature of reality");
        movie1.put("tags", "sci-fi,action,thriller");
        movie1.put("rating", "8.7");
        redis.hmset("movie:1", movie1);

        Map<String, String> movie2 = new HashMap<>();
        movie2.put("title", "Matrix Reloaded");
        movie2.put("description", "Neo and the rebel leaders estimate they have 72 hours");
        movie2.put("tags", "sci-fi,action");
        movie2.put("rating", "7.2");
        redis.hmset("movie:2", movie2);

        Map<String, String> movie3 = new HashMap<>();
        movie3.put("title", "Inception");
        movie3.put("description", "A thief who steals corporate secrets through dream-sharing technology");
        movie3.put("tags", "sci-fi,thriller,drama");
        movie3.put("rating", "8.8");
        redis.hmset("movie:3", movie3);

        Map<String, String> movie4 = new HashMap<>();
        movie4.put("title", "The Dark Knight");
        movie4.put("description", "Batman faces the Joker in Gotham City");
        movie4.put("tags", "action,crime,drama");
        movie4.put("rating", "9.0");
        redis.hmset("movie:4", movie4);

        // Test 1: Boolean AND operation
        SearchReply<String, String> results = redis.ftSearch(MOVIES_INDEX, "((@tags:{thriller}) (@tags:{action}))");
        assertThat(results.getCount()).isEqualTo(1); // The Matrix
        assertThat(results.getResults().get(0).getFields().get("title")).isEqualTo("The Matrix");

        // Test 2: Boolean OR operation
        results = redis.ftSearch(MOVIES_INDEX, "((@tags:{thriller}) | (@tags:{crime}))");
        assertThat(results.getCount()).isEqualTo(3); // Matrix, Inception, Dark Knight

        // Test 3: Boolean NOT operation
        results = redis.ftSearch(MOVIES_INDEX, "((@tags:{action}) (-@tags:{thriller}))");
        assertThat(results.getCount()).isEqualTo(2); // Matrix Reloaded, The Dark Knight

        // Test 4: Phrase matching

        results = redis.ftSearch(MOVIES_INDEX, "@title:\"Inception\"");
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("title")).isEqualTo("Inception");

        // Test 5: Wildcard search
        results = redis.ftSearch(MOVIES_INDEX, "Matrix*");
        assertThat(results.getCount()).isEqualTo(2); // Both Matrix movies

        // Test 6: Complex query with numeric range and text search
        results = redis.ftSearch(MOVIES_INDEX, "@rating:[8.0 9.5] @tags:{action}");
        assertThat(results.getCount()).isEqualTo(2); // The Matrix and The Dark Knight

        // Test 7: Field-specific search with OR
        results = redis.ftSearch(MOVIES_INDEX, "@title:(Matrix | Inception)");
        assertThat(results.getCount()).isEqualTo(3); // All Matrix movies and Inception

        // Cleanup
        redis.ftDropindex(MOVIES_INDEX);
    }

    /**
     * Test empty search results and edge cases.
     */
    @Test
    void testEmptyResultsAndEdgeCases() {
        // Create a simple index
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Collections.singletonList(titleField));

        // Add one document
        Map<String, String> post1 = new HashMap<>();
        post1.put("title", "Redis Tutorial");
        redis.hmset("blog:post:1", post1);

        // Test 1: Search for non-existent term
        SearchReply<String, String> results = redis.ftSearch(BLOG_INDEX, "nonexistent");
        assertThat(results.getCount()).isEqualTo(0);
        assertThat(results.getResults()).isEmpty();

        // Test 2: Search with LIMIT beyond available results
        SearchArgs<String, String> limitArgs = SearchArgs.<String, String> builder().limit(10, 20).build();
        results = redis.ftSearch(BLOG_INDEX, "Redis", limitArgs);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults()).isEmpty(); // No results in range 10-20

        // Test 3: Search with NOCONTENT and WITHSCORES
        SearchArgs<String, String> combinedArgs = SearchArgs.<String, String> builder().noContent().withScores().build();
        results = redis.ftSearch(BLOG_INDEX, "Redis", combinedArgs);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults()).hasSize(1);
        assertThat(results.getResults().get(0).getFields()).isEmpty();
        assertThat(results.getResults().get(0).getScore()).isNotNull();

        // Cleanup
        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test FT.ALTER command to add new fields to an existing index.
     */
    @Test
    void testFtAlterAddingNewFields() {
        String testIndex = "alter-test-idx";

        // Create initial index with one field
        List<FieldArgs<String>> initialFields = Collections
                .singletonList(TextFieldArgs.<String> builder().name("title").build());

        assertThat(redis.ftCreate(testIndex, initialFields)).isEqualTo("OK");

        // Add some test data
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "Test Document");
        redis.hset("doc:1", doc1);

        // Verify initial search works
        SearchReply<String, String> initialSearch = redis.ftSearch(testIndex, "Test");
        assertThat(initialSearch.getCount()).isEqualTo(1);

        // Add new fields to the index
        List<FieldArgs<String>> newFields = Arrays.asList(
                NumericFieldArgs.<String> builder().name("published_at").sortable().build(),
                TextFieldArgs.<String> builder().name("author").build());

        assertThat(redis.ftAlter(testIndex, false, newFields)).isEqualTo("OK");

        // Update existing document with new fields
        Map<String, String> updateDoc1 = new HashMap<>();
        updateDoc1.put("published_at", "1640995200");
        updateDoc1.put("author", "John Doe");
        redis.hset("doc:1", updateDoc1);

        // Add new document with all fields
        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Another Document");
        doc2.put("published_at", "1641081600");
        doc2.put("author", "Jane Smith");
        redis.hset("doc:2", doc2);

        // Verify search still works and new fields are indexed
        SearchReply<String, String> searchAfterAlter = redis.ftSearch(testIndex, "Document");
        assertThat(searchAfterAlter.getCount()).isEqualTo(2);

        // Search by new field
        SearchReply<String, String> authorSearch = redis.ftSearch(testIndex, "@author:John");
        assertThat(authorSearch.getCount()).isEqualTo(1);
        assertThat(authorSearch.getResults().get(0).getId()).isEqualTo("doc:1");

        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.ALTER command with SKIPINITIALSCAN option.
     */
    @Test
    void testFtAlterWithSkipInitialScan() {
        String testIndex = "alter-skip-test-idx";

        // Create initial index
        List<FieldArgs<String>> initialFields = Collections
                .singletonList(TextFieldArgs.<String> builder().name("title").build());

        assertThat(redis.ftCreate(testIndex, initialFields)).isEqualTo("OK");

        // Add test data before altering
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "Existing Document");
        doc1.put("category", "Technology");
        redis.hset("doc:1", doc1);

        // Add new field with SKIPINITIALSCAN
        List<FieldArgs<String>> newFields = Collections
                .singletonList(TextFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftAlter(testIndex, true, newFields)).isEqualTo("OK");

        // The existing document should not be indexed for the new field due to SKIPINITIALSCAN
        SearchReply<String, String> categorySearch = redis.ftSearch(testIndex, "@category:Technology");
        assertThat(categorySearch.getCount()).isEqualTo(0);

        // But new documents should be indexed for the new field
        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "New Document");
        doc2.put("category", "Science");
        redis.hset("doc:2", doc2);

        SearchReply<String, String> newCategorySearch = redis.ftSearch(testIndex, "@category:Science");
        assertThat(newCategorySearch.getCount()).isEqualTo(1);
        assertThat(newCategorySearch.getResults().get(0).getId()).isEqualTo("doc:2");

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

        // Create test indexes
        List<FieldArgs<String>> fields = Collections.singletonList(TextFieldArgs.<String> builder().name("title").build());

        assertThat(redis.ftCreate(testIndex, fields)).isEqualTo("OK");
        assertThat(redis.ftCreate(testIndex2, fields)).isEqualTo("OK");

        // Test FT.ALIASADD
        assertThat(redis.ftAliasadd(alias, testIndex)).isEqualTo("OK");

        // Add test data and verify alias works
        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Test Document");
        redis.hset("doc:1", doc);

        // Search using alias should work
        SearchReply<String, String> aliasSearch = redis.ftSearch(alias, "Test");
        assertThat(aliasSearch.getCount()).isEqualTo(1);

        // Test FT.ALIASUPDATE - switch alias to different index
        assertThat(redis.ftAliasupdate(alias, testIndex2)).isEqualTo("OK");

        // Add different data to second index
        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Different Document");
        redis.hset("doc:2", doc2);

        // Search using alias should now return results from second index
        SearchReply<String, String> updatedAliasSearch = redis.ftSearch(alias, "Different");
        assertThat(updatedAliasSearch.getCount()).isEqualTo(1);
        assertThat(updatedAliasSearch.getResults().get(0).getId()).isEqualTo("doc:2");

        // Test FT.ALIASDEL
        assertThat(redis.ftAliasdel(alias)).isEqualTo("OK");

        // Cleanup
        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
        assertThat(redis.ftDropindex(testIndex2)).isEqualTo("OK");
    }

    /**
     * Test FT.TAGVALS command to retrieve distinct values from a tag field.
     */
    @Test
    void testFtTagvals() {
        String testIndex = "tagvals-test-idx";

        // Create index with a tag field
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TagFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftCreate(testIndex, fields)).isEqualTo("OK");

        // Add test data with different tag values
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "Document 1");
        doc1.put("category", "Technology");
        redis.hset("doc:1", doc1);

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Document 2");
        doc2.put("category", "Science");
        redis.hset("doc:2", doc2);

        Map<String, String> doc3 = new HashMap<>();
        doc3.put("title", "Document 3");
        doc3.put("category", "Technology"); // Duplicate category
        redis.hset("doc:3", doc3);

        Map<String, String> doc4 = new HashMap<>();
        doc4.put("title", "Document 4");
        doc4.put("category", "Arts");
        redis.hset("doc:4", doc4);

        // Test FT.TAGVALS to get distinct tag values
        List<String> tagValues = redis.ftTagvals(testIndex, "category");

        // Should return distinct values (Technology, Science, Arts)
        assertThat(tagValues).hasSize(3);
        assertThat(tagValues).containsExactlyInAnyOrder("Technology".toLowerCase(), "Science".toLowerCase(),
                "Arts".toLowerCase());

        // Test with non-existent field should return empty list

        assertThrows(RedisCommandExecutionException.class, () -> redis.ftTagvals(testIndex, "nonexistent"));
        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.SUGADD, FT.SUGGET, FT.SUGDEL, and FT.SUGLEN commands for auto-complete functionality.
     */
    @Test
    void testFtSuggestionCommands() {
        String suggestionKey = "autocomplete:cities";

        // Test FT.SUGADD - Add suggestions with different scores
        assertThat(redis.ftSugadd(suggestionKey, "New York", 1.0)).isEqualTo(1L);
        assertThat(redis.ftSugadd(suggestionKey, "New Orleans", 0.8)).isEqualTo(2L);
        assertThat(redis.ftSugadd(suggestionKey, "Newark", 0.6)).isEqualTo(3L);
        assertThat(redis.ftSugadd(suggestionKey, "Boston", 0.9)).isEqualTo(4L);
        assertThat(redis.ftSugadd(suggestionKey, "Barcelona", 0.7)).isEqualTo(5L);

        // Test FT.SUGLEN - Get dictionary size
        assertThat(redis.ftSuglen(suggestionKey)).isEqualTo(5L);

        // Test FT.SUGGET - Get suggestions for prefix
        List<Suggestion<String>> suggestions = redis.ftSugget(suggestionKey, "New");
        assertThat(suggestions).hasSize(3);
        assertThat(suggestions.stream().map(Suggestion::getValue)).containsExactlyInAnyOrder("New York", "New Orleans",
                "Newark");

        // Test FT.SUGGET with MAX limit
        SugGetArgs<String, String> maxArgs = SugGetArgs.Builder.max(2);
        List<Suggestion<String>> limitedSuggestions = redis.ftSugget(suggestionKey, "New", maxArgs);
        assertThat(limitedSuggestions).hasSize(2);

        // Test FT.SUGGET with FUZZY matching
        SugGetArgs<String, String> fuzzyArgs = SugGetArgs.Builder.fuzzy();
        List<Suggestion<String>> fuzzySuggestions = redis.ftSugget(suggestionKey, "Bost", fuzzyArgs);
        assertThat(fuzzySuggestions.stream().map(Suggestion::getValue)).contains("Boston");

        // Test FT.SUGDEL - Delete a suggestion
        assertThat(redis.ftSugdel(suggestionKey, "Newark")).isTrue();
        assertThat(redis.ftSuglen(suggestionKey)).isEqualTo(4L);

        // Verify deletion
        List<Suggestion<String>> afterDeletion = redis.ftSugget(suggestionKey, "New");
        assertThat(afterDeletion).hasSize(2);
        assertThat(afterDeletion.stream().map(Suggestion::getValue)).containsExactlyInAnyOrder("New York", "New Orleans");

        // Test deleting non-existent suggestion
        assertThat(redis.ftSugdel(suggestionKey, "NonExistent")).isFalse();

        // Test FT.SUGADD with INCR and PAYLOAD
        SugAddArgs<String, String> incrArgs = SugAddArgs.Builder.<String, String> incr().payload("US-East");
        assertThat(redis.ftSugadd(suggestionKey, "New York", 0.5, incrArgs)).isEqualTo(4L);

        // Test FT.SUGGET with WITHSCORES and WITHPAYLOADS
        SugGetArgs<String, String> withExtrasArgs = SugGetArgs.Builder.<String, String> withScores().withPayloads();
        List<Suggestion<String>> detailedSuggestions = redis.ftSugget(suggestionKey, "New", withExtrasArgs);
        assertThat(detailedSuggestions).isNotEmpty();

        // Verify that suggestions with scores and payloads are properly parsed
        for (Suggestion<String> suggestion : detailedSuggestions) {
            assertThat(suggestion.getValue()).isNotNull();
            if ("New York".equals(suggestion.getValue())) {
                assertThat(suggestion.hasScore()).isTrue();
                assertThat(suggestion.hasPayload()).isTrue();
                assertThat(suggestion.getPayload()).isEqualTo("US-East");
            }
        }

        // Cleanup - delete all suggestions
        redis.ftSugdel(suggestionKey, "New York");
        redis.ftSugdel(suggestionKey, "New Orleans");
        redis.ftSugdel(suggestionKey, "Boston");
        redis.ftSugdel(suggestionKey, "Barcelona");

        assertThat(redis.ftSuglen(suggestionKey)).isEqualTo(0L);
    }

    /**
     * Test FT.DICTADD, FT.DICTDEL, and FT.DICTDUMP commands for dictionary functionality.
     */
    @Test
    void testFtDictionaryCommands() {
        String dictKey = "stopwords:english";

        // Test FT.DICTADD - Add terms to dictionary
        assertThat(redis.ftDictadd(dictKey, "the", "and", "or")).isEqualTo(3L);
        assertThat(redis.ftDictadd(dictKey, "but", "not")).isEqualTo(2L);

        // Test adding duplicate terms (should return 0 for duplicates)
        assertThat(redis.ftDictadd(dictKey, "the", "and")).isEqualTo(0L);

        // Test FT.DICTDUMP - Get all terms in dictionary
        List<String> allTerms = redis.ftDictdump(dictKey);
        assertThat(allTerms).hasSize(5);
        assertThat(allTerms).containsExactlyInAnyOrder("the", "and", "or", "but", "not");

        // Test FT.DICTDEL - Delete terms from dictionary
        assertThat(redis.ftDictdel(dictKey, "or", "not")).isEqualTo(2L);

        // Test deleting non-existent terms
        assertThat(redis.ftDictdel(dictKey, "nonexistent")).isEqualTo(0L);

        // Verify deletion
        List<String> remainingTerms = redis.ftDictdump(dictKey);
        assertThat(remainingTerms).hasSize(3);
        assertThat(remainingTerms).containsExactlyInAnyOrder("the", "and", "but");

        // Test adding more terms
        assertThat(redis.ftDictadd(dictKey, "with", "from", "by")).isEqualTo(3L);

        // Final verification
        List<String> finalTerms = redis.ftDictdump(dictKey);
        assertThat(finalTerms).hasSize(6);
        assertThat(finalTerms).containsExactlyInAnyOrder("the", "and", "but", "with", "from", "by");

        // Cleanup - delete all terms
        redis.ftDictdel(dictKey, finalTerms.toArray(new String[0]));

        // Verify empty dictionary
        List<String> emptyDict = redis.ftDictdump(dictKey);
        assertThat(emptyDict).isEmpty();
    }

    /**
     * Test FT.SPELLCHECK command for spelling correction functionality.
     */
    @Test
    void testFtSpellcheckCommand() {
        String testIndex = "spellcheck-idx";

        // Create field definitions
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();

        // Create an index with some documents
        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("doc:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, contentField))).isEqualTo("OK");

        // Add some documents to build the vocabulary
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "Redis Search");
        doc1.put("content", "Redis is a fast in-memory database");
        redis.hmset("doc:1", doc1);

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Database Performance");
        doc2.put("content", "Performance optimization techniques");
        redis.hmset("doc:2", doc2);

        Map<String, String> doc3 = new HashMap<>();
        doc3.put("title", "Memory Management");
        doc3.put("content", "Efficient memory usage patterns");
        redis.hmset("doc:3", doc3);

        Map<String, String> doc4 = new HashMap<>();
        doc4.put("title", "Search Engine");
        doc4.put("content", "Full text search capabilities");
        redis.hmset("doc:4", doc4);

        // Test basic spellcheck with misspelled words
        SpellCheckResult<String> result = redis.ftSpellcheck(testIndex, "reids serch");
        assertThat(result.hasMisspelledTerms()).isTrue();
        assertThat(result.getMisspelledTermCount()).isEqualTo(2);

        // Check first misspelled term "reids"
        SpellCheckResult.MisspelledTerm<String> firstTerm = result.getMisspelledTerms().get(0);
        assertThat(firstTerm.getTerm()).isEqualTo("reids");
        assertThat(firstTerm.hasSuggestions()).isFalse();

        // Check second misspelled term "serch"
        SpellCheckResult.MisspelledTerm<String> secondTerm = result.getMisspelledTerms().get(1);
        assertThat(secondTerm.getTerm()).isEqualTo("serch");
        assertThat(secondTerm.hasSuggestions()).isTrue();

        // Check if "search" is suggested for "serch"
        boolean hasSearchSuggestion = secondTerm.getSuggestions().stream()
                .anyMatch(suggestion -> "search".equalsIgnoreCase(suggestion.getSuggestion()));
        assertThat(hasSearchSuggestion).isTrue();

        // Test spellcheck with distance parameter
        SpellCheckArgs<String, String> distanceArgs = SpellCheckArgs.Builder.distance(2);
        SpellCheckResult<String> distanceResult = redis.ftSpellcheck(testIndex, "databse", distanceArgs);
        assertThat(distanceResult.hasMisspelledTerms()).isTrue();

        // Test spellcheck with custom dictionary
        String dictKey = "custom-dict";
        redis.ftDictadd(dictKey, "elasticsearch", "solr", "lucene");

        SpellCheckArgs<String, String> includeArgs = SpellCheckArgs.Builder.termsInclude(dictKey);
        SpellCheckResult<String> includeResult = redis.ftSpellcheck(testIndex, "elasticsearh", includeArgs);
        assertThat(includeResult.hasMisspelledTerms()).isTrue();

        // Test spellcheck with exclude dictionary
        SpellCheckArgs<String, String> excludeArgs = SpellCheckArgs.Builder.termsExclude(dictKey);
        SpellCheckResult<String> excludeResult = redis.ftSpellcheck(testIndex, "elasticsearh", excludeArgs);
        assertThat(excludeResult.hasMisspelledTerms()).isTrue();

        // Test spellcheck with correct words (should return no misspelled terms)
        SpellCheckResult<String> correctResult = redis.ftSpellcheck(testIndex, "redis search");
        assertThat(correctResult.hasMisspelledTerms()).isFalse();
        assertThat(correctResult.getMisspelledTermCount()).isEqualTo(0);

        // Cleanup
        redis.ftDictdel(dictKey, "elasticsearch", "solr", "lucene");
        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.EXPLAIN command for query execution plan analysis.
     */
    @Test
    void testFtExplainCommand() {
        String testIndex = "explain-idx";

        // Create field definitions
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();

        // Create an index
        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("doc:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, contentField))).isEqualTo("OK");

        // Test basic explain
        String basicExplain = redis.ftExplain(testIndex, "hello world");
        assertThat(basicExplain).isNotNull();
        assertThat(basicExplain).isNotEmpty();
        assertThat(basicExplain).contains("INTERSECT", "UNION", "hello", "world");

        // Test explain with dialect
        ExplainArgs<String, String> dialectArgs = ExplainArgs.Builder.dialect(QueryDialects.DIALECT1);
        String dialectExplain = redis.ftExplain(testIndex, "hello world", dialectArgs);
        assertThat(dialectExplain).isNotNull();
        assertThat(dialectExplain).isNotEmpty();
        assertThat(dialectExplain).contains("INTERSECT", "UNION", "hello", "world");

        // Test complex query explain
        String complexExplain = redis.ftExplain(testIndex, "@title:hello @content:world");
        assertThat(complexExplain).isNotNull();
        assertThat(complexExplain).isNotEmpty();
        assertThat(complexExplain).contains("INTERSECT", "@title:UNION", "@title:hello", "@content:UNION", "@content:world");

        // Cleanup
        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT._LIST command for listing all indexes.
     */
    @Test
    void testFtListCommand() {
        String testIndex1 = "list-idx-1";
        String testIndex2 = "list-idx-2";

        // Get initial list of indexes
        List<String> initialIndexes = redis.ftList();

        // Create field definitions
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();

        // Create first index
        CreateArgs<String, String> createArgs1 = CreateArgs.<String, String> builder().withPrefix("doc1:")
                .on(CreateArgs.TargetType.HASH).build();
        assertThat(redis.ftCreate(testIndex1, createArgs1, Collections.singletonList(titleField))).isEqualTo("OK");

        // Create second index
        CreateArgs<String, String> createArgs2 = CreateArgs.<String, String> builder().withPrefix("doc2:")
                .on(CreateArgs.TargetType.HASH).build();
        assertThat(redis.ftCreate(testIndex2, createArgs2, Collections.singletonList(titleField))).isEqualTo("OK");

        // Get updated list of indexes
        List<String> updatedIndexes = redis.ftList();

        // Verify that the new indexes are in the list
        assertThat(updatedIndexes).contains(testIndex1, testIndex2);
        assertThat(updatedIndexes.size()).isEqualTo(initialIndexes.size() + 2);

        // Cleanup
        assertThat(redis.ftDropindex(testIndex1)).isEqualTo("OK");
        assertThat(redis.ftDropindex(testIndex2)).isEqualTo("OK");

        // Verify indexes are removed
        List<String> finalIndexes = redis.ftList();
        assertThat(finalIndexes).doesNotContain(testIndex1, testIndex2);
        assertThat(finalIndexes.size()).isEqualTo(initialIndexes.size());
    }

    /**
     * Test field aliases in RETURN clause to rename fields in search results.
     */
    @Test
    void testSearchWithFieldAliases() {
        String testIndex = "alias-field-idx";

        // Create index with multiple fields
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> authorField = TextFieldArgs.<String> builder().name("author").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("price").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("book:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, authorField, priceField))).isEqualTo("OK");

        // Add sample books
        Map<String, String> book1 = new HashMap<>();
        book1.put("title", "Redis in Action");
        book1.put("author", "Josiah Carlson");
        book1.put("price", "39.99");
        redis.hmset("book:1", book1);

        Map<String, String> book2 = new HashMap<>();
        book2.put("title", "Redis Essentials");
        book2.put("author", "Maxwell Dayvson");
        book2.put("price", "29.99");
        redis.hmset("book:2", book2);

        // Test 1: Search with field alias - rename single field
        SearchArgs<String, String> aliasArgs = SearchArgs.<String, String> builder().returnField("title", "book_title").build();
        SearchReply<String, String> results = redis.ftSearch(testIndex, "Redis", aliasArgs);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);

        // Verify that the field is returned with the alias name
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getFields()).containsKey("book_title");
            assertThat(result.getFields()).doesNotContainKey("title");
            assertThat(result.getFields().get("book_title")).contains("Redis");
        }

        // Test 2: Search with multiple field aliases
        SearchArgs<String, String> multiAliasArgs = SearchArgs.<String, String> builder().returnField("title", "book_title")
                .returnField("author", "writer").returnField("price", "cost").build();
        results = redis.ftSearch(testIndex, "Redis", multiAliasArgs);

        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            // Verify aliased fields are present
            assertThat(result.getFields()).containsKey("book_title");
            assertThat(result.getFields()).containsKey("writer");
            assertThat(result.getFields()).containsKey("cost");

            // Verify original field names are not present
            assertThat(result.getFields()).doesNotContainKey("title");
            assertThat(result.getFields()).doesNotContainKey("author");
            assertThat(result.getFields()).doesNotContainKey("price");
        }

        // Test 3: Mix of aliased and non-aliased fields
        SearchArgs<String, String> mixedArgs = SearchArgs.<String, String> builder().returnField("title", "book_title")
                .returnField("author").build();
        results = redis.ftSearch(testIndex, "Redis", mixedArgs);

        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            // Verify aliased field
            assertThat(result.getFields()).containsKey("book_title");
            assertThat(result.getFields()).doesNotContainKey("title");

            // Verify non-aliased field
            assertThat(result.getFields()).containsKey("author");

            // Verify price is not returned
            assertThat(result.getFields()).doesNotContainKey("price");
        }

        // Cleanup
        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

    /**
     * Test FT.SYNDUMP and FT.SYNUPDATE commands for synonym management.
     */
    @Test
    void testFtSynonymCommands() {
        String testIndex = "synonym-idx";

        // Create field definitions
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();

        // Create an index
        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("doc:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate(testIndex, createArgs, Arrays.asList(titleField, contentField))).isEqualTo("OK");

        // Test initial synonym dump (should be empty)
        Map<String, List<String>> initialSynonyms = redis.ftSyndump(testIndex);
        assertThat(initialSynonyms).isEmpty();

        // Test basic synonym update
        String result1 = redis.ftSynupdate(testIndex, "group1", "car", "automobile", "vehicle");
        assertThat(result1).isEqualTo("OK");

        // Test synonym dump after update
        Map<String, List<String>> synonymsAfterUpdate = redis.ftSyndump(testIndex);
        assertThat(synonymsAfterUpdate).isNotEmpty();

        // Verify the synonym group structure
        // Redis returns a map where each synonym is a key and the value is a list containing the group ID
        assertThat(synonymsAfterUpdate).hasSize(3);
        assertThat(synonymsAfterUpdate).containsKeys("car", "automobile", "vehicle");
        assertThat(synonymsAfterUpdate.get("car")).containsExactly("group1");
        assertThat(synonymsAfterUpdate.get("automobile")).containsExactly("group1");
        assertThat(synonymsAfterUpdate.get("vehicle")).containsExactly("group1");

        // Test synonym update with SKIPINITIALSCAN
        SynUpdateArgs<String, String> skipArgs = SynUpdateArgs.Builder.skipInitialScan();
        String result2 = redis.ftSynupdate(testIndex, "group2", skipArgs, "fast", "quick", "rapid");
        assertThat(result2).isEqualTo("OK");

        // Test synonym dump after second update
        Map<String, List<String>> finalSynonyms = redis.ftSyndump(testIndex);
        assertThat(finalSynonyms).isNotEmpty();
        assertThat(finalSynonyms.size()).isGreaterThan(synonymsAfterUpdate.size());

        // Verify both synonym groups exist (each synonym maps to its group)
        assertThat(finalSynonyms).containsKeys("car", "automobile", "vehicle", "fast", "quick", "rapid");
        assertThat(finalSynonyms.get("fast")).containsExactly("group2");
        assertThat(finalSynonyms.get("quick")).containsExactly("group2");
        assertThat(finalSynonyms.get("rapid")).containsExactly("group2");

        // Test updating existing synonym group
        String result3 = redis.ftSynupdate(testIndex, "group1", "car", "automobile", "vehicle", "auto");
        assertThat(result3).isEqualTo("OK");

        // Verify updated synonym group
        Map<String, List<String>> updatedSynonyms = redis.ftSyndump(testIndex);
        assertThat(updatedSynonyms).containsKeys("car", "automobile", "vehicle", "auto");
        assertThat(updatedSynonyms.get("auto")).containsExactly("group1");

        // Cleanup
        assertThat(redis.ftDropindex(testIndex)).isEqualTo("OK");
    }

}
