/*
 * Copyright 2024-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for Redis Search functionality in a cluster environment.
 * <p>
 * These tests verify that FT.SEARCH and FT.CURSOR commands work correctly across multiple cluster nodes, ensuring that search
 * operations can find data distributed across different shards.
 * </p>
 *
 * @author Tihomir Mateev
 * @since 6.8
 */
@Tag(INTEGRATION_TEST)
public class RediSearchClusterIntegrationTests {

    // Index names
    private static final String PRODUCTS_INDEX = "products-cluster-idx";

    private static final String BOOKS_INDEX = "books-cluster-idx";

    // Prefixes
    private static final String PRODUCT_PREFIX = "product:cluster:";

    private static final String BOOK_PREFIX = "book:cluster:";

    protected static RedisClusterClient client;

    protected static RedisAdvancedClusterCommands<String, String> redis;

    public RediSearchClusterIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(36379).build();
        client = RedisClusterClient.create(redisURI);
        redis = client.connect().sync();
    }

    @BeforeEach
    public void prepare() {
        // 7.4 and 7.2 have a different behavior, but we do not want to test for old versions
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));

        redis.flushall();
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * Test FT.SEARCH command in cluster environment with data distributed across multiple shards. This test creates an index,
     * inserts data with keys that hash to different slots, and verifies that search works across all cluster nodes.
     */
    @Test
    void testFtSearchAcrossMultipleShards() {
        // Create field definitions
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();
        FieldArgs<String> categoryField = TagFieldArgs.<String> builder().name("category").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("price").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PRODUCT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        // Create index on all cluster nodes
        assertThat(redis.ftCreate(PRODUCTS_INDEX, createArgs, Arrays.asList(nameField, categoryField, priceField)))
                .isEqualTo("OK");

        // Create test data with keys that hash to different slots
        String[] productKeys = { "product:cluster:laptop1", // Different hash slots
                "product:cluster:mouse2", "product:cluster:keyboard3", "product:cluster:monitor4", "product:cluster:tablet5",
                "product:cluster:phone6" };

        // Verify keys are distributed across different slots
        Map<String, Integer> keySlots = new HashMap<>();
        for (String key : productKeys) {
            int slot = SlotHash.getSlot(key);
            keySlots.put(key, slot);
        }

        // Ensure we have keys in at least 2 different slots
        long uniqueSlots = keySlots.values().stream().distinct().count();
        assertThat(uniqueSlots).isGreaterThanOrEqualTo(2);

        // Insert test data
        Map<String, String> laptop = new HashMap<>();
        laptop.put("name", "Gaming Laptop");
        laptop.put("category", "electronics");
        laptop.put("price", "1299.99");
        redis.hmset(productKeys[0], laptop);

        Map<String, String> mouse = new HashMap<>();
        mouse.put("name", "Wireless Mouse");
        mouse.put("category", "electronics");
        mouse.put("price", "29.99");
        redis.hmset(productKeys[1], mouse);

        Map<String, String> keyboard = new HashMap<>();
        keyboard.put("name", "Mechanical Keyboard");
        keyboard.put("category", "electronics");
        keyboard.put("price", "149.99");
        redis.hmset(productKeys[2], keyboard);

        Map<String, String> monitor = new HashMap<>();
        monitor.put("name", "4K Monitor");
        monitor.put("category", "electronics");
        monitor.put("price", "399.99");
        redis.hmset(productKeys[3], monitor);

        Map<String, String> tablet = new HashMap<>();
        tablet.put("name", "Android Tablet");
        tablet.put("category", "mobile");
        tablet.put("price", "299.99");
        redis.hmset(productKeys[4], tablet);

        Map<String, String> phone = new HashMap<>();
        phone.put("name", "Smartphone");
        phone.put("category", "mobile");
        phone.put("price", "699.99");
        redis.hmset(productKeys[5], phone);

        // Test 1: Search for all electronics across cluster
        SearchReply<String, String> searchResults = redis.ftSearch(PRODUCTS_INDEX, "@category:{electronics}");

        // Verify we get results - should find laptop, mouse, keyboard, monitor
        assertThat(searchResults.getCount()).isEqualTo(4);
        assertThat(searchResults.getResults()).hasSize(4);

        // Test 2: Search with price range across cluster
        SearchArgs<String, String> priceSearchArgs = SearchArgs.<String, String> builder().build();
        SearchReply<String, String> priceResults = redis.ftSearch(PRODUCTS_INDEX, "@price:[100 500]", priceSearchArgs);

        // Should find keyboard, monitor, tablet (prices 149.99, 399.99, 299.99)
        assertThat(priceResults.getCount()).isEqualTo(3);

        // Test 3: Text search across cluster
        SearchReply<String, String> textResults = redis.ftSearch(PRODUCTS_INDEX, "@name:Gaming");

        // Should find only the Gaming Laptop
        assertThat(textResults.getCount()).isEqualTo(1);
        assertThat(textResults.getResults().get(0).getFields().get("name")).isEqualTo("Gaming Laptop");

        // Cleanup
        redis.ftDropindex(PRODUCTS_INDEX);
    }

    /**
     * Test FT.CURSOR functionality in cluster environment. This test creates an aggregation with cursor and verifies cursor
     * operations work across cluster nodes.
     */
    @Test
    void testFtCursorAcrossMultipleShards() {
        // Create field definitions for books
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> authorField = TagFieldArgs.<String> builder().name("author").build();
        FieldArgs<String> yearField = NumericFieldArgs.<String> builder().name("year").sortable().build();
        FieldArgs<String> ratingField = NumericFieldArgs.<String> builder().name("rating").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(BOOK_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        // Create index on cluster
        String createResult = redis.ftCreate(BOOKS_INDEX, createArgs,
                Arrays.asList(titleField, authorField, yearField, ratingField));

        // Verify index creation
        assertThat(createResult).isEqualTo("OK");

        // Create test data with keys that hash to different slots
        String[] bookKeys = { "book:cluster:scifi1", "book:cluster:fantasy2", "book:cluster:mystery3", "book:cluster:romance4",
                "book:cluster:thriller5", "book:cluster:biography6", "book:cluster:history7", "book:cluster:science8" };

        // Insert books data
        String[][] booksData = { { "Dune", "frank_herbert", "1965", "4.2" }, { "Lord of the Rings", "tolkien", "1954", "4.5" },
                { "Sherlock Holmes", "doyle", "1887", "4.1" }, { "Pride and Prejudice", "austen", "1813", "4.0" },
                { "Gone Girl", "flynn", "2012", "3.9" }, { "Steve Jobs", "isaacson", "2011", "4.3" },
                { "Sapiens", "harari", "2011", "4.4" }, { "Cosmos", "sagan", "1980", "4.6" } };

        for (int i = 0; i < bookKeys.length; i++) {
            Map<String, String> book = new HashMap<>();
            book.put("title", booksData[i][0]);
            book.put("author", booksData[i][1]);
            book.put("year", booksData[i][2]);
            book.put("rating", booksData[i][3]);
            redis.hmset(bookKeys[i], book);
        }

        // Test aggregation with cursor - group by author and get average rating
        AggregateArgs<String, String> aggregateArgs = AggregateArgs.<String, String> builder()
                .groupBy(AggregateArgs.GroupBy.<String, String> of("author")
                        .reduce(AggregateArgs.Reducer.<String, String> avg("@rating").as("avg_rating")))
                .withCursor(AggregateArgs.WithCursor.of(2L)) // Small batch size to test cursor functionality
                .build();

        // Execute aggregation with cursor
        AggregationReply<String, String> aggregateResults = redis.ftAggregate(BOOKS_INDEX, "*", aggregateArgs);

        // Verify we get results with cursor
        assertThat(aggregateResults).isNotNull();
        assertThat(aggregateResults.getAggregationGroups()).isGreaterThan(0);

        // Test cursor read functionality if cursor is available
        if (aggregateResults.getCursorId() != -1 && aggregateResults.getCursorId() > 0) {
            // Read next batch using cursor
            AggregationReply<String, String> cursorResults = redis.ftCursorread(BOOKS_INDEX, aggregateResults.getCursorId());

            // Verify cursor read works
            assertThat(cursorResults).isNotNull();

            // The cursor results should be valid (either have data or indicate completion)
            // Cursor ID of 0 indicates end of results
            assertThat(cursorResults.getCursorId()).isGreaterThanOrEqualTo(0);
        }

        // Cleanup
        redis.ftDropindex(BOOKS_INDEX);
    }

}
