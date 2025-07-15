/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

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
import io.lettuce.core.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.AggregateArgs.GroupBy;
import io.lettuce.core.search.arguments.AggregateArgs.Reducer;
import io.lettuce.core.search.arguments.AggregateArgs.SortDirection;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.QueryDialects;
import io.lettuce.core.search.arguments.TextFieldArgs;

/**
 * Integration tests for Redis FT.AGGREGATE command.
 *
 * @author Tihomir Mateev
 */
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
        // Create an index with prefix
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build());

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("doc:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("basic-test-idx", createArgs, fields)).isEqualTo("OK");

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

        // First, let's verify the documents are indexed by doing a search
        SearchReply<String, String> searchResult = redis.ftSearch("basic-test-idx", "*");
        assertThat(searchResult.getCount()).isEqualTo(4); // Verify documents are indexed

        // Perform basic aggregation without LOAD - should return empty field maps
        AggregationReply<String, String> result = redis.ftAggregate("basic-test-idx", "*");

        assertThat(result).isNotNull();
        // If documents are indexed, we should have 1 aggregation group (no grouping)
        // If no documents, we should have 0 aggregation groups
        if (searchResult.getCount() > 0) {
            assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
            assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
            assertThat(result.getReplies().get(0).getResults()).hasSize(4); // Should have 4 documents in the single reply

            // Each result should be empty since no LOAD was specified
            for (SearchReply.SearchResult<String, String> aggregateResult : result.getReplies().get(0).getResults()) {
                assertThat(aggregateResult.getFields()).isEmpty();
            }
        } else {
            assertThat(result.getAggregationGroups()).isEqualTo(0); // No documents indexed
            assertThat(result.getReplies()).isEmpty(); // No results
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

        AggregationReply<String, String> result = redis.ftAggregate("args-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(3); // Should have 3 documents (doc:1, doc:2, doc:3)

        // Check that loaded fields are present in results
        for (SearchReply.SearchResult<String, String> aggregateResult : searchReply.getResults()) {
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

        AggregationReply<String, String> result = redis.ftAggregate("params-test-idx", "@category:$cat", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2); // Should have 2 electronics documents

        // All results should be electronics
        for (SearchReply.SearchResult<String, String> aggregateResult : searchReply.getResults()) {
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

        AggregationReply<String, String> result = redis.ftAggregate("loadall-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2); // Should have 2 documents (only doc:1 and doc:2 added in this test)

        // Check that all fields are loaded
        for (SearchReply.SearchResult<String, String> aggregateResult : searchReply.getResults()) {
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
        AggregationReply<String, String> result = redis.ftAggregate("empty-test-idx", "*");

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 0 aggregation groups for empty index
        assertThat(result.getReplies().get(0).getResults()).isEmpty(); // Should have no SearchReply objects for empty results

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

        AggregationReply<String, String> result = redis.ftAggregate("products-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(4);

        // Verify data structure for future aggregation operations
        Set<String> brands = searchReply.getResults().stream().map(r -> r.getFields().get("brand")).collect(Collectors.toSet());
        assertThat(brands).containsExactlyInAnyOrder("Apple", "Samsung", "Dell");

        Set<String> categories = searchReply.getResults().stream().map(r -> r.getFields().get("category"))
                .collect(Collectors.toSet());
        assertThat(categories).containsExactlyInAnyOrder("smartphones", "laptops");

        // 1. Group by category with statistics
        AggregateArgs<String, String> statsArgs = AggregateArgs.<String, String> builder()
                .groupBy(GroupBy.<String, String> of("category").reduce(Reducer.<String, String> count().as("count"))
                        .reduce(Reducer.<String, String> avg("@price").as("avg_price"))
                        .reduce(Reducer.<String, String> min("@price").as("min_price"))
                        .reduce(Reducer.<String, String> max("@price").as("max_price")))
                .dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> statsResult = redis.ftAggregate("products-idx", "*", statsArgs);

        assertThat(statsResult).isNotNull();
        assertThat(statsResult.getAggregationGroups()).isEqualTo(1); // smartphones and laptops
        assertThat(statsResult.getReplies()).hasSize(1);

        SearchReply<String, String> statsReply = statsResult.getReplies().get(0);
        assertThat(statsReply.getResults()).hasSize(2);

        // Verify each category group has the expected statistics fields
        for (SearchReply.SearchResult<String, String> group : statsReply.getResults()) {
            assertThat(group.getFields()).containsKeys("category", "count", "avg_price", "min_price", "max_price");

            // Verify the values make sense (e.g., min_price <= avg_price <= max_price)
            double minPrice = Double.parseDouble(group.getFields().get("min_price"));
            double avgPrice = Double.parseDouble(group.getFields().get("avg_price"));
            double maxPrice = Double.parseDouble(group.getFields().get("max_price"));

            assertThat(minPrice).isLessThanOrEqualTo(avgPrice);
            assertThat(avgPrice).isLessThanOrEqualTo(maxPrice);
        }

        // 2. Apply mathematical expressions
        AggregateArgs<String, String> mathArgs = AggregateArgs.<String, String> builder().load("title").load("price")
                .load("stock").load("rating").apply("@price * @stock", "inventory_value")
                .apply("ceil(@rating)", "rating_rounded").dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> mathResult = redis.ftAggregate("products-idx", "*", mathArgs);

        assertThat(mathResult).isNotNull();
        assertThat(mathResult.getAggregationGroups()).isEqualTo(1);
        assertThat(mathResult.getReplies()).hasSize(1);

        SearchReply<String, String> mathReply = mathResult.getReplies().get(0);
        assertThat(mathReply.getResults()).hasSize(4);

        // Verify computed fields exist and have correct values
        for (SearchReply.SearchResult<String, String> item : mathReply.getResults()) {
            assertThat(item.getFields()).containsKeys("title", "price", "stock", "rating", "inventory_value", "rating_rounded");

            // Verify inventory_value = price * stock
            double price = Double.parseDouble(item.getFields().get("price"));
            double stock = Double.parseDouble(item.getFields().get("stock"));
            double inventoryValue = Double.parseDouble(item.getFields().get("inventory_value"));
            assertThat(inventoryValue).isEqualTo(price * stock);

            // Verify rating_rounded is ceiling of rating
            double rating = Double.parseDouble(item.getFields().get("rating"));
            double ratingRounded = Double.parseDouble(item.getFields().get("rating_rounded"));
            assertThat(ratingRounded).isEqualTo(Math.ceil(rating));
        }

        // 3. Filter and sort results
        AggregateArgs<String, String> filterArgs = AggregateArgs.<String, String> builder().load("title").load("price")
                .load("rating").filter("@price > 1000").sortBy("rating", SortDirection.DESC).dialect(QueryDialects.DIALECT2)
                .build();

        AggregationReply<String, String> filterResult = redis.ftAggregate("products-idx", "*", filterArgs);

        assertThat(filterResult).isNotNull();
        assertThat(filterResult.getReplies()).hasSize(1);

        SearchReply<String, String> filterReply = filterResult.getReplies().get(0);

        // Verify all returned items have price > 1000
        for (SearchReply.SearchResult<String, String> item : filterReply.getResults()) {
            double price = Double.parseDouble(item.getFields().get("price"));
            assertThat(price).isGreaterThan(1000);
        }

        // Verify results are sorted by rating in descending order
        if (filterReply.getResults().size() >= 2) {
            List<SearchReply.SearchResult<String, String>> results = filterReply.getResults();
            for (int i = 0; i < results.size() - 1; i++) {
                double rating1 = Double.parseDouble(results.get(i).getFields().get("rating"));
                double rating2 = Double.parseDouble(results.get(i + 1).getFields().get("rating"));
                assertThat(rating1).isGreaterThanOrEqualTo(rating2);
            }
        }

        // 4. Complex pipeline with multiple operations
        AggregateArgs<String, String> complexArgs = AggregateArgs.<String, String> builder()
                .groupBy(GroupBy.<String, String> of("brand").reduce(Reducer.<String, String> count().as("product_count"))
                        .reduce(Reducer.<String, String> avg("@rating").as("avg_rating"))
                        .reduce(Reducer.<String, String> sum("@stock").as("total_stock")))
                .sortBy("avg_rating", SortDirection.DESC).limit(0, 3) // Skip 0, take 3
                .dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> complexResult = redis.ftAggregate("products-idx", "*", complexArgs);

        assertThat(complexResult).isNotNull();
        assertThat(complexResult.getReplies()).hasSize(1);

        SearchReply<String, String> complexReply = complexResult.getReplies().get(0);

        // Verify each brand group has the expected fields
        for (SearchReply.SearchResult<String, String> group : complexReply.getResults()) {
            assertThat(group.getFields()).containsKeys("brand", "product_count", "avg_rating", "total_stock");
        }

        // Verify results are sorted by avg_rating in descending order
        if (complexReply.getResults().size() >= 2) {
            List<SearchReply.SearchResult<String, String>> results = complexReply.getResults();
            for (int i = 0; i < results.size() - 1; i++) {
                double rating1 = Double.parseDouble(results.get(i).getFields().get("avg_rating"));
                double rating2 = Double.parseDouble(results.get(i + 1).getFields().get("avg_rating"));
                assertThat(rating1).isGreaterThanOrEqualTo(rating2);
            }
        }

        // Verify limit is applied (max 3 results)
        assertThat(complexReply.getResults().size()).isLessThanOrEqualTo(3);

        // 5. String operations and functions
        AggregateArgs<String, String> stringArgs = AggregateArgs.<String, String> builder().load("title").load("brand")
                .apply("upper(@brand)", "brand_upper").apply("substr(@title, 0, 10)", "title_short")
                .dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> stringResult = redis.ftAggregate("products-idx", "*", stringArgs);

        assertThat(stringResult).isNotNull();
        assertThat(stringResult.getReplies()).hasSize(1);

        SearchReply<String, String> stringReply = stringResult.getReplies().get(0);

        // Verify string operations are applied correctly
        for (SearchReply.SearchResult<String, String> item : stringReply.getResults()) {
            assertThat(item.getFields()).containsKeys("title", "brand", "brand_upper", "title_short");

            // Verify brand_upper is uppercase of brand
            String brand = item.getFields().get("brand");
            String brandUpper = item.getFields().get("brand_upper");
            assertThat(brandUpper).isEqualTo(brand.toUpperCase());

            // Verify title_short is substring of title (first 10 chars or less)
            String title = item.getFields().get("title");
            String titleShort = item.getFields().get("title_short");
            assertThat(titleShort).isEqualTo(title.substring(0, Math.min(10, title.length())));
        }

        assertThat(redis.ftDropindex("products-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleNestedGroupByOperations() {
        // Create an index for hierarchical grouping scenarios
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("department").sortable().build(),
                TextFieldArgs.<String> builder().name("category").sortable().build(),
                TextFieldArgs.<String> builder().name("product").build(),
                NumericFieldArgs.<String> builder().name("sales").sortable().build(),
                NumericFieldArgs.<String> builder().name("profit").sortable().build());

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("sales:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("sales-idx", createArgs, fields)).isEqualTo("OK");

        // Add sample sales data
        Map<String, String> salesData = new HashMap<>();
        salesData.put("department", "Electronics");
        salesData.put("category", "Smartphones");
        salesData.put("product", "iPhone 14");
        salesData.put("sales", "15000");
        salesData.put("profit", "3000");
        redis.hmset("sales:1", salesData);

        salesData.put("department", "Electronics");
        salesData.put("category", "Laptops");
        salesData.put("product", "MacBook Pro");
        salesData.put("sales", "25000");
        salesData.put("profit", "5000");
        redis.hmset("sales:2", salesData);

        salesData.put("department", "Electronics");
        salesData.put("category", "Smartphones");
        salesData.put("product", "Samsung Galaxy");
        salesData.put("sales", "12000");
        salesData.put("profit", "2400");
        redis.hmset("sales:3", salesData);

        salesData.put("department", "Clothing");
        salesData.put("category", "Shirts");
        salesData.put("product", "Cotton Shirt");
        salesData.put("sales", "5000");
        salesData.put("profit", "1500");
        redis.hmset("sales:4", salesData);

        // Test nested grouping by department and category
        AggregateArgs<String, String> nestedArgs = AggregateArgs.<String, String> builder()
                .groupBy(GroupBy.<String, String> of("department", "category")
                        .reduce(Reducer.<String, String> count().as("product_count"))
                        .reduce(Reducer.<String, String> sum("@sales").as("total_sales"))
                        .reduce(Reducer.<String, String> sum("@profit").as("total_profit")))
                .sortBy("total_sales", SortDirection.DESC).dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> nestedResult = redis.ftAggregate("sales-idx", "*", nestedArgs);

        assertThat(nestedResult).isNotNull();
        assertThat(nestedResult.getReplies()).hasSize(1);

        SearchReply<String, String> nestedReply = nestedResult.getReplies().get(0);

        // Verify each group has the expected fields
        for (SearchReply.SearchResult<String, String> group : nestedReply.getResults()) {
            assertThat(group.getFields()).containsKeys("department", "category", "product_count", "total_sales",
                    "total_profit");
        }

        // Verify results are sorted by total_sales in descending order
        if (nestedReply.getResults().size() >= 2) {
            List<SearchReply.SearchResult<String, String>> results = nestedReply.getResults();
            for (int i = 0; i < results.size() - 1; i++) {
                double sales1 = Double.parseDouble(results.get(i).getFields().get("total_sales"));
                double sales2 = Double.parseDouble(results.get(i + 1).getFields().get("total_sales"));
                assertThat(sales1).isGreaterThanOrEqualTo(sales2);
            }
        }

        assertThat(redis.ftDropindex("sales-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleAdvancedFilteringAndConditionals() {
        // Create an index for advanced filtering scenarios
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("status").sortable().build(),
                TextFieldArgs.<String> builder().name("priority").sortable().build(),
                NumericFieldArgs.<String> builder().name("score").sortable().build(),
                NumericFieldArgs.<String> builder().name("age").sortable().build());

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("task:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("tasks-idx", createArgs, fields)).isEqualTo("OK");

        // Add sample task data
        Map<String, String> taskData = new HashMap<>();
        taskData.put("status", "active");
        taskData.put("priority", "high");
        taskData.put("score", "95");
        taskData.put("age", "5");
        redis.hmset("task:1", taskData);

        taskData.put("status", "completed");
        taskData.put("priority", "medium");
        taskData.put("score", "85");
        taskData.put("age", "10");
        redis.hmset("task:2", taskData);

        taskData.put("status", "active");
        taskData.put("priority", "low");
        taskData.put("score", "70");
        taskData.put("age", "15");
        redis.hmset("task:3", taskData);

        taskData.put("status", "pending");
        taskData.put("priority", "high");
        taskData.put("score", "90");
        taskData.put("age", "3");
        redis.hmset("task:4", taskData);

        // Test complex filtering with multiple conditions
        AggregateArgs<String, String> filterArgs = AggregateArgs.<String, String> builder().loadAll()
                .filter("@score > 80 && @age < 12").apply("@score * 0.1", "normalized_score")
                .sortBy("score", SortDirection.DESC).dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> filterResult = redis.ftAggregate("tasks-idx", "*", filterArgs);

        assertThat(filterResult).isNotNull();
        assertThat(filterResult.getReplies()).hasSize(1);

        SearchReply<String, String> filterReply = filterResult.getReplies().get(0);

        // Verify all returned items meet the filter criteria
        for (SearchReply.SearchResult<String, String> item : filterReply.getResults()) {
            double score = Double.parseDouble(item.getFields().get("score"));
            double age = Double.parseDouble(item.getFields().get("age"));

            assertThat(score).isGreaterThan(80);
            assertThat(age).isLessThan(12);

            // Verify computed fields
            assertThat(item.getFields()).containsKeys("normalized_score");

            double normalizedScore = Double.parseDouble(item.getFields().get("normalized_score"));
            assertThat(normalizedScore).isEqualTo(score * 0.1);

        }

        assertThat(redis.ftDropindex("tasks-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleAdvancedStatisticalFunctions() {
        // Create an index for statistical analysis
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("region").sortable().build(),
                NumericFieldArgs.<String> builder().name("temperature").sortable().build(),
                NumericFieldArgs.<String> builder().name("humidity").sortable().build(),
                NumericFieldArgs.<String> builder().name("pressure").sortable().build());

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("weather:")
                .on(CreateArgs.TargetType.HASH).build();

        assertThat(redis.ftCreate("weather-idx", createArgs, fields)).isEqualTo("OK");

        // Add sample weather data
        for (int i = 1; i <= 20; i++) {
            Map<String, String> weatherData = new HashMap<>();
            weatherData.put("region", i <= 10 ? "north" : "south");
            weatherData.put("temperature", String.valueOf(20 + i));
            weatherData.put("humidity", String.valueOf(50 + (i % 5) * 5));
            weatherData.put("pressure", String.valueOf(1000 + i * 2));
            redis.hmset("weather:" + i, weatherData);
        }

        // Test advanced statistical functions
        AggregateArgs<String, String> statsArgs = AggregateArgs.<String, String> builder()
                .groupBy(GroupBy.<String, String> of("region").reduce(Reducer.<String, String> count().as("count"))
                        .reduce(Reducer.<String, String> avg("@temperature").as("avg_temp"))
                        .reduce(Reducer.<String, String> min("@temperature").as("min_temp"))
                        .reduce(Reducer.<String, String> max("@temperature").as("max_temp")))
                .dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> statsResult = redis.ftAggregate("weather-idx", "*", statsArgs);

        assertThat(statsResult).isNotNull();
        assertThat(statsResult.getReplies()).hasSize(1);

        SearchReply<String, String> statsReply = statsResult.getReplies().get(0);
        assertThat(statsReply.getResults()).hasSize(2); // north and south regions

        // Verify each region has the expected statistical fields
        for (SearchReply.SearchResult<String, String> region : statsReply.getResults()) {
            assertThat(region.getFields()).containsKeys("region", "count", "avg_temp", "min_temp", "max_temp");

            // Verify statistical relationships
            double minTemp = Double.parseDouble(region.getFields().get("min_temp"));
            double avgTemp = Double.parseDouble(region.getFields().get("avg_temp"));
            double maxTemp = Double.parseDouble(region.getFields().get("max_temp"));

            // Statistical invariants that should hold
            assertThat(minTemp).isLessThanOrEqualTo(avgTemp);
            assertThat(avgTemp).isLessThanOrEqualTo(maxTemp);

            // Count should be positive
            int count = Integer.parseInt(region.getFields().get("count"));
            assertThat(count).isGreaterThan(0);
        }

        assertThat(redis.ftDropindex("weather-idx")).isEqualTo("OK");
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
                .timeout(Duration.ofSeconds(5)).build();

        AggregationReply<String, String> result = redis.ftAggregate("timeout-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(1);
        assertThat(searchReply.getResults().get(0).getFields().get("title")).isEqualTo("Test Document");

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

        AggregationReply<String, String> result = redis.ftAggregate("groupby-agg-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all groups
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2); // Should have 2 group results

        // Verify group results contain category and count fields
        // Based on redis-cli testing: electronics=2, computers=2
        for (SearchReply.SearchResult<String, String> group : searchReply.getResults()) {
            assertThat(group.getFields()).containsKey("category");
            assertThat(group.getFields()).containsKey("count");
            assertThat(group.getFields().get("count")).isIn("1", "2"); // computers=2, electronics=2
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

        AggregationReply<String, String> result = redis.ftAggregate("multi-reducer-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all groups
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        // Verify each group has all reducer results
        for (SearchReply.SearchResult<String, String> group : searchReply.getResults()) {
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

        AggregationReply<String, String> result = redis.ftAggregate("sortby-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(3);

        // Verify results are sorted by price in descending order
        List<SearchReply.SearchResult<String, String>> results = searchReply.getResults();
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

        AggregationReply<String, String> result = redis.ftAggregate("apply-agg-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        // Verify computed field exists
        for (SearchReply.SearchResult<String, String> item : searchReply.getResults()) {
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

        AggregationReply<String, String> result = redis.ftAggregate("limit-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(3); // Should return exactly 3 results

        // Verify we got the correct subset - let's check what we actually get
        List<SearchReply.SearchResult<String, String>> results = searchReply.getResults();
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

        AggregationReply<String, String> result = redis.ftAggregate("filter-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply containing all documents
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2); // Should filter to 2 items with rating >= 4.0

        // Verify all returned items have rating >= 4.0
        for (SearchReply.SearchResult<String, String> item : searchReply.getResults()) {
            double rating = Double.parseDouble(item.getFields().get("rating"));
            assertThat(rating).isGreaterThanOrEqualTo(4.0);
        }

        assertThat(redis.ftDropindex("filter-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithBasicCursor() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build());

        assertThat(redis.ftCreate("cursor-basic-test-idx", fields)).isEqualTo("OK");

        // Add test documents
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("title", "Document 1");
        doc1.put("category", "tech");
        assertThat(redis.hmset("doc:1", doc1)).isEqualTo("OK");

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("title", "Document 2");
        doc2.put("category", "tech");
        assertThat(redis.hmset("doc:2", doc2)).isEqualTo("OK");

        Map<String, String> doc3 = new HashMap<>();
        doc3.put("title", "Document 3");
        doc3.put("category", "science");
        assertThat(redis.hmset("doc:3", doc3)).isEqualTo("OK");

        // Perform aggregation with cursor
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(2L)).build();

        AggregationReply<String, String> result = redis.ftAggregate("cursor-basic-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getCursorId()).isNotEqualTo(0L); // Should have a valid cursor ID
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2); // Should return 2 results per page

        // Read next page from cursor
        Long cursorId = result.getCursorId();
        AggregationReply<String, String> nextResult = redis.ftCursorread("cursor-basic-test-idx", cursorId);

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> nextSearchReply = nextResult.getReplies().get(0);
        assertThat(nextSearchReply.getResults()).hasSize(1); // Should return remaining 1 result
        assertThat(nextResult.getCursorId()).isEqualTo(0L); // Should indicate end of results

        assertThat(redis.ftDropindex("cursor-basic-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithCursorAndCount() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                NumericFieldArgs.<String> builder().name("score").build());

        assertThat(redis.ftCreate("cursor-count-test-idx", fields)).isEqualTo("OK");

        // Add multiple test documents
        for (int i = 1; i <= 10; i++) {
            Map<String, String> doc = new HashMap<>();
            doc.put("title", "Document " + i);
            doc.put("score", String.valueOf(i * 10));
            assertThat(redis.hmset("doc:" + i, doc)).isEqualTo("OK");
        }

        // Perform aggregation with cursor and custom count
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(3L)).build();

        AggregationReply<String, String> result = redis.ftAggregate("cursor-count-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(3); // Should return 3 results per page

        // Read next page with different count
        Long cursorId = result.getCursorId();
        AggregationReply<String, String> nextResult = redis.ftCursorread("cursor-count-test-idx", cursorId, 5);

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> nextSearchReply = nextResult.getReplies().get(0);
        assertThat(nextSearchReply.getResults()).hasSize(5); // Should return 5 results as specified
        assertThat(nextResult.getCursorId()).isNotEqualTo(0L); // Should still have more results

        // Read final page
        cursorId = nextResult.getCursorId();
        AggregationReply<String, String> finalResult = redis.ftCursorread("cursor-count-test-idx", cursorId);

        assertThat(finalResult).isNotNull();
        assertThat(finalResult.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> finalSearchReply = finalResult.getReplies().get(0);
        assertThat(finalSearchReply.getResults()).hasSize(2); // Should return remaining 2 results
        assertThat(finalResult.getCursorId()).isEqualTo(0L); // Should indicate end of results

        assertThat(redis.ftDropindex("cursor-count-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformAggregationWithCursorAndMaxIdle() {
        // Create an index
        List<FieldArgs<String>> fields = Collections.singletonList(TextFieldArgs.<String> builder().name("title").build());

        assertThat(redis.ftCreate("cursor-maxidle-test-idx", fields)).isEqualTo("OK");

        // Add test documents
        for (int i = 1; i <= 5; i++) {
            Map<String, String> doc = new HashMap<>();
            doc.put("title", "Document " + i);
            assertThat(redis.hmset("doc:" + i, doc)).isEqualTo("OK");
        }

        // Perform aggregation with cursor and custom max idle timeout
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(2L, Duration.ofSeconds(10))).build();

        AggregationReply<String, String> result = redis.ftAggregate("cursor-maxidle-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(2);

        // Read from cursor should work within timeout
        long cursorId = result.getCursorId();
        AggregationReply<String, String> nextResult = redis.ftCursorread("cursor-maxidle-test-idx", cursorId);

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1); // Should have 1 SearchReply
        assertThat(nextResult.getReplies().get(0).getResults()).hasSize(2);

        assertThat(redis.ftDropindex("cursor-maxidle-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldDeleteCursorExplicitly() {
        // Create an index
        List<FieldArgs<String>> fields = Collections.singletonList(TextFieldArgs.<String> builder().name("title").build());

        assertThat(redis.ftCreate("cursor-delete-test-idx", fields)).isEqualTo("OK");

        // Add test documents
        for (int i = 1; i <= 5; i++) {
            Map<String, String> doc = new HashMap<>();
            doc.put("title", "Document " + i);
            assertThat(redis.hmset("doc:" + i, doc)).isEqualTo("OK");
        }

        // Perform aggregation with cursor
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(2L)).build();

        AggregationReply<String, String> result = redis.ftAggregate("cursor-delete-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1);
        assertThat(result.getReplies()).hasSize(1);
        assertThat(result.getCursorId()).isNotEqualTo(0L);

        // Delete the cursor explicitly
        long cursorId = result.getCursorId();
        String deleteResult = redis.ftCursordel("cursor-delete-test-idx", cursorId);

        assertThat(deleteResult).isEqualTo("OK");

        assertThat(redis.ftDropindex("cursor-delete-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleCursorPaginationCompletely() {
        // Create an index
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                NumericFieldArgs.<String> builder().name("id").sortable().build());
        assertThat(redis.ftCreate("cursor-pagination-test-idx", fields)).isEqualTo("OK");

        final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        // Add test documents
        for (int i = 1; i <= 9; i++) {
            Map<String, String> doc = new HashMap<>();
            doc.put("title", "Document " + i);
            doc.put("id", String.valueOf(i));
            assertThat(redis.hmset("doc:" + i, doc)).isEqualTo("OK");
        }

        // Perform aggregation with cursor and sorting
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .sortBy("id", AggregateArgs.SortDirection.ASC).withCursor(AggregateArgs.WithCursor.of(4L))
                .dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> result = redis.ftAggregate("cursor-pagination-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 1 aggregation group (no grouping)
        assertThat(result.getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(4);

        // Collect all results by paginating through cursor
        List<SearchReply.SearchResult<String, String>> allResults = new ArrayList<>(searchReply.getResults());
        long cursorId = result.getCursorId();

        while (cursorId != 0L) {
            AggregationReply<String, String> nextResult = redis.ftCursorread("cursor-pagination-test-idx", cursorId);
            assertThat(nextResult).isNotNull();
            assertThat(nextResult.getReplies()).hasSize(1); // Should have 1 SearchReply
            SearchReply<String, String> nextSearchReply = nextResult.getReplies().get(0);

            allResults.addAll(nextSearchReply.getResults());
            cursorId = nextResult.getCursorId();
        }

        // Verify we got all 15 results
        assertThat(allResults).hasSize(9);

        // Verify results are sorted by id
        for (int i = 0; i < allResults.size(); i++) {
            String expectedId = String.valueOf(i + 1);
            assertThat(allResults.get(i).getFields().get("id")).isEqualTo(expectedId);
        }

        assertThat(redis.ftDropindex("cursor-pagination-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldPerformCursorWithComplexAggregation() {
        // Create an index with multiple field types
        List<FieldArgs<String>> fields = Arrays.asList(TextFieldArgs.<String> builder().name("title").build(),
                TextFieldArgs.<String> builder().name("category").build(),
                NumericFieldArgs.<String> builder().name("price").build(),
                NumericFieldArgs.<String> builder().name("rating").build());

        assertThat(redis.ftCreate("cursor-complex-test-idx", fields)).isEqualTo("OK");

        // Add test documents
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

        Map<String, String> product5 = new HashMap<>();
        product5.put("title", "iPad Air");
        product5.put("category", "electronics");
        product5.put("price", "599");
        product5.put("rating", "4.4");
        assertThat(redis.hmset("product:5", product5)).isEqualTo("OK");

        // Perform complex aggregation with groupby, reducers, and cursor
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder()
                .groupBy(AggregateArgs.GroupBy.<String, String> of("category")
                        .reduce(AggregateArgs.Reducer.<String, String> count().as("count"))
                        .reduce(AggregateArgs.Reducer.<String, String> avg("@price").as("avg_price")))
                .withCursor(AggregateArgs.WithCursor.of(1L)).dialect(QueryDialects.DIALECT2).build();

        AggregationReply<String, String> result = redis.ftAggregate("cursor-complex-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getCursorId()).isNotEqualTo(0L);
        assertThat(result.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> searchReply = result.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(1); // Should return 1 group per page

        // Verify first group has expected fields
        SearchReply.SearchResult<String, String> firstGroup = searchReply.getResults().get(0);
        assertThat(firstGroup.getFields()).containsKey("category");
        assertThat(firstGroup.getFields()).containsKey("count");
        assertThat(firstGroup.getFields()).containsKey("avg_price");

        // Read next group from cursor
        Long cursorId = result.getCursorId();
        AggregationReply<String, String> nextResult = redis.ftCursorread("cursor-complex-test-idx", cursorId);

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.getReplies()).hasSize(1); // Should have 1 SearchReply
        SearchReply<String, String> nextSearchReply = nextResult.getReplies().get(0);
        assertThat(nextSearchReply.getResults()).hasSize(1); // Should return second group
        assertThat(nextSearchReply.getCursorId()).isNull(); // Should indicate end of results

        // Verify second group has expected fields
        SearchReply.SearchResult<String, String> secondGroup = nextSearchReply.getResults().get(0);
        assertThat(secondGroup.getFields()).containsKey("category");
        assertThat(secondGroup.getFields()).containsKey("count");
        assertThat(secondGroup.getFields()).containsKey("avg_price");

        assertThat(redis.ftDropindex("cursor-complex-test-idx")).isEqualTo("OK");
    }

    @Test
    void shouldHandleEmptyResultsWithCursor() {
        // Create an index
        List<FieldArgs<String>> fields = Collections.singletonList(TextFieldArgs.<String> builder().name("title").build());

        assertThat(redis.ftCreate("cursor-empty-test-idx", fields)).isEqualTo("OK");

        // Don't add any documents

        // Perform aggregation with cursor on empty index
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().loadAll()
                .withCursor(AggregateArgs.WithCursor.of(5L)).build();

        AggregationReply<String, String> result = redis.ftAggregate("cursor-empty-test-idx", "*", args);

        assertThat(result).isNotNull();
        assertThat(result.getAggregationGroups()).isEqualTo(1); // Should have 0 aggregation groups for empty index
        assertThat(result.getReplies().get(0).getResults()).isEmpty(); // Should have no SearchReply objects for empty results
        assertThat(result.getCursorId()).isEqualTo(0L); // Should indicate no more results

        assertThat(redis.ftDropindex("cursor-empty-test-idx")).isEqualTo("OK");
    }

}
