/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.bytearraycodec;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.AggregateArgs.GroupBy;
import io.lettuce.core.search.arguments.AggregateArgs.Reducer;
import io.lettuce.core.search.arguments.AggregateArgs.SortDirection;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.QueryDialects;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for Redis FT.AGGREGATE command using {@link ByteArrayCodec}.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchAggregateByteArrayCodecIntegrationTests {

    private static RedisClient client;

    private RedisCommands<byte[], byte[]> redis;

    RediSearchAggregateByteArrayCodecIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect(ByteArrayCodec.INSTANCE).sync();
    }

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
    }

    @BeforeEach
    void setUp() {
        redis.flushall();
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * Helper method to find a field value in a byte[] keyed map.
     */
    private byte[] getField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] key = fieldName.getBytes();
        for (Map.Entry<byte[], byte[]> entry : fields.entrySet()) {
            if (Arrays.equals(entry.getKey(), key)) {
                return entry.getValue(); // may be null for JSON null fields
            }
        }
        return null;
    }

    /**
     * Helper to check that a byte[] map contains a key.
     */
    private boolean hasField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] key = fieldName.getBytes();
        return fields.entrySet().stream().anyMatch(e -> Arrays.equals(e.getKey(), key));
    }

    /**
     * Helper to get a String value from a byte[] map.
     */
    private String getStringField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] val = getField(fields, fieldName);
        return val == null ? null : new String(val);
    }

    /**
     * Helper to put a key-value pair into a byte[] map.
     */
    private void putField(Map<byte[], byte[]> map, String key, String value) {
        map.put(key.getBytes(), value.getBytes());
    }

    @Test
    void shouldPerformBasicAggregation() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("doc:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("basic-test-idx", createArgs, fields)).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        putField(doc1, "title", "iPhone 13");
        putField(doc1, "category", "electronics");
        assertThat(redis.hmset("doc:1".getBytes(), doc1)).isEqualTo("OK");

        Map<byte[], byte[]> doc2 = new HashMap<>();
        putField(doc2, "title", "Samsung Galaxy");
        putField(doc2, "category", "electronics");
        assertThat(redis.hmset("doc:2".getBytes(), doc2)).isEqualTo("OK");

        Map<byte[], byte[]> doc3 = new HashMap<>();
        putField(doc3, "title", "MacBook Pro");
        putField(doc3, "category", "computers");
        assertThat(redis.hmset("doc:3".getBytes(), doc3)).isEqualTo("OK");

        Map<byte[], byte[]> doc4 = new HashMap<>();
        putField(doc4, "title", "iPad Air");
        putField(doc4, "category", "electronics");
        assertThat(redis.hmset("doc:4".getBytes(), doc4)).isEqualTo("OK");

        SearchReply<byte[], byte[]> searchResult = redis.ftSearch("basic-test-idx", "*".getBytes());
        assertThat(searchResult.getCount()).isEqualTo(4);

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("basic-test-idx", "*".getBytes());

        assertThat(result).isNotNull();
        if (searchResult.getCount() > 0) {
            assertThat(result.getAggregationGroups()).isEqualTo(1);
            assertThat(result.getReplies()).hasSize(1);
            assertThat(result.getReplies().get(0).getResults()).hasSize(4);

            for (SearchReply.SearchResult<byte[], byte[]> aggregateResult : result.getReplies().get(0).getResults()) {
                assertThat(aggregateResult.getFields()).isEmpty();
            }
        } else {
            assertThat(result.getAggregationGroups()).isEqualTo(0);
            assertThat(result.getReplies()).isEmpty();
        }

        assertThat(redis.ftDropindex("basic-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithArgs() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        assertThat(redis.ftCreate("args-test-idx", fields)).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        putField(doc1, "title", "iPhone 13");
        putField(doc1, "category", "electronics");
        assertThat(redis.hmset("doc:1".getBytes(), doc1)).isEqualTo("OK");

        Map<byte[], byte[]> doc2 = new HashMap<>();
        putField(doc2, "title", "Samsung Galaxy");
        putField(doc2, "category", "electronics");
        assertThat(redis.hmset("doc:2".getBytes(), doc2)).isEqualTo("OK");

        Map<byte[], byte[]> doc3 = new HashMap<>();
        putField(doc3, "title", "MacBook Pro");
        putField(doc3, "category", "computers");
        assertThat(redis.hmset("doc:3".getBytes(), doc3)).isEqualTo("OK");

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().verbatim().load("title".getBytes())
                .load("category".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("args-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(3);

        for (SearchReply.SearchResult<byte[], byte[]> aggregateResult : searchReply.getResults()) {
            assertThat(hasField(aggregateResult.getFields(), "title")).isTrue();
            assertThat(hasField(aggregateResult.getFields(), "category")).isTrue();
            assertThat(getField(aggregateResult.getFields(), "title")).isNotNull();
            assertThat(getField(aggregateResult.getFields(), "category")).isNotNull();
        }

        assertThat(redis.ftDropindex("args-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithParams() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        assertThat(redis.ftCreate("params-test-idx", fields)).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        putField(doc1, "title", "iPhone 13");
        putField(doc1, "category", "electronics");
        assertThat(redis.hmset("doc:1".getBytes(), doc1)).isEqualTo("OK");

        Map<byte[], byte[]> doc2 = new HashMap<>();
        putField(doc2, "title", "Samsung Galaxy");
        putField(doc2, "category", "electronics");
        assertThat(redis.hmset("doc:2".getBytes(), doc2)).isEqualTo("OK");

        Map<byte[], byte[]> doc3 = new HashMap<>();
        putField(doc3, "title", "MacBook Pro");
        putField(doc3, "category", "computers");
        assertThat(redis.hmset("doc:3".getBytes(), doc3)).isEqualTo("OK");

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().load("title".getBytes())
                .load("category".getBytes()).param("cat".getBytes(), "electronics".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("params-test-idx", "@category:$cat".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> aggregateResult : searchReply.getResults()) {
            assertThat(hasField(aggregateResult.getFields(), "title")).isTrue();
            assertThat(hasField(aggregateResult.getFields(), "category")).isTrue();
            assertThat(getStringField(aggregateResult.getFields(), "category")).isEqualTo("electronics");
        }

        assertThat(redis.ftDropindex("params-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithLoadAll() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        assertThat(redis.ftCreate("loadall-test-idx", fields)).isEqualTo("OK");

        Map<byte[], byte[]> doc1 = new HashMap<>();
        putField(doc1, "title", "iPhone 13");
        putField(doc1, "category", "electronics");
        assertThat(redis.hmset("doc:1".getBytes(), doc1)).isEqualTo("OK");

        Map<byte[], byte[]> doc2 = new HashMap<>();
        putField(doc2, "title", "Samsung Galaxy");
        putField(doc2, "category", "electronics");
        assertThat(redis.hmset("doc:2".getBytes(), doc2)).isEqualTo("OK");

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll().build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("loadall-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> aggregateResult : searchReply.getResults()) {
            assertThat(hasField(aggregateResult.getFields(), "title")).isTrue();
            assertThat(hasField(aggregateResult.getFields(), "category")).isTrue();
            assertThat(getField(aggregateResult.getFields(), "title")).isNotNull();
            assertThat(getField(aggregateResult.getFields(), "category")).isNotNull();
        }

        assertThat(redis.ftDropindex("loadall-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleEmptyResults() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        assertThat(redis.ftCreate("empty-test-idx", fields)).isEqualTo("OK");

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("empty-test-idx", "*".getBytes());

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies().get(0).getResults()).isEmpty();

        assertThat(redis.ftDropindex("empty-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldDemonstrateAdvancedAggregationScenarios() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("brand".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("stock".getBytes()).sortable().build());

        assertThat(redis.ftCreate("products-idx", fields)).isEqualTo("OK");

        Map<byte[], byte[]> product1 = new HashMap<>();
        putField(product1, "title", "iPhone 13 Pro");
        putField(product1, "brand", "Apple");
        putField(product1, "category", "smartphones");
        putField(product1, "price", "999");
        putField(product1, "rating", "4.5");
        putField(product1, "stock", "50");
        assertThat(redis.hmset("product:1".getBytes(), product1)).isEqualTo("OK");

        Map<byte[], byte[]> product2 = new HashMap<>();
        putField(product2, "title", "Samsung Galaxy S21");
        putField(product2, "brand", "Samsung");
        putField(product2, "category", "smartphones");
        putField(product2, "price", "799");
        putField(product2, "rating", "4.3");
        putField(product2, "stock", "30");
        assertThat(redis.hmset("product:2".getBytes(), product2)).isEqualTo("OK");

        Map<byte[], byte[]> product3 = new HashMap<>();
        putField(product3, "title", "MacBook Pro");
        putField(product3, "brand", "Apple");
        putField(product3, "category", "laptops");
        putField(product3, "price", "2499");
        putField(product3, "rating", "4.8");
        putField(product3, "stock", "15");
        assertThat(redis.hmset("product:3".getBytes(), product3)).isEqualTo("OK");

        Map<byte[], byte[]> product4 = new HashMap<>();
        putField(product4, "title", "Dell XPS 13");
        putField(product4, "brand", "Dell");
        putField(product4, "category", "laptops");
        putField(product4, "price", "1299");
        putField(product4, "rating", "4.2");
        putField(product4, "stock", "25");
        assertThat(redis.hmset("product:4".getBytes(), product4)).isEqualTo("OK");

        // Test basic aggregation with all fields loaded
        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll().build();
        AggregationReply<byte[], byte[]> result = redis.ftAggregate("products-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(4);

        Set<String> brands = searchReply.getResults().stream().map(r -> getStringField(r.getFields(), "brand"))
                .collect(Collectors.toSet());
        assertThat(brands).containsExactlyInAnyOrder("Apple", "Samsung", "Dell");

        Set<String> categories = searchReply.getResults().stream().map(r -> getStringField(r.getFields(), "category"))
                .collect(Collectors.toSet());
        assertThat(categories).containsExactlyInAnyOrder("smartphones", "laptops");

        // 1. Group by category with statistics
        AggregateArgs<byte[], byte[]> statsArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("category".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@price".getBytes()).as("avg_price".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> min("@price".getBytes()).as("min_price".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> max("@price".getBytes()).as("max_price".getBytes())))
                .build();

        AggregationReply<byte[], byte[]> statsResult = redis.ftAggregate("products-idx", "*".getBytes(), statsArgs);

        assertThat(statsResult).isNotNull();
        assertThat(statsResult.getAggregationGroups()).isEqualTo(1);
        assertThat(statsResult.getReplies()).hasSize(1);

        SearchReply<byte[], byte[]> statsReply = statsResult.getReplies().get(0);
        assertThat(statsReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> group : statsReply.getResults()) {
            assertThat(hasField(group.getFields(), "category")).isTrue();
            assertThat(hasField(group.getFields(), "count")).isTrue();
            assertThat(hasField(group.getFields(), "avg_price")).isTrue();
            assertThat(hasField(group.getFields(), "min_price")).isTrue();
            assertThat(hasField(group.getFields(), "max_price")).isTrue();

            double minPrice = Double.parseDouble(getStringField(group.getFields(), "min_price"));
            double avgPrice = Double.parseDouble(getStringField(group.getFields(), "avg_price"));
            double maxPrice = Double.parseDouble(getStringField(group.getFields(), "max_price"));

            assertThat(minPrice).isLessThanOrEqualTo(avgPrice);
            assertThat(avgPrice).isLessThanOrEqualTo(maxPrice);
        }

        // 2. Apply mathematical expressions
        AggregateArgs<byte[], byte[]> mathArgs = AggregateArgs.<byte[], byte[]> builder().load("title".getBytes())
                .load("price".getBytes()).load("stock".getBytes()).load("rating".getBytes())
                .apply("@price * @stock".getBytes(), "inventory_value".getBytes())
                .apply("ceil(@rating)".getBytes(), "rating_rounded".getBytes()).build();

        AggregationReply<byte[], byte[]> mathResult = redis.ftAggregate("products-idx", "*".getBytes(), mathArgs);

        assertThat(mathResult).isNotNull();
        assertThat(mathResult.getAggregationGroups()).isEqualTo(1);
        assertThat(mathResult.getReplies()).hasSize(1);

        SearchReply<byte[], byte[]> mathReply = mathResult.getReplies().get(0);
        assertThat(mathReply.getResults()).hasSize(4);

        for (SearchReply.SearchResult<byte[], byte[]> item : mathReply.getResults()) {
            assertThat(hasField(item.getFields(), "title")).isTrue();
            assertThat(hasField(item.getFields(), "inventory_value")).isTrue();
            assertThat(hasField(item.getFields(), "rating_rounded")).isTrue();

            double price = Double.parseDouble(getStringField(item.getFields(), "price"));
            double stock = Double.parseDouble(getStringField(item.getFields(), "stock"));
            double inventoryValue = Double.parseDouble(getStringField(item.getFields(), "inventory_value"));
            assertThat(inventoryValue).isEqualTo(price * stock);

            double rating = Double.parseDouble(getStringField(item.getFields(), "rating"));
            double ratingRounded = Double.parseDouble(getStringField(item.getFields(), "rating_rounded"));
            assertThat(ratingRounded).isEqualTo(Math.ceil(rating));
        }

        // 3. Filter and sort results
        AggregateArgs<byte[], byte[]> filterArgs = AggregateArgs.<byte[], byte[]> builder().load("title".getBytes())
                .load("price".getBytes()).load("rating".getBytes()).filter("@price > 1000".getBytes())
                .sortBy("rating".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> filterResult = redis.ftAggregate("products-idx", "*".getBytes(), filterArgs);

        assertThat(filterResult).isNotNull();
        assertThat(filterResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> filterReply = filterResult.getReplies().get(0);

        for (SearchReply.SearchResult<byte[], byte[]> item : filterReply.getResults()) {
            double price = Double.parseDouble(getStringField(item.getFields(), "price"));
            assertThat(price).isGreaterThan(1000);
        }

        if (filterReply.getResults().size() >= 2) {
            List<SearchReply.SearchResult<byte[], byte[]>> results = filterReply.getResults();
            for (int i = 0; i < results.size() - 1; i++) {
                double rating1 = Double.parseDouble(getStringField(results.get(i).getFields(), "rating"));
                double rating2 = Double.parseDouble(getStringField(results.get(i + 1).getFields(), "rating"));
                assertThat(rating1).isGreaterThanOrEqualTo(rating2);
            }
        }

        // 4. Complex pipeline with multiple operations
        AggregateArgs<byte[], byte[]> complexArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("brand".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("product_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@rating".getBytes()).as("avg_rating".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@stock".getBytes()).as("total_stock".getBytes())))
                .sortBy("avg_rating".getBytes(), SortDirection.DESC).limit(0, 3).build();

        AggregationReply<byte[], byte[]> complexResult = redis.ftAggregate("products-idx", "*".getBytes(), complexArgs);

        assertThat(complexResult).isNotNull();
        assertThat(complexResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> complexReply = complexResult.getReplies().get(0);

        for (SearchReply.SearchResult<byte[], byte[]> group : complexReply.getResults()) {
            assertThat(hasField(group.getFields(), "brand")).isTrue();
            assertThat(hasField(group.getFields(), "product_count")).isTrue();
            assertThat(hasField(group.getFields(), "avg_rating")).isTrue();
            assertThat(hasField(group.getFields(), "total_stock")).isTrue();
        }

        if (complexReply.getResults().size() >= 2) {
            List<SearchReply.SearchResult<byte[], byte[]>> results = complexReply.getResults();
            for (int i = 0; i < results.size() - 1; i++) {
                double rating1 = Double.parseDouble(getStringField(results.get(i).getFields(), "avg_rating"));
                double rating2 = Double.parseDouble(getStringField(results.get(i + 1).getFields(), "avg_rating"));
                assertThat(rating1).isGreaterThanOrEqualTo(rating2);
            }
        }

        assertThat(complexReply.getResults().size()).isLessThanOrEqualTo(3);

        // 5. String operations and functions
        AggregateArgs<byte[], byte[]> stringArgs = AggregateArgs.<byte[], byte[]> builder().load("title".getBytes())
                .load("brand".getBytes()).apply("upper(@brand)".getBytes(), "brand_upper".getBytes())
                .apply("substr(@title, 0, 10)".getBytes(), "title_short".getBytes()).build();

        AggregationReply<byte[], byte[]> stringResult = redis.ftAggregate("products-idx", "*".getBytes(), stringArgs);

        assertThat(stringResult).isNotNull();
        assertThat(stringResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> stringReply = stringResult.getReplies().get(0);

        for (SearchReply.SearchResult<byte[], byte[]> item : stringReply.getResults()) {
            assertThat(hasField(item.getFields(), "title")).isTrue();
            assertThat(hasField(item.getFields(), "brand")).isTrue();
            assertThat(hasField(item.getFields(), "brand_upper")).isTrue();
            assertThat(hasField(item.getFields(), "title_short")).isTrue();

            String brand = getStringField(item.getFields(), "brand");
            String brandUpper = getStringField(item.getFields(), "brand_upper");
            assertThat(brandUpper).isEqualTo(brand.toUpperCase());

            String title = getStringField(item.getFields(), "title");
            String titleShort = getStringField(item.getFields(), "title_short");
            assertThat(titleShort).isEqualTo(title.substring(0, Math.min(10, title.length())));
        }

        assertThat(redis.ftDropindex("products-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleNestedGroupByOperations() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("department".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("product".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("sales".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("profit".getBytes()).sortable().build());

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("sales:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("sales-idx", createArgs, fields)).isEqualTo("OK");

        Map<byte[], byte[]> salesData = new HashMap<>();
        putField(salesData, "department", "Electronics");
        putField(salesData, "category", "Smartphones");
        putField(salesData, "product", "iPhone 14");
        putField(salesData, "sales", "15000");
        putField(salesData, "profit", "3000");
        redis.hmset("sales:1".getBytes(), salesData);

        salesData = new HashMap<>();
        putField(salesData, "department", "Electronics");
        putField(salesData, "category", "Laptops");
        putField(salesData, "product", "MacBook Pro");
        putField(salesData, "sales", "25000");
        putField(salesData, "profit", "5000");
        redis.hmset("sales:2".getBytes(), salesData);

        salesData = new HashMap<>();
        putField(salesData, "department", "Electronics");
        putField(salesData, "category", "Smartphones");
        putField(salesData, "product", "Samsung Galaxy");
        putField(salesData, "sales", "12000");
        putField(salesData, "profit", "2400");
        redis.hmset("sales:3".getBytes(), salesData);

        salesData = new HashMap<>();
        putField(salesData, "department", "Clothing");
        putField(salesData, "category", "Shirts");
        putField(salesData, "product", "Cotton Shirt");
        putField(salesData, "sales", "5000");
        putField(salesData, "profit", "1500");
        redis.hmset("sales:4".getBytes(), salesData);

        AggregateArgs<byte[], byte[]> nestedArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("department".getBytes(), "category".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("product_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@sales".getBytes()).as("total_sales".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@profit".getBytes()).as("total_profit".getBytes())))
                .sortBy("total_sales".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> nestedResult = redis.ftAggregate("sales-idx", "*".getBytes(), nestedArgs);

        assertThat(nestedResult).isNotNull();
        assertThat(nestedResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> nestedReply = nestedResult.getReplies().get(0);

        for (SearchReply.SearchResult<byte[], byte[]> group : nestedReply.getResults()) {
            assertThat(hasField(group.getFields(), "department")).isTrue();
            assertThat(hasField(group.getFields(), "category")).isTrue();
            assertThat(hasField(group.getFields(), "product_count")).isTrue();
            assertThat(hasField(group.getFields(), "total_sales")).isTrue();
            assertThat(hasField(group.getFields(), "total_profit")).isTrue();
        }

        if (nestedReply.getResults().size() >= 2) {
            List<SearchReply.SearchResult<byte[], byte[]>> results = nestedReply.getResults();
            for (int i = 0; i < results.size() - 1; i++) {
                double sales1 = Double.parseDouble(getStringField(results.get(i).getFields(), "total_sales"));
                double sales2 = Double.parseDouble(getStringField(results.get(i + 1).getFields(), "total_sales"));
                assertThat(sales1).isGreaterThanOrEqualTo(sales2);
            }
        }

        assertThat(redis.ftDropindex("sales-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleAdvancedFilteringAndConditionals() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("status".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("priority".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("score".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("age".getBytes()).sortable().build());

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("task:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("tasks-idx", createArgs, fields)).isEqualTo("OK");

        Map<byte[], byte[]> taskData = new HashMap<>();
        putField(taskData, "status", "active");
        putField(taskData, "priority", "high");
        putField(taskData, "score", "95");
        putField(taskData, "age", "5");
        redis.hmset("task:1".getBytes(), taskData);

        taskData = new HashMap<>();
        putField(taskData, "status", "completed");
        putField(taskData, "priority", "medium");
        putField(taskData, "score", "85");
        putField(taskData, "age", "10");
        redis.hmset("task:2".getBytes(), taskData);

        taskData = new HashMap<>();
        putField(taskData, "status", "active");
        putField(taskData, "priority", "low");
        putField(taskData, "score", "70");
        putField(taskData, "age", "15");
        redis.hmset("task:3".getBytes(), taskData);

        taskData = new HashMap<>();
        putField(taskData, "status", "pending");
        putField(taskData, "priority", "high");
        putField(taskData, "score", "90");
        putField(taskData, "age", "3");
        redis.hmset("task:4".getBytes(), taskData);

        AggregateArgs<byte[], byte[]> filterArgs = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .filter("@score > 80 && @age < 12".getBytes()).apply("@score * 0.1".getBytes(), "normalized_score".getBytes())
                .sortBy("score".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> filterResult = redis.ftAggregate("tasks-idx", "*".getBytes(), filterArgs);

        assertThat(filterResult).isNotNull();
        assertThat(filterResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> filterReply = filterResult.getReplies().get(0);

        for (SearchReply.SearchResult<byte[], byte[]> item : filterReply.getResults()) {
            double score = Double.parseDouble(getStringField(item.getFields(), "score"));
            double age = Double.parseDouble(getStringField(item.getFields(), "age"));

            assertThat(score).isGreaterThan(80);
            assertThat(age).isLessThan(12);

            assertThat(hasField(item.getFields(), "normalized_score")).isTrue();
            double normalizedScore = Double.parseDouble(getStringField(item.getFields(), "normalized_score"));
            assertThat(normalizedScore).isEqualTo(score * 0.1);
        }

        assertThat(redis.ftDropindex("tasks-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleAdvancedStatisticalFunctions() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("region".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("temperature".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("humidity".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("pressure".getBytes()).sortable().build());

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("weather:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("weather-idx", createArgs, fields)).isEqualTo("OK");

        for (int i = 1; i <= 20; i++) {
            Map<byte[], byte[]> weatherData = new HashMap<>();
            putField(weatherData, "region", i <= 10 ? "north" : "south");
            putField(weatherData, "temperature", String.valueOf(20 + i));
            putField(weatherData, "humidity", String.valueOf(50 + (i % 5) * 5));
            putField(weatherData, "pressure", String.valueOf(1000 + i * 2));
            redis.hmset(("weather:" + i).getBytes(), weatherData);
        }

        AggregateArgs<byte[], byte[]> statsArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("region".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@temperature".getBytes()).as("avg_temp".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> min("@temperature".getBytes()).as("min_temp".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> max("@temperature".getBytes()).as("max_temp".getBytes())))
                .build();

        AggregationReply<byte[], byte[]> statsResult = redis.ftAggregate("weather-idx", "*".getBytes(), statsArgs);

        assertThat(statsResult).isNotNull();
        assertThat(statsResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> statsReply = statsResult.getReplies().get(0);
        assertThat(statsReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> region : statsReply.getResults()) {
            assertThat(hasField(region.getFields(), "region")).isTrue();
            assertThat(hasField(region.getFields(), "count")).isTrue();

            double minTemp = Double.parseDouble(getStringField(region.getFields(), "min_temp"));
            double avgTemp = Double.parseDouble(getStringField(region.getFields(), "avg_temp"));
            double maxTemp = Double.parseDouble(getStringField(region.getFields(), "max_temp"));

            assertThat(minTemp).isLessThanOrEqualTo(avgTemp);
            assertThat(avgTemp).isLessThanOrEqualTo(maxTemp);

            int count = Integer.parseInt(getStringField(region.getFields(), "count"));
            assertThat(count).isGreaterThan(0);
        }

        assertThat(redis.ftDropindex("weather-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleTimeoutParameter() {
        List<FieldArgs<byte[]>> fields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build());

        assertThat(redis.ftCreate("timeout-test-idx", fields)).isEqualTo("OK");

        Map<byte[], byte[]> doc = new HashMap<>();
        putField(doc, "title", "Test Document");
        assertThat(redis.hmset("doc:1".getBytes(), doc)).isEqualTo("OK");

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().load("title".getBytes())
                .timeout(Duration.ofSeconds(5)).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("timeout-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(1);
        assertThat(getStringField(searchReply.getResults().get(0).getFields(), "title")).isEqualTo("Test Document");

        assertThat(redis.ftDropindex("timeout-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithGroupBy() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).build());

        assertThat(redis.ftCreate("groupby-agg-test-idx", fields)).isEqualTo("OK");

        Map<byte[], byte[]> product1 = new HashMap<>();
        putField(product1, "title", "iPhone 13");
        putField(product1, "category", "electronics");
        putField(product1, "price", "999");
        putField(product1, "rating", "4.5");
        assertThat(redis.hmset("product:1".getBytes(), product1)).isEqualTo("OK");

        Map<byte[], byte[]> product2 = new HashMap<>();
        putField(product2, "title", "Samsung Galaxy");
        putField(product2, "category", "electronics");
        putField(product2, "price", "799");
        putField(product2, "rating", "4.3");
        assertThat(redis.hmset("product:2".getBytes(), product2)).isEqualTo("OK");

        Map<byte[], byte[]> product3 = new HashMap<>();
        putField(product3, "title", "MacBook Pro");
        putField(product3, "category", "computers");
        putField(product3, "price", "2499");
        putField(product3, "rating", "4.8");
        assertThat(redis.hmset("product:3".getBytes(), product3)).isEqualTo("OK");

        Map<byte[], byte[]> product4 = new HashMap<>();
        putField(product4, "title", "Dell XPS");
        putField(product4, "category", "computers");
        putField(product4, "price", "1299");
        putField(product4, "rating", "4.2");
        assertThat(redis.hmset("product:4".getBytes(), product4)).isEqualTo("OK");

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().groupBy(GroupBy
                .<byte[], byte[]> of("category".getBytes()).reduce(Reducer.<byte[], byte[]> count().as("count".getBytes())))
                .build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("groupby-agg-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> group : searchReply.getResults()) {
            assertThat(hasField(group.getFields(), "category")).isTrue();
            assertThat(hasField(group.getFields(), "count")).isTrue();
            assertThat(getStringField(group.getFields(), "count")).isIn("1", "2");
        }

        assertThat(redis.ftDropindex("groupby-agg-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithGroupByAndMultipleReducers() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("stock".getBytes()).build());

        assertThat(redis.ftCreate("multi-reducer-test-idx", fields)).isEqualTo("OK");

        String[][] items = { { "item:1", "Product A", "electronics", "100", "50" },
                { "item:2", "Product B", "electronics", "200", "30" }, { "item:3", "Product C", "books", "25", "100" },
                { "item:4", "Product D", "books", "35", "75" } };

        for (String[] item : items) {
            Map<byte[], byte[]> map = new HashMap<>();
            putField(map, "title", item[1]);
            putField(map, "category", item[2]);
            putField(map, "price", item[3]);
            putField(map, "stock", item[4]);
            assertThat(redis.hmset(item[0].getBytes(), map)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("category".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@price".getBytes()).as("avg_price".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@stock".getBytes()).as("total_stock".getBytes())))
                .build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("multi-reducer-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> group : searchReply.getResults()) {
            assertThat(hasField(group.getFields(), "category")).isTrue();
            assertThat(hasField(group.getFields(), "count")).isTrue();
            assertThat(hasField(group.getFields(), "avg_price")).isTrue();
            assertThat(hasField(group.getFields(), "total_stock")).isTrue();
        }

        assertThat(redis.ftDropindex("multi-reducer-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithSortBy() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build());

        assertThat(redis.ftCreate("sortby-test-idx", fields)).isEqualTo("OK");

        String[][] prods = { { "prod:1", "Product A", "300", "4.1" }, { "prod:2", "Product B", "100", "4.8" },
                { "prod:3", "Product C", "200", "4.5" } };

        for (String[] prod : prods) {
            Map<byte[], byte[]> map = new HashMap<>();
            putField(map, "title", prod[1]);
            putField(map, "price", prod[2]);
            putField(map, "rating", prod[3]);
            assertThat(redis.hmset(prod[0].getBytes(), map)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .sortBy("price".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("sortby-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(3);

        List<SearchReply.SearchResult<byte[], byte[]>> results = searchReply.getResults();
        assertThat(getStringField(results.get(0).getFields(), "price")).isEqualTo("300");
        assertThat(getStringField(results.get(1).getFields(), "price")).isEqualTo("200");
        assertThat(getStringField(results.get(2).getFields(), "price")).isEqualTo("100");

        assertThat(redis.ftDropindex("sortby-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithApply() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("quantity".getBytes()).build());

        assertThat(redis.ftCreate("apply-agg-test-idx", fields)).isEqualTo("OK");

        Map<byte[], byte[]> order1 = new HashMap<>();
        putField(order1, "title", "Product A");
        putField(order1, "price", "10");
        putField(order1, "quantity", "5");
        assertThat(redis.hmset("order:1".getBytes(), order1)).isEqualTo("OK");

        Map<byte[], byte[]> order2 = new HashMap<>();
        putField(order2, "title", "Product B");
        putField(order2, "price", "20");
        putField(order2, "quantity", "3");
        assertThat(redis.hmset("order:2".getBytes(), order2)).isEqualTo("OK");

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().load("title".getBytes())
                .load("price".getBytes()).load("quantity".getBytes())
                .apply("@price * @quantity".getBytes(), "total_value".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("apply-agg-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> item : searchReply.getResults()) {
            assertThat(hasField(item.getFields(), "total_value")).isTrue();
            assertThat(hasField(item.getFields(), "title")).isTrue();
            assertThat(hasField(item.getFields(), "price")).isTrue();
            assertThat(hasField(item.getFields(), "quantity")).isTrue();
        }

        assertThat(redis.ftDropindex("apply-agg-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithLimit() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("score".getBytes()).sortable().build());

        assertThat(redis.ftCreate("limit-test-idx", fields)).isEqualTo("OK");

        for (int i = 1; i <= 10; i++) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "title", "Document " + i);
            putField(doc, "score", String.valueOf(i * 10));
            assertThat(redis.hmset(("doc:" + i).getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .sortBy("score".getBytes(), SortDirection.DESC).limit(2, 3).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("limit-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(3);

        List<SearchReply.SearchResult<byte[], byte[]>> results = searchReply.getResults();
        assertThat(getStringField(results.get(0).getFields(), "score")).isIn("80", "70");
        assertThat(getStringField(results.get(1).getFields(), "score")).isIn("70", "60");
        assertThat(getStringField(results.get(2).getFields(), "score")).isIn("60", "50");

        assertThat(redis.ftDropindex("limit-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithFilter() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).build());

        assertThat(redis.ftCreate("filter-test-idx", fields)).isEqualTo("OK");

        String[][] items = { { "item:1", "Cheap Item", "50", "3.0" }, { "item:2", "Expensive Item", "500", "4.5" },
                { "item:3", "Mid Range Item", "150", "4.0" } };

        for (String[] item : items) {
            Map<byte[], byte[]> map = new HashMap<>();
            putField(map, "title", item[1]);
            putField(map, "price", item[2]);
            putField(map, "rating", item[3]);
            assertThat(redis.hmset(item[0].getBytes(), map)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .filter("@rating >= 4.0".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("filter-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> item : searchReply.getResults()) {
            double rating = Double.parseDouble(getStringField(item.getFields(), "rating"));
            assertThat(rating).isGreaterThanOrEqualTo(4.0);
        }

        assertThat(redis.ftDropindex("filter-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithBasicCursor() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build());

        assertThat(redis.ftCreate("cursor-basic-test-idx", fields)).isEqualTo("OK");

        for (int i = 1; i <= 3; i++) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "title", "Document " + i);
            putField(doc, "category", i <= 2 ? "tech" : "science");
            assertThat(redis.hmset(("doc:" + i).getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(2L)).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("cursor-basic-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getCursor()).isPresent();
        assertThat(result.getCursor().get().getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        AggregationReply<byte[], byte[]> nextResult = redis.ftCursorread("cursor-basic-test-idx", result.getCursor().get());

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> nextSearchReply = nextResult.getReplies().get(0);
        assertThat(nextSearchReply.getResults()).hasSize(1);
        assertThat(nextResult.getCursor()).isPresent();
        assertThat(nextResult.getCursor().get().getCursorId()).isEqualTo(0L);

        assertThat(redis.ftDropindex("cursor-basic-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithCursorAndCount() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("score".getBytes()).build());

        assertThat(redis.ftCreate("cursor-count-test-idx", fields)).isEqualTo("OK");

        for (int i = 1; i <= 10; i++) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "title", "Document " + i);
            putField(doc, "score", String.valueOf(i * 10));
            assertThat(redis.hmset(("doc:" + i).getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(3L)).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("cursor-count-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getCursor()).isPresent();
        assertThat(result.getCursor().get().getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1);
        assertThat(result.getReplies().get(0).getResults()).hasSize(3);

        AggregationReply<byte[], byte[]> nextResult = redis.ftCursorread("cursor-count-test-idx", result.getCursor().get(), 5);

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1);
        assertThat(nextResult.getReplies().get(0).getResults()).hasSize(5);
        assertThat(nextResult.getCursor()).isPresent();
        assertThat(nextResult.getCursor().get().getCursorId()).isNotEqualTo(0L);

        AggregationReply<byte[], byte[]> finalResult = redis.ftCursorread("cursor-count-test-idx",
                nextResult.getCursor().get());

        assertThat(finalResult).isNotNull();
        assertThat(finalResult.getReplies()).hasSize(1);
        assertThat(finalResult.getReplies().get(0).getResults()).hasSize(2);
        assertThat(finalResult.getCursor()).isPresent();
        assertThat(finalResult.getCursor().get().getCursorId()).isEqualTo(0L);

        assertThat(redis.ftDropindex("cursor-count-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithCursorAndMaxIdle() {
        List<FieldArgs<byte[]>> fields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build());

        assertThat(redis.ftCreate("cursor-maxidle-test-idx", fields)).isEqualTo("OK");

        for (int i = 1; i <= 5; i++) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "title", "Document " + i);
            assertThat(redis.hmset(("doc:" + i).getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(2L, Duration.ofSeconds(10))).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("cursor-maxidle-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getCursor()).isPresent();
        assertThat(result.getCursor().get().getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1);
        assertThat(result.getReplies().get(0).getResults()).hasSize(2);

        AggregationReply<byte[], byte[]> nextResult = redis.ftCursorread("cursor-maxidle-test-idx", result.getCursor().get());

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1);
        assertThat(nextResult.getReplies().get(0).getResults()).hasSize(2);

        assertThat(redis.ftDropindex("cursor-maxidle-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldDeleteCursorExplicitly() {
        List<FieldArgs<byte[]>> fields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build());

        assertThat(redis.ftCreate("cursor-delete-test-idx", fields)).isEqualTo("OK");

        for (int i = 1; i <= 5; i++) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "title", "Document " + i);
            assertThat(redis.hmset(("doc:" + i).getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(2L)).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("cursor-delete-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        assertThat(result.getCursor()).isPresent();
        assertThat(result.getCursor().get().getCursorId()).isNotEqualTo(0L);

        String deleteResult = redis.ftCursordel("cursor-delete-test-idx", result.getCursor().get());
        assertThat(deleteResult).isEqualTo("OK");

        assertThat(redis.ftDropindex("cursor-delete-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleCursorPaginationCompletely() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("id".getBytes()).sortable().build());
        assertThat(redis.ftCreate("cursor-pagination-test-idx", fields)).isEqualTo("OK");

        for (int i = 1; i <= 9; i++) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "title", "Document " + i);
            putField(doc, "id", String.valueOf(i));
            assertThat(redis.hmset(("doc:" + i).getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .sortBy("id".getBytes(), SortDirection.ASC).withCursor(AggregateArgs.WithCursor.of(4L)).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("cursor-pagination-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getCursor()).isPresent();
        assertThat(result.getCursor().get().getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1);
        assertThat(result.getReplies().get(0).getResults()).hasSize(4);

        List<SearchReply.SearchResult<byte[], byte[]>> allResults = new ArrayList<>(result.getReplies().get(0).getResults());
        AggregationReply<byte[], byte[]> current = result;
        while (current.getCursor().isPresent() && current.getCursor().get().getCursorId() != 0L) {
            AggregationReply<byte[], byte[]> nextResult = redis.ftCursorread("cursor-pagination-test-idx",
                    current.getCursor().get());
            assertThat(nextResult).isNotNull();
            assertThat(nextResult.getReplies()).hasSize(1);
            allResults.addAll(nextResult.getReplies().get(0).getResults());
            current = nextResult;
        }

        assertThat(allResults).hasSize(9);

        for (int i = 0; i < allResults.size(); i++) {
            String expectedId = String.valueOf(i + 1);
            assertThat(getStringField(allResults.get(i).getFields(), "id")).isEqualTo(expectedId);
        }

        assertThat(redis.ftDropindex("cursor-pagination-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformCursorWithComplexAggregation() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).build());

        assertThat(redis.ftCreate("cursor-complex-test-idx", fields)).isEqualTo("OK");

        String[][] products = { { "product:1", "iPhone 13", "electronics", "999", "4.5" },
                { "product:2", "Samsung Galaxy", "electronics", "799", "4.3" },
                { "product:3", "MacBook Pro", "computers", "2499", "4.8" },
                { "product:4", "Dell XPS", "computers", "1299", "4.2" },
                { "product:5", "iPad Air", "electronics", "599", "4.4" } };

        for (String[] product : products) {
            Map<byte[], byte[]> map = new HashMap<>();
            putField(map, "title", product[1]);
            putField(map, "category", product[2]);
            putField(map, "price", product[3]);
            putField(map, "rating", product[4]);
            assertThat(redis.hmset(product[0].getBytes(), map)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("category".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@price".getBytes()).as("avg_price".getBytes())))
                .withCursor(AggregateArgs.WithCursor.of(1L)).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("cursor-complex-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getCursor()).isPresent();
        assertThat(result.getCursor().get().getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1);
        assertThat(result.getReplies().get(0).getResults()).hasSize(1);

        SearchReply.SearchResult<byte[], byte[]> firstGroup = result.getReplies().get(0).getResults().get(0);
        assertThat(hasField(firstGroup.getFields(), "category")).isTrue();
        assertThat(hasField(firstGroup.getFields(), "count")).isTrue();
        assertThat(hasField(firstGroup.getFields(), "avg_price")).isTrue();

        AggregationReply<byte[], byte[]> nextResult = redis.ftCursorread("cursor-complex-test-idx", result.getCursor().get());

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1);
        assertThat(nextResult.getReplies().get(0).getResults()).hasSize(1);

        long effective = nextResult.getCursor().map(AggregationReply.Cursor::getCursorId).orElse(0L);
        if (effective != 0L) {
            AggregationReply<byte[], byte[]> finalPage = redis.ftCursorread("cursor-complex-test-idx",
                    nextResult.getCursor().get());
            assertThat(finalPage).isNotNull();
            assertThat(finalPage.getReplies()).hasSize(1);
            assertThat(finalPage.getReplies().get(0).getResults()).isEmpty();
            assertThat(finalPage.getCursor().map(AggregationReply.Cursor::getCursorId).orElse(0L)).isEqualTo(0L);
        }

        SearchReply.SearchResult<byte[], byte[]> secondGroup = nextResult.getReplies().get(0).getResults().get(0);
        assertThat(hasField(secondGroup.getFields(), "category")).isTrue();
        assertThat(hasField(secondGroup.getFields(), "count")).isTrue();
        assertThat(hasField(secondGroup.getFields(), "avg_price")).isTrue();

        assertThat(redis.ftDropindex("cursor-complex-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleEmptyResultsWithCursor() {
        List<FieldArgs<byte[]>> fields = Collections
                .singletonList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build());

        assertThat(redis.ftCreate("cursor-empty-test-idx", fields)).isEqualTo("OK");

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(5L)).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("cursor-empty-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies().get(0).getResults()).isEmpty();
        assertThat(result.getCursor()).isPresent();
        assertThat(result.getCursor().get().getCursorId()).isEqualTo(0L);

        assertThat(redis.ftDropindex("cursor-empty-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithGroupByAndAdvancedReducers() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("department".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("role".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("salary".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("experience".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("performance_score".getBytes()).sortable().build());

        assertThat(redis.ftCreate("groupby-advanced-test-idx", fields)).isEqualTo("OK");

        String[][] employees = { { "emp:1", "Engineering", "Senior", "120000", "8", "4.5" },
                { "emp:2", "Engineering", "Junior", "80000", "2", "4.2" },
                { "emp:3", "Marketing", "Senior", "95000", "6", "4.7" },
                { "emp:4", "Marketing", "Junior", "65000", "1", "4.0" },
                { "emp:5", "Engineering", "Senior", "130000", "10", "4.8" } };

        for (String[] emp : employees) {
            Map<byte[], byte[]> map = new HashMap<>();
            putField(map, "department", emp[1]);
            putField(map, "role", emp[2]);
            putField(map, "salary", emp[3]);
            putField(map, "experience", emp[4]);
            putField(map, "performance_score", emp[5]);
            assertThat(redis.hmset(emp[0].getBytes(), map)).isEqualTo("OK");
        }

        // Test 1: Group by department with comprehensive statistics
        AggregateArgs<byte[], byte[]> deptStatsArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("department".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("employee_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@salary".getBytes()).as("total_salary".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@salary".getBytes()).as("avg_salary".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> min("@salary".getBytes()).as("min_salary".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> max("@salary".getBytes()).as("max_salary".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@performance_score".getBytes()).as("avg_performance".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> countDistinct("@role".getBytes()).as("role_diversity".getBytes())))
                .sortBy("avg_salary".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> deptStatsResult = redis.ftAggregate("groupby-advanced-test-idx", "*".getBytes(),
                deptStatsArgs);

        assertThat(deptStatsResult).isNotNull();
        assertThat(deptStatsResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> deptStatsReply = deptStatsResult.getReplies().get(0);
        assertThat(deptStatsReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> deptGroup : deptStatsReply.getResults()) {
            assertThat(hasField(deptGroup.getFields(), "department")).isTrue();
            assertThat(hasField(deptGroup.getFields(), "employee_count")).isTrue();
            assertThat(hasField(deptGroup.getFields(), "avg_salary")).isTrue();

            double minSalary = Double.parseDouble(getStringField(deptGroup.getFields(), "min_salary"));
            double avgSalary = Double.parseDouble(getStringField(deptGroup.getFields(), "avg_salary"));
            double maxSalary = Double.parseDouble(getStringField(deptGroup.getFields(), "max_salary"));

            assertThat(minSalary).isLessThanOrEqualTo(avgSalary);
            assertThat(avgSalary).isLessThanOrEqualTo(maxSalary);

            int empCount = Integer.parseInt(getStringField(deptGroup.getFields(), "employee_count"));
            assertThat(empCount).isGreaterThan(0);
        }

        // Test 2: Multi-level grouping by department and role
        AggregateArgs<byte[], byte[]> multiGroupArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("department".getBytes(), "role".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@salary".getBytes()).as("avg_salary".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@performance_score".getBytes()).as("avg_performance".getBytes())))
                .sortBy("avg_salary".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> multiGroupResult = redis.ftAggregate("groupby-advanced-test-idx", "*".getBytes(),
                multiGroupArgs);

        assertThat(multiGroupResult).isNotNull();
        assertThat(multiGroupResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> multiGroupReply = multiGroupResult.getReplies().get(0);
        assertThat(multiGroupReply.getResults()).hasSize(4);

        for (SearchReply.SearchResult<byte[], byte[]> group : multiGroupReply.getResults()) {
            assertThat(hasField(group.getFields(), "department")).isTrue();
            assertThat(hasField(group.getFields(), "role")).isTrue();

            String dept = getStringField(group.getFields(), "department");
            String role = getStringField(group.getFields(), "role");
            assertThat(dept.toLowerCase()).isIn("engineering", "marketing");
            assertThat(role.toLowerCase()).isIn("senior", "junior");
        }

        assertThat(redis.ftDropindex("groupby-advanced-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithSortByAndMaxOptimization() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("product_name".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("category".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("sales_count".getBytes()).sortable().build());

        assertThat(redis.ftCreate("sortby-max-test-idx", fields)).isEqualTo("OK");

        for (int i = 1; i <= 20; i++) {
            Map<byte[], byte[]> product = new HashMap<>();
            putField(product, "product_name", "Product " + i);
            putField(product, "category", i <= 10 ? "electronics" : "books");
            putField(product, "price", String.valueOf(50 + i * 10));
            putField(product, "rating", String.valueOf(3.0 + (i % 5) * 0.4));
            putField(product, "sales_count", String.valueOf(100 + i * 5));
            assertThat(redis.hmset(("product:" + i).getBytes(), product)).isEqualTo("OK");
        }

        // Test 1: Basic sorting with limit
        AggregateArgs<byte[], byte[]> basicSortArgs = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .sortBy(AggregateArgs.SortBy.of("price".getBytes(), SortDirection.ASC)).limit(0, 5).build();

        AggregationReply<byte[], byte[]> basicSortResult = redis.ftAggregate("sortby-max-test-idx", "*".getBytes(),
                basicSortArgs);

        assertThat(basicSortResult).isNotNull();
        assertThat(basicSortResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> basicSortReply = basicSortResult.getReplies().get(0);
        assertThat(basicSortReply.getResults()).hasSize(5);

        List<SearchReply.SearchResult<byte[], byte[]>> sortedResults = basicSortReply.getResults();
        double firstPrice = Double.parseDouble(getStringField(sortedResults.get(0).getFields(), "price"));
        double lastPrice = Double.parseDouble(getStringField(sortedResults.get(sortedResults.size() - 1).getFields(), "price"));
        assertThat(firstPrice).isLessThanOrEqualTo(lastPrice);

        for (int i = 0; i < sortedResults.size() - 1; i++) {
            double price1 = Double.parseDouble(getStringField(sortedResults.get(i).getFields(), "price"));
            double price2 = Double.parseDouble(getStringField(sortedResults.get(i + 1).getFields(), "price"));
            assertThat(price1).isLessThanOrEqualTo(price2);
        }

        // Test 2: Sorting with MAX optimization
        AggregateArgs<byte[], byte[]> maxSortArgs = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .sortBy(AggregateArgs.SortBy.of("rating".getBytes(), SortDirection.DESC).max(10)).build();

        AggregationReply<byte[], byte[]> maxSortResult = redis.ftAggregate("sortby-max-test-idx", "*".getBytes(), maxSortArgs);

        assertThat(maxSortResult).isNotNull();
        assertThat(maxSortResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> maxSortReply = maxSortResult.getReplies().get(0);
        assertThat(maxSortReply.getResults()).hasSize(10);

        List<SearchReply.SearchResult<byte[], byte[]>> maxSortedResults = maxSortReply.getResults();
        for (int i = 0; i < maxSortedResults.size() - 1; i++) {
            double rating1 = Double.parseDouble(getStringField(maxSortedResults.get(i).getFields(), "rating"));
            double rating2 = Double.parseDouble(getStringField(maxSortedResults.get(i + 1).getFields(), "rating"));
            assertThat(rating1).isGreaterThanOrEqualTo(rating2);
        }

        assertThat(redis.ftDropindex("sortby-max-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithGroupByAndComplexReducers() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("region".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("product_type".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("revenue".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("units_sold".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("profit_margin".getBytes()).sortable().build());

        assertThat(redis.ftCreate("groupby-complex-test-idx", fields)).isEqualTo("OK");

        String[] regions = { "North", "South", "East", "West" };
        String[] productTypes = { "Premium", "Standard" };

        int recordId = 1;
        for (String region : regions) {
            for (String productType : productTypes) {
                for (int i = 1; i <= 3; i++) {
                    Map<byte[], byte[]> salesRecord = new HashMap<>();
                    putField(salesRecord, "region", region);
                    putField(salesRecord, "product_type", productType);
                    putField(salesRecord, "revenue", String.valueOf(1000 + recordId * 100));
                    putField(salesRecord, "units_sold", String.valueOf(50 + recordId * 5));
                    putField(salesRecord, "profit_margin", String.valueOf(0.15 + (recordId % 3) * 0.05));
                    assertThat(redis.hmset(("sales:" + recordId).getBytes(), salesRecord)).isEqualTo("OK");
                    recordId++;
                }
            }
        }

        // Test 1: Group by region with comprehensive statistical reducers
        AggregateArgs<byte[], byte[]> regionStatsArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("region".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("total_records".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@revenue".getBytes()).as("total_revenue".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@revenue".getBytes()).as("avg_revenue".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> min("@revenue".getBytes()).as("min_revenue".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> max("@revenue".getBytes()).as("max_revenue".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@units_sold".getBytes()).as("total_units".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@profit_margin".getBytes()).as("avg_profit_margin".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> countDistinct("@product_type".getBytes())
                                .as("product_diversity".getBytes())))
                .sortBy("total_revenue".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> regionStatsResult = redis.ftAggregate("groupby-complex-test-idx", "*".getBytes(),
                regionStatsArgs);

        assertThat(regionStatsResult).isNotNull();
        assertThat(regionStatsResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> regionStatsReply = regionStatsResult.getReplies().get(0);
        assertThat(regionStatsReply.getResults()).hasSize(4);

        for (SearchReply.SearchResult<byte[], byte[]> regionGroup : regionStatsReply.getResults()) {
            assertThat(hasField(regionGroup.getFields(), "region")).isTrue();
            assertThat(hasField(regionGroup.getFields(), "total_records")).isTrue();

            double minRevenue = Double.parseDouble(getStringField(regionGroup.getFields(), "min_revenue"));
            double avgRevenue = Double.parseDouble(getStringField(regionGroup.getFields(), "avg_revenue"));
            double maxRevenue = Double.parseDouble(getStringField(regionGroup.getFields(), "max_revenue"));

            assertThat(minRevenue).isLessThanOrEqualTo(avgRevenue);
            assertThat(avgRevenue).isLessThanOrEqualTo(maxRevenue);

            int totalRecords = Integer.parseInt(getStringField(regionGroup.getFields(), "total_records"));
            assertThat(totalRecords).isEqualTo(6);

            String region = getStringField(regionGroup.getFields(), "region");
            assertThat(region.toLowerCase()).isIn("north", "south", "east", "west");
        }

        // Test 2: Multi-dimensional grouping by region and product_type
        AggregateArgs<byte[], byte[]> multiDimArgs = AggregateArgs.<byte[], byte[]> builder()
                .groupBy(GroupBy.<byte[], byte[]> of("region".getBytes(), "product_type".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("record_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@revenue".getBytes()).as("avg_revenue".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@units_sold".getBytes()).as("avg_units".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@profit_margin".getBytes()).as("avg_margin".getBytes())))
                .sortBy("avg_revenue".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> multiDimResult = redis.ftAggregate("groupby-complex-test-idx", "*".getBytes(),
                multiDimArgs);

        assertThat(multiDimResult).isNotNull();
        assertThat(multiDimResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> multiDimReply = multiDimResult.getReplies().get(0);
        assertThat(multiDimReply.getResults()).hasSize(8);

        for (SearchReply.SearchResult<byte[], byte[]> comboGroup : multiDimReply.getResults()) {
            assertThat(hasField(comboGroup.getFields(), "region")).isTrue();
            assertThat(hasField(comboGroup.getFields(), "product_type")).isTrue();

            int recordCount = Integer.parseInt(getStringField(comboGroup.getFields(), "record_count"));
            assertThat(recordCount).isEqualTo(3);

            String region = getStringField(comboGroup.getFields(), "region");
            String productType = getStringField(comboGroup.getFields(), "product_type");
            assertThat(region.toLowerCase()).isIn("north", "south", "east", "west");
            assertThat(productType.toLowerCase()).isIn("premium", "standard");
        }

        assertThat(redis.ftDropindex("groupby-complex-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithSortByMultipleFields() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("team".getBytes()).sortable().build(),
                TextFieldArgs.<byte[]> builder().name("player".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("score".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("assists".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("rebounds".getBytes()).sortable().build());

        assertThat(redis.ftCreate("sortby-multi-test-idx", fields)).isEqualTo("OK");

        String[] teams = { "Lakers", "Warriors", "Celtics" };
        String[] players = { "Player1", "Player2", "Player3", "Player4" };

        int playerId = 1;
        for (String team : teams) {
            for (String player : players) {
                Map<byte[], byte[]> playerStats = new HashMap<>();
                putField(playerStats, "team", team);
                putField(playerStats, "player", player + "_" + team);
                putField(playerStats, "score", String.valueOf(15 + playerId * 2));
                putField(playerStats, "assists", String.valueOf(3 + playerId));
                putField(playerStats, "rebounds", String.valueOf(5 + (playerId % 3) * 2));
                assertThat(redis.hmset(("player:" + playerId).getBytes(), playerStats)).isEqualTo("OK");
                playerId++;
            }
        }

        AggregateArgs<byte[], byte[]> multiSortArgs = AggregateArgs.<byte[], byte[]> builder().loadAll()
                .sortBy(AggregateArgs.SortBy.of(new AggregateArgs.SortProperty<>("score".getBytes(), SortDirection.DESC),
                        new AggregateArgs.SortProperty<>("assists".getBytes(), SortDirection.DESC)))
                .limit(0, 8).build();

        AggregationReply<byte[], byte[]> multiSortResult = redis.ftAggregate("sortby-multi-test-idx", "*".getBytes(),
                multiSortArgs);

        assertThat(multiSortResult).isNotNull();
        assertThat(multiSortResult.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> multiSortReply = multiSortResult.getReplies().get(0);
        assertThat(multiSortReply.getResults()).hasSize(8);

        List<SearchReply.SearchResult<byte[], byte[]>> sortedPlayers = multiSortReply.getResults();
        for (int i = 0; i < sortedPlayers.size() - 1; i++) {
            int score1 = Integer.parseInt(getStringField(sortedPlayers.get(i).getFields(), "score"));
            int score2 = Integer.parseInt(getStringField(sortedPlayers.get(i + 1).getFields(), "score"));
            int assists1 = Integer.parseInt(getStringField(sortedPlayers.get(i).getFields(), "assists"));
            int assists2 = Integer.parseInt(getStringField(sortedPlayers.get(i + 1).getFields(), "assists"));

            if (score1 != score2) {
                assertThat(score1).isGreaterThanOrEqualTo(score2);
            } else {
                assertThat(assists1).isGreaterThanOrEqualTo(assists2);
            }
        }

        for (SearchReply.SearchResult<byte[], byte[]> player : sortedPlayers) {
            assertThat(hasField(player.getFields(), "team")).isTrue();
            String team = getStringField(player.getFields(), "team");
            assertThat(team.toLowerCase()).isIn("lakers", "warriors", "celtics");
        }

        assertThat(redis.ftDropindex("sortby-multi-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldRespectUserSpecifiedPipelineOperationOrder() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("title".getBytes()).build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("quantity".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("category".getBytes()).sortable().build());

        assertThat(redis.ftCreate("pipeline-order-test-idx", fields)).isEqualTo("OK");

        String[][] products = { { "product:1", "Product A", "100", "5", "electronics" },
                { "product:2", "Product B", "200", "3", "electronics" }, { "product:3", "Product C", "50", "10", "books" } };

        for (String[] product : products) {
            Map<byte[], byte[]> map = new HashMap<>();
            putField(map, "title", product[1]);
            putField(map, "price", product[2]);
            putField(map, "quantity", product[3]);
            putField(map, "category", product[4]);
            assertThat(redis.hmset(product[0].getBytes(), map)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> args = AggregateArgs.<byte[], byte[]> builder().load("title".getBytes())
                .load("price".getBytes()).load("quantity".getBytes()).load("category".getBytes())
                .apply("@price * @quantity".getBytes(), "total_value".getBytes()).filter("@total_value > 550".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("category".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("product_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@total_value".getBytes()).as("category_total".getBytes())))
                .limit(0, 10).sortBy("category_total".getBytes(), SortDirection.DESC).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("pipeline-order-test-idx", "*".getBytes(), args);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);

        assertThat(searchReply.getResults()).hasSize(1);

        SearchReply.SearchResult<byte[], byte[]> electronicsGroup = searchReply.getResults().get(0);
        assertThat(getStringField(electronicsGroup.getFields(), "category")).isEqualTo("electronics");
        assertThat(getStringField(electronicsGroup.getFields(), "product_count")).isEqualTo("1");
        assertThat(getStringField(electronicsGroup.getFields(), "category_total")).isEqualTo("600");
    }

    @Test
    void shouldSupportDynamicReentrantPipeline() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("product_name".getBytes()).build(),
                TagFieldArgs.<byte[]> builder().name("category".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("brand".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("sales_count".getBytes()).sortable().build());

        assertThat(redis.ftCreate("reentrant-pipeline-idx", fields)).isEqualTo("OK");

        String[][] products = { { "laptop:1", "Gaming Laptop", "electronics", "BrandA", "1200", "4.5", "150" },
                { "laptop:2", "Business Laptop", "electronics", "BrandB", "800", "4.2", "200" },
                { "laptop:3", "Budget Laptop", "electronics", "BrandA", "400", "3.8", "300" },
                { "phone:1", "Flagship Phone", "electronics", "BrandC", "900", "4.7", "500" },
                { "phone:2", "Mid-range Phone", "electronics", "BrandC", "500", "4.1", "400" },
                { "book:1", "Programming Book", "books", "PublisherA", "50", "4.6", "100" },
                { "book:2", "Design Book", "books", "PublisherB", "40", "4.3", "80" },
                { "book:3", "Business Book", "books", "PublisherA", "35", "4.0", "120" } };

        for (String[] product : products) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "product_name", product[1]);
            putField(doc, "category", product[2]);
            putField(doc, "brand", product[3]);
            putField(doc, "price", product[4]);
            putField(doc, "rating", product[5]);
            putField(doc, "sales_count", product[6]);
            assertThat(redis.hmset(product[0].getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> complexArgs = AggregateArgs.<byte[], byte[]> builder().load("category".getBytes())
                .load("brand".getBytes()).load("price".getBytes()).load("rating".getBytes()).load("sales_count".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("category".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("product_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@price".getBytes()).as("avg_price".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@sales_count".getBytes()).as("total_sales".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@rating".getBytes()).as("avg_rating".getBytes())))
                .apply("@avg_rating * @total_sales / 100".getBytes(), "performance_score".getBytes())
                .filter("@performance_score > 15".getBytes()).sortBy("performance_score".getBytes(), SortDirection.DESC)
                .limit(0, 2).apply("@avg_price / 100".getBytes(), "price_tier".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("reentrant-pipeline-idx", "*".getBytes(), complexArgs);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).isNotEmpty();

        SearchReply.SearchResult<byte[], byte[]> topCategory = searchReply.getResults().get(0);
        assertThat(hasField(topCategory.getFields(), "category")).isTrue();
        assertThat(hasField(topCategory.getFields(), "performance_score")).isTrue();
        assertThat(hasField(topCategory.getFields(), "price_tier")).isTrue();

        assertThat(getStringField(topCategory.getFields(), "category")).isEqualTo("electronics");
    }

    @Test
    void shouldSupportMultipleRepeatedOperations() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("employee_name".getBytes()).build(),
                TagFieldArgs.<byte[]> builder().name("department".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("level".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("salary".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("experience".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("performance_score".getBytes()).sortable().build());

        assertThat(redis.ftCreate("repeated-ops-idx", fields)).isEqualTo("OK");

        String[][] employees = { { "emp:1", "Alice Johnson", "engineering", "senior", "120000", "8", "92" },
                { "emp:2", "Bob Smith", "engineering", "junior", "80000", "3", "85" },
                { "emp:3", "Carol Davis", "engineering", "mid", "100000", "5", "88" },
                { "emp:4", "David Wilson", "sales", "senior", "110000", "7", "90" },
                { "emp:5", "Eve Brown", "sales", "junior", "70000", "2", "82" },
                { "emp:6", "Frank Miller", "marketing", "mid", "90000", "4", "87" },
                { "emp:7", "Grace Lee", "marketing", "senior", "105000", "6", "91" } };

        for (String[] emp : employees) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "employee_name", emp[1]);
            putField(doc, "department", emp[2]);
            putField(doc, "level", emp[3]);
            putField(doc, "salary", emp[4]);
            putField(doc, "experience", emp[5]);
            putField(doc, "performance_score", emp[6]);
            assertThat(redis.hmset(emp[0].getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> repeatedOpsArgs = AggregateArgs.<byte[], byte[]> builder().load("department".getBytes())
                .load("level".getBytes()).load("salary".getBytes()).load("experience".getBytes())
                .load("performance_score".getBytes()).apply("@salary / @experience".getBytes(), "salary_per_year".getBytes())
                .filter("@experience >= 3".getBytes())
                .apply("@performance_score * 1000".getBytes(), "performance_bonus".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("department".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("employee_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@salary".getBytes()).as("avg_salary".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@performance_score".getBytes()).as("avg_performance".getBytes())))
                .apply("@avg_performance / (@avg_salary / 1000)".getBytes(), "efficiency_ratio".getBytes())
                .filter("@efficiency_ratio > 0.8".getBytes()).sortBy("efficiency_ratio".getBytes(), SortDirection.DESC)
                .apply("@efficiency_ratio * 100".getBytes(), "performance_score".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("efficiency_ratio".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("dept_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@avg_salary".getBytes()).as("class_avg_salary".getBytes())))
                .sortBy("class_avg_salary".getBytes(), SortDirection.DESC).filter("@dept_count > 0".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("repeated-ops-idx", "*".getBytes(), repeatedOpsArgs);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).isNotEmpty();

        for (SearchReply.SearchResult<byte[], byte[]> efficiencyGroup : searchReply.getResults()) {
            assertThat(hasField(efficiencyGroup.getFields(), "efficiency_ratio")).isTrue();
            assertThat(hasField(efficiencyGroup.getFields(), "dept_count")).isTrue();
            assertThat(hasField(efficiencyGroup.getFields(), "class_avg_salary")).isTrue();

            double efficiencyRatio = Double.parseDouble(getStringField(efficiencyGroup.getFields(), "efficiency_ratio"));
            assertThat(efficiencyRatio).isGreaterThan(0.0);
        }
    }

    @Test
    void shouldSupportComplexPipelineWithInterleavedOperations() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("transaction_id".getBytes()).build(),
                TagFieldArgs.<byte[]> builder().name("customer_segment".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("product_category".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("region".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("amount".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("quantity".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("discount".getBytes()).sortable().build());

        assertThat(redis.ftCreate("interleaved-ops-idx", fields)).isEqualTo("OK");

        String[][] transactions = { { "txn:1", "T001", "premium", "electronics", "north", "1500", "2", "5" },
                { "txn:2", "T002", "premium", "electronics", "south", "1200", "1", "10" },
                { "txn:3", "T003", "standard", "electronics", "north", "800", "3", "0" },
                { "txn:4", "T004", "standard", "books", "east", "150", "5", "15" },
                { "txn:5", "T005", "budget", "books", "west", "80", "8", "20" },
                { "txn:6", "T006", "premium", "clothing", "north", "600", "4", "8" },
                { "txn:7", "T007", "standard", "clothing", "south", "300", "6", "12" },
                { "txn:8", "T008", "budget", "electronics", "east", "400", "2", "25" },
                { "txn:9", "T009", "premium", "books", "west", "200", "10", "5" },
                { "txn:10", "T010", "standard", "electronics", "north", "900", "1", "7" } };

        for (String[] txn : transactions) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "transaction_id", txn[1]);
            putField(doc, "customer_segment", txn[2]);
            putField(doc, "product_category", txn[3]);
            putField(doc, "region", txn[4]);
            putField(doc, "amount", txn[5]);
            putField(doc, "quantity", txn[6]);
            putField(doc, "discount", txn[7]);
            assertThat(redis.hmset(txn[0].getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> interleavedArgs = AggregateArgs.<byte[], byte[]> builder()
                .load("customer_segment".getBytes()).load("product_category".getBytes()).load("region".getBytes())
                .load("amount".getBytes()).load("quantity".getBytes()).load("discount".getBytes())
                .apply("@amount * (100 - @discount) / 100".getBytes(), "net_amount".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("customer_segment".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("segment_transactions".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@net_amount".getBytes()).as("segment_revenue".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@quantity".getBytes()).as("avg_quantity".getBytes())))
                .apply("@segment_revenue / @segment_transactions".getBytes(), "revenue_per_transaction".getBytes())
                .sortBy("segment_transactions".getBytes(), SortDirection.DESC).limit(0, 10)
                .filter("@segment_revenue > 500".getBytes())
                .apply("@revenue_per_transaction / 100".getBytes(), "value_score".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("value_score".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("tier_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@segment_revenue".getBytes()).as("tier_total_revenue".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@revenue_per_transaction".getBytes())
                                .as("tier_avg_revenue".getBytes())))
                .sortBy("tier_total_revenue".getBytes(), SortDirection.DESC)
                .apply("@tier_total_revenue / @tier_count".getBytes(), "revenue_efficiency".getBytes())
                .filter("@tier_count > 0".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("interleaved-ops-idx", "*".getBytes(), interleavedArgs);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).isNotEmpty();

        for (SearchReply.SearchResult<byte[], byte[]> valueGroup : searchReply.getResults()) {
            assertThat(hasField(valueGroup.getFields(), "value_score")).isTrue();
            assertThat(hasField(valueGroup.getFields(), "tier_count")).isTrue();
            assertThat(hasField(valueGroup.getFields(), "tier_total_revenue")).isTrue();
            assertThat(hasField(valueGroup.getFields(), "revenue_efficiency")).isTrue();

            double valueScore = Double.parseDouble(getStringField(valueGroup.getFields(), "value_score"));
            assertThat(valueScore).isGreaterThan(0.0);
        }
    }

    @Test
    void shouldSupportPipelineWithMultipleFiltersAndSorts() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("product_id".getBytes()).build(),
                TagFieldArgs.<byte[]> builder().name("category".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("brand".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("price".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("stock".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("reviews_count".getBytes()).sortable().build());

        assertThat(redis.ftCreate("multi-filter-sort-idx", fields)).isEqualTo("OK");

        String[][] products = { { "prod:1", "P001", "electronics", "BrandA", "299", "50", "4.2", "120" },
                { "prod:2", "P002", "electronics", "BrandB", "199", "30", "3.8", "85" },
                { "prod:3", "P003", "electronics", "BrandA", "399", "20", "4.5", "200" },
                { "prod:4", "P004", "books", "PublisherX", "25", "100", "4.1", "45" },
                { "prod:5", "P005", "books", "PublisherY", "35", "75", "4.3", "60" },
                { "prod:6", "P006", "clothing", "BrandC", "89", "40", "3.9", "30" },
                { "prod:7", "P007", "clothing", "BrandD", "129", "25", "4.0", "55" },
                { "prod:8", "P008", "electronics", "BrandB", "599", "15", "4.7", "300" },
                { "prod:9", "P009", "books", "PublisherX", "45", "60", "4.4", "80" },
                { "prod:10", "P010", "clothing", "BrandC", "159", "35", "4.2", "70" } };

        for (String[] prod : products) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "product_id", prod[1]);
            putField(doc, "category", prod[2]);
            putField(doc, "brand", prod[3]);
            putField(doc, "price", prod[4]);
            putField(doc, "stock", prod[5]);
            putField(doc, "rating", prod[6]);
            putField(doc, "reviews_count", prod[7]);
            assertThat(redis.hmset(prod[0].getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> multiFilterSortArgs = AggregateArgs.<byte[], byte[]> builder().load("category".getBytes())
                .load("brand".getBytes()).load("price".getBytes()).load("stock".getBytes()).load("rating".getBytes())
                .load("reviews_count".getBytes()).filter("@rating >= 4.0".getBytes())
                .apply("@rating * @reviews_count".getBytes(), "popularity_score".getBytes())
                .filter("@popularity_score > 200".getBytes()).sortBy("popularity_score".getBytes(), SortDirection.DESC)
                .apply("@price * @stock".getBytes(), "inventory_value".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("category".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("product_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@inventory_value".getBytes())
                                .as("total_inventory_value".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@popularity_score".getBytes()).as("avg_popularity".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> max("@price".getBytes()).as("max_price".getBytes())))
                .filter("@total_inventory_value > 5000".getBytes())
                .apply("@total_inventory_value / @product_count".getBytes(), "value_density".getBytes())
                .sortBy("value_density".getBytes(), SortDirection.DESC).filter("@value_density > 1000".getBytes())
                .apply("@avg_popularity / 100".getBytes(), "category_score".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("category_score".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("tier_category_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@total_inventory_value".getBytes())
                                .as("tier_inventory_value".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@max_price".getBytes()).as("tier_avg_max_price".getBytes())))
                .sortBy("tier_inventory_value".getBytes(), SortDirection.DESC).filter("@tier_category_count > 0".getBytes())
                .limit(0, 5).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("multi-filter-sort-idx", "*".getBytes(),
                multiFilterSortArgs);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).isNotEmpty();

        for (SearchReply.SearchResult<byte[], byte[]> categoryGroup : searchReply.getResults()) {
            assertThat(hasField(categoryGroup.getFields(), "category_score")).isTrue();
            assertThat(hasField(categoryGroup.getFields(), "tier_category_count")).isTrue();

            double categoryScore = Double.parseDouble(getStringField(categoryGroup.getFields(), "category_score"));
            assertThat(categoryScore).isGreaterThan(0.0);

            int categoryCount = Integer.parseInt(getStringField(categoryGroup.getFields(), "tier_category_count"));
            assertThat(categoryCount).isGreaterThan(0);
        }
    }

    @Test
    void shouldSupportAdvancedDynamicPipelineWithConditionalLogic() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(TextFieldArgs.<byte[]> builder().name("order_id".getBytes()).build(),
                TagFieldArgs.<byte[]> builder().name("customer_type".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("product_line".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("sales_channel".getBytes()).sortable().build(),
                TagFieldArgs.<byte[]> builder().name("season".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("order_value".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("cost".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("shipping_cost".getBytes()).sortable().build(),
                NumericFieldArgs.<byte[]> builder().name("customer_satisfaction".getBytes()).sortable().build());

        assertThat(redis.ftCreate("advanced-pipeline-idx", fields)).isEqualTo("OK");

        String[][] orders = { { "order:1", "O001", "enterprise", "software", "online", "spring", "15000", "8000", "200", "9" },
                { "order:2", "O002", "smb", "software", "direct", "spring", "5000", "2500", "100", "8" },
                { "order:3", "O003", "individual", "hardware", "online", "summer", "800", "500", "50", "7" },
                { "order:4", "O004", "enterprise", "hardware", "partner", "summer", "25000", "15000", "500", "9" },
                { "order:5", "O005", "smb", "services", "direct", "fall", "3000", "1800", "0", "8" },
                { "order:6", "O006", "individual", "software", "online", "fall", "200", "100", "25", "6" },
                { "order:7", "O007", "enterprise", "services", "partner", "winter", "12000", "7000", "300", "9" },
                { "order:8", "O008", "smb", "hardware", "online", "winter", "2000", "1200", "75", "7" },
                { "order:9", "O009", "individual", "services", "direct", "spring", "500", "300", "30", "8" },
                { "order:10", "O010", "enterprise", "software", "online", "summer", "18000", "10000", "250", "9" } };

        for (String[] order : orders) {
            Map<byte[], byte[]> doc = new HashMap<>();
            putField(doc, "order_id", order[1]);
            putField(doc, "customer_type", order[2]);
            putField(doc, "product_line", order[3]);
            putField(doc, "sales_channel", order[4]);
            putField(doc, "season", order[5]);
            putField(doc, "order_value", order[6]);
            putField(doc, "cost", order[7]);
            putField(doc, "shipping_cost", order[8]);
            putField(doc, "customer_satisfaction", order[9]);
            assertThat(redis.hmset(order[0].getBytes(), doc)).isEqualTo("OK");
        }

        AggregateArgs<byte[], byte[]> advancedArgs = AggregateArgs.<byte[], byte[]> builder().load("customer_type".getBytes())
                .load("product_line".getBytes()).load("sales_channel".getBytes()).load("season".getBytes())
                .load("order_value".getBytes()).load("cost".getBytes()).load("shipping_cost".getBytes())
                .load("customer_satisfaction".getBytes())
                .apply("@order_value - @cost - @shipping_cost".getBytes(), "profit".getBytes())
                .apply("@profit / @order_value * 100".getBytes(), "profit_margin".getBytes()).filter("@profit > 0".getBytes())
                .apply("@order_value / 1000".getBytes(), "customer_value_score".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("customer_type".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("segment_orders".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@profit".getBytes()).as("segment_profit".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@profit_margin".getBytes()).as("avg_margin".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@customer_satisfaction".getBytes())
                                .as("avg_satisfaction".getBytes())))
                .apply("(@avg_satisfaction * @avg_margin * @segment_orders) / 100".getBytes(), "performance_score".getBytes())
                .filter("@performance_score > 0".getBytes()).sortBy("performance_score".getBytes(), SortDirection.DESC)
                .limit(0, 5).apply("@performance_score / 10".getBytes(), "normalized_performance".getBytes())
                .apply("@segment_profit / @segment_orders".getBytes(), "profit_per_order".getBytes())
                .apply("@profit_per_order / 1000".getBytes(), "business_impact_score".getBytes())
                .groupBy(GroupBy.<byte[], byte[]> of("business_impact_score".getBytes())
                        .reduce(Reducer.<byte[], byte[]> count().as("impact_segment_count".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> sum("@segment_profit".getBytes()).as("total_impact_profit".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> avg("@performance_score".getBytes())
                                .as("avg_impact_performance".getBytes()))
                        .reduce(Reducer.<byte[], byte[]> max("@avg_satisfaction".getBytes()).as("max_satisfaction".getBytes())))
                .apply("@total_impact_profit / @impact_segment_count".getBytes(), "profit_efficiency".getBytes())
                .apply("(@avg_impact_performance + @max_satisfaction * 10) / 2".getBytes(), "composite_score".getBytes())
                .filter("@composite_score > 0".getBytes()).sortBy("composite_score".getBytes(), SortDirection.DESC)
                .apply("@composite_score / 50".getBytes(), "strategic_score".getBytes()).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("advanced-pipeline-idx", "*".getBytes(), advancedArgs);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).isNotEmpty();

        for (SearchReply.SearchResult<byte[], byte[]> impactGroup : searchReply.getResults()) {
            assertThat(hasField(impactGroup.getFields(), "business_impact_score")).isTrue();
            assertThat(hasField(impactGroup.getFields(), "impact_segment_count")).isTrue();
            assertThat(hasField(impactGroup.getFields(), "total_impact_profit")).isTrue();
            assertThat(hasField(impactGroup.getFields(), "profit_efficiency")).isTrue();
            assertThat(hasField(impactGroup.getFields(), "composite_score")).isTrue();
            assertThat(hasField(impactGroup.getFields(), "strategic_score")).isTrue();

            double impactScore = Double.parseDouble(getStringField(impactGroup.getFields(), "business_impact_score"));
            assertThat(impactScore).isGreaterThan(0.0);

            double strategicScore = Double.parseDouble(getStringField(impactGroup.getFields(), "strategic_score"));
            assertThat(strategicScore).isGreaterThan(0.0);

            double compositeScore = Double.parseDouble(getStringField(impactGroup.getFields(), "composite_score"));
            assertThat(compositeScore).isGreaterThan(0.0);

            int segmentCount = Integer.parseInt(getStringField(impactGroup.getFields(), "impact_segment_count"));
            assertThat(segmentCount).isGreaterThan(0);
        }
    }

    @Test
    void shouldPerformAggregationOnJson() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("$.country".getBytes()).as("country".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("$.city".getBytes()).as("city".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("$.office".getBytes()).as("office".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("$.code".getBytes()).as("code".getBytes()).build());
        CreateArgs<byte[], byte[]> args = CreateArgs.<byte[], byte[]> builder().on(CreateArgs.TargetType.JSON)
                .withPrefix("doc:".getBytes()).build();

        assertThat(redis.ftCreate("args-test-idx", args, fields)).isEqualTo("OK");

        JsonParser parser = redis.getJsonParser();

        JsonObject doc1 = parser.createJsonObject();
        doc1.put("country", parser.createJsonValue("\"SE\""));
        doc1.put("city", parser.createJsonValue("\"Stockholm\""));
        doc1.put("office", parser.createJsonValue("\"HQ\""));
        doc1.put("code", parser.createJsonValue("\"S1\""));
        assertThat(redis.jsonSet("doc:1".getBytes(), JsonPath.ROOT_PATH, doc1)).isEqualTo("OK");

        JsonObject doc2 = parser.createJsonObject();
        doc2.put("country", parser.createJsonValue("\"FI\""));
        doc2.put("city", parser.createJsonValue("\"Åbo\""));
        doc2.put("office", parser.createJsonValue("\"Office2\""));
        doc2.put("code", parser.createJsonValue("\"S2\""));
        assertThat(redis.jsonSet("doc:2".getBytes(), JsonPath.ROOT_PATH, doc2)).isEqualTo("OK");

        AggregateArgs.GroupBy<byte[], byte[]> groupBy = AggregateArgs.GroupBy
                .<byte[], byte[]> of("country".getBytes(), "city".getBytes(), "office".getBytes(), "code".getBytes())
                .reduce(AggregateArgs.Reducer.<byte[], byte[]> count().as("__count".getBytes()));

        AggregateArgs<byte[], byte[]> aggargs = AggregateArgs.<byte[], byte[]> builder().loadAll().groupBy(groupBy)
                .dialect(QueryDialects.DIALECT2).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("args-test-idx", "*".getBytes(), aggargs);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        for (SearchReply.SearchResult<byte[], byte[]> aggregateResult : searchReply.getResults()) {
            assertThat(hasField(aggregateResult.getFields(), "country")).isTrue();
            assertThat(hasField(aggregateResult.getFields(), "city")).isTrue();
            assertThat(hasField(aggregateResult.getFields(), "office")).isTrue();
            assertThat(hasField(aggregateResult.getFields(), "code")).isTrue();
            assertThat(getField(aggregateResult.getFields(), "country")).isNotNull();
            assertThat(getField(aggregateResult.getFields(), "city")).isNotNull();
            assertThat(getField(aggregateResult.getFields(), "office")).isNotNull();
            assertThat(getField(aggregateResult.getFields(), "code")).isNotNull();
        }

        assertThat(redis.ftDropindex("args-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationOnJsonWithNulls() {
        List<FieldArgs<byte[]>> fields = Arrays.asList(
                TextFieldArgs.<byte[]> builder().name("$.country".getBytes()).as("country".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("$.city".getBytes()).as("city".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("$.office".getBytes()).as("office".getBytes()).build(),
                TextFieldArgs.<byte[]> builder().name("$.code".getBytes()).as("code".getBytes()).build());
        CreateArgs<byte[], byte[]> args = CreateArgs.<byte[], byte[]> builder().on(CreateArgs.TargetType.JSON)
                .withPrefix("doc:".getBytes()).build();

        assertThat(redis.ftCreate("args-test-idx", args, fields)).isEqualTo("OK");

        JsonParser parser = redis.getJsonParser();

        JsonObject doc1 = parser.createJsonObject();
        doc1.put("country", parser.createJsonValue("\"SE\""));
        doc1.put("city", parser.createJsonValue("null"));
        doc1.put("office", parser.createJsonValue("\"HQ\""));
        doc1.put("code", parser.createJsonValue("\"S1\""));
        assertThat(redis.jsonSet("doc:1".getBytes(), JsonPath.ROOT_PATH, doc1)).isEqualTo("OK");

        AggregateArgs.GroupBy<byte[], byte[]> groupBy = AggregateArgs.GroupBy
                .<byte[], byte[]> of("country".getBytes(), "city".getBytes(), "office".getBytes(), "code".getBytes())
                .reduce(AggregateArgs.Reducer.<byte[], byte[]> count().as("__count".getBytes()));

        AggregateArgs<byte[], byte[]> aggArgs = AggregateArgs.<byte[], byte[]> builder().loadAll().groupBy(groupBy)
                .dialect(QueryDialects.DIALECT2).build();

        AggregationReply<byte[], byte[]> result = redis.ftAggregate("args-test-idx", "*".getBytes(), aggArgs);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        SearchReply<byte[], byte[]> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(1);

        SearchReply.SearchResult<byte[], byte[]> aggregateResult = searchReply.getResults().get(0);
        assertThat(hasField(aggregateResult.getFields(), "country")).isTrue();
        assertThat(hasField(aggregateResult.getFields(), "city")).isTrue();
        assertThat(hasField(aggregateResult.getFields(), "office")).isTrue();
        assertThat(hasField(aggregateResult.getFields(), "code")).isTrue();
        assertThat(getStringField(aggregateResult.getFields(), "country")).isEqualTo("SE");
        assertThat(getField(aggregateResult.getFields(), "city")).isNull();
        assertThat(getStringField(aggregateResult.getFields(), "office")).isEqualTo("HQ");
        assertThat(getStringField(aggregateResult.getFields(), "code")).isEqualTo("S1");

        assertThat(redis.ftDropindex("args-test-idx")).isEqualTo("OK");
    }

}
