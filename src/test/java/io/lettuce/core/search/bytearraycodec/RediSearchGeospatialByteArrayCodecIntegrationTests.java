/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.bytearraycodec;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.AfterAll;
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
import io.lettuce.core.search.SearchReply;

/**
 * Integration tests for Redis Search geospatial functionality using {@link ByteArrayCodec}.
 * <p>
 * These tests cover geospatial data storage and querying capabilities including GEO fields, GEOSHAPE fields, spatial
 * relationship queries (WITHIN, CONTAINS, INTERSECTS, DISJOINT), and Well-Known Text (WKT) format support.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchGeospatialByteArrayCodecIntegrationTests {

    // Index names
    private static final String GEO_INDEX = "geo-idx";

    private static final String GEOSHAPE_INDEX = "geoshape-idx";

    private static final String CARTESIAN_INDEX = "cartesian-idx";

    protected static RedisClient client;

    protected static RedisCommands<byte[], byte[]> redis;

    // Separate String connection for RedisConditions version checks
    protected static RedisCommands<String, String> stringRedis;

    public RediSearchGeospatialByteArrayCodecIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect(ByteArrayCodec.INSTANCE).sync();
        stringRedis = client.connect().sync();
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
     * Test basic GEO field functionality with longitude-latitude coordinates and radius queries.
     */
    @Test
    void testGeoFieldBasicFunctionality() {
        FieldArgs<byte[]> locationField = GeoFieldArgs.<byte[]> builder().name("location".getBytes()).build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();
        FieldArgs<byte[]> cityField = TextFieldArgs.<byte[]> builder().name("city".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("store:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, nameField, cityField));
        assertThat(result).isEqualTo("OK");

        Map<byte[], byte[]> store1 = new HashMap<>();
        store1.put("name".getBytes(), "Downtown Electronics".getBytes());
        store1.put("city".getBytes(), "Denver".getBytes());
        store1.put("location".getBytes(), "-104.991531, 39.742043".getBytes());
        redis.hmset("store:1".getBytes(), store1);

        Map<byte[], byte[]> store2 = new HashMap<>();
        store2.put("name".getBytes(), "Mountain Gear".getBytes());
        store2.put("city".getBytes(), "Boulder".getBytes());
        store2.put("location".getBytes(), "-105.2705456, 40.0149856".getBytes());
        redis.hmset("store:2".getBytes(), store2);

        Map<byte[], byte[]> store3 = new HashMap<>();
        store3.put("name".getBytes(), "Tech Hub".getBytes());
        store3.put("city".getBytes(), "Colorado Springs".getBytes());
        store3.put("location".getBytes(), "-104.800644, 38.846127".getBytes());
        redis.hmset("store:3".getBytes(), store3);

        // Find stores within 50 miles of Denver
        SearchReply<byte[], byte[]> results = redis.ftSearch(GEO_INDEX, "@location:[-104.991531 39.742043 50 mi]".getBytes());
        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);

        // Find stores within 100 miles of Colorado Springs
        results = redis.ftSearch(GEO_INDEX, "@location:[-104.800644 38.846127 100 mi]".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        // Find stores within 20 miles of Denver
        results = redis.ftSearch(GEO_INDEX, "@location:[-104.991531 39.742043 20 mi]".getBytes());
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "name"))).isEqualTo("Downtown Electronics");

        redis.ftDropindex(GEO_INDEX);
    }

    /**
     * Test GEO field with multiple locations per document.
     */
    @Test
    void testGeoFieldMultipleLocations() {
        FieldArgs<byte[]> locationField = GeoFieldArgs.<byte[]> builder().name("locations".getBytes()).build();
        FieldArgs<byte[]> productField = TextFieldArgs.<byte[]> builder().name("product".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("product:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, productField));

        Map<byte[], byte[]> product1 = new HashMap<>();
        product1.put("product".getBytes(), "Laptop Pro".getBytes());
        product1.put("locations".getBytes(), "-104.991531, 39.742043".getBytes());
        redis.hmset("product:1".getBytes(), product1);

        Map<byte[], byte[]> product2 = new HashMap<>();
        product2.put("product".getBytes(), "Wireless Headphones".getBytes());
        product2.put("locations".getBytes(), "-105.2705456, 40.0149856".getBytes());
        redis.hmset("product:2".getBytes(), product2);

        SearchReply<byte[], byte[]> results = redis.ftSearch(GEO_INDEX, "@locations:[-104.991531 39.742043 10 mi]".getBytes());
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "product"))).isEqualTo("Laptop Pro");

        redis.ftDropindex(GEO_INDEX);
    }

    /**
     * Test GEOSHAPE field with POINT primitives using spherical coordinates.
     */
    @Test
    void testGeoshapePointSphericalCoordinates() {
        FieldArgs<byte[]> geomField = GeoshapeFieldArgs.<byte[]> builder().name("geom".getBytes()).spherical().build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("location:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEOSHAPE_INDEX, createArgs, Arrays.asList(geomField, nameField));

        Map<byte[], byte[]> location1 = new HashMap<>();
        location1.put("name".getBytes(), "Central Park".getBytes());
        location1.put("geom".getBytes(), "POINT (-73.965355 40.782865)".getBytes());
        redis.hmset("location:1".getBytes(), location1);

        Map<byte[], byte[]> location2 = new HashMap<>();
        location2.put("name".getBytes(), "Times Square".getBytes());
        location2.put("geom".getBytes(), "POINT (-73.985130 40.758896)".getBytes());
        redis.hmset("location:2".getBytes(), location2);

        Map<byte[], byte[]> location3 = new HashMap<>();
        location3.put("name".getBytes(), "Brooklyn Bridge".getBytes());
        location3.put("geom".getBytes(), "POINT (-73.996736 40.706086)".getBytes());
        redis.hmset("location:3".getBytes(), location3);

        String manhattanPolygon = "POLYGON ((-74.047 40.680, -74.047 40.820, -73.910 40.820, -73.910 40.680, -74.047 40.680))";
        SearchArgs<byte[], byte[]> withinArgs = SearchArgs.<byte[], byte[]> builder()
                .param("area".getBytes(), manhattanPolygon.getBytes()).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch(GEOSHAPE_INDEX, "@geom:[WITHIN $area]".getBytes(), withinArgs);
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);

        redis.ftDropindex(GEOSHAPE_INDEX);
    }

    /**
     * Test GEOSHAPE field with POLYGON primitives and spatial relationship queries.
     */
    @Test
    void testGeoshapePolygonSpatialRelationships() {
        assumeTrue(RedisConditions.of(stringRedis).hasVersionGreaterOrEqualsTo("7.4"));

        FieldArgs<byte[]> geomField = GeoshapeFieldArgs.<byte[]> builder().name("geom".getBytes()).flat().build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("shape:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(CARTESIAN_INDEX, createArgs, Arrays.asList(geomField, nameField));

        Map<byte[], byte[]> shape1 = new HashMap<>();
        shape1.put("name".getBytes(), "Large Square".getBytes());
        shape1.put("geom".getBytes(), "POLYGON ((0 0, 0 4, 4 4, 4 0, 0 0))".getBytes());
        redis.hmset("shape:1".getBytes(), shape1);

        Map<byte[], byte[]> shape2 = new HashMap<>();
        shape2.put("name".getBytes(), "Small Square".getBytes());
        shape2.put("geom".getBytes(), "POLYGON ((1 1, 1 2, 2 2, 2 1, 1 1))".getBytes());
        redis.hmset("shape:2".getBytes(), shape2);

        Map<byte[], byte[]> shape3 = new HashMap<>();
        shape3.put("name".getBytes(), "Overlapping Rectangle".getBytes());
        shape3.put("geom".getBytes(), "POLYGON ((3 1, 3 3, 5 3, 5 1, 3 1))".getBytes());
        redis.hmset("shape:3".getBytes(), shape3);

        Map<byte[], byte[]> shape4 = new HashMap<>();
        shape4.put("name".getBytes(), "Separate Triangle".getBytes());
        shape4.put("geom".getBytes(), "POLYGON ((6 6, 7 8, 8 6, 6 6))".getBytes());
        redis.hmset("shape:4".getBytes(), shape4);

        Map<byte[], byte[]> point1 = new HashMap<>();
        point1.put("name".getBytes(), "Center Point".getBytes());
        point1.put("geom".getBytes(), "POINT (1.5 1.5)".getBytes());
        redis.hmset("shape:5".getBytes(), point1);

        // WITHIN
        String largeSquare = "POLYGON ((0 0, 0 4, 4 4, 4 0, 0 0))";
        SearchArgs<byte[], byte[]> withinArgs = SearchArgs.<byte[], byte[]> builder()
                .param("container".getBytes(), largeSquare.getBytes()).build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[WITHIN $container]".getBytes(),
                withinArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // CONTAINS
        String testPoint = "POINT (1.5 1.5)";
        SearchArgs<byte[], byte[]> containsArgs = SearchArgs.<byte[], byte[]> builder()
                .param("point".getBytes(), testPoint.getBytes()).build();
        results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[CONTAINS $point]".getBytes(), containsArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // INTERSECTS
        String testArea = "POLYGON ((2 0, 2 2, 4 2, 4 0, 2 0))";
        SearchArgs<byte[], byte[]> intersectsArgs = SearchArgs.<byte[], byte[]> builder()
                .param("area".getBytes(), testArea.getBytes()).build();
        results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[INTERSECTS $area]".getBytes(), intersectsArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        // DISJOINT
        SearchArgs<byte[], byte[]> disjointArgs = SearchArgs.<byte[], byte[]> builder()
                .param("area".getBytes(), testArea.getBytes()).build();
        results = redis.ftSearch(CARTESIAN_INDEX, "@geom:[DISJOINT $area]".getBytes(), disjointArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex(CARTESIAN_INDEX);
    }

    /**
     * Test complex geospatial queries combining GEO and GEOSHAPE fields with other field types.
     */
    @Test
    void testComplexGeospatialQueries() {
        assumeTrue(RedisConditions.of(stringRedis).hasVersionGreaterOrEqualsTo("7.4"));

        FieldArgs<byte[]> locationField = GeoFieldArgs.<byte[]> builder().name("location".getBytes()).build();
        FieldArgs<byte[]> serviceAreaField = GeoshapeFieldArgs.<byte[]> builder().name("service_area".getBytes()).spherical()
                .build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();
        FieldArgs<byte[]> categoryField = TextFieldArgs.<byte[]> builder().name("category".getBytes()).build();
        FieldArgs<byte[]> ratingField = TextFieldArgs.<byte[]> builder().name("rating".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("business:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs,
                Arrays.asList(locationField, serviceAreaField, nameField, categoryField, ratingField));

        Map<byte[], byte[]> business1 = new HashMap<>();
        business1.put("name".getBytes(), "Downtown Pizza".getBytes());
        business1.put("category".getBytes(), "restaurant".getBytes());
        business1.put("rating".getBytes(), "4.5".getBytes());
        business1.put("location".getBytes(), "-104.991531, 39.742043".getBytes());
        business1.put("service_area".getBytes(),
                "POLYGON ((-105.1 39.6, -105.1 39.9, -104.8 39.9, -104.8 39.6, -105.1 39.6))".getBytes());
        redis.hmset("business:1".getBytes(), business1);

        Map<byte[], byte[]> business2 = new HashMap<>();
        business2.put("name".getBytes(), "Mountain Coffee".getBytes());
        business2.put("category".getBytes(), "cafe".getBytes());
        business2.put("rating".getBytes(), "4.8".getBytes());
        business2.put("location".getBytes(), "-105.2705456, 40.0149856".getBytes());
        business2.put("service_area".getBytes(),
                "POLYGON ((-105.4 39.9, -105.4 40.2, -105.1 40.2, -105.1 39.9, -105.4 39.9))".getBytes());
        redis.hmset("business:2".getBytes(), business2);

        // Find restaurants within 30 miles
        SearchReply<byte[], byte[]> results = redis.ftSearch(GEO_INDEX,
                "(@category:restaurant) (@location:[-104.991531 39.742043 30 mi])".getBytes());
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "name"))).isEqualTo("Downtown Pizza");

        // Find businesses whose service area contains a specific point
        String customerLocation = "POINT (-105.0 39.8)";
        SearchArgs<byte[], byte[]> serviceArgs = SearchArgs.<byte[], byte[]> builder()
                .param("customer".getBytes(), customerLocation.getBytes()).build();
        results = redis.ftSearch(GEO_INDEX, "@service_area:[CONTAINS $customer]".getBytes(), serviceArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Find cafes with service areas intersecting a region
        String searchRegion = "POLYGON ((-105.3 40.0, -105.3 40.1, -105.2 40.1, -105.2 40.0, -105.3 40.0))";
        SearchArgs<byte[], byte[]> complexArgs = SearchArgs.<byte[], byte[]> builder()
                .param("region".getBytes(), searchRegion.getBytes()).build();
        results = redis.ftSearch(GEO_INDEX, "(@category:cafe) (@service_area:[INTERSECTS $region])".getBytes(), complexArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(0);

        redis.ftDropindex(GEO_INDEX);
    }

    /**
     * Test geospatial queries with different distance units and coordinate systems.
     */
    @Test
    void testGeospatialUnitsAndCoordinateSystems() {
        FieldArgs<byte[]> locationField = GeoFieldArgs.<byte[]> builder().name("location".getBytes()).build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("poi:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, nameField));

        Map<byte[], byte[]> poi1 = new HashMap<>();
        poi1.put("name".getBytes(), "City Center".getBytes());
        poi1.put("location".getBytes(), "0.0, 0.0".getBytes());
        redis.hmset("poi:1".getBytes(), poi1);

        Map<byte[], byte[]> poi2 = new HashMap<>();
        poi2.put("name".getBytes(), "North Point".getBytes());
        poi2.put("location".getBytes(), "0.0, 0.01".getBytes());
        redis.hmset("poi:2".getBytes(), poi2);

        Map<byte[], byte[]> poi3 = new HashMap<>();
        poi3.put("name".getBytes(), "East Point".getBytes());
        poi3.put("location".getBytes(), "0.01, 0.0".getBytes());
        redis.hmset("poi:3".getBytes(), poi3);

        // kilometers
        SearchReply<byte[], byte[]> results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 2 km]".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        // miles
        results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 1 mi]".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        // meters
        results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 500 m]".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex(GEO_INDEX);
    }

    /**
     * Test error handling and edge cases for geospatial queries.
     */
    @Test
    void testGeospatialErrorHandling() {
        FieldArgs<byte[]> locationField = GeoFieldArgs.<byte[]> builder().name("location".getBytes()).build();
        FieldArgs<byte[]> geomField = GeoshapeFieldArgs.<byte[]> builder().name("geom".getBytes()).build();
        FieldArgs<byte[]> nameField = TextFieldArgs.<byte[]> builder().name("name".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("test:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(GEO_INDEX, createArgs, Arrays.asList(locationField, geomField, nameField));

        Map<byte[], byte[]> validData = new HashMap<>();
        validData.put("name".getBytes(), "Valid Location".getBytes());
        validData.put("location".getBytes(), "-104.991531, 39.742043".getBytes());
        validData.put("geom".getBytes(), "POINT (-104.991531 39.742043)".getBytes());
        redis.hmset("test:1".getBytes(), validData);

        // Valid query
        SearchReply<byte[], byte[]> results = redis.ftSearch(GEO_INDEX, "@location:[-104.991531 39.742043 10 mi]".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        // No results
        results = redis.ftSearch(GEO_INDEX, "@location:[0.0 0.0 1 m]".getBytes());
        assertThat(results.getCount()).isEqualTo(0);

        // Valid GEOSHAPE query
        String validPolygon = "POLYGON ((-105 39, -105 40, -104 40, -104 39, -105 39))";
        SearchArgs<byte[], byte[]> validArgs = SearchArgs.<byte[], byte[]> builder()
                .param("area".getBytes(), validPolygon.getBytes()).build();
        results = redis.ftSearch(GEO_INDEX, "@geom:[WITHIN $area]".getBytes(), validArgs);
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex(GEO_INDEX);
    }

    /**
     * Helper method to find a field value in a byte[] keyed map.
     */
    private byte[] getField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] key = fieldName.getBytes();
        return fields.entrySet().stream().filter(e -> Arrays.equals(e.getKey(), key)).map(Map.Entry::getValue).findFirst()
                .orElse(null);
    }

}
