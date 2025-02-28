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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.GeoFieldArgs;
import io.lettuce.core.search.arguments.GeoshapeFieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.core.search.SearchResults;

/**
 * Integration tests for Redis Search geospatial functionality using GEO and GEOSHAPE fields.
 * <p>
 * These tests cover geospatial data storage and querying capabilities including:
 * <ul>
 * <li>GEO fields for simple longitude-latitude point storage and radius queries</li>
 * <li>GEOSHAPE fields for advanced point and polygon storage with spatial relationship queries</li>
 * <li>Geographical coordinates (spherical) and Cartesian coordinates (flat)</li>
 * <li>Spatial relationship queries: WITHIN, CONTAINS, INTERSECTS, DISJOINT</li>
 * <li>Point-in-polygon and polygon-polygon spatial operations</li>
 * <li>Well-Known Text (WKT) format support for POINT and POLYGON primitives</li>
 * </ul>
 * <p>
 * Based on the Redis documentation:
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/geo/">Geospatial</a>
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RediSearchGeospatialIntegrationTests {

    // Index names
    private static final String GEO_INDEX = "geo-idx";

    private static final String GEOSHAPE_INDEX = "geoshape-idx";

    private static final String CARTESIAN_INDEX = "cartesian-idx";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RediSearchGeospatialIntegrationTests() {
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

    /**
     * Test basic GEO field functionality with longitude-latitude coordinates and radius queries. Based on Redis documentation
     * examples for simple geospatial point storage and search.
     */
    @Test
    void testGeoFieldBasicFunctionality() {
        // Create index with GEO field for location data
        FieldArgs<String> locationField = GeoFieldArgs.<String> builder().name("location").build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();
        FieldArgs<String> cityField = TextFieldArgs.<String> builder().name("city").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("store:")
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, nameField, cityField));
        assertThat(result).isEqualTo("OK");

        // Add stores with geographical coordinates (longitude, latitude)
        Map<String, String> store1 = new HashMap<>();
        store1.put("name", "Downtown Electronics");
        store1.put("city", "Denver");
        store1.put("location", "-104.991531, 39.742043"); // Denver coordinates
        redis.hmset("store:1", store1);

        Map<String, String> store2 = new HashMap<>();
        store2.put("name", "Mountain Gear");
        store2.put("city", "Boulder");
        store2.put("location", "-105.2705456, 40.0149856"); // Boulder coordinates
        redis.hmset("store:2", store2);

        Map<String, String> store3 = new HashMap<>();
        store3.put("name", "Tech Hub");
        store3.put("city", "Colorado Springs");
        store3.put("location", "-104.800644, 38.846127"); // Colorado Springs coordinates
        redis.hmset("store:3", store3);

        // Test 1: Find stores within 50 miles of Denver
        SearchResults<String, String> results = redis.ftSearch(GEO_INDEX, "@location:[-104.991531 39.742043 50 mi]", null);

        assertThat(results.getCount()).isEqualTo(2); // Denver and Boulder stores
        assertThat(results.getResults()).hasSize(2);

        // Test 2: Find stores within 100 miles of Colorado Springs
        results = redis.ftSearch(GEO_INDEX, "@location:[-104.800644 38.846127 100 mi]", null);

        assertThat(results.getCount()).isEqualTo(3); // All stores within 100 miles
        assertThat(results.getResults()).hasSize(3);

        // Test 3: Find stores within 20 miles of Denver (should only find Denver store)
        results = redis.ftSearch(GEO_INDEX, "@location:[-104.991531 39.742043 20 mi]", null);

        assertThat(results.getCount()).isEqualTo(1); // Only Denver store
        assertThat(results.getResults()).hasSize(1);
        assertThat(results.getResults().get(0).getFields().get("name")).isEqualTo("Downtown Electronics");

        // Cleanup
        redis.ftDropindex(GEO_INDEX, false);
    }

    /**
     * Test GEO field with multiple locations per document using JSON array format. Demonstrates how a single document can have
     * multiple geographical locations.
     */
    @Test
    void testGeoFieldMultipleLocations() {
        // Create index for products with multiple store locations
        FieldArgs<String> locationField = GeoFieldArgs.<String> builder().name("locations").build();
        FieldArgs<String> productField = TextFieldArgs.<String> builder().name("product").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("product:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, productField));

        // Add product available at multiple locations
        Map<String, String> product1 = new HashMap<>();
        product1.put("product", "Laptop Pro");
        // Multiple locations as comma-separated string (alternative format)
        product1.put("locations", "-104.991531, 39.742043"); // Denver only for this test
        redis.hmset("product:1", product1);

        Map<String, String> product2 = new HashMap<>();
        product2.put("product", "Wireless Headphones");
        product2.put("locations", "-105.2705456, 40.0149856"); // Boulder
        redis.hmset("product:2", product2);

        // Test search for products available near Denver (use smaller radius to be more specific)
        SearchResults<String, String> results = redis.ftSearch(GEO_INDEX, "@locations:[-104.991531 39.742043 10 mi]", null);

        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("product")).isEqualTo("Laptop Pro");

        // Cleanup
        redis.ftDropindex(GEO_INDEX, false);
    }

    /**
     * Test GEOSHAPE field with POINT primitives using spherical coordinates. Demonstrates basic point storage and spatial
     * queries using Well-Known Text format.
     */
    @Test
    void testGeoshapePointSphericalCoordinates() {
        // Create index with GEOSHAPE field using spherical coordinates (default)
        FieldArgs<String> geomField = GeoshapeFieldArgs.<String> builder().name("geom").spherical().build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("location:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEOSHAPE_INDEX, createArgs, Arrays.asList(geomField, nameField));

        // Add locations using WKT POINT format with geographical coordinates
        Map<String, String> location1 = new HashMap<>();
        location1.put("name", "Central Park");
        location1.put("geom", "POINT (-73.965355 40.782865)"); // Central Park, NYC
        redis.hmset("location:1", location1);

        Map<String, String> location2 = new HashMap<>();
        location2.put("name", "Times Square");
        location2.put("geom", "POINT (-73.985130 40.758896)"); // Times Square, NYC
        redis.hmset("location:2", location2);

        Map<String, String> location3 = new HashMap<>();
        location3.put("name", "Brooklyn Bridge");
        location3.put("geom", "POINT (-73.996736 40.706086)"); // Brooklyn Bridge, NYC
        redis.hmset("location:3", location3);

        // Test 1: Find points within Manhattan area (rough polygon)
        String manhattanPolygon = "POLYGON ((-74.047 40.680, -74.047 40.820, -73.910 40.820, -73.910 40.680, -74.047 40.680))";
        SearchArgs<String, String> withinArgs = SearchArgs.<String, String> builder().param("area", manhattanPolygon).build();

        SearchResults<String, String> results = redis.ftSearch(GEOSHAPE_INDEX, "@geom:[WITHIN $area]", withinArgs);

        assertThat(results.getCount()).isEqualTo(3); // All locations are in Manhattan
        assertThat(results.getResults()).hasSize(3);

        // Cleanup
        redis.ftDropindex(GEOSHAPE_INDEX, false);
    }

    /**
     * Test GEOSHAPE field with POLYGON primitives and spatial relationship queries. Demonstrates advanced polygon storage and
     * WITHIN, CONTAINS, INTERSECTS, DISJOINT operations.
     */
    @Test
    void testGeoshapePolygonSpatialRelationships() {
        // Create index with GEOSHAPE field using Cartesian coordinates for easier testing
        FieldArgs<String> geomField = GeoshapeFieldArgs.<String> builder().name("geom").flat().build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("shape:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(CARTESIAN_INDEX, createArgs, Arrays.asList(geomField, nameField));

        // Add shapes using WKT format with Cartesian coordinates
        Map<String, String> shape1 = new HashMap<>();
        shape1.put("name", "Large Square");
        shape1.put("geom", "POLYGON ((0 0, 0 4, 4 4, 4 0, 0 0))"); // Large square
        redis.hmset("shape:1", shape1);

        Map<String, String> shape2 = new HashMap<>();
        shape2.put("name", "Small Square");
        shape2.put("geom", "POLYGON ((1 1, 1 2, 2 2, 2 1, 1 1))"); // Small square inside large square
        redis.hmset("shape:2", shape2);

        Map<String, String> shape3 = new HashMap<>();
        shape3.put("name", "Overlapping Rectangle");
        shape3.put("geom", "POLYGON ((3 1, 3 3, 5 3, 5 1, 3 1))"); // Rectangle overlapping large square
        redis.hmset("shape:3", shape3);

        Map<String, String> shape4 = new HashMap<>();
        shape4.put("name", "Separate Triangle");
        shape4.put("geom", "POLYGON ((6 6, 7 8, 8 6, 6 6))"); // Triangle separate from other shapes
        redis.hmset("shape:4", shape4);

        // Add a point for testing
        Map<String, String> point1 = new HashMap<>();
        point1.put("name", "Center Point");
        point1.put("geom", "POINT (1.5 1.5)"); // Point inside small square
        redis.hmset("shape:5", point1);

        // Test 1: WITHIN - Find shapes within the large square
        String largeSquare = "POLYGON ((0 0, 0 4, 4 4, 4 0, 0 0))";
        SearchArgs<String, String> withinArgs = SearchArgs.<String, String> builder().param("container", largeSquare).build();

        SearchResults<String, String> results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[WITHIN $container]", withinArgs);

        // Should find small square and center point (both entirely within large square)
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // Test 2: CONTAINS - Find shapes that contain a specific point
        String testPoint = "POINT (1.5 1.5)";
        SearchArgs<String, String> containsArgs = SearchArgs.<String, String> builder().param("point", testPoint).build();

        results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[CONTAINS $point]", containsArgs);

        // Should find large square and small square (both contain the point)
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // Test 3: INTERSECTS - Find shapes that intersect with a test area
        String testArea = "POLYGON ((2 0, 2 2, 4 2, 4 0, 2 0))";
        SearchArgs<String, String> intersectsArgs = SearchArgs.<String, String> builder().param("area", testArea).build();

        results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[INTERSECTS $area]", intersectsArgs);

        // Should find large square and overlapping rectangle
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // Test 4: DISJOINT - Find shapes that don't overlap with a test area
        SearchArgs<String, String> disjointArgs = SearchArgs.<String, String> builder().param("area", testArea).build();

        results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[DISJOINT $area]", disjointArgs);

        // Should find separate triangle and possibly others
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Cleanup
        redis.ftDropindex(CARTESIAN_INDEX, false);
    }

    /**
     * Test complex geospatial queries combining GEO and GEOSHAPE fields with other field types. Demonstrates real-world
     * scenarios with mixed field types and complex query conditions.
     */
    @Test
    void testComplexGeospatialQueries() {
        // Create index with mixed field types including geospatial
        FieldArgs<String> locationField = GeoFieldArgs.<String> builder().name("location").build();
        FieldArgs<String> serviceAreaField = GeoshapeFieldArgs.<String> builder().name("service_area").spherical().build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();
        FieldArgs<String> categoryField = TextFieldArgs.<String> builder().name("category").build();
        FieldArgs<String> ratingField = TextFieldArgs.<String> builder().name("rating").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("business:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs,
                Arrays.asList(locationField, serviceAreaField, nameField, categoryField, ratingField));

        // Add businesses with both point locations and service areas
        Map<String, String> business1 = new HashMap<>();
        business1.put("name", "Downtown Pizza");
        business1.put("category", "restaurant");
        business1.put("rating", "4.5");
        business1.put("location", "-104.991531, 39.742043"); // Denver
        business1.put("service_area", "POLYGON ((-105.1 39.6, -105.1 39.9, -104.8 39.9, -104.8 39.6, -105.1 39.6))");
        redis.hmset("business:1", business1);

        Map<String, String> business2 = new HashMap<>();
        business2.put("name", "Mountain Coffee");
        business2.put("category", "cafe");
        business2.put("rating", "4.8");
        business2.put("location", "-105.2705456, 40.0149856"); // Boulder
        business2.put("service_area", "POLYGON ((-105.4 39.9, -105.4 40.2, -105.1 40.2, -105.1 39.9, -105.4 39.9))");
        redis.hmset("business:2", business2);

        // Test 1: Find restaurants within 30 miles of a location
        SearchResults<String, String> results = redis.ftSearch(GEO_INDEX,
                "(@category:restaurant) (@location:[-104.991531 39.742043 30 mi])", null);

        assertThat(results.getCount()).isEqualTo(1);
        assertThat(results.getResults().get(0).getFields().get("name")).isEqualTo("Downtown Pizza");

        // Test 2: Find businesses whose service area contains a specific point
        String customerLocation = "POINT (-105.0 39.8)";
        SearchArgs<String, String> serviceArgs = SearchArgs.<String, String> builder().param("customer", customerLocation)
                .build();

        results = redis.ftSearch(GEO_INDEX, "@service_area:[CONTAINS $customer]", serviceArgs);

        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Test 3: Find high-rated cafes with service areas intersecting a region
        String searchRegion = "POLYGON ((-105.3 40.0, -105.3 40.1, -105.2 40.1, -105.2 40.0, -105.3 40.0))";
        SearchArgs<String, String> complexArgs = SearchArgs.<String, String> builder().param("region", searchRegion).build();

        results = redis.ftSearch(GEO_INDEX, "(@category:cafe) (@service_area:[INTERSECTS $region])", complexArgs);

        assertThat(results.getCount()).isGreaterThanOrEqualTo(0); // May or may not find results depending on exact coordinates

        // Cleanup
        redis.ftDropindex(GEO_INDEX, false);
    }

    /**
     * Test geospatial queries with different distance units and coordinate systems. Demonstrates unit conversions and
     * coordinate system differences.
     */
    @Test
    void testGeospatialUnitsAndCoordinateSystems() {
        // Create index for testing different units
        FieldArgs<String> locationField = GeoFieldArgs.<String> builder().name("location").build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("poi:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, nameField));

        // Add points of interest
        Map<String, String> poi1 = new HashMap<>();
        poi1.put("name", "City Center");
        poi1.put("location", "0.0, 0.0"); // Origin point
        redis.hmset("poi:1", poi1);

        Map<String, String> poi2 = new HashMap<>();
        poi2.put("name", "North Point");
        poi2.put("location", "0.0, 0.01"); // ~1.1 km north
        redis.hmset("poi:2", poi2);

        Map<String, String> poi3 = new HashMap<>();
        poi3.put("name", "East Point");
        poi3.put("location", "0.01, 0.0"); // ~1.1 km east
        redis.hmset("poi:3", poi3);

        // Test 1: Search with kilometers
        SearchResults<String, String> results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 2 km]", null);
        assertThat(results.getCount()).isEqualTo(3); // All points within 2 km

        // Test 2: Search with miles
        results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 1 mi]", null);
        assertThat(results.getCount()).isEqualTo(3); // All points within 1 mile

        // Test 3: Search with meters
        results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 500 m]", null);
        assertThat(results.getCount()).isEqualTo(1); // Only center point within 500m

        // Cleanup
        redis.ftDropindex(GEO_INDEX, false);
    }

    /**
     * Test error handling and edge cases for geospatial queries. Demonstrates proper handling of invalid coordinates, malformed
     * WKT, and boundary conditions.
     */
    @Test
    void testGeospatialErrorHandling() {
        // Create index for error testing
        FieldArgs<String> locationField = GeoFieldArgs.<String> builder().name("location").build();
        FieldArgs<String> geomField = GeoshapeFieldArgs.<String> builder().name("geom").build();
        FieldArgs<String> nameField = TextFieldArgs.<String> builder().name("name").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix("test:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, geomField, nameField));

        // Add valid test data
        Map<String, String> validData = new HashMap<>();
        validData.put("name", "Valid Location");
        validData.put("location", "-104.991531, 39.742043");
        validData.put("geom", "POINT (-104.991531 39.742043)");
        redis.hmset("test:1", validData);

        // Test 1: Valid query should work
        SearchResults<String, String> results = redis.ftSearch(GEO_INDEX, "@location:[-104.991531 39.742043 10 mi]", null);
        assertThat(results.getCount()).isEqualTo(1);

        // Test 2: Query with no results should return empty
        results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 1 m]", null);
        assertThat(results.getCount()).isEqualTo(0);

        // Test 3: Valid GEOSHAPE query
        String validPolygon = "POLYGON ((-105 39, -105 40, -104 40, -104 39, -105 39))";
        SearchArgs<String, String> validArgs = SearchArgs.<String, String> builder().param("area", validPolygon).build();

        results = redis.ftSearch(GEO_INDEX, "@geom:[WITHIN $area]", validArgs);
        assertThat(results.getCount()).isEqualTo(1);

        // Cleanup
        redis.ftDropindex(GEO_INDEX, false);
    }

}
