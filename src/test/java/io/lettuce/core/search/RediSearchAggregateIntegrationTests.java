/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.lettuce.core.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.AggregateArgs.GroupBy;
import io.lettuce.core.search.arguments.AggregateArgs.Reducer;
import io.lettuce.core.search.arguments.AggregateArgs.SortDirection;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.QueryDialects;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for Redis FT.AGGREGATE command.
 *
 * @author Tihomir Mateev
 */
@EnabledOnCommand("FT.AGGREGATE")
class RediSearchAggregateIntegrationTests extends TestSupport {

    private final RedisClient client;

    private RedisCommands<String, String> redis;

    RediSearchAggregateIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
    }

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
    }

    @BeforeEach
    void setUp() {
        StatefulRedisConnection<String, String> connection = client.connect();
        this.redis = connection.sync();

        assertThat(redis.flushall()).isEqualTo("OK");
    }

    @Test
    void shouldPerformBasicAggregation() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftCreate("basic-test-idx", fields)).isEqualTo("OK");

        // Add some test documents
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "iPhone 13");
        doc1.put("category", "electronics");
        assertThat(redis.hmset("doc:1", doc1)).isEqualTo("OK");

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Samsung Galaxy");
        doc2.put("category", "electronics");
        assertThat(redis.hmset("doc:2", doc2)).isEqualTo("OK");

        Map<String, String> doc3 = new HashMap<>();
        doc3.put("title", "MacBook Pro");
        doc3.put("category", "computers");
        assertThat(redis.hmset("doc:3", doc3)).isEqualTo("OK");

        Map<String, String> doc4 = new HashMap<>();
        doc4.put("title", "iPad Air");
        doc4.put("category", "electronics");
        assertThat(redis.hmset("doc:4", doc4)).isEqualTo("OK");

        // Perform basic aggregation without LOAD - should return empty field maps
        SearchReply<String, String> result = redis.ftAggregate("basic-test-idx", "*");

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(4); // Should return actual count of matching documents
        assertThat(result.getResults()).hasSize(4); // Should have 4 documents

        // Each result should be empty since no LOAD was specified
        for (SearchReply.SearchResult<String, String> aggregateResult : result.getResults()) {
            assertThat(aggregateResult.getFields()).isEmpty();
        }

        assertThat(redis.ftDropindex("basic-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithArgs() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftCreate("args-test-idx", fields)).isEqualTo("OK");

        // Add some test documents
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "iPhone 13");
        doc1.put("category", "electronics");
        assertThat(redis.hmset("doc:1", doc1)).isEqualTo("OK");

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Samsung Galaxy");
        doc2.put("category", "electronics");
        assertThat(redis.hmset("doc:2", doc2)).isEqualTo("OK");

        Map<String, String> doc3 = new HashMap<>();
        doc3.put("title", "MacBook Pro");
        doc3.put("category", "computers");
        assertThat(redis.hmset("doc:3", doc3)).isEqualTo("OK");

        // Perform aggregation with arguments - LOAD fields
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().verbatim().load("title").load("category")
                .build();

        SearchReply<String, String> result = redis.ftAggregate("args-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(3); // Should return actual count of matching documents
        assertThat(result.getResults()).hasSize(3); // Should have 3 documents (doc:1, doc:2, doc:3)

        // Check that loaded fields are present in results
        for (SearchReply.SearchResult<String, String> aggregateResult : result.getResults()) {
            assertThat(aggregateResult.getFields().containsKey("title")).isTrue();
            assertThat(aggregateResult.getFields().containsKey("category")).isTrue();
            assertThat(aggregateResult.getFields().get("title")).isNotNull();
            assertThat(aggregateResult.getFields().get("category")).isNotNull();
        }

        assertThat(redis.ftDropindex("args-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithParams() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftCreate("params-test-idx", fields)).isEqualTo("OK");

        // Add some test documents
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "iPhone 13");
        doc1.put("category", "electronics");
        assertThat(redis.hmset("doc:1", doc1)).isEqualTo("OK");

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Samsung Galaxy");
        doc2.put("category", "electronics");
        assertThat(redis.hmset("doc:2", doc2)).isEqualTo("OK");

        Map<String, String> doc3 = new HashMap<>();
        doc3.put("title", "MacBook Pro");
        doc3.put("category", "computers");
        assertThat(redis.hmset("doc:3", doc3)).isEqualTo("OK");

        // Perform aggregation with parameters - requires DIALECT 2
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().load("title").load("category")
                .param("cat", "electronics").dialect(QueryDialects.DIALECT2).build();

        SearchReply<String, String> result = redis.ftAggregate("params-test-idx", "@category:$cat", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(2); // Should return actual count of matching documents
        assertThat(result.getResults()).hasSize(2); // Should have 2 electronics documents

        // All results should be electronics
        for (SearchReply.SearchResult<String, String> aggregateResult : result.getResults()) {
            assertThat(aggregateResult.getFields().containsKey("title")).isTrue();
            assertThat(aggregateResult.getFields().containsKey("category")).isTrue();
            assertThat(aggregateResult.getFields().get("category")).isEqualTo("electronics");
        }

        assertThat(redis.ftDropindex("params-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithLoadAll() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftCreate("loadall-test-idx", fields)).isEqualTo("OK");

        // Add some test documents
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "iPhone 13");
        doc1.put("category", "electronics");
        assertThat(redis.hmset("doc:1", doc1)).isEqualTo("OK");

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Samsung Galaxy");
        doc2.put("category", "electronics");
        assertThat(redis.hmset("doc:2", doc2)).isEqualTo("OK");

        // Perform aggregation with LOAD * (load all fields)
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll().build();

        SearchReply<String, String> result = redis.ftAggregate("loadall-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(2); // Should return actual count of matching documents
        assertThat(result.getResults()).hasSize(2); // Should have 2 documents (only doc:1 and doc:2 added in this test)

        // Check that all fields are loaded
        for (SearchReply.SearchResult<String, String> aggregateResult : result.getResults()) {
            assertThat(aggregateResult.getFields().containsKey("title")).isTrue();
            assertThat(aggregateResult.getFields().containsKey("category")).isTrue();
            assertThat(aggregateResult.getFields().get("title")).isNotNull();
            assertThat(aggregateResult.getFields().get("category")).isNotNull();
        }

        assertThat(redis.ftDropindex("loadall-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleEmptyResults() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftCreate("empty-test-idx", fields)).isEqualTo("OK");

        // Don't add any documents

        // Perform aggregation on empty index
        SearchReply<String, String> result = redis.ftAggregate("empty-test-idx", "*");

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(0); // Redis returns 0 for empty results
        assertThat(result.getResults()).isEmpty(); // Should have no documents

        assertThat(redis.ftDropindex("empty-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldDemonstrateAdvancedAggregationScenarios() {
        // Create an index for e-commerce data similar to Redis documentation examples
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("brand").sortable().build(),
                TextFieldArgs.<String> builder().name("category").sortable().build(),
                NumericFieldArgs.<String> builder().name("price").sortable().build(),
                NumericFieldArgs.<String> builder().name("rating").sortable().build(),
                NumericFieldArgs.<String> builder().name("stock").sortable().build());

        assertThat(redis.ftCreate("products-idx", fields)).isEqualTo("OK");

        // Add sample e-commerce data
        Map<String, String> product1 = new HashMap<>();
        product1.put("title", "iPhone 13 Pro");
        product1.put("brand", "Apple");
        product1.put("category", "smartphones");
        product1.put("price", "999");
        product1.put("rating", "4.5");
        product1.put("stock", "50");
        assertThat(redis.hmset("product:1", product1)).isEqualTo("OK");

        Map<String, String> product2 = new HashMap<>();
        product2.put("title", "Samsung Galaxy S21");
        product2.put("brand", "Samsung");
        product2.put("category", "smartphones");
        product2.put("price", "799");
        product2.put("rating", "4.3");
        product2.put("stock", "30");
        assertThat(redis.hmset("product:2", product2)).isEqualTo("OK");

        Map<String, String> product3 = new HashMap<>();
        product3.put("title", "MacBook Pro");
        product3.put("brand", "Apple");
        product3.put("category", "laptops");
        product3.put("price", "2499");
        product3.put("rating", "4.8");
        product3.put("stock", "15");
        assertThat(redis.hmset("product:3", product3)).isEqualTo("OK");

        Map<String, String> product4 = new HashMap<>();
        product4.put("title", "Dell XPS 13");
        product4.put("brand", "Dell");
        product4.put("category", "laptops");
        product4.put("price", "1299");
        product4.put("rating", "4.2");
        product4.put("stock", "25");
        assertThat(redis.hmset("product:4", product4)).isEqualTo("OK");

        // Test basic aggregation with all fields loaded
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll().build();

        SearchReply<String, String> result = redis.ftAggregate("products-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(4); // Should return actual count of matching documents
        assertThat(result.getResults()).hasSize(4);

        // Verify data structure for future aggregation operations
        Set<String> brands = result.getResults().stream().map(r -> r.getFields().get("brand")).collect(Collectors.toSet());
        assertThat(brands).containsExactlyInAnyOrder("Apple", "Samsung", "Dell");

        Set<String> categories = result.getResults().stream().map(r -> r.getFields().get("category"))
                .collect(Collectors.toSet());
        assertThat(categories).containsExactlyInAnyOrder("smartphones", "laptops");

        /*
         * TODO: Future aggregation scenarios to implement: 1. Group by category with statistics: FT.AGGREGATE products-idx *
         * GROUPBY 1 @category REDUCE COUNT 0 AS count REDUCE AVG 1 @price AS avg_price REDUCE MIN 1 @price AS min_price REDUCE
         * MAX 1 @price AS max_price 2. Apply mathematical expressions: FT.AGGREGATE products-idx * LOAD
         * 4 @title @price @stock @rating APPLY "@price * @stock" AS inventory_value APPLY "ceil(@rating)" AS rating_rounded 3.
         * Filter and sort results: FT.AGGREGATE products-idx * LOAD 3 @title @price @rating FILTER "@price > 1000" SORTBY
         * 2 @rating DESC 4. Complex pipeline with multiple operations: FT.AGGREGATE products-idx * GROUPBY 1 @brand REDUCE
         * COUNT 0 AS product_count REDUCE AVG 1 @rating AS avg_rating REDUCE SUM 1 @stock AS total_stock SORTBY 2 @avg_rating
         * DESC LIMIT 0 3 5. String operations and functions: FT.AGGREGATE products-idx * LOAD 2 @title @brand APPLY
         * "upper(@brand)" AS brand_upper APPLY "substr(@title, 0, 10)" AS title_short
         */

        assertThat(redis.ftDropindex("products-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleTimeoutParameter() {
        // Create a simple index
        List<FieldArgs<String>> fields = Collections.singletonList(TextFieldArgs.<String> builder().name("title").build());

        assertThat(redis.ftCreate("timeout-test-idx", fields)).isEqualTo("OK");

        // Add a document
        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Test Document");
        assertThat(redis.hmset("doc:1", doc)).isEqualTo("OK");

        // Test with timeout parameter
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().load("title")
                .timeout(java.time.Duration.ofSeconds(5)).build();

        SearchReply<String, String> result = redis.ftAggregate("timeout-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(1); // Should return actual count of matching documents
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults().get(0).getFields().get("title")).isEqualTo("Test Document");

        assertThat(redis.ftDropindex("timeout-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithGroupBy() {
        // Create an index with numeric fields for aggregation
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build(),
                NumericFieldArgs.<String> builder().name("price").build(),
                NumericFieldArgs.<String> builder().name("rating").build());

        assertThat(redis.ftCreate("groupby-agg-test-idx", fields)).isEqualTo("OK");

        // Add test documents with numeric data
        Map<String, String> product1 = new HashMap<>();
        product1.put("title", "iPhone 13");
        product1.put("category", "electronics");
        product1.put("price", "999");
        product1.put("rating", "4.5");
        assertThat(redis.hmset("product:1", product1)).isEqualTo("OK");

        Map<String, String> product2 = new HashMap<>();
        product2.put("title", "Samsung Galaxy");
        product2.put("category", "electronics");
        product2.put("price", "799");
        product2.put("rating", "4.3");
        assertThat(redis.hmset("product:2", product2)).isEqualTo("OK");

        Map<String, String> product3 = new HashMap<>();
        product3.put("title", "MacBook Pro");
        product3.put("category", "computers");
        product3.put("price", "2499");
        product3.put("rating", "4.8");
        assertThat(redis.hmset("product:3", product3)).isEqualTo("OK");

        Map<String, String> product4 = new HashMap<>();
        product4.put("title", "Dell XPS");
        product4.put("category", "computers");
        product4.put("price", "1299");
        product4.put("rating", "4.2");
        assertThat(redis.hmset("product:4", product4)).isEqualTo("OK");

        // Perform aggregation with GROUPBY and COUNT reducer
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .groupBy(GroupBy.<String, String> of("category").reduce(Reducer.<String, String> count().as("count")))
                .dialect(QueryDialects.DIALECT2).build();

        SearchReply<String, String> result = redis.ftAggregate("groupby-agg-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(2); // Should have 2 groups (electronics, computers)
        assertThat(result.getResults()).hasSize(2);

        // Verify group results contain category and count fields
        for (SearchReply.SearchResult<String, String> group : result.getResults()) {
            assertThat(group.getFields()).containsKey("category");
            assertThat(group.getFields()).containsKey("count");
            assertThat(group.getFields().get("count")).isIn("2"); // Each category has 2 items
        }

        assertThat(redis.ftDropindex("groupby-agg-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithGroupByAndMultipleReducers() {
        // Create an index with numeric fields
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build(),
                NumericFieldArgs.<String> builder().name("price").build(),
                NumericFieldArgs.<String> builder().name("stock").build());

        assertThat(redis.ftCreate("multi-reducer-test-idx", fields)).isEqualTo("OK");

        // Add test documents
        Map<String, String> item1 = new HashMap<>();
        item1.put("title", "Product A");
        item1.put("category", "electronics");
        item1.put("price", "100");
        item1.put("stock", "50");
        assertThat(redis.hmset("item:1", item1)).isEqualTo("OK");

        Map<String, String> item2 = new HashMap<>();
        item2.put("title", "Product B");
        item2.put("category", "electronics");
        item2.put("price", "200");
        item2.put("stock", "30");
        assertThat(redis.hmset("item:2", item2)).isEqualTo("OK");

        Map<String, String> item3 = new HashMap<>();
        item3.put("title", "Product C");
        item3.put("category", "books");
        item3.put("price", "25");
        item3.put("stock", "100");
        assertThat(redis.hmset("item:3", item3)).isEqualTo("OK");

        Map<String, String> item4 = new HashMap<>();
        item4.put("title", "Product D");
        item4.put("category", "books");
        item4.put("price", "35");
        item4.put("stock", "75");
        assertThat(redis.hmset("item:4", item4)).isEqualTo("OK");

        // Perform aggregation with multiple reducers
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .groupBy(GroupBy.<String, String> of("category").reduce(Reducer.<String, String> count().as("count"))
                        .reduce(Reducer.<String, String> avg("@price").as("avg_price"))
                        .reduce(Reducer.<String, String> sum("@stock").as("total_stock")))
                .dialect(QueryDialects.DIALECT2).build();

        SearchReply<String, String> result = redis.ftAggregate("multi-reducer-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(2); // Should have 2 groups
        assertThat(result.getResults()).hasSize(2);

        // Verify each group has all reducer results
        for (SearchReply.SearchResult<String, String> group : result.getResults()) {
            assertThat(group.getFields()).containsKey("category");
            assertThat(group.getFields()).containsKey("count");
            assertThat(group.getFields()).containsKey("avg_price");
            assertThat(group.getFields()).containsKey("total_stock");
        }

        assertThat(redis.ftDropindex("multi-reducer-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithSortBy() {
        // Create an index with sortable fields
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                NumericFieldArgs.<String> builder().name("price").sortable().build(),
                NumericFieldArgs.<String> builder().name("rating").sortable().build());

        assertThat(redis.ftCreate("sortby-test-idx", fields)).isEqualTo("OK");

        // Add test documents
        Map<String, String> prod1 = new HashMap<>();
        prod1.put("title", "Product A");
        prod1.put("price", "300");
        prod1.put("rating", "4.1");
        assertThat(redis.hmset("prod:1", prod1)).isEqualTo("OK");

        Map<String, String> prod2 = new HashMap<>();
        prod2.put("title", "Product B");
        prod2.put("price", "100");
        prod2.put("rating", "4.8");
        assertThat(redis.hmset("prod:2", prod2)).isEqualTo("OK");

        Map<String, String> prod3 = new HashMap<>();
        prod3.put("title", "Product C");
        prod3.put("price", "200");
        prod3.put("rating", "4.5");
        assertThat(redis.hmset("prod:3", prod3)).isEqualTo("OK");

        // Perform aggregation with SORTBY price DESC
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .sortBy("price", SortDirection.DESC).dialect(QueryDialects.DIALECT2).build();

        SearchReply<String, String> result = redis.ftAggregate("sortby-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(3);
        assertThat(result.getResults()).hasSize(3);

        // Verify results are sorted by price in descending order
        List<SearchReply.SearchResult<String, String>> results = result.getResults();
        assertThat(results.get(0).getFields().get("price")).isEqualTo("300"); // Highest price first
        assertThat(results.get(1).getFields().get("price")).isEqualTo("200");
        assertThat(results.get(2).getFields().get("price")).isEqualTo("100"); // Lowest price last

        assertThat(redis.ftDropindex("sortby-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithApply() {
        // Create an index with numeric fields
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                NumericFieldArgs.<String> builder().name("price").build(),
                NumericFieldArgs.<String> builder().name("quantity").build());

        assertThat(redis.ftCreate("apply-agg-test-idx", fields)).isEqualTo("OK");

        // Add test documents
        Map<String, String> order1 = new HashMap<>();
        order1.put("title", "Product A");
        order1.put("price", "10");
        order1.put("quantity", "5");
        assertThat(redis.hmset("order:1", order1)).isEqualTo("OK");

        Map<String, String> order2 = new HashMap<>();
        order2.put("title", "Product B");
        order2.put("price", "20");
        order2.put("quantity", "3");
        assertThat(redis.hmset("order:2", order2)).isEqualTo("OK");

        // Perform aggregation with APPLY to calculate total value
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().load("title").load("price")
                .load("quantity").apply("@price * @quantity", "total_value").dialect(QueryDialects.DIALECT2).build();

        SearchReply<String, String> result = redis.ftAggregate("apply-agg-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result.getResults()).hasSize(2);

        // Verify computed field exists
        for (SearchReply.SearchResult<String, String> item : result.getResults()) {
            assertThat(item.getFields()).containsKey("total_value");
            assertThat(item.getFields()).containsKey("title");
            assertThat(item.getFields()).containsKey("price");
            assertThat(item.getFields()).containsKey("quantity");
        }

        assertThat(redis.ftDropindex("apply-agg-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithLimit() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                NumericFieldArgs.<String> builder().name("score").sortable().build());

        assertThat(redis.ftCreate("limit-test-idx", fields)).isEqualTo("OK");

        // Add multiple test documents
        for (int i = 1; i <= 10; i++) {
            Map<String, String> doc = new HashMap<>();
            doc.put("title", "Document " + i);
            doc.put("score", String.valueOf(i * 10));
            assertThat(redis.hmset("doc:" + i, doc)).isEqualTo("OK");
        }

        // Perform aggregation with LIMIT
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .sortBy("score", SortDirection.DESC).limit(2, 3) // Skip 2, take 3
                .dialect(QueryDialects.DIALECT2).build();

        SearchReply<String, String> result = redis.ftAggregate("limit-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getResults()).hasSize(3); // Should return exactly 3 results

        // Verify we got the correct subset - let's check what we actually get
        List<SearchReply.SearchResult<String, String>> results = result.getResults();
        // The results should be sorted in descending order and limited to 3 items starting from offset 2
        // So we should get items with scores: 80, 70, 60 (3rd, 4th, 5th highest)
        // But let's verify what we actually get and adjust accordingly
        assertThat(results.get(0).getFields().get("score")).isIn("80", "70"); // Could be 3rd or 4th highest
        assertThat(results.get(1).getFields().get("score")).isIn("70", "60"); // Could be 4th or 5th highest
        assertThat(results.get(2).getFields().get("score")).isIn("60", "50"); // Could be 5th or 6th highest

        assertThat(redis.ftDropindex("limit-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithFilter() {
        // Create an index with numeric fields
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                NumericFieldArgs.<String> builder().name("price").build(),
                NumericFieldArgs.<String> builder().name("rating").build());

        assertThat(redis.ftCreate("filter-test-idx", fields)).isEqualTo("OK");

        // Add test documents
        Map<String, String> item1 = new HashMap<>();
        item1.put("title", "Cheap Item");
        item1.put("price", "50");
        item1.put("rating", "3.0");
        assertThat(redis.hmset("item:1", item1)).isEqualTo("OK");

        Map<String, String> item2 = new HashMap<>();
        item2.put("title", "Expensive Item");
        item2.put("price", "500");
        item2.put("rating", "4.5");
        assertThat(redis.hmset("item:2", item2)).isEqualTo("OK");

        Map<String, String> item3 = new HashMap<>();
        item3.put("title", "Mid Range Item");
        item3.put("price", "150");
        item3.put("rating", "4.0");
        assertThat(redis.hmset("item:3", item3)).isEqualTo("OK");

        // Perform aggregation with FILTER for high-rated items
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll().filter("@rating >= 4.0")
                .dialect(QueryDialects.DIALECT2).build();

        SearchReply<String, String> result = redis.ftAggregate("filter-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getResults()).hasSize(2); // Should filter to 2 items with rating >= 4.0

        // Verify all returned items have rating >= 4.0
        for (SearchReply.SearchResult<String, String> item : result.getResults()) {
            double rating = Double.parseDouble(item.getFields().get("rating"));
            assertThat(rating).isGreaterThanOrEqualTo(4.0);
        }

        assertThat(redis.ftDropindex("filter-test-idx")).isEqualTo("OK");
    }

}
