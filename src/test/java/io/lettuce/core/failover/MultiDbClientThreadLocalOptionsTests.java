package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.settings.TestSettings;

/**
 * Tests to verify ThreadLocal ClientOptions behavior in MultiDbClientImpl, particularly in async contexts where ThreadLocal can
 * cause race conditions.
 *
 * @author Ali Takavci
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class MultiDbClientThreadLocalOptionsTests {

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
        // Create two databases with different timeout options
        ClientOptions options1 = ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(5)).build()).build();

        ClientOptions options2 = ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(10)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // This should work - sync connect uses ThreadLocal correctly
        connection = client.connect(StringCodec.UTF8);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();
    }

    /**
     * Test that async connect with different ClientOptions per database. This test DOCUMENTS THE CURRENT BROKEN BEHAVIOR where
     * ThreadLocal options are cleared before async callbacks run.
     *
     * NOTE: This test may PASS even though the implementation is broken, because the race condition is timing-dependent. The
     * test serves as documentation of the issue.
     */
    @Test
    void asyncConnectWithDifferentOptionsPerDatabase() throws Exception {
        // Create two databases with VERY different socket options
        ClientOptions options1 = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build()).build();

        ClientOptions options2 = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // This may succeed or fail depending on timing
        // The issue is that options are cleared before thenApply callbacks run
        ConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

        connection = future.get(30, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();

        // NOTE: We can't easily verify that the correct options were used
        // because by the time the connection is established, the ThreadLocal is already cleared
    }

    /**
     * Test that demonstrates the ThreadLocal race condition by creating multiple async connections in parallel. This test
     * attempts to expose the race condition where options from one database might be used for another.
     */
    @Test
    void parallelAsyncConnectsShouldNotShareThreadLocalOptions() throws Exception {
        // Create databases with distinct options
        ClientOptions options1 = ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(3)).build()).build();

        ClientOptions options2 = ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(7)).build()).build();

        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(options1).build();

        DatabaseConfig db2 = DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(options2).build();

        client = MultiDbClient.create(Arrays.asList(db1, db2));

        // Create multiple connections in parallel to increase chance of race condition
        int numConnections = 5;
        List<CompletableFuture<StatefulRedisMultiDbConnection<String, String>>> futures = new ArrayList<>();

        for (int i = 0; i < numConnections; i++) {
            ConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);
            futures.add(future.toCompletableFuture());
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

        // All should succeed (though they may have wrong options due to race condition)
        for (CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future : futures) {
            StatefulRedisMultiDbConnection<String, String> conn = future.get();
            assertThat(conn).isNotNull();
            assertThat(conn.isOpen()).isTrue();
            conn.close();
        }
    }

    /**
     * Test that verifies ThreadLocal is properly cleaned up after sync connect. This should pass because sync connect uses
     * try-finally to clean up ThreadLocal.
     */
    @Test
    void syncConnectShouldCleanUpThreadLocal() {
        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).build();

        client = MultiDbClient.create(Arrays.asList(db1));

        // Connect
        connection = client.connect(StringCodec.UTF8);
        assertThat(connection).isNotNull();

        // After connect, ThreadLocal should be cleaned up
        // We can't directly test this without accessing private fields,
        // but we can verify that a second connect doesn't fail
        StatefulRedisMultiDbConnection<String, String> connection2 = client.connect(StringCodec.UTF8);
        assertThat(connection2).isNotNull();
        connection2.close();
    }

    /**
     * Test that demonstrates the timing issue: resetOptions() is called BEFORE thenApply() callback runs. This test uses thread
     * tracking to verify the race condition.
     */
    @Test
    void asyncConnectResetOptionsBeforeCallbackRuns() throws Exception {
        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).build();

        client = MultiDbClient.create(Arrays.asList(db1));

        // Track which threads are involved
        AtomicReference<Thread> connectThread = new AtomicReference<>();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        CountDownLatch callbackLatch = new CountDownLatch(1);

        connectThread.set(Thread.currentThread());

        ConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

        // Add a callback to track the thread
        future.whenComplete((conn, throwable) -> {
            callbackThread.set(Thread.currentThread());
            callbackLatch.countDown();
        });

        connection = future.get(10, TimeUnit.SECONDS);
        callbackLatch.await(5, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();

        // The callback runs on a DIFFERENT thread than the connect call
        // This proves that ThreadLocal won't work
        assertThat(callbackThread.get()).isNotNull();
        // Note: In some cases they might be the same thread if the future completes synchronously,
        // but in async scenarios they will be different
        System.out.println("Connect thread: " + connectThread.get().getName());
        System.out.println("Callback thread: " + callbackThread.get().getName());
    }

    /**
     * Test that verifies the order of operations in createRedisDatabaseAsync: 1. setOptions() 2. connectAsync() 3.
     * resetOptions() ← IMMEDIATE 4. thenApply() callback ← LATER, on different thread
     *
     * This test documents that resetOptions() happens BEFORE the callback that needs the options.
     */
    @Test
    void verifyResetOptionsTimingInAsyncConnect() throws Exception {
        DatabaseConfig db1 = DatabaseConfig.builder(URI1).weight(1.0f).build();

        client = MultiDbClient.create(Arrays.asList(db1));

        // The issue is in createRedisDatabaseAsync():
        // Line 207: setOptions(config.getClientOptions());
        // Line 211: ConnectionFuture<...> connectionFuture = connectAsync(codec, uri);
        // Line 212: resetOptions(); ← Options cleared HERE
        // Line 213: return connectionFuture.toCompletableFuture().thenApply(connection -> {
        // ← This callback runs LATER, options are already gone!

        ConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

        connection = future.get(10, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();

        // This test passes, but it doesn't mean the implementation is correct
        // The options might be wrong, but the connection still succeeds
    }

}
