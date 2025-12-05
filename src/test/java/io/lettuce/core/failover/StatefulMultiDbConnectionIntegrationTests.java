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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.TestFutures;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for {@link StatefulRedisMultiDbConnection} with basic commands and database switching.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class StatefulMultiDbConnectionIntegrationTests extends MultiDbTestSupport {

    @Inject
    StatefulMultiDbConnectionIntegrationTests(MultiDbClient client) {
        super(client);
    }

    @BeforeEach
    void setUp() {
        directClient1.connect().sync().flushall();
        directClient2.connect().sync().flushall();
    }

    @AfterEach
    void tearDownAfter() {
        directClient1.shutdown();
        directClient2.shutdown();
    }

    // ============ Basic Connection Tests ============

    @Test
    void shouldConnectToMultipleEndpoints() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        assertNotNull(connection);
        assertThat(connection.getEndpoints()).isNotNull();
        connection.close();
    }

    @Test
    void shouldGetCurrentEndpoint() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        assertNotNull(currentEndpoint);
        assertThat(currentEndpoint).isIn(connection.getEndpoints());
        connection.close();
    }

    @Test
    void shouldListAllEndpoints() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        Iterable<RedisURI> endpoints = connection.getEndpoints();
        assertThat(endpoints).isNotNull();
        assertThat(StreamSupport.stream(endpoints.spliterator(), false).count()).isGreaterThanOrEqualTo(2);
        connection.close();
    }

    // ============ Basic Command Tests (Sync) ============

    @Test
    void shouldSetAndGetValueSync() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.sync().set("testKey", "testValue");
        String value = connection.sync().get("testKey");
        assertEquals("testValue", value);
        connection.close();
    }

    @Test
    void shouldDeleteKeySync() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.sync().set("deleteKey", "value");
        Long deleted = connection.sync().del("deleteKey");
        assertEquals(1L, deleted);
        String value = connection.sync().get("deleteKey");
        assertNull(value);
        connection.close();
    }

    @Test
    void shouldIncrementValueSync() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.sync().set("counter", "10");
        Long result = connection.sync().incr("counter");
        assertEquals(11L, result);
        connection.close();
    }

    @Test
    void shouldAppendToStringSync() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.sync().set("mykey", "Hello");
        Long length = connection.sync().append("mykey", " World");
        assertEquals(11L, length);
        String value = connection.sync().get("mykey");
        assertEquals("Hello World", value);
        connection.close();
    }

    // ============ Basic Command Tests (Async) ============

    @Test
    void shouldSetAndGetValueAsync() throws Exception {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisFuture<String> setFuture = connection.async().set("asyncKey", "asyncValue");
        TestFutures.awaitOrTimeout(setFuture);
        RedisFuture<String> getFuture = connection.async().get("asyncKey");
        TestFutures.awaitOrTimeout(getFuture);
        assertEquals("asyncValue", getFuture.get());
        connection.close();
    }

    @Test
    void shouldDeleteKeyAsync() throws Exception {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.async().set("asyncDeleteKey", "value");
        RedisFuture<Long> deleteFuture = connection.async().del("asyncDeleteKey");
        TestFutures.awaitOrTimeout(deleteFuture);
        assertEquals(1L, deleteFuture.get());
        connection.close();
    }

    @Test
    void shouldIncrementValueAsync() throws Exception {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.async().set("asyncCounter", "5");
        RedisFuture<Long> incrFuture = connection.async().incr("asyncCounter");
        TestFutures.awaitOrTimeout(incrFuture);
        assertEquals(6L, incrFuture.get());
        connection.close();
    }

    // ============ Database Switching Tests ============

    @Test
    void shouldSwitchBetweenDatabases() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Set value in first database
        connection.sync().set("switchKey", "value1");
        assertEquals("value1", connection.sync().get("switchKey"));

        // Switch to second database
        RedisURI other = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(connection.getCurrentEndpoint())).findFirst().get();
        connection.switchToDatabase(other);

        // Value should not exist in second database
        assertNull(connection.sync().get("switchKey"));

        // Set different value in second database
        connection.sync().set("switchKey", "value2");
        assertEquals("value2", connection.sync().get("switchKey"));

        connection.close();
    }

    @Test
    void shouldMaintainDataAfterSwitch() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Set value in first database
        connection.sync().set("persistKey", "persistValue");
        RedisURI firstDb = connection.getCurrentEndpoint();

        // Switch to second database
        RedisURI secondDb = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(firstDb)).findFirst().get();
        connection.switchToDatabase(secondDb);

        // Switch back to first database
        connection.switchToDatabase(firstDb);

        // Original value should still exist
        assertEquals("persistValue", connection.sync().get("persistKey"));

        connection.close();
    }

    @Test
    void shouldSwitchAndExecuteCommandsAsync() throws Exception {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Set value in first database
        RedisFuture<String> setFuture1 = connection.async().set("asyncSwitchKey", "asyncValue1");
        TestFutures.awaitOrTimeout(setFuture1);

        // Switch to second database
        RedisURI other = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(connection.getCurrentEndpoint())).findFirst().get();
        connection.switchToDatabase(other);

        // Set different value in second database
        RedisFuture<String> setFuture2 = connection.async().set("asyncSwitchKey", "asyncValue2");
        TestFutures.awaitOrTimeout(setFuture2);

        // Get value from second database
        RedisFuture<String> getFuture = connection.async().get("asyncSwitchKey");
        TestFutures.awaitOrTimeout(getFuture);
        assertEquals("asyncValue2", getFuture.get());

        connection.close();
    }

    @Test
    void shouldHandleMultipleSwitches() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisURI firstDb = connection.getCurrentEndpoint();
        RedisURI secondDb = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(firstDb)).findFirst().get();

        // First database
        connection.sync().set("key", "value1");
        assertEquals("value1", connection.sync().get("key"));

        // Switch to second
        connection.switchToDatabase(secondDb);
        connection.sync().set("key", "value2");
        assertEquals("value2", connection.sync().get("key"));

        // Switch back to first
        connection.switchToDatabase(firstDb);
        assertEquals("value1", connection.sync().get("key"));

        // Switch to second again
        connection.switchToDatabase(secondDb);
        assertEquals("value2", connection.sync().get("key"));

        connection.close();
    }

    @Test
    void shouldHandleSwitchToSameDatabase() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisURI currentDb = connection.getCurrentEndpoint();

        // Set a value
        connection.sync().set("sameDbKey", "testValue");
        assertEquals("testValue", connection.sync().get("sameDbKey"));

        // Switch to the same database (should be a no-op)
        connection.switchToDatabase(currentDb);

        // Verify we're still on the same database and value is intact
        assertEquals(currentDb, connection.getCurrentEndpoint());
        assertEquals("testValue", connection.sync().get("sameDbKey"));

        // Verify commands still work after no-op switch
        connection.sync().set("anotherKey", "anotherValue");
        assertEquals("anotherValue", connection.sync().get("anotherKey"));

        connection.close();
    }

    // ============ List Operations Tests ============

    @Test
    void shouldPushAndPopFromList() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.sync().rpush("mylist", "a", "b", "c");
        String value = connection.sync().lpop("mylist");
        assertEquals("a", value);
        connection.close();
    }

    @Test
    void shouldGetListLength() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        connection.sync().rpush("listlen", "one", "two", "three");
        Long length = connection.sync().llen("listlen");
        assertEquals(3L, length);
        connection.close();
    }

    // ========== Dynamic Database Management Tests ==========

    @Test
    void shouldAddDatabase() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Get initial endpoint count
        List<RedisURI> initialEndpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());
        int initialCount = initialEndpoints.size();

        // Add a new database (using port(2) which should be available in test environment)
        RedisURI newUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(6))
                .withPassword(TestSettings.password()).build();
        connection.addDatabase(newUri, 1.0f);

        // Verify it was added
        List<RedisURI> updatedEndpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());

        assertThat(updatedEndpoints).hasSize(initialCount + 1);
        assertThat(updatedEndpoints).contains(newUri);

        connection.close();
    }

    @Test
    void shouldRejectAddingDuplicateDatabase() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Get an existing endpoint
        RedisURI existingUri = connection.getCurrentEndpoint();

        // Try to add it again - should fail
        assertThatThrownBy(() -> connection.addDatabase(existingUri, 1.0f)).isInstanceOf(IllegalArgumentException.class);

        connection.close();
    }

    @Test
    void shouldRemoveDatabase() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Add a new database
        RedisURI newUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(6))
                .withPassword(TestSettings.password()).build();
        connection.addDatabase(newUri, 1.0f);

        // Verify it was added
        List<RedisURI> endpointsAfterAdd = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());
        assertThat(endpointsAfterAdd).contains(newUri);

        // Remove it
        connection.removeDatabase(newUri);

        // Verify it was removed
        List<RedisURI> endpointsAfterRemove = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());
        assertThat(endpointsAfterRemove).doesNotContain(newUri);

        connection.close();
    }

    @Test
    void shouldRejectRemovingNonExistentDatabase() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Try to remove a database that doesn't exist
        RedisURI nonExistentUri = RedisURI.create("redis://localhost:9999");

        assertThatThrownBy(() -> connection.removeDatabase(nonExistentUri)).isInstanceOf(IllegalArgumentException.class);

        connection.close();
    }

    @Test
    void shouldRejectRemovingLastDatabase() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Get all endpoints
        List<RedisURI> endpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());

        // If we have more than one endpoint, remove all but one
        if (endpoints.size() > 1) {
            for (int i = 0; i < endpoints.size() - 1; i++) {
                RedisURI endpoint = endpoints.get(i);
                // Switch away from this endpoint before removing it
                if (endpoint.equals(connection.getCurrentEndpoint())) {
                    connection.switchToDatabase(endpoints.get(endpoints.size() - 1));
                }
                connection.removeDatabase(endpoint);
            }
        }

        // Now we should have exactly one endpoint left
        List<RedisURI> remainingEndpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());
        assertThat(remainingEndpoints).hasSize(1);

        // Try to remove the last one - should fail
        RedisURI lastEndpoint = remainingEndpoints.get(0);

        assertThatThrownBy(() -> connection.removeDatabase(lastEndpoint)).isInstanceOf(UnsupportedOperationException.class);

        connection.close();
    }

    @Test
    void shouldAddDatabaseAndSwitchToIt() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Add a new database
        RedisURI newUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(6))
                .withPassword(TestSettings.password()).build();
        connection.addDatabase(newUri, 1.0f);

        // Switch to it
        connection.switchToDatabase(newUri);

        // Verify it's now active
        assertThat(connection.getCurrentEndpoint()).isEqualTo(newUri);

        // Verify we can execute commands on it
        connection.sync().set("test-key", "test-value");
        assertThat(connection.sync().get("test-key")).isEqualTo("test-value");

        connection.close();
    }

    @Test
    void shouldRejectRemovingActiveDatabase() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Get the current active endpoint
        RedisURI activeEndpoint = connection.getCurrentEndpoint();

        // Try to remove it - should fail
        assertThatThrownBy(() -> connection.removeDatabase(activeEndpoint)).isInstanceOf(UnsupportedOperationException.class);

        connection.close();
    }

    @Test
    void shouldRejectSwitchingToNonExistentEndpoint() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        // Create a URI that's not in the configured endpoints
        RedisURI nonExistentUri = RedisURI.create("redis://localhost:9999");

        // Note: Current implementation throws UnsupportedOperationException for non-existent endpoints
        assertThatThrownBy(() -> connection.switchToDatabase(nonExistentUri)).isInstanceOf(UnsupportedOperationException.class);

        connection.close();
    }

    // ========== Add/Remove Thread Safety Tests =========

    @Test
    void shouldHandleConcurrentAddsAndRemovesOnMultipleUris() throws Exception {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        int initialEndpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false).collect(Collectors.toList())
                .size();
        // Create multiple URIs
        RedisURI[] uris = new RedisURI[5];
        AtomicInteger[] addCounts = new AtomicInteger[5];
        AtomicInteger[] removeCount = new AtomicInteger[5];
        for (int i = 0; i < 5; i++) {
            uris[i] = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(6 + i))
                    .withPassword(TestSettings.password()).build();
            addCounts[i] = new AtomicInteger(0);
            removeCount[i] = new AtomicInteger(0);
        }

        int threadCount = 20;
        Thread[] threads = new Thread[threadCount];

        // Half threads add URIs, half remove URIs
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                int uriIndex = index % uris.length;
                if (index % 2 == 0) {
                    // Add operation
                    RedisURI uri = uris[uriIndex];
                    try {
                        connection.addDatabase(uri, 1.0f);
                        addCounts[uriIndex].incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        // Expected: "Database already exists"
                    }
                } else {
                    // Remove operation
                    RedisURI uri = uris[uriIndex];
                    try {
                        // Switch away if it's current
                        if (connection.getCurrentEndpoint().equals(uri)) {
                            RedisURI other = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                                    .filter(u -> !u.equals(uri)).findFirst().orElse(null);
                            if (other != null) {
                                connection.switchToDatabase(other);
                            }
                        }
                        connection.removeDatabase(uri);
                        removeCount[uriIndex].incrementAndGet();
                    } catch (IllegalArgumentException | UnsupportedOperationException e) {
                        // Expected: "Database not found" or "Cannot remove active database"
                    }
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        // Verify: Each URI should appear at most once (no duplicates)
        List<RedisURI> endpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());
        for (RedisURI uri : uris) {
            long count = endpoints.stream().filter(u -> u.equals(uri)).count();
            assertThat(count).isLessThanOrEqualTo(1);
        }

        // Verify: Add and remove counts match
        int totalRemaniningEndpoints = initialEndpoints;
        for (int i = 0; i < uris.length; i++) {
            int netCount = addCounts[i].get() - removeCount[i].get();
            if (netCount == 1) {
                assertThat(endpoints).contains(uris[i]);
                totalRemaniningEndpoints++;
            } else {
                assertThat(endpoints).doesNotContain(uris[i]);
            }
        }
        // Verify: Total remaining endpoints match
        assertThat(endpoints).hasSize(totalRemaniningEndpoints);

        // Verify connection is still functional
        connection.sync().set("thread-safety-test", "success");
        assertThat(connection.sync().get("thread-safety-test")).isEqualTo("success");

        connection.close();
    }

}
