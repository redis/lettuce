/*
 * Copyright 2024-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.bytearraycodec;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.test.condition.RedisByteArrayConditions;
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
 * Integration tests for Redis Search functionality in a cluster environment using {@link ByteArrayCodec}.
 * <p>
 * These tests verify that FT.SEARCH and FT.CURSOR commands work correctly across multiple cluster nodes, ensuring that search
 * operations can find data distributed across different shards.
 * </p>
 *
 * @author Viktoriya Kutsarova
 * @since 6.8
 */
@Tag(INTEGRATION_TEST)
public class RediSearchClusterByteArrayCodecIntegrationTests {

    // Index names
    private static final String PRODUCTS_INDEX = "products-cluster-idx";

    private static final String BOOKS_INDEX = "books-cluster-idx";

    // Prefixes
    private static final String PRODUCT_PREFIX = "product:cluster:";

    private static final String BOOK_PREFIX = "book:cluster:";

    protected static RedisClusterClient client;

    protected static RedisAdvancedClusterCommands<byte[], byte[]> redis;

    public RediSearchClusterByteArrayCodecIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(36379).build();
        client = RedisClusterClient.create(redisURI);
        redis = client.connect(ByteArrayCodec.INSTANCE).sync();
    }

    @BeforeEach
    public void prepare() {
        // 7.4 and 7.2 have a different behavior, but we do not want to test for old versions
        assumeTrue(RedisByteArrayConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));

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
     * Test FT.SEARCH command in cluster environment with data distributed across multiple shards. This test creates an index,
     * inserts data with keys that hash to different slots, and verifies that search works across all cluster nodes.
     */
    @Test
    void testFtSearchAcrossMultipleShards() {
        // Create field definitions
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();
        FieldArgs<byte[]> categoryField = TagFieldArgs.<byte[]> builder().name("category".getBytes()).build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(PRODUCT_PREFIX.getBytes())
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
        Map<byte[], byte[]> laptop = new HashMap<>();
        laptop.put("name".getBytes(), "Gaming Laptop".getBytes());
        laptop.put("category".getBytes(), "electronics".getBytes());
        laptop.put("price".getBytes(), "1299.99".getBytes());
        redis.hmset(productKeys[0].getBytes(), laptop);

        Map<byte[], byte[]> mouse = new HashMap<>();
        mouse.put("name".getBytes(), "Wireless Mouse".getBytes());
        mouse.put("category".getBytes(), "electronics".getBytes());
        mouse.put("price".getBytes(), "29.99".getBytes());
        redis.hmset(productKeys[1].getBytes(), mouse);

        Map<byte[], byte[]> keyboard = new HashMap<>();
        keyboard.put("name".getBytes(), "Mechanical Keyboard".getBytes());
        keyboard.put("category".getBytes(), "electronics".getBytes());
        keyboard.put("price".getBytes(), "149.99".getBytes());
        redis.hmset(productKeys[2].getBytes(), keyboard);

        Map<byte[], byte[]> monitor = new HashMap<>();
        monitor.put("name".getBytes(), "4K Monitor".getBytes());
        monitor.put("category".getBytes(), "electronics".getBytes());
        monitor.put("price".getBytes(), "399.99".getBytes());
        redis.hmset(productKeys[3].getBytes(), monitor);

        Map<byte[], byte[]> tablet = new HashMap<>();
        tablet.put("name".getBytes(), "Android Tablet".getBytes());
        tablet.put("category".getBytes(), "mobile".getBytes());
        tablet.put("price".getBytes(), "299.99".getBytes());
        redis.hmset(productKeys[4].getBytes(), tablet);

        Map<byte[], byte[]> phone = new HashMap<>();
        phone.put("name".getBytes(), "Smartphone".getBytes());
        phone.put("category".getBytes(), "mobile".getBytes());
        phone.put("price".getBytes(), "699.99".getBytes());
        redis.hmset(productKeys[5].getBytes(), phone);

        // Test 1: Search for all electronics across cluster
        SearchReply<byte[], byte[]> searchResults = redis.ftSearch(PRODUCTS_INDEX, "@category:{electronics}".getBytes());

        // Verify we get results - should find laptop, mouse, keyboard, monitor
        assertThat(searchResults.getCount()).isEqualTo(4);
        assertThat(searchResults.getResults()).hasSize(4);

        // Test 2: Search with price range across cluster
        SearchArgs<byte[], byte[]> priceSearchArgs = SearchArgs.<byte[], byte[]> builder().build();
        SearchReply<byte[], byte[]> priceResults = redis.ftSearch(PRODUCTS_INDEX, "@price:[100 500]".getBytes(),
                priceSearchArgs);

        // Should find keyboard, monitor, tablet (prices 149.99, 399.99, 299.99)
        assertThat(priceResults.getCount()).isEqualTo(3);

        // Test 3: Text search across cluster
        SearchReply<byte[], byte[]> textResults = redis.ftSearch(PRODUCTS_INDEX, "@name:Gaming".getBytes());

        // Should find only the Gaming Laptop
        assertThat(textResults.getCount()).isEqualTo(1);
        assertThat(new String(getField(textResults.getResults().get(0).getFields(), "name"))).isEqualTo("Gaming Laptop");

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
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> authorField = TagFieldArgs.<byte[]> builder().name("author".getBytes()).build();
        FieldArgs<byte[]> yearField = NumericFieldArgs.<byte[]> builder().name("year".getBytes()).sortable().build();
        FieldArgs<byte[]> ratingField = NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BOOK_PREFIX.getBytes())
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
            Map<byte[], byte[]> book = new HashMap<>();
            book.put("title".getBytes(), booksData[i][0].getBytes());
            book.put("author".getBytes(), booksData[i][1].getBytes());
            book.put("year".getBytes(), booksData[i][2].getBytes());
            book.put("rating".getBytes(), booksData[i][3].getBytes());
            redis.hmset(bookKeys[i].getBytes(), book);
        }

        // Test aggregation with cursor - group by author and get average rating
        AggregateArgs<byte[], byte[]> aggregateArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(AggregateArgs.GroupBy.<byte[], byte[]> of("author".getBytes())
                        .reduce(AggregateArgs.Reducer.<byte[], byte[]> avg("@rating".getBytes()).as("avg_rating".getBytes())))
                .withCursor(AggregateArgs.WithCursor.of(2L)) // Small batch size to test cursor functionality
                .build();

        // Execute aggregation with cursor
        AggregationReply<byte[], byte[]> aggregateResults = redis.ftAggregate(BOOKS_INDEX, "*".getBytes(), aggregateArgs);

        // Verify we get results with cursor
        assertThat(aggregateResults).isNotNull();
        assertThat(aggregateResults.getAggregationGroups()).isGreaterThan(0);

        // Test cursor read functionality if cursor is available
        if (aggregateResults.getCursor().isPresent() && aggregateResults.getCursor().get().getCursorId() > 0) {
            // Read next batch using cursor
            AggregationReply<byte[], byte[]> cursorResults = redis.ftCursorread(BOOKS_INDEX,
                    aggregateResults.getCursor().get());

            // Verify cursor read works
            assertThat(cursorResults).isNotNull();

            // The cursor results should be valid (either have data or indicate completion)
            // Cursor ID of 0 indicates end of results
            assertThat(cursorResults.getCursor().get().getCursorId()).isGreaterThanOrEqualTo(0);
        }

        // Cleanup
        redis.ftDropindex(BOOKS_INDEX);
    }

}
