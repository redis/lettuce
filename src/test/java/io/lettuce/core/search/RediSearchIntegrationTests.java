/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SortByArgs;
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
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

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

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(BLOG_PREFIX)
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
        SearchResults<String, String> searchResults = redis.ftSearch(BLOG_INDEX, "@title:(Redis)", null);
        assertThat(searchResults.getCount()).isEqualTo(2);
        assertThat(searchResults.getResults()).hasSize(2);
        assertThat(searchResults.getResults().get(1).getFields().get("title")).isEqualTo("Redis Search Tutorial");
        assertThat(searchResults.getResults().get(0).getFields().get("title")).isEqualTo("Advanced Redis Techniques");
        assertThat(searchResults.getResults().get(1).getFields().get("author")).isEqualTo("john_doe");
        assertThat(searchResults.getResults().get(0).getFields().get("author")).isEqualTo("jane_smith");

        // Test 2: Search with field-specific query
        SearchArgs<String, String> titleSearchArgs = SearchArgs.<String, String> builder().build();
        searchResults = redis.ftSearch(BLOG_INDEX, "@title:Redis", titleSearchArgs);
        assertThat(searchResults.getCount()).isEqualTo(2);

        // Test 3: Tag search
        searchResults = redis.ftSearch(BLOG_INDEX, "@author:{john_doe}", null);
        assertThat(searchResults.getCount()).isEqualTo(2);

        // Test 4: Numeric range search
        searchResults = redis.ftSearch(BLOG_INDEX, "@views:[100 300]", null);
        assertThat(searchResults.getCount()).isEqualTo(2);

        // Cleanup
        redis.ftDropindex(BLOG_INDEX, false);
    }

    /**
     * Test search options like WITHSCORES, WITHPAYLOADS, NOCONTENT, LIMIT, SORTBY.
     */
    @Test
    void testSearchOptionsAndModifiers() {
        // Create a simple index for testing search options
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").sortable().build();
        FieldArgs<String> ratingField = NumericFieldArgs.<String> builder().name("rating").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(MOVIE_PREFIX)
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
        SearchResults<String, String> results = redis.ftSearch(MOVIES_INDEX, "Matrix", withScoresArgs);
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);
        // Verify that scores are present
        for (SearchResults.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getScore()).isNotNull();
            assertThat(result.getScore()).isGreaterThan(0.0);
        }

        // Test 2: Search with NOCONTENT
        SearchArgs<String, String> noContentArgs = SearchArgs.<String, String> builder().noContent().build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix", noContentArgs);
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);
        // Verify that fields are not present
        for (SearchResults.SearchResult<String, String> result : results.getResults()) {
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
        for (SearchResults.SearchResult<String, String> result : results.getResults()) {
            double currentRating = Double.parseDouble(result.getFields().get("rating"));
            assertThat(currentRating).isLessThanOrEqualTo(previousRating);
            previousRating = currentRating;
        }

        // Test 5: Search with RETURN fields
        SearchArgs<String, String> returnArgs = SearchArgs.<String, String> builder().returnField("title").build();
        results = redis.ftSearch(MOVIES_INDEX, "Matrix", returnArgs);
        assertThat(results.getCount()).isEqualTo(3);
        for (SearchResults.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getFields()).containsKey("title");
            assertThat(result.getFields()).doesNotContainKey("rating");
        }

        // Cleanup
        redis.ftDropindex(MOVIES_INDEX, false);
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

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(BOOK_PREFIX)
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
        SearchResults<String, String> results = redis.ftSearch(BOOKS_INDEX, "@categories:{databases}", null);
        assertThat(results.getCount()).isEqualTo(3);

        // Test 2: Search for books with "nosql" category
        results = redis.ftSearch(BOOKS_INDEX, "@categories:{nosql}", null);
        assertThat(results.getCount()).isEqualTo(2);

        // Test 3: Search for books with "programming" category
        results = redis.ftSearch(BOOKS_INDEX, "@categories:{programming}", null);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("title")).isEqualTo("Redis in Action");

        // Test 4: Search for books with multiple categories (OR)
        results = redis.ftSearch(BOOKS_INDEX, "@categories:{programming|design}", null);
        assertThat(results.getCount()).isEqualTo(2);

        // Cleanup
        redis.ftDropindex(BOOKS_INDEX, false);
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

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(PRODUCT_PREFIX)
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
        SearchResults<String, String> results = redis.ftSearch(PRODUCTS_INDEX, "@price:[50 500]", null);
        assertThat(results.getCount()).isEqualTo(2); // Keyboard and Monitor

        // Test 2: Open range query - products over $100
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[100 +inf]", null);
        assertThat(results.getCount()).isEqualTo(2); // Laptop and Monitor

        // Test 3: Open range query - products under $100
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[-inf 100]", null);
        assertThat(results.getCount()).isEqualTo(2); // Mouse and Keyboard

        // Test 4: Exact numeric value
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[29.99 29.99]", null);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("name")).isEqualTo("Mouse");

        // Test 5: Stock range query
        results = redis.ftSearch(PRODUCTS_INDEX, "@stock:[20 60]", null);
        assertThat(results.getCount()).isEqualTo(2); // Monitor and Keyboard

        // Test 6: Combined query - products with price > 50 AND stock > 20
        results = redis.ftSearch(PRODUCTS_INDEX, "@price:[50 +inf] @stock:[20 +inf]", null);
        assertThat(results.getCount()).isEqualTo(2); // Keyboard and Monitor

        // Cleanup
        redis.ftDropindex(PRODUCTS_INDEX, false);
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

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(BLOG_PREFIX)
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
        SearchResults<String, String> results = redis.ftSearch(BLOG_INDEX, "Redis", inKeysArgs);
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
        redis.ftDropindex(BLOG_INDEX, false);
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

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(MOVIE_PREFIX)
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
        SearchResults<String, String> results = redis.ftSearch(MOVIES_INDEX, "((@tags:{thriller}) (@tags:{action}))", null);
        assertThat(results.getCount()).isEqualTo(1); // The Matrix
        assertThat(results.getResults().get(0).getFields().get("title")).isEqualTo("The Matrix");

        // Test 2: Boolean OR operation
        results = redis.ftSearch(MOVIES_INDEX, "((@tags:{thriller}) | (@tags:{crime}))", null);
        assertThat(results.getCount()).isEqualTo(3); // Matrix, Inception, Dark Knight

        // Test 3: Boolean NOT operation
        results = redis.ftSearch(MOVIES_INDEX, "((@tags:{action}) (-@tags:{thriller}))", null);
        assertThat(results.getCount()).isEqualTo(2); // Matrix Reloaded, The Dark Knight

        // Test 4: Phrase matching

        results = redis.ftSearch(MOVIES_INDEX, "@title:\"Inception\"", null);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("title")).isEqualTo("Inception");

        // Test 5: Wildcard search
        results = redis.ftSearch(MOVIES_INDEX, "Matrix*", null);
        assertThat(results.getCount()).isEqualTo(2); // Both Matrix movies

        // Test 6: Complex query with numeric range and text search
        results = redis.ftSearch(MOVIES_INDEX, "@rating:[8.0 9.5] @tags:{action}", null);
        assertThat(results.getCount()).isEqualTo(2); // The Matrix and The Dark Knight

        // Test 7: Field-specific search with OR
        results = redis.ftSearch(MOVIES_INDEX, "@title:(Matrix | Inception)", null);
        assertThat(results.getCount()).isEqualTo(3); // All Matrix movies and Inception

        // Cleanup
        redis.ftDropindex(MOVIES_INDEX, false);
    }

    /**
     * Test empty search results and edge cases.
     */
    @Test
    void testEmptyResultsAndEdgeCases() {
        // Create a simple index
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Collections.singletonList(titleField));

        // Add one document
        Map<String, String> post1 = new HashMap<>();
        post1.put("title", "Redis Tutorial");
        redis.hmset("blog:post:1", post1);

        // Test 1: Search for non-existent term
        SearchResults<String, String> results = redis.ftSearch(BLOG_INDEX, "nonexistent", null);
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
        redis.ftDropindex(BLOG_INDEX, false);
    }

}
