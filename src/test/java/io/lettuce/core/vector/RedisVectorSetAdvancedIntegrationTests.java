/*
 * Copyright 2024-2025, Redis Ltd. and Contributors
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

package io.lettuce.core.vector;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.VAddArgs;
import io.lettuce.core.VSimArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for Redis Vector Sets based on the examples from the Redis documentation.
 *
 * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/">Redis Vector Sets Documentation</a>
 */
@Tag(INTEGRATION_TEST)
public class RedisVectorSetAdvancedIntegrationTests {

    private static final String POINTS_KEY = "points";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RedisVectorSetAdvancedIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = client.connect();
        redis = connection.sync();
    }

    @BeforeEach
    public void prepare() {
        redis.flushall();

        // Add the example points from the Redis documentation
        // A: (1.0, 1.0), B: (-1.0, -1.0), C: (-1.0, 1.0), D: (1.0, -1.0), and E: (1.0, 0)
        redis.vadd(POINTS_KEY, "pt:A", 1.0, 1.0);
        redis.vadd(POINTS_KEY, "pt:B", -1.0, -1.0);
        redis.vadd(POINTS_KEY, "pt:C", -1.0, 1.0);
        redis.vadd(POINTS_KEY, "pt:D", 1.0, -1.0);
        redis.vadd(POINTS_KEY, "pt:E", 1.0, 0.0);
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * Test basic vector set operations as shown in the Redis documentation.
     */
    @Test
    void testBasicOperations() {
        // Check the type of the vector set
        String type = redis.type(POINTS_KEY);
        assertThat(type).isEqualTo("vectorset");

        // Check the cardinality of the vector set
        Long count = redis.vcard(POINTS_KEY);
        assertThat(count).isEqualTo(5);

        // Check the dimensionality of the vectors
        Long dim = redis.vdim(POINTS_KEY);
        assertThat(dim).isEqualTo(2);

        // Retrieve the vectors for each point
        List<Double> vectorA = redis.vemb(POINTS_KEY, "pt:A");
        assertThat(vectorA).hasSize(2);
        assertThat(vectorA.get(0)).isCloseTo(1.0, within(0.001));
        assertThat(vectorA.get(1)).isCloseTo(1.0, within(0.001));

        List<Double> vectorB = redis.vemb(POINTS_KEY, "pt:B");
        assertThat(vectorB).hasSize(2);
        assertThat(vectorB.get(0)).isCloseTo(-1.0, within(0.001));
        assertThat(vectorB.get(1)).isCloseTo(-1.0, within(0.001));

        List<Double> vectorC = redis.vemb(POINTS_KEY, "pt:C");
        assertThat(vectorC).hasSize(2);
        assertThat(vectorC.get(0)).isCloseTo(-1.0, within(0.001));
        assertThat(vectorC.get(1)).isCloseTo(1.0, within(0.001));

        List<Double> vectorD = redis.vemb(POINTS_KEY, "pt:D");
        assertThat(vectorD).hasSize(2);
        assertThat(vectorD.get(0)).isCloseTo(1.0, within(0.001));
        assertThat(vectorD.get(1)).isCloseTo(-1.0, within(0.001));

        List<Double> vectorE = redis.vemb(POINTS_KEY, "pt:E");
        assertThat(vectorE).hasSize(2);
        assertThat(vectorE.get(0)).isCloseTo(1.0, within(0.001));
        assertThat(vectorE.get(1)).isCloseTo(0.0, within(0.001));
    }

    /**
     * Test attribute operations on vector set elements.
     */
    @Test
    void testAttributeOperations() {
        // Set attributes for point A
        String attributes = "{\"name\":\"Point A\",\"description\":\"First point added\"}";
        Boolean result = redis.vsetattr(POINTS_KEY, "pt:A", attributes);
        assertThat(result).isTrue();

        // Get attributes for point A
        String retrievedAttributes = redis.vgetattr(POINTS_KEY, "pt:A");
        assertThat(retrievedAttributes).contains("\"name\":\"Point A\"");
        assertThat(retrievedAttributes).contains("\"description\":\"First point added\"");

        // Delete attributes by setting an empty string
        result = redis.vsetattr(POINTS_KEY, "pt:A", "");
        assertThat(result).isTrue();

        // Verify attributes are deleted
        retrievedAttributes = redis.vgetattr(POINTS_KEY, "pt:A");
        assertThat(retrievedAttributes).isNull();
    }

    /**
     * Test adding and removing elements from a vector set.
     */
    @Test
    void testAddAndRemoveElements() {
        // Add a new point F at (0, 0)
        Boolean result = redis.vadd(POINTS_KEY, "pt:F", 0.0, 0.0);
        assertThat(result).isTrue();

        // Check the updated cardinality
        Long count = redis.vcard(POINTS_KEY);
        assertThat(count).isEqualTo(6);

        // Remove point F
        result = redis.vrem(POINTS_KEY, "pt:F");
        assertThat(result).isTrue();

        // Check the cardinality after removal
        count = redis.vcard(POINTS_KEY);
        assertThat(count).isEqualTo(5);
    }

    /**
     * Test vector similarity search as shown in the Redis documentation.
     */
    @Test
    void testVectorSimilaritySearch() {
        // Search for vectors similar to (0.9, 0.1)
        List<String> similar = redis.vsim(POINTS_KEY, 0.9, 0.1);
        assertThat(similar).isNotEmpty();

        // The expected order based on similarity to (0.9, 0.1) should be:
        // E (1.0, 0.0), A (1.0, 1.0), D (1.0, -1.0), C (-1.0, 1.0), B (-1.0, -1.0)
        assertThat(similar.get(0)).isEqualTo("pt:E");
        assertThat(similar.get(1)).isEqualTo("pt:A");
        assertThat(similar.get(2)).isEqualTo("pt:D");
        assertThat(similar.get(3)).isEqualTo("pt:C");
        assertThat(similar.get(4)).isEqualTo("pt:B");

        // Search for vectors similar to point A with scores
        Map<String, Double> similarWithScores = redis.vsimWithScore(POINTS_KEY, "pt:A");
        assertThat(similarWithScores).isNotEmpty();

        // Point A should have a perfect similarity score of 1.0 with itself
        assertThat(similarWithScores.get("pt:A")).isCloseTo(1.0, within(0.001));

        // Limit the number of results to 4
        VSimArgs args = new VSimArgs();
        args.count(4L);
        similar = redis.vsim(POINTS_KEY, args, "pt:A");
        assertThat(similar).hasSize(4);
    }

    /**
     * Test filtered vector similarity search as shown in the Redis documentation.
     */
    @Test
    void testFilteredVectorSimilaritySearch() {
        // Set attributes for all points
        redis.vsetattr(POINTS_KEY, "pt:A", "{\"size\":\"large\",\"price\":18.99}");
        redis.vsetattr(POINTS_KEY, "pt:B", "{\"size\":\"large\",\"price\":35.99}");
        redis.vsetattr(POINTS_KEY, "pt:C", "{\"size\":\"large\",\"price\":25.99}");
        redis.vsetattr(POINTS_KEY, "pt:D", "{\"size\":\"small\",\"price\":21.00}");
        redis.vsetattr(POINTS_KEY, "pt:E", "{\"size\":\"small\",\"price\":17.75}");

        // Filter by size = "large"
        VSimArgs args = new VSimArgs();
        args.filter(".size == \"large\"");
        List<String> similar = redis.vsim(POINTS_KEY, args, "pt:A");
        assertThat(similar).hasSize(3);
        assertThat(similar).containsExactly("pt:A", "pt:C", "pt:B");

        // Filter by size = "large" AND price > 20.00
        args = new VSimArgs();
        args.filter(".size == \"large\" && .price > 20.00");
        similar = redis.vsim(POINTS_KEY, args, "pt:A");
        assertThat(similar).hasSize(2);
        assertThat(similar).containsExactly("pt:C", "pt:B");
    }

    /**
     * Test advanced filter expressions for vector similarity search.
     *
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/filtered-search/">Redis Vector Sets Filter
     *      Expressions</a>
     */
    @Test
    void testAdvancedFilterExpressions() {
        // Create a vector set with movie data
        String moviesKey = "movies";

        // Add movie vectors with attributes
        VAddArgs args = new VAddArgs();
        args.attributes(
                "{\"title\":\"The Godfather\",\"year\":1972,\"rating\":9.2,\"genre\":\"drama\",\"director\":\"Coppola\"}");
        redis.vadd(moviesKey, "movie1", args, 0.1, 0.2, 0.3);

        args = new VAddArgs();
        args.attributes("{\"title\":\"Star Wars\",\"year\":1977,\"rating\":8.6,\"genre\":\"sci-fi\",\"director\":\"Lucas\"}");
        redis.vadd(moviesKey, "movie2", args, 0.2, 0.3, 0.4);

        args = new VAddArgs();
        args.attributes(
                "{\"title\":\"Jurassic Park\",\"year\":1993,\"rating\":8.1,\"genre\":\"adventure\",\"director\":\"Spielberg\"}");
        redis.vadd(moviesKey, "movie3", args, 0.3, 0.4, 0.5);

        args = new VAddArgs();
        args.attributes(
                "{\"title\":\"The Dark Knight\",\"year\":2008,\"rating\":9.0,\"genre\":\"action\",\"director\":\"Nolan\"}");
        redis.vadd(moviesKey, "movie4", args, 0.4, 0.5, 0.6);

        args = new VAddArgs();
        args.attributes("{\"title\":\"Inception\",\"year\":2010,\"rating\":8.8,\"genre\":\"sci-fi\",\"director\":\"Nolan\"}");
        redis.vadd(moviesKey, "movie5", args, 0.5, 0.6, 0.7);

        args = new VAddArgs();
        args.attributes(
                "{\"title\":\"Interstellar\",\"year\":2014,\"rating\":8.6,\"genre\":\"sci-fi\",\"director\":\"Nolan\"}");
        redis.vadd(moviesKey, "movie6", args, 0.6, 0.7, 0.8);

        args = new VAddArgs();
        args.attributes("{\"title\":\"E.T.\",\"year\":1982,\"rating\":7.8,\"genre\":\"sci-fi\",\"director\":\"Spielberg\"}");
        redis.vadd(moviesKey, "movie7", args, 0.7, 0.8, 0.9);

        // Test filter by year range
        VSimArgs vSimArgs = new VSimArgs();
        vSimArgs.filter(".year >= 1980 and .year < 1990");
        List<String> similar = redis.vsim(moviesKey, vSimArgs, 0.5, 0.6, 0.7);
        assertThat(similar).hasSize(1);
        assertThat(similar.get(0)).isEqualTo("movie7"); // E.T. (1982)

        // Test filter by genre and rating
        vSimArgs = new VSimArgs();
        vSimArgs.filter(".genre == \"sci-fi\" and .rating > 8.5");
        similar = redis.vsim(moviesKey, vSimArgs, 0.5, 0.6, 0.7);
        assertThat(similar).hasSize(3);
        // Should include Star Wars, Inception, and Interstellar
        assertThat(similar).contains("movie2", "movie5", "movie6");

        // Test IN operator with array
        vSimArgs = new VSimArgs();
        vSimArgs.filter(".director in [\"Spielberg\", \"Nolan\"]");
        similar = redis.vsim(moviesKey, vSimArgs, 0.5, 0.6, 0.7);
        assertThat(similar).hasSize(5);
        // Should include Jurassic Park, The Dark Knight, Inception, Interstellar, and E.T.
        assertThat(similar).contains("movie3", "movie4", "movie5", "movie6", "movie7");

        // Test mathematical expressions
        vSimArgs = new VSimArgs();
        vSimArgs.filter("(.year - 2000) ** 2 < 100 and .rating / 2 > 4");
        similar = redis.vsim(moviesKey, vSimArgs, 0.5, 0.6, 0.7);
        assertThat(similar).hasSize(2);
        // Should include The Dark Knight (2008) and Inception (2010)
        assertThat(similar).contains("movie4", "movie3");
    }

    /**
     * Test the FILTER-EF option for controlling how many candidate nodes are inspected.
     *
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/filtered-search/">Redis Vector Sets Filter
     *      Expressions</a>
     */
    @Test
    void testFilterEfOption() {
        // Create a vector set with 100 elements
        String largeSetKey = "large_set";

        // Add 100 vectors with different attributes
        for (int i = 0; i < 100; i++) {
            VAddArgs args = new VAddArgs();
            // Only 10% of elements have category = "rare"
            String category = (i % 10 == 0) ? "rare" : "common";
            // Prices range from 10 to 109
            double price = 10 + i;
            // Years range from 1950 to 2049
            int year = 1950 + i;

            args.attributes(
                    String.format("{\"id\":%d,\"category\":\"%s\",\"price\":%.2f,\"year\":%d}", i, category, price, year));

            // Create vectors with slightly different values
            double x = 0.1 + (i * 0.01);
            double y = 0.2 + (i * 0.01);
            double z = 0.3 + (i * 0.01);

            redis.vadd(largeSetKey, "item" + i, args, x, y, z);
        }

        // Test with default FILTER-EF (COUNT * 100)
        VSimArgs args = new VSimArgs();
        args.count(3L);
        args.filter(".category == \"rare\"");
        List<String> similar = redis.vsim(largeSetKey, args, 0.5, 0.6, 0.7);
        assertThat(similar).hasSize(3);

        // Test with explicit FILTER-EF
        args = new VSimArgs();
        args.count(5L);
        args.filter(".category == \"rare\"");
        args.filterEfficiency(500L);
        similar = redis.vsim(largeSetKey, args, 0.5, 0.6, 0.7);
        assertThat(similar).hasSize(5);

        // Test with FILTER-EF = 0 (scan as many as needed)
        args = new VSimArgs();
        args.count(10L);
        args.filter(".category == \"rare\" and .year > 2000");
        // args.filterEfficiency(0L);
        similar = redis.vsim(largeSetKey, args, 0.5, 0.6, 0.7);
        // Should find all rare items with year > 2000
        assertThat(similar).isNotEmpty();

        // Verify all results match the filter
        for (String item : similar) {
            String attributes = redis.vgetattr(largeSetKey, item);
            assertThat(attributes).contains("\"category\":\"rare\"");

            // Extract the year from the attributes
            int startIndex = attributes.indexOf("\"year\":") + 7;
            int endIndex = attributes.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = attributes.indexOf("}", startIndex);
            }
            int year = Integer.parseInt(attributes.substring(startIndex, endIndex));
            assertThat(year).isGreaterThan(2000);
        }
    }

    /**
     * Test handling of missing attributes and edge cases in filter expressions.
     *
     * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/filtered-search/">Redis Vector Sets Filter
     *      Expressions</a>
     */
    @Test
    void testMissingAttributesAndEdgeCases() {
        // Create a vector set with elements having different attributes
        String edgeCaseKey = "edge_cases";

        // Element with complete attributes
        VAddArgs args = new VAddArgs();
        args.attributes("{\"category\":\"electronics\",\"price\":100.0,\"inStock\":true,\"tags\":[\"laptop\",\"computer\"]}");
        redis.vadd(edgeCaseKey, "complete", args, 0.1, 0.2, 0.3);

        // Element with missing price attribute
        args = new VAddArgs();
        args.attributes("{\"category\":\"electronics\",\"inStock\":false,\"tags\":[\"phone\"]}");
        redis.vadd(edgeCaseKey, "missing_price", args, 0.2, 0.3, 0.4);

        // Element with missing category attribute
        args = new VAddArgs();
        args.attributes("{\"price\":50.0,\"inStock\":true}");
        redis.vadd(edgeCaseKey, "missing_category", args, 0.3, 0.4, 0.5);

        // Element with boolean attributes
        args = new VAddArgs();
        args.attributes("{\"category\":\"clothing\",\"price\":25.0,\"inStock\":true,\"onSale\":true}");
        redis.vadd(edgeCaseKey, "boolean_attrs", args, 0.4, 0.5, 0.6);

        // Element with no attributes
        redis.vadd(edgeCaseKey, "no_attrs", 0.5, 0.6, 0.7);

        // Element with empty attributes
        args = new VAddArgs();
        args.attributes("{}");
        redis.vadd(edgeCaseKey, "empty_attrs", args, 0.6, 0.7, 0.8);

        // Test filtering on missing attributes
        VSimArgs vSimArgs = new VSimArgs();
        vSimArgs.filter(".price > 50");
        List<String> similar = redis.vsim(edgeCaseKey, vSimArgs, 0.3, 0.4, 0.5);
        // Should only include "complete" as it's the only one with price > 50
        assertThat(similar).hasSize(1);
        assertThat(similar.get(0)).isEqualTo("complete");

        // Test filtering with boolean attributes
        vSimArgs = new VSimArgs();
        vSimArgs.filter(".inStock");
        similar = redis.vsim(edgeCaseKey, vSimArgs, 0.3, 0.4, 0.5);
        // Should include "complete", "missing_category", and "boolean_attrs"
        assertThat(similar).hasSize(3);
        assertThat(similar).contains("complete", "missing_category", "boolean_attrs");

        // Test filtering with the IN operator and arrays
        vSimArgs = new VSimArgs();
        vSimArgs.filter("\"laptop\" in .tags");
        similar = redis.vsim(edgeCaseKey, vSimArgs, 0.3, 0.4, 0.5);
        // Should only include "complete"
        assertThat(similar).hasSize(1);
        assertThat(similar.get(0)).isEqualTo("complete");
    }

    /**
     * Test different quantization types as shown in the Redis documentation.
     */
    @Test
    void testQuantizationTypes() {
        // Test Q8 quantization
        VAddArgs q8Args = new VAddArgs();
        q8Args.quantizationType(QuantizationType.Q8);
        redis.vadd("quantSetQ8", "quantElement", q8Args, 1.262185, 1.958231);
        List<Double> q8Vector = redis.vemb("quantSetQ8", "quantElement");
        assertThat(q8Vector).hasSize(2);
        // Values will be slightly different due to quantization

        // Test NOQUANT (no quantization)
        VAddArgs noQuantArgs = new VAddArgs();
        noQuantArgs.quantizationType(QuantizationType.NO_QUANTIZATION);
        redis.vadd("quantSetNoQ", "quantElement", noQuantArgs, 1.262185, 1.958231);
        List<Double> noQuantVector = redis.vemb("quantSetNoQ", "quantElement");
        assertThat(noQuantVector).hasSize(2);
        assertThat(noQuantVector.get(0)).isCloseTo(1.262185, within(0.0001));
        assertThat(noQuantVector.get(1)).isCloseTo(1.958231, within(0.0001));

        // Test BIN (binary) quantization
        VAddArgs binArgs = new VAddArgs();
        binArgs.quantizationType(QuantizationType.BINARY);
        redis.vadd("quantSetBin", "quantElement", binArgs, 1.262185, 1.958231);
        List<Double> binVector = redis.vemb("quantSetBin", "quantElement");
        assertThat(binVector).hasSize(2);
        // Binary quantization will convert values to either 1 or -1
        assertThat(binVector.get(0)).isIn(1.0, -1.0);
        assertThat(binVector.get(1)).isIn(1.0, -1.0);
    }

    /**
     * Test dimensionality reduction as shown in the Redis documentation.
     */
    @Test
    void testDimensionalityReduction() {
        // Create a vector with 300 dimensions
        Double[] values = new Double[300];
        for (int i = 0; i < 300; i++) {
            values[i] = (double) i / 299;
        }

        // Add the vector without dimensionality reduction
        redis.vadd("setNotReduced", "element", values);
        Long dim = redis.vdim("setNotReduced");
        assertThat(dim).isEqualTo(300);

        redis.vadd("setReduced", 100, "element", values);
        dim = redis.vdim("setReduced");
        assertThat(dim).isEqualTo(100);
    }

    /**
     * Test vector set metadata information.
     */
    @Test
    void testVectorSetMetadata() {
        VectorMetadata info = redis.vinfo(POINTS_KEY);
        assertThat(info).isNotNull();
        assertThat(info.getDimensionality()).isEqualTo(2);
        assertThat(info.getSize()).isEqualTo(5);
        assertThat(info.getType()).isNotNull();
    }

    /**
     * Test random sampling from a vector set.
     */
    @Test
    void testRandomSampling() {
        // Get a single random element
        String randomElement = redis.vrandmember(POINTS_KEY);
        assertThat(randomElement).isNotNull();
        assertThat(randomElement).startsWith("pt:");

        // Get multiple random elements
        List<String> randomElements = redis.vrandmember(POINTS_KEY, 3);
        assertThat(randomElements).hasSize(3);
        for (String element : randomElements) {
            assertThat(element).startsWith("pt:");
        }
    }

    /**
     * Test HNSW graph links.
     */
    @Test
    void testHnswGraphLinks() {
        // Get links for point A
        List<String> links = redis.vlinks(POINTS_KEY, "pt:A");
        assertThat(links).isNotEmpty();

        // Get links with scores
        Map<String, Double> linksWithScores = redis.vlinksWithScores(POINTS_KEY, "pt:A");
        assertThat(linksWithScores).isNotEmpty();
    }

}
