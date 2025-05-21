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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
public class RedisVectorSetAdvancedIntegrationTests {

    private static final String VECTOR_SET_KEY = "products:vectors";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected static RedisClient client;
    protected static RedisCommands<String, String> redis;

    public RedisVectorSetAdvancedIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = client.connect();
        redis = connection.sync();
    }

    @BeforeEach
    public void prepare() throws IOException {
        redis.flushall();
        
        // Load sample vectors from JSON file
        Path path = Paths.get("src/test/resources/sample-vectors.json");
        String jsonContent = String.join("", Files.readAllLines(path));
        JsonNode root = objectMapper.readTree(jsonContent);
        JsonNode vectors = root.get("vectors");
        
        // Add vectors to Redis
        for (JsonNode item : vectors) {
            String id = item.get("id").asText();
            JsonNode vectorNode = item.get("vector");
            JsonNode metadataNode = item.get("metadata");
            
            // Convert vector to Double array
            List<Double> vectorList = new ArrayList<>();
            for (JsonNode value : vectorNode) {
                vectorList.add(value.asDouble());
            }
            Double[] vector = vectorList.toArray(new Double[0]);
            
            // Add vector to Redis
            VAddArgs args = new VAddArgs();
            args.quantizationType(QuantizationType.Q8);
            
            // Add vector with metadata
            Boolean result = redis.vadd(VECTOR_SET_KEY, id, args, vector);
            assertThat(result).isTrue();
            
            // Set metadata as attributes
            String metadata = metadataNode.toString();
            redis.vsetattr(VECTOR_SET_KEY, id, metadata);
        }
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void testVectorSetCreation() {
        Long count = redis.vcard(VECTOR_SET_KEY);
        assertThat(count).isEqualTo(8);
        
        VectorMetadata info = redis.vinfo(VECTOR_SET_KEY);
        assertThat(info).isNotNull();
        assertThat(info.getSize()).isEqualTo(8);
        assertThat(info.getDimensionality()).isEqualTo(3);
    }

    @Test
    void testSimilaritySearch() {
        // Search for vectors similar to item1
        List<String> similar = redis.vsim(VECTOR_SET_KEY, "item1");
        assertThat(similar).isNotEmpty();
        
        // The most similar items to item1 should be item2 and item3
        assertThat(similar.get(0)).isEqualTo("item2");
        assertThat(similar.get(1)).isEqualTo("item3");
    }

    @Test
    void testSimilaritySearchWithFiltering() {
        // Create VSimArgs with filtering by category
        VSimArgs args = new VSimArgs();
        args.count(3L);
        args.filter("$.category == \"clothing\"");
        
        // Search for vectors similar to item1 with filtering
        List<String> similar = redis.vsim(VECTOR_SET_KEY, "item1", args);
        assertThat(similar).isNotEmpty();
        
        // All results should be clothing items
        for (String item : similar) {
            String attrs = redis.vgetattr(VECTOR_SET_KEY, item);
            assertThat(attrs).contains("\"category\":\"clothing\"");
        }
    }

    @Test
    void testSimilaritySearchWithScores() {
        // Search for vectors similar to item1 with scores
        Map<String, Long> similarWithScores = redis.vsimWithScore(VECTOR_SET_KEY, "item1");
        assertThat(similarWithScores).isNotEmpty();
        
        // Check that scores are ordered (lower scores are more similar)
        Long previousScore = null;
        for (Map.Entry<String, Long> entry : similarWithScores.entrySet()) {
            if (previousScore != null) {
                assertThat(entry.getValue()).isGreaterThanOrEqualTo(previousScore);
            }
            previousScore = entry.getValue();
        }
    }

    @Test
    void testVectorAttributes() {
        // Get attributes for item1
        String attrs = redis.vgetattr(VECTOR_SET_KEY, "item1");
        assertThat(attrs).contains("\"category\":\"electronics\"");
        assertThat(attrs).contains("\"price\":100.0");
        
        // Update attributes
        String newAttrs = "{\"category\":\"electronics\",\"price\":120.0,\"discount\":true}";
        Boolean result = redis.vsetattr(VECTOR_SET_KEY, "item1", newAttrs);
        assertThat(result).isTrue();
        
        // Verify updated attributes
        attrs = redis.vgetattr(VECTOR_SET_KEY, "item1");
        assertThat(attrs).contains("\"price\":120.0");
        assertThat(attrs).contains("\"discount\":true");
    }

    @Test
    void testVectorRemoval() {
        // Remove item1
        Boolean result = redis.vrem(VECTOR_SET_KEY, "item1");
        assertThat(result).isTrue();
        
        // Verify item count
        Long count = redis.vcard(VECTOR_SET_KEY);
        assertThat(count).isEqualTo(7);
        
        // Verify item1 is no longer in the results
        List<String> similar = redis.vsim(VECTOR_SET_KEY, "item2");
        assertThat(similar).doesNotContain("item1");
    }

    @Test
    void testRandomSampling() {
        // Get multiple random items
        List<String> randomItems = redis.vrandmember(VECTOR_SET_KEY, 3);
        assertThat(randomItems).hasSize(3);
        
        // Verify all items are valid
        for (String item : randomItems) {
            List<Double> vector = redis.vemb(VECTOR_SET_KEY, item);
            assertThat(vector).isNotEmpty();
        }
    }

    @Test
    void testHnswGraphLinks() {
        // Get links for item1
        List<String> links = redis.vlinks(VECTOR_SET_KEY, "item1");
        assertThat(links).isNotEmpty();
        
        // Get links with scores
        List<String> linksWithScores = redis.vlinksWithScores(VECTOR_SET_KEY, "item1");
        assertThat(linksWithScores).isNotEmpty();
        
        // The links list should have fewer elements than the linksWithScores list
        // because linksWithScores includes both elements and scores
        assertThat(linksWithScores.size()).isGreaterThan(links.size());
    }
}
