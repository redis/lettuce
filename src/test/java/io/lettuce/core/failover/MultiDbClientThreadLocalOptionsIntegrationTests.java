package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.settings.TestSettings;

/**
 * Tests to verify ThreadLocal ClientOptions behavior in MultiDbClientImpl, particularly in async contexts where ThreadLocal can
 * cause race conditions.
 *
 * @author Ali Takavci
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class MultiDbClientThreadLocalOptionsIntegrationTests {

    private static final RedisURI URI1 = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();

    private static final RedisURI URI2 = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port() + 1).build();

    private MultiDbClient client;

    private StatefulRedisMultiDbConnection<String, String> connection;

    @BeforeEach
    void setUp() {
        // Will be created per test with specific options
    }

    @AfterEach
    void tearDown() {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * Test that different databases can have different ClientOptions in sync connect. This should work because sync connect
     * uses ThreadLocal correctly (set -> use -> reset on same thread).
     */
    @Test
    void syncConnectShouldRespectPerDatabaseOptions() {
        // Create two databases with DISTINCT options that we can verify
        ClientOptions options1 = ClientOptions.builder().autoReconnect(true)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build()).build();

        ClientOptions options2 = ClientOptions.builder().autoReconnect(false)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // This should work - sync connect uses ThreadLocal correctly
        connection = client.connect(StringCodec.UTF8);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();

        // Verify that each database connection has the correct options
        StatefulRedisConnection<?, ?> conn1 = ((RedisDatabaseImpl<?>) connection.getDatabase(URI1)).getConnection();
        StatefulRedisConnection<?, ?> conn2 = ((RedisDatabaseImpl<?>) connection.getDatabase(URI2)).getConnection();

        // Verify database 1 has options1
        assertThat(conn1.getOptions().isAutoReconnect()).isTrue();
        assertThat(conn1.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));

        // Verify database 2 has options2
        assertThat(conn2.getOptions().isAutoReconnect()).isFalse();
        assertThat(conn2.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    /**
     * Test that async connect with different ClientOptions per database properly applies options to each database connection.
     */
    @Test
    void asyncConnectWithDifferentOptionsPerDatabase() throws Exception {
        // Create two databases with DISTINCT options that we can verify
        ClientOptions options1 = ClientOptions.builder().autoReconnect(true)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build()).build();

        ClientOptions options2 = ClientOptions.builder().autoReconnect(false)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // Connect asynchronously
        MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

        connection = future.get(10, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();

        // Verify that each database connection has the correct options
        StatefulRedisConnection<?, ?> conn1 = ((RedisDatabaseImpl<?>) connection.getDatabase(URI1)).getConnection();
        StatefulRedisConnection<?, ?> conn2 = ((RedisDatabaseImpl<?>) connection.getDatabase(URI2)).getConnection();

        // Verify database 1 has options1
        assertThat(conn1.getOptions().isAutoReconnect()).isTrue();
        assertThat(conn1.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));

        // Verify database 2 has options2
        assertThat(conn2.getOptions().isAutoReconnect()).isFalse();
        assertThat(conn2.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    /**
     * Test that verifies multiple parallel async connections each get the correct per-database options. This test ensures that
     * ThreadLocal options don't leak between different connection attempts.
     */
    @Test
    void parallelAsyncConnectsShouldNotShareThreadLocalOptions() throws Exception {
        // Create databases with distinct options
        ClientOptions options1 = ClientOptions.builder().autoReconnect(true)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(3)).build()).build();

        ClientOptions options2 = ClientOptions.builder().autoReconnect(false)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(7)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // Create multiple connections in parallel to increase chance of race condition if it exists
        int numConnections = 5;
        List<CompletableFuture<StatefulRedisMultiDbConnection<String, String>>> futures = new ArrayList<>();

        for (int i = 0; i < numConnections; i++) {
            MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client
                    .connectAsync(StringCodec.UTF8);

            futures.add(future.toCompletableFuture());
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        // Verify all connections have correct per-database options
        for (CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future : futures) {
            StatefulRedisMultiDbConnection<String, String> conn = future.get();
            assertThat(conn).isNotNull();
            assertThat(conn.isOpen()).isTrue();

            // Verify that each database connection has the correct options
            StatefulRedisConnection<?, ?> conn1 = ((RedisDatabaseImpl<?>) conn.getDatabase(URI1)).getConnection();
            StatefulRedisConnection<?, ?> conn2 = ((RedisDatabaseImpl<?>) conn.getDatabase(URI2)).getConnection();

            // Verify database 1 has options1 (autoReconnect=true, connectTimeout=3s)
            assertThat(conn1.getOptions().isAutoReconnect()).isTrue();
            assertThat(conn1.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));

            // Verify database 2 has options2 (autoReconnect=false, connectTimeout=7s)
            assertThat(conn2.getOptions().isAutoReconnect()).isFalse();
            assertThat(conn2.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(7));

            conn.close();
        }
    }

    private static class OperationInfo {

        @SuppressWarnings("unused")
        final String operation;

        final Thread thread;

        final ClientOptions options;

        public OperationInfo(String operation, Thread thread, ClientOptions options) {
            this.operation = operation;
            this.thread = thread;
            this.options = options;
        }

    }

    private static class ThreadLocalWrapper extends ThreadLocal<ClientOptions> {

        ConcurrentLinkedQueue<OperationInfo> operations = new ConcurrentLinkedQueue<>();

        @Override
        public void remove() {
            operations.add(new OperationInfo("remove", Thread.currentThread(), super.get()));
            super.remove();
        }

        @Override
        public void set(ClientOptions value) {
            operations.add(new OperationInfo("set", Thread.currentThread(), value));
            super.set(value);
        }

    }

    /**
     * Test that verifies ThreadLocal is properly cleaned up after sync connect. This should pass because sync connect uses
     * try-finally to clean up ThreadLocal.
     */
    @Test
    void syncConnectShouldCleanUpThreadLocal() {
        // Create databases with distinct options
        ClientOptions options1 = ClientOptions.builder().autoReconnect(true)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(3)).build()).build();

        ClientOptions options2 = ClientOptions.builder().autoReconnect(false)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(7)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1)
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2)
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // Wrap ThreadLocal to track set/remove operations
        ThreadLocalWrapper wrapper = new ThreadLocalWrapper();

        ReflectionTestUtils.setField(client, "localClientOptions", wrapper);

        // Connect
        connection = client.connect(StringCodec.UTF8);
        assertThat(connection).isNotNull();

        StatefulRedisMultiDbConnection<String, String> connection2 = client.connect(StringCodec.UTF8);
        assertThat(connection2).isNotNull();
        connection2.close();

        // there should be one set and one corresponding remove for each database in connectAsync
        assertThat(wrapper.operations).hasSize(8);
        assertThat(wrapper.operations).extracting("operation").containsExactly("set", "remove", "set", "remove", "set",
                "remove", "set", "remove");
        // it contains 4 sets with non null options object and the exact same values should show up in 4 removes with
        // exact same threads and options instances.
        // so each pair received from queue should be matching with threads and options objects
        for (int i = 0; i < 4; i++) {
            OperationInfo set = wrapper.operations.poll();
            OperationInfo remove = wrapper.operations.poll();
            assertThat(set.options).isNotNull();
            assertThat(set.thread).isEqualTo(remove.thread);
            assertThat(set.options).isSameAs(remove.options);
        }

    }

    /**
     * Test that verifies ThreadLocal set/reset pairs are completed synchronously on the calling thread when connectAsync()
     * returns. This test verifies: 1) For multiple databases, each gets its own set/reset pair on the calling thread 2) All
     * pairs are completed before connectAsync() returns 3) Despite immediate reset, correct options are applied to each
     * database
     */
    @Test
    void asyncConnectShouldCompleteThreadLocalOpsOnCallingThread() throws Exception {
        // Create two databases with distinct options
        ClientOptions options1 = ClientOptions.builder().autoReconnect(true)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(8)).build()).build();

        ClientOptions options2 = ClientOptions.builder().autoReconnect(false)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(12)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1)
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2)
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // Track ThreadLocal operations
        ThreadLocalWrapper wrapper = new ThreadLocalWrapper();

        ReflectionTestUtils.setField(client, "localClientOptions", wrapper);

        MultiDbConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

        // Once connectAsync() returns, all set/reset pairs should be complete
        assertThat(wrapper.operations).hasSize(4); // 2 sets and 2 removes
        assertThat(wrapper.operations).extracting("operation").contains("set", "remove", "set", "remove");

        // Verify all ThreadLocal operations happened on the calling thread
        for (OperationInfo op : wrapper.operations) {
            assertThat(op.thread).isEqualTo(Thread.currentThread());
        }

        connection = future.get(10, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();

        MultiDbTestSupport.waitForEndpoints(connection, 2, 2);

        // Verify that the connection has the correct options despite resetOptions() being called immediately
        StatefulRedisConnection<?, ?> conn1 = ((RedisDatabaseImpl<?>) connection.getDatabase(URI1)).getConnection();
        StatefulRedisConnection<?, ?> conn2 = ((RedisDatabaseImpl<?>) connection.getDatabase(URI2)).getConnection();

        assertThat(conn1.getOptions().isAutoReconnect()).isTrue();
        assertThat(conn1.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(8));

        assertThat(conn2.getOptions().isAutoReconnect()).isFalse();
        assertThat(conn2.getOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(12));
    }

}
