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
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis JSON indexing functionality based on the Redis documentation tutorial.
 * <p>
 * These tests are based on the examples from the Redis documentation:
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/indexing/">...</a>
 * <p>
 * The tests demonstrate how to index JSON documents, perform searches, and use various field types including TEXT, TAG,
 * NUMERIC, and VECTOR fields with JSON data.
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RedisJsonIndexingIntegrationTests {

    // Index names
    private static final String ITEM_INDEX = "itemIdx";

    private static final String ITEM_INDEX_2 = "itemIdx2";

    private static final String ITEM_INDEX_3 = "itemIdx3";

    private static final String ITEM_INDEX_4 = "itemIdx4";

    // Key prefixes
    private static final String ITEM_PREFIX = "item:";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RedisJsonIndexingIntegrationTests() {
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
     * Test basic JSON indexing and search functionality based on the Redis documentation tutorial. Creates an index for
     * inventory items with TEXT and NUMERIC fields.
     */
    @Test
    void testBasicJsonIndexingAndSearch() {
        // Create index based on Redis documentation example:
        // FT.CREATE itemIdx ON JSON PREFIX 1 item: SCHEMA $.name AS name TEXT $.description as description TEXT $.price AS
        // price NUMERIC
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("$.name").as("name").build();
        FieldArgs<String> descriptionField = TextFieldArgs.<String> builder().name("$.description").as("description").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("$.price").as("price").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(ITEM_PREFIX)
                .on(CreateArgs.TargetType.JSON).build();

        String result = redis.ftCreate(ITEM_INDEX, createArgs, Arrays.asList(nameField, descriptionField, priceField));
        assertThat(result).isEqualTo("OK");

        // Add JSON documents using JSON.SET
        JsonValue item1 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"Noise-cancelling Bluetooth headphones\","
                        + "\"description\":\"Wireless Bluetooth headphones with noise-cancelling technology\","
                        + "\"connection\":{\"wireless\":true,\"type\":\"Bluetooth\"}," + "\"price\":99.98,\"stock\":25,"
                        + "\"colors\":[\"black\",\"silver\"]}");

        JsonValue item2 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"Wireless earbuds\"," + "\"description\":\"Wireless Bluetooth in-ear headphones\","
                        + "\"connection\":{\"wireless\":true,\"type\":\"Bluetooth\"}," + "\"price\":64.99,\"stock\":17,"
                        + "\"colors\":[\"black\",\"white\"]}");

        assertThat(redis.jsonSet("item:1", JsonPath.ROOT_PATH, item1)).isEqualTo("OK");
        assertThat(redis.jsonSet("item:2", JsonPath.ROOT_PATH, item2)).isEqualTo("OK");

        // Test 1: Search for items with "earbuds" in the name
        SearchReply<String, String> searchReply = redis.ftSearch(ITEM_INDEX, "@name:(earbuds)", null);
        assertThat(searchReply.getCount()).isEqualTo(1);
        assertThat(searchReply.getResults()).hasSize(1);
        assertThat(searchReply.getResults().get(0).getId()).isEqualTo("item:2");

        // Test 2: Search for items with "bluetooth" and "headphones" in description
        searchReply = redis.ftSearch(ITEM_INDEX, "@description:(bluetooth headphones)", null);
        assertThat(searchReply.getCount()).isEqualTo(2);
        assertThat(searchReply.getResults()).hasSize(2);

        // Test 3: Search for Bluetooth headphones with price less than 70
        searchReply = redis.ftSearch(ITEM_INDEX, "@description:(bluetooth headphones) @price:[0 70]", null);
        assertThat(searchReply.getCount()).isEqualTo(1);
        assertThat(searchReply.getResults().get(0).getId()).isEqualTo("item:2");

        // Cleanup
        redis.ftDropindex(ITEM_INDEX, false);
    }

    /**
     * Test indexing JSON arrays as TAG fields with custom separators. Based on the Redis documentation example for indexing
     * colors.
     */
    @Test
    void testJsonArraysAsTagFields() {
        // Create index with TAG field for colors using wildcard JSONPath
        // FT.CREATE itemIdx2 ON JSON PREFIX 1 item: SCHEMA $.colors.* AS colors TAG $.name AS name TEXT
        FieldArgs<String> colorsField = TagFieldArgs.<String> builder().name("$.colors.*").as("colors").build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("$.name").as("name").build();
        FieldArgs<String> descriptionField = TextFieldArgs.<String> builder().name("$.description").as("description").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(ITEM_PREFIX)
                .on(CreateArgs.TargetType.JSON).build();

        redis.ftCreate(ITEM_INDEX_2, createArgs, Arrays.asList(colorsField, nameField, descriptionField));

        // Add sample items with color arrays
        JsonValue item1 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"Noise-cancelling Bluetooth headphones\","
                        + "\"description\":\"Wireless Bluetooth headphones with noise-cancelling technology\","
                        + "\"colors\":[\"black\",\"silver\"]}");

        JsonValue item2 = redis.getJsonParser().createJsonValue("{\"name\":\"Wireless earbuds\","
                + "\"description\":\"Wireless Bluetooth in-ear headphones\"," + "\"colors\":[\"black\",\"white\"]}");

        JsonValue item3 = redis.getJsonParser().createJsonValue("{\"name\":\"True Wireless earbuds\","
                + "\"description\":\"True Wireless Bluetooth in-ear headphones\"," + "\"colors\":[\"red\",\"light blue\"]}");

        redis.jsonSet("item:1", JsonPath.ROOT_PATH, item1);
        redis.jsonSet("item:2", JsonPath.ROOT_PATH, item2);
        redis.jsonSet("item:3", JsonPath.ROOT_PATH, item3);

        // Test 1: Search for silver headphones
        SearchReply<String, String> results = redis.ftSearch(ITEM_INDEX_2,
                "@colors:{silver} (@name:(headphones)|@description:(headphones))", null);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getId()).isEqualTo("item:1");

        // Test 2: Search for black items
        results = redis.ftSearch(ITEM_INDEX_2, "@colors:{black}", null);
        assertThat(results.getCount()).isEqualTo(2);

        // Test 3: Search for white or light colored items
        results = redis.ftSearch(ITEM_INDEX_2, "@colors:{white|light}", null);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getId()).isEqualTo("item:2");

        // Cleanup
        redis.ftDropindex(ITEM_INDEX_2, false);
    }

    /**
     * Test indexing JSON arrays as TEXT fields for full-text search. Based on Redis documentation example for searching within
     * array content.
     */
    @Test
    void testJsonArraysAsTextFields() {
        // Create index with TEXT field for colors array
        // FT.CREATE itemIdx3 ON JSON PREFIX 1 item: SCHEMA $.colors AS colors TEXT $.name AS name TEXT
        FieldArgs<String> colorsField = TextFieldArgs.<String> builder().name("$.colors").as("colors").build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("$.name").as("name").build();
        FieldArgs<String> descriptionField = TextFieldArgs.<String> builder().name("$.description").as("description").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(ITEM_PREFIX)
                .on(CreateArgs.TargetType.JSON).build();

        redis.ftCreate(ITEM_INDEX_3, createArgs, Arrays.asList(colorsField, nameField, descriptionField));

        // Add sample items
        JsonValue item2 = redis.getJsonParser().createJsonValue("{\"name\":\"Wireless earbuds\","
                + "\"description\":\"Wireless Bluetooth in-ear headphones\"," + "\"colors\":[\"black\",\"white\"]}");

        JsonValue item3 = redis.getJsonParser().createJsonValue("{\"name\":\"True Wireless earbuds\","
                + "\"description\":\"True Wireless Bluetooth in-ear headphones\"," + "\"colors\":[\"red\",\"light blue\"]}");

        redis.jsonSet("item:2", JsonPath.ROOT_PATH, item2);
        redis.jsonSet("item:3", JsonPath.ROOT_PATH, item3);

        // Test full text search for light colored headphones
        SearchArgs<String, String> returnArgs = SearchArgs.<String, String> builder().returnField("$.colors").build();
        SearchReply<String, String> results = redis.ftSearch(ITEM_INDEX_3,
                "@colors:(white|light) (@name|description:(headphones))", returnArgs);
        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);

        // Cleanup
        redis.ftDropindex(ITEM_INDEX_3, false);
    }

    /**
     * Test indexing JSON arrays as NUMERIC fields for range queries. Based on Redis documentation example for indexing
     * max_level arrays.
     */
    @Test
    void testJsonArraysAsNumericFields() {
        // Create index with NUMERIC field for max_level array
        // FT.CREATE itemIdx4 ON JSON PREFIX 1 item: SCHEMA $.max_level AS dB NUMERIC
        FieldArgs<String> dbField = NumericFieldArgs.<String> builder().name("$.max_level").as("dB").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(ITEM_PREFIX)
                .on(CreateArgs.TargetType.JSON).build();

        redis.ftCreate(ITEM_INDEX_4, createArgs, Collections.singletonList(dbField));

        // Add sample items with max_level arrays
        JsonValue item1 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"Noise-cancelling Bluetooth headphones\"," + "\"max_level\":[60,70,80,90,100]}");

        JsonValue item2 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"Wireless earbuds\"," + "\"max_level\":[80,100,120]}");

        JsonValue item3 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"True Wireless earbuds\"," + "\"max_level\":[90,100,110,120]}");

        redis.jsonSet("item:1", JsonPath.ROOT_PATH, item1);
        redis.jsonSet("item:2", JsonPath.ROOT_PATH, item2);
        redis.jsonSet("item:3", JsonPath.ROOT_PATH, item3);

        // Test 1: Search for headphones with max volume between 70 and 80 (inclusive)
        SearchReply<String, String> results = redis.ftSearch(ITEM_INDEX_4, "@dB:[70 80]", null);
        assertThat(results.getCount()).isEqualTo(2); // item:1 and item:2

        // Test 2: Search for items with all values in range [90, 120]
        results = redis.ftSearch(ITEM_INDEX_4, "-@dB:[-inf (90] -@dB:[(120 +inf]", null);
        assertThat(results.getCount()).isEqualTo(1); // item:3
        assertThat(results.getResults().get(0).getId()).isEqualTo("item:3");

        // Cleanup
        redis.ftDropindex(ITEM_INDEX_4, false);
    }

    /**
     * Test field projection with JSONPath expressions. Based on Redis documentation example for returning specific attributes.
     */
    @Test
    void testFieldProjectionWithJsonPath() {
        // Create basic index
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("$.name").as("name").build();
        FieldArgs<String> descriptionField = TextFieldArgs.<String> builder().name("$.description").as("description").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("$.price").as("price").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(ITEM_PREFIX)
                .on(CreateArgs.TargetType.JSON).build();

        redis.ftCreate(ITEM_INDEX, createArgs, Arrays.asList(nameField, descriptionField, priceField));

        // Add sample items
        JsonValue item1 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"Noise-cancelling Bluetooth headphones\","
                        + "\"description\":\"Wireless Bluetooth headphones with noise-cancelling technology\","
                        + "\"price\":99.98,\"stock\":25}");

        JsonValue item2 = redis.getJsonParser().createJsonValue("{\"name\":\"Wireless earbuds\","
                + "\"description\":\"Wireless Bluetooth in-ear headphones\"," + "\"price\":64.99,\"stock\":17}");

        redis.jsonSet("item:1", JsonPath.ROOT_PATH, item1);
        redis.jsonSet("item:2", JsonPath.ROOT_PATH, item2);

        // Test 1: Return specific attributes (name and price)
        SearchArgs<String, String> returnArgs = SearchArgs.<String, String> builder().returnField("name").returnField("price")
                .build();
        SearchReply<String, String> results = redis.ftSearch(ITEM_INDEX, "@description:(headphones)", returnArgs);
        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getFields()).containsKey("name");
            assertThat(result.getFields()).containsKey("price");
            assertThat(result.getFields()).doesNotContainKey("description");
        }

        // Test 2: Project with JSONPath expression (including non-indexed field)
        SearchArgs<String, String> jsonPathArgs = SearchArgs.<String, String> builder().returnField("name").returnField("price")
                .returnField("$.stock") // JSONPath without alias
                .build();
        results = redis.ftSearch(ITEM_INDEX, "@description:(headphones)", jsonPathArgs);
        assertThat(results.getCount()).isEqualTo(2);
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getFields()).containsKey("name");
            assertThat(result.getFields()).containsKey("price");
            assertThat(result.getFields()).containsKey("$.stock");
        }

        // Cleanup
        redis.ftDropindex(ITEM_INDEX, false);
    }

    /**
     * Test indexing JSON objects by indexing individual elements. Based on Redis documentation example for connection object.
     */
    @Test
    void testJsonObjectIndexing() {
        // Create index for individual object elements
        // FT.CREATE itemIdx ON JSON SCHEMA $.connection.wireless AS wireless TAG $.connection.type AS connectionType TEXT
        FieldArgs<String> wirelessField = TagFieldArgs.<String> builder().name("$.connection.wireless").as("wireless").build();
        FieldArgs<String> connectionTypeField = TextFieldArgs.<String> builder().name("$.connection.type").as("connectionType")
                .build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(ITEM_PREFIX)
                .on(CreateArgs.TargetType.JSON).build();

        redis.ftCreate(ITEM_INDEX, createArgs, Arrays.asList(wirelessField, connectionTypeField));

        // Add sample items with connection objects
        JsonValue item1 = redis.getJsonParser().createJsonValue("{\"name\":\"Noise-cancelling Bluetooth headphones\","
                + "\"connection\":{\"wireless\":true,\"type\":\"Bluetooth\"}}");

        JsonValue item2 = redis.getJsonParser()
                .createJsonValue("{\"name\":\"Wired headphones\"," + "\"connection\":{\"wireless\":false,\"type\":\"3.5mm\"}}");

        redis.jsonSet("item:1", JsonPath.ROOT_PATH, item1);
        redis.jsonSet("item:2", JsonPath.ROOT_PATH, item2);

        // Test 1: Search for wireless items
        SearchReply<String, String> results = redis.ftSearch(ITEM_INDEX, "@wireless:{true}", null);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getId()).isEqualTo("item:1");

        // Test 2: Search for Bluetooth connection type
        results = redis.ftSearch(ITEM_INDEX, "@connectionType:(bluetooth)", null);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getId()).isEqualTo("item:1");

        // Cleanup
        redis.ftDropindex(ITEM_INDEX, false);
    }

}
