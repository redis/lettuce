/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.NoFailback;
import io.lettuce.test.TestFutures;

/**
 * Integration tests for {@link MultiDbClient} validating database switching with key distribution across multiple Redis
 * endpoints.
 *
 * <p>
 * These tests validate the core functionality of MultiDbClient by:
 * <ul>
 * <li>Writing keys to one database and verifying immediate reads (sync mode)</li>
 * <li>Writing keys asynchronously and verifying correct distribution (async mode)</li>
 * <li>Switching to another database and continuing operations</li>
 * <li>Switching back to the original database</li>
 * <li>Verifying that keys are correctly distributed across databases</li>
 * <li>Testing PubSub message delivery across database switches</li>
 * </ul>
 *
 * @author Ali Takavci
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class MultiDbClientBehaviourIntegrationTests extends MultiDbTestSupport {

    private final RedisURI endpoint1;

    private final RedisURI endpoint2;

    private StatefulRedisMultiDbConnection<String, String> multiDbConnection;

    private StatefulRedisConnection<String, String> directConnection1;

    private StatefulRedisConnection<String, String> directConnection2;

    @Inject
    MultiDbClientBehaviourIntegrationTests(@NoFailback MultiDbClient multiDbClient) {
        super(multiDbClient);
        Iterator<RedisURI> endpoints = multiDbClient.getRedisURIs().iterator();
        this.endpoint1 = endpoints.next();
        this.endpoint2 = endpoints.next();
    }

    @BeforeEach
    void setUp() {
        // Create connections
        multiDbConnection = multiDbClient.connect();
        directConnection1 = directClient1.connect();
        directConnection2 = directClient2.connect();

        // Wait for at least 3 endpoints to be available
        waitForEndpoints(multiDbConnection, 3, 2);
    }

    @AfterEach
    void tearDownEach() {
        // Close connections
        if (multiDbConnection != null) {
            multiDbConnection.close();
        }
    }

    /**
     * Database switching with key distribution (SYNC mode).
     *
     * <p>
     * This test demonstrates the core MultiDbClient functionality:
     * <ol>
     * <li>Write keys 1-500 to endpoint1, verify each write immediately</li>
     * <li>Switch to endpoint2, write keys 501-1000, verify each write</li>
     * <li>Switch back to endpoint1, write keys 1001-1500, verify each write</li>
     * <li>Verify final key distribution using direct connections:
     * <ul>
     * <li>Endpoint1 should have keys 1-500 and 1001-1500</li>
     * <li>Endpoint2 should have keys 501-1000</li>
     * <li>Endpoint1 should NOT have keys 501-1000</li>
     * <li>Endpoint2 should NOT have keys 1-500 and 1001-1500</li>
     * </ul>
     * </li>
     * </ol>
     */
    @Test
    void testDatabaseSwitchingWithKeyDistribution() {
        RedisCommands<String, String> multiDbSync = multiDbConnection.sync();

        // Phase 1: Write keys 1-500 to endpoint1 (default endpoint)
        multiDbConnection.switchTo(endpoint1);
        assertThat(multiDbConnection.getCurrentEndpoint()).isEqualTo(endpoint1);

        for (int i = 1; i <= 500; i++) {
            String key = "key" + i;
            String value = String.valueOf(i);

            // Write and immediately verify
            String setResult = multiDbSync.set(key, value);
            assertThat(setResult).isEqualTo("OK");

            String readValue = multiDbSync.get(key);
            assertThat(readValue).isEqualTo(value);
        }

        // Phase 2: Switch to endpoint2 and write keys 501-1000
        multiDbConnection.switchTo(endpoint2);
        assertThat(multiDbConnection.getCurrentEndpoint()).isEqualTo(endpoint2);

        for (int i = 501; i <= 1000; i++) {
            String key = "key" + i;
            String value = String.valueOf(i);

            // Write and immediately verify
            String setResult = multiDbSync.set(key, value);
            assertThat(setResult).isEqualTo("OK");

            String readValue = multiDbSync.get(key);
            assertThat(readValue).isEqualTo(value);
        }

        // Phase 3: Switch back to endpoint1 and write keys 1001-1500
        multiDbConnection.switchTo(endpoint1);
        assertThat(multiDbConnection.getCurrentEndpoint()).isEqualTo(endpoint1);

        for (int i = 1001; i <= 1500; i++) {
            String key = "key" + i;
            String value = String.valueOf(i);

            // Write and immediately verify
            String setResult = multiDbSync.set(key, value);
            assertThat(setResult).isEqualTo("OK");

            String readValue = multiDbSync.get(key);
            assertThat(readValue).isEqualTo(value);
        }

        // Phase 4: Verify key distribution using direct connections
        RedisCommands<String, String> directSync1 = directConnection1.sync();
        RedisCommands<String, String> directSync2 = directConnection2.sync();

        // Verify endpoint1 has keys 1-500 with correct values
        for (int i = 1; i <= 500; i++) {
            String key = "key" + i;
            String expectedValue = String.valueOf(i);
            String actualValue = directSync1.get(key);
            assertThat(actualValue).as("Endpoint1 should have %s with value %s", key, expectedValue).isEqualTo(expectedValue);
        }

        // Verify endpoint1 does NOT have keys 501-1000
        for (int i = 501; i <= 1000; i++) {
            String key = "key" + i;
            String actualValue = directSync1.get(key);
            assertThat(actualValue).as("Endpoint1 should NOT have %s", key).isNull();
        }

        // Verify endpoint1 has keys 1001-1500 with correct values
        for (int i = 1001; i <= 1500; i++) {
            String key = "key" + i;
            String expectedValue = String.valueOf(i);
            String actualValue = directSync1.get(key);
            assertThat(actualValue).as("Endpoint1 should have %s with value %s", key, expectedValue).isEqualTo(expectedValue);
        }

        // Verify endpoint2 does NOT have keys 1-500
        for (int i = 1; i <= 500; i++) {
            String key = "key" + i;
            String actualValue = directSync2.get(key);
            assertThat(actualValue).as("Endpoint2 should NOT have %s", key).isNull();
        }

        // Verify endpoint2 has keys 501-1000 with correct values
        for (int i = 501; i <= 1000; i++) {
            String key = "key" + i;
            String expectedValue = String.valueOf(i);
            String actualValue = directSync2.get(key);
            assertThat(actualValue).as("Endpoint2 should have %s with value %s", key, expectedValue).isEqualTo(expectedValue);
        }

        // Verify endpoint2 does NOT have keys 1001-1500
        for (int i = 1001; i <= 1500; i++) {
            String key = "key" + i;
            String actualValue = directSync2.get(key);
            assertThat(actualValue).as("Endpoint2 should NOT have %s", key).isNull();
        }

        // Summary verification: count total keys in each database
        long endpoint1KeyCount = 0;
        long endpoint2KeyCount = 0;

        for (int i = 1; i <= 1500; i++) {
            String key = "key" + i;
            if (directSync1.exists(key) == 1) {
                endpoint1KeyCount++;
            }
            if (directSync2.exists(key) == 1) {
                endpoint2KeyCount++;
            }
        }

        assertThat(endpoint1KeyCount).as("Endpoint1 should have exactly 1000 keys (1-500 and 1001-1500)").isEqualTo(1000);
        assertThat(endpoint2KeyCount).as("Endpoint2 should have exactly 500 keys (501-1000)").isEqualTo(500);
    }

    /**
     * Database switching with key distribution (ASYNC mode).
     *
     * <p>
     * This test validates the MultiDbClient's ability to handle concurrent async operations across database switches by firing
     * all commands without waiting for results.
     *
     * <p>
     * Test phases:
     * <ol>
     * <li>Fire all write commands without waiting:
     * <ul>
     * <li>Write keys 1-500 to endpoint1</li>
     * <li>Switch to endpoint2, write keys 501-1000</li>
     * <li>Switch back to endpoint1, write keys 1001-1500</li>
     * </ul>
     * </li>
     * <li>Fire all read commands without waiting (verify writes completed)</li>
     * <li>Wait for all futures to complete</li>
     * <li>Verify key distribution using direct connections</li>
     * <li>Assert: read success rate &gt; 99%, correct endpoint placement &gt; 99%, missing keys &lt; 1%, wrong endpoint &lt;
     * 1%</li>
     * </ol>
     */
    @Test
    void testDatabaseSwitchingWithKeyDistributionAsync() throws Exception {
        RedisAsyncCommands<String, String> multiDbAsync = multiDbConnection.async();

        // Lists to track all futures
        List<RedisFuture<String>> writeFutures = new ArrayList<>();
        Map<String, Integer> keyToExpectedEndpoint = new HashMap<>();

        // Phase 1: Fire write commands for keys 1-500 to endpoint1 (NO WAITING)
        multiDbConnection.switchTo(endpoint1);
        assertThat(multiDbConnection.getCurrentEndpoint()).isEqualTo(endpoint1);

        for (int i = 1; i <= 500; i++) {
            String key = "key" + i;
            String value = String.valueOf(i);

            // Fire async write without waiting
            RedisFuture<String> setFuture = multiDbAsync.set(key, value);
            writeFutures.add(setFuture);
            keyToExpectedEndpoint.put(key, 1); // Expected on endpoint1
        }

        // Phase 2: Switch to endpoint2 and fire write commands for keys 501-1000 (NO WAITING)
        multiDbConnection.switchTo(endpoint2);
        assertThat(multiDbConnection.getCurrentEndpoint()).isEqualTo(endpoint2);

        for (int i = 501; i <= 1000; i++) {
            String key = "key" + i;
            String value = String.valueOf(i);

            // Fire async write without waiting
            RedisFuture<String> setFuture = multiDbAsync.set(key, value);
            writeFutures.add(setFuture);
            keyToExpectedEndpoint.put(key, 2); // Expected on endpoint2
        }

        // Phase 3: Switch back to endpoint1 and fire write commands for keys 1001-1500 (NO WAITING)
        multiDbConnection.switchTo(endpoint1);
        assertThat(multiDbConnection.getCurrentEndpoint()).isEqualTo(endpoint1);

        for (int i = 1001; i <= 1500; i++) {
            String key = "key" + i;
            String value = String.valueOf(i);

            // Fire async write without waiting
            RedisFuture<String> setFuture = multiDbAsync.set(key, value);
            writeFutures.add(setFuture);
            keyToExpectedEndpoint.put(key, 1); // Expected on endpoint1
        }

        // Wait for all writes to complete
        for (RedisFuture<String> future : writeFutures) {
            TestFutures.getOrTimeout(future);
        }

        // Phase 4: Fire read commands to verify writes (NO WAITING)
        Map<String, RedisFuture<String>> keyToReadFuture = new HashMap<>();

        // Read from endpoint1 (should have keys 1-500 and 1001-1500)
        multiDbConnection.switchTo(endpoint1);
        for (int i = 1; i <= 500; i++) {
            String key = "key" + i;
            RedisFuture<String> getFuture = multiDbAsync.get(key);
            keyToReadFuture.put(key, getFuture);
        }
        for (int i = 1001; i <= 1500; i++) {
            String key = "key" + i;
            RedisFuture<String> getFuture = multiDbAsync.get(key);
            keyToReadFuture.put(key, getFuture);
        }

        // Read from endpoint2 (should have keys 501-1000)
        multiDbConnection.switchTo(endpoint2);
        for (int i = 501; i <= 1000; i++) {
            String key = "key" + i;
            RedisFuture<String> getFuture = multiDbAsync.get(key);
            keyToReadFuture.put(key, getFuture);
        }

        // Wait for all reads to complete and collect results
        Map<String, String> readResults = new HashMap<>();
        for (Map.Entry<String, RedisFuture<String>> entry : keyToReadFuture.entrySet()) {
            String key = entry.getKey();
            try {
                String value = TestFutures.getOrTimeout(entry.getValue());
                readResults.put(key, value);
            } catch (Exception e) {
                readResults.put(key, null); // Read failed
            }
        }

        // Phase 5: Verify key distribution using direct connections and collect statistics
        RedisCommands<String, String> directSync1 = directConnection1.sync();
        RedisCommands<String, String> directSync2 = directConnection2.sync();

        int readSuccessCount = 0;
        int readFailureCount = 0;
        int missingKeysCount = 0;
        int wrongEndpointCount = 0;
        int correctEndpointCount = 0;

        List<String> missingKeys = new ArrayList<>();
        List<String> wrongEndpointKeys = new ArrayList<>();

        // Analyze each key
        for (int i = 1; i <= 1500; i++) {
            String key = "key" + i;
            String expectedValue = String.valueOf(i);
            int expectedEndpoint = keyToExpectedEndpoint.get(key);

            // Check read result
            String readValue = readResults.get(key);
            boolean readSuccess = expectedValue.equals(readValue);

            if (readSuccess) {
                readSuccessCount++;
            } else {
                readFailureCount++;
            }

            // Check actual location
            boolean onEndpoint1 = directSync1.exists(key) == 1;
            boolean onEndpoint2 = directSync2.exists(key) == 1;

            if (!onEndpoint1 && !onEndpoint2) {
                missingKeysCount++;
                missingKeys.add(key);
            } else if ((expectedEndpoint == 1 && onEndpoint1) || (expectedEndpoint == 2 && onEndpoint2)) {
                correctEndpointCount++;
            } else {
                wrongEndpointCount++;
                wrongEndpointKeys.add(String.format("%s (expected: endpoint%d, actual: endpoint%d)", key, expectedEndpoint,
                        onEndpoint1 ? 1 : 2));
            }
        }

        // Calculate percentages
        double readSuccessRate = readSuccessCount * 100.0 / 1500;
        double correctEndpointRate = correctEndpointCount * 100.0 / 1500;
        double missingKeysRate = missingKeysCount * 100.0 / 1500;
        double wrongEndpointRate = wrongEndpointCount * 100.0 / 1500;

        // Assert with reasonable thresholds for async operations
        assertThat(readSuccessRate)
                .as("Read success rate should be > 99%% (actual: %.2f%%, failures: %d)", readSuccessRate, readFailureCount)
                .isGreaterThan(99.0);

        assertThat(correctEndpointRate)
                .as("Correct endpoint placement rate should be > 99%% (actual: %.2f%%)", correctEndpointRate)
                .isGreaterThan(99.0);

        assertThat(missingKeysRate)
                .as("Missing keys rate should be < 1%% (actual: %.2f%%, missing: %s)", missingKeysRate, missingKeys)
                .isLessThan(1.0);

        assertThat(wrongEndpointRate)
                .as("Wrong endpoint rate should be < 1%% (actual: %.2f%%, wrong: %s)", wrongEndpointRate, wrongEndpointKeys)
                .isLessThan(1.0);

        // Verify total key count
        assertThat(readSuccessCount + readFailureCount).as("Should have attempted to read all 1500 keys").isEqualTo(1500);
        assertThat(correctEndpointCount + wrongEndpointCount + missingKeysCount)
                .as("All keys should be accounted for in endpoint distribution").isEqualTo(1500);
    }

    /**
     * Test PubSub functionality with MultiDbClient.
     *
     * <p>
     * This test validates PubSub message delivery across database switches:
     * <ol>
     * <li>Subscribes to a channel using MultiDbClient PubSub connection</li>
     * <li>Runs for 12 seconds, collecting all received messages</li>
     * <li>Simultaneously publishes messages to both databases using direct connections (async)</li>
     * <li>Publishes message pairs (one to each DB) every 20ms with incrementing messageId</li>
     * <li>Switches databases every 3 seconds (db1 -&gt; db2 -&gt; db1 -&gt; db2)</li>
     * <li>Message format: "messageId:X database:Y"</li>
     * <li>Asserts: messages received from both databases, reasonable message counts based on timing</li>
     * </ol>
     */
    @Test
    void testPubSubWithMultiDbClient() throws Exception {
        // Thread-safe list to collect messages
        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        AtomicInteger messageCount = new AtomicInteger(0);

        // Create PubSub connection with MultiDbClient
        StatefulRedisMultiDbPubSubConnection<String, String> multiDbPubSub = multiDbClient.connectPubSub();

        // Add listener to collect messages
        CountDownLatch latch = new CountDownLatch(1);
        multiDbPubSub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                receivedMessages.add(message);
                messageCount.incrementAndGet();
            }

            @Override
            public void subscribed(String channel, long count) {
                latch.countDown();
            }

        });

        // Subscribe to the test channel
        String testChannel = "test-channel";
        multiDbPubSub.sync().subscribe(testChannel);

        // Give subscription time to establish
        latch.await(1, TimeUnit.SECONDS);

        // Create direct PubSub connections for publishing
        StatefulRedisPubSubConnection<String, String> publisher1 = directClient1.connectPubSub();
        StatefulRedisPubSubConnection<String, String> publisher2 = directClient2.connectPubSub();

        // Publisher thread that sends messages to both databases using async methods
        AtomicBoolean publisherRunning = new AtomicBoolean(true);
        AtomicInteger publishedCount = new AtomicInteger(0);

        Thread publisherThread = new Thread(() -> {
            int messageId = 1;
            while (publisherRunning.get()) {
                try {
                    // Publish to database 1 (async - fire and forget)
                    String message1 = "messageId:" + messageId + " database:1";
                    publisher1.async().publish(testChannel, message1);

                    // Publish to database 2 (async - fire and forget)
                    String message2 = "messageId:" + messageId + " database:2";
                    publisher2.async().publish(testChannel, message2);

                    publishedCount.addAndGet(2);
                    messageId++;

                    // Wait 20ms before next pair
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Ignore errors and continue
                }
            }
        });

        // Start publisher thread
        publisherThread.start();

        // Start on endpoint1 (default) - 3 seconds
        Thread.sleep(3000);

        // Switch to endpoint2 - 3 seconds
        multiDbPubSub.switchTo(endpoint2);
        Thread.sleep(3000);

        // Switch back to endpoint1 - 3 seconds
        multiDbPubSub.switchTo(endpoint1);
        Thread.sleep(3000);

        // Switch to endpoint2 - 3 seconds
        multiDbPubSub.switchTo(endpoint2);
        Thread.sleep(3000);

        // Stop publisher
        publisherRunning.set(false);
        publisherThread.join(1000);

        // Give time for final messages to arrive
        Thread.sleep(500);

        // Close connections
        multiDbPubSub.close();
        publisher1.close();
        publisher2.close();

        // Analyze received messages
        int db1Count = 0;
        int db2Count = 0;
        Set<Integer> db1MessageIds = new TreeSet<>();
        Set<Integer> db2MessageIds = new TreeSet<>();

        for (String message : receivedMessages) {
            if (message.contains("database:1")) {
                db1Count++;
                // Extract messageId
                String[] parts = message.split(" ");
                if (parts.length >= 1) {
                    String idPart = parts[0].replace("messageId:", "");
                    try {
                        db1MessageIds.add(Integer.parseInt(idPart));
                    } catch (NumberFormatException e) {
                        // Ignore malformed messages
                    }
                }
            } else if (message.contains("database:2")) {
                db2Count++;
                // Extract messageId
                String[] parts = message.split(" ");
                if (parts.length >= 1) {
                    String idPart = parts[0].replace("messageId:", "");
                    try {
                        db2MessageIds.add(Integer.parseInt(idPart));
                    } catch (NumberFormatException e) {
                        // Ignore malformed messages
                    }
                }
            }
        }

        // Calculate expected message counts
        // Test runs for ~12 seconds, publishing every 20ms = ~50 messages/second
        // Expected: ~600 messages total (300 per database)
        // With 4 switches (2 periods per database), we expect messages from both databases
        int totalMessages = receivedMessages.size();
        int expectedMinMessages = 400; // Allow for timing variations
        int expectedMaxMessages = 800; // Allow for timing variations

        // Assert: Should receive messages
        assertThat(totalMessages).as("Should have received messages").isGreaterThan(expectedMinMessages);
        assertThat(totalMessages).as("Should not receive excessive messages").isLessThan(expectedMaxMessages);

        // Assert: Should receive messages from both databases
        assertThat(db1Count).as("Should have received messages from database 1").isGreaterThan(0);
        assertThat(db2Count).as("Should have received messages from database 2").isGreaterThan(0);

        // Assert: Message distribution should be reasonable (each database gets ~2 periods of 3 seconds)
        // Expected: roughly equal distribution, but allow for timing variations
        // Each database should get at least 20% of total messages (very conservative threshold)
        double db1Percentage = db1Count * 100.0 / totalMessages;
        double db2Percentage = db2Count * 100.0 / totalMessages;

        assertThat(db1Percentage).as("Database 1 should receive at least 20%% of messages (actual: %.1f%%)", db1Percentage)
                .isGreaterThan(20.0);
        assertThat(db2Percentage).as("Database 2 should receive at least 20%% of messages (actual: %.1f%%)", db2Percentage)
                .isGreaterThan(20.0);

        // Assert: Should have unique message IDs (no duplicates within each database)
        assertThat(db1MessageIds.size()).as("Database 1 should have unique message IDs").isEqualTo(db1Count);
        assertThat(db2MessageIds.size()).as("Database 2 should have unique message IDs").isEqualTo(db2Count);

        // Assert: Total count should match
        assertThat(db1Count + db2Count).as("Sum of db1 and db2 counts should equal total messages").isEqualTo(totalMessages);
    }

}
