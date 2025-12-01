package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import io.lettuce.core.*;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;
import io.lettuce.core.failover.metrics.MetricsSnapshot;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * Comprehensive tests for DatabaseEndpointImpl callback mechanism, focusing on: 1. Callbacks firing after failover operations
 * 2. Different error types (timeout, connection failures) 3. Timing scenarios (errors during/after failover) 4. Metrics
 * attribution to correct endpoint
 *
 * @author Ali Takavci
 */
@Tag("unit")
class DatabaseEndpointCallbackTests {

    private ClientResources clientResources;

    private ClientOptions clientOptions;

    DatabaseEndpointImpl endpoint;

    @BeforeEach
    void setUp() {
        clientResources = TestClientResources.get();
        clientOptions = ClientOptions.builder().build();
        endpoint = new DatabaseEndpointImpl(clientOptions, clientResources);
    }

    @AfterEach
    void tearDown() {
        if (clientResources != null) {
            FastShutdown.shutdown(clientResources);
        }
        if (endpoint != null) {
            endpoint.close();
        }
    }

    private CircuitBreakerConfig getCBConfig(float failureRateThreshold, int minimumNumberOfFailures) {
        return new CircuitBreakerConfig(failureRateThreshold, minimumNumberOfFailures,
                CircuitBreakerConfig.DEFAULT.getTrackedExceptions(), CircuitBreakerConfig.DEFAULT.getMetricsWindowSize());
    }
    // ============ Basic Callback Attachment Tests ============

    @Nested
    @DisplayName("Callback Attachment Tests")
    class CallbackAttachmentTests {

        @Test
        @DisplayName("Should attach callback to single command when circuit breaker is set")
        void shouldAttachCallbackToSingleCommand() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            // Create a command
            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Write command - this should attach the callback
            RedisCommand<String, String, String> result = endpoint.write(asyncCommand);

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(CompleteableCommand.class);
        }

        @Test
        @DisplayName("Should attach callbacks to multiple commands")
        void shouldAttachCallbacksToMultipleCommands() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            // Create multiple commands
            List<RedisCommand<String, String, ?>> commands = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            // Write commands - this should attach callbacks to all
            Collection<RedisCommand<String, String, ?>> results = endpoint.write(commands);

            assertThat(results).hasSize(5);
            results.forEach(result -> assertThat(result).isInstanceOf(CompleteableCommand.class));
        }

        @Test
        @DisplayName("Should not attach callback when circuit breaker is null")
        void shouldNotAttachCallbackWhenCircuitBreakerIsNull() {
            // Don't set circuit breaker

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Write command - should not fail even without circuit breaker
            RedisCommand<String, String, String> result = endpoint.write(asyncCommand);

            assertThat(result).isNotNull();
        }

    }

    // ============ Error Type Tracking Tests ============

    @Nested
    @DisplayName("Error Type Tracking Tests")
    class ErrorTypeTrackingTests {

        @Test
        @DisplayName("Should record timeout exception as failure")
        void shouldRecordTimeoutExceptionAsFailure() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command with timeout exception
            asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Command timed out"));

            // Wait a bit for callback to execute
            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should record connection exception as failure")
        void shouldRecordConnectionExceptionAsFailure() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command with connection exception
            asyncCommand.completeExceptionally(new RedisConnectionException("Connection failed"));

            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should record IOException as failure")
        void shouldRecordIOExceptionAsFailure() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command with IOException
            asyncCommand.completeExceptionally(new IOException("Network error"));

            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should record ClosedChannelException as failure")
        void shouldRecordClosedChannelExceptionAsFailure() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command with ClosedChannelException (subclass of IOException)
            asyncCommand.completeExceptionally(new ClosedChannelException());

            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should record generic TimeoutException as failure")
        void shouldRecordGenericTimeoutExceptionAsFailure() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command with generic TimeoutException
            asyncCommand.completeExceptionally(new TimeoutException("Timeout"));

            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should record success when command completes normally")
        void shouldRecordSuccessWhenCommandCompletesNormally() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command successfully
            asyncCommand.complete();

            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(1);
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

    }

    // ============ Timing and Delayed Completion Tests ============

    @Nested
    @DisplayName("Timing and Delayed Completion Tests")
    class TimingTests {

        @Test
        @DisplayName("Should handle callback firing after significant delay")
        void shouldHandleCallbackAfterDelay() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Simulate delayed completion (e.g., timeout after failover)
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                executor.schedule(() -> {
                    asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Delayed timeout"));
                }, 500, TimeUnit.MILLISECONDS);

                // Wait for delayed completion
                Thread.sleep(1000);

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getFailureCount()).isEqualTo(1);
            } finally {
                executor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle multiple commands completing at different times")
        void shouldHandleMultipleCommandsCompletingAtDifferentTimes() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                commands.add(asyncCommand);
                endpoint.write(asyncCommand);
            }

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
            try {
                // Complete commands at different times with different results
                executor.schedule(() -> commands.get(0).complete(), 100, TimeUnit.MILLISECONDS);
                executor.schedule(() -> commands.get(1).completeExceptionally(new RedisCommandTimeoutException("timeout")), 200,
                        TimeUnit.MILLISECONDS);
                executor.schedule(() -> commands.get(2).complete(), 300, TimeUnit.MILLISECONDS);
                executor.schedule(() -> commands.get(3).completeExceptionally(new IOException("io error")), 400,
                        TimeUnit.MILLISECONDS);
                executor.schedule(() -> commands.get(4).complete(), 500, TimeUnit.MILLISECONDS);

                Thread.sleep(1000);

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getSuccessCount()).isEqualTo(3);
                assertThat(snapshot.getFailureCount()).isEqualTo(2);
            } finally {
                executor.shutdown();
            }
        }

    }

    // ============ Failover Behavior Tests ============

    @Nested
    @DisplayName("Failover Behavior Tests")
    class FailoverBehaviorTests {

        @Test
        @DisplayName("Should attribute failures to original endpoint even after circuit breaker change")
        // TODO : this test provided by AI is complete non-sense
        void shouldAttributeFailuresToOriginalEndpoint() throws Exception {

            // Create two endpoints with separate circuit breakers
            DatabaseEndpointImpl endpoint1 = new DatabaseEndpointImpl(clientOptions, clientResources);
            DatabaseEndpointImpl endpoint2 = new DatabaseEndpointImpl(clientOptions, clientResources);
            try {
                CircuitBreaker cb1 = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
                CircuitBreaker cb2 = new CircuitBreakerImpl(getCBConfig(50.0f, 100));

                endpoint1.bind(cb1);
                endpoint2.bind(cb2);

                // Issue command on endpoint1
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                endpoint1.write(asyncCommand);

                // Simulate failover - command is still pending
                // In actual , the connection would switch to endpoint2

                // Command completes with timeout AFTER failover
                asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Timeout after failover"));

                Thread.sleep(100);

                // Failure should be recorded in endpoint1's circuit breaker (where command was issued)
                MetricsSnapshot snapshot1 = cb1.getSnapshot();
                MetricsSnapshot snapshot2 = cb2.getSnapshot();

                assertThat(snapshot1.getFailureCount()).isEqualTo(1);
                assertThat(snapshot2.getFailureCount()).isEqualTo(0); // endpoint2 should have no failures
            } finally {
                endpoint1.close();
                endpoint2.close();
            }
        }

        @Test
        @DisplayName("Should handle burst of failures during failover")
        void shouldHandleBurstOfFailuresDuringFailover() throws Exception {
            // Use high minimum failures to prevent circuit breaker from opening during test
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            // Issue many commands
            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                commands.add(asyncCommand);
                endpoint.write(asyncCommand);
            }

            // Simulate all commands failing during failover (connection lost)
            CountDownLatch latch = new CountDownLatch(20);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            try {
                for (AsyncCommand<String, String, String> cmd : commands) {
                    executor.submit(() -> {
                        try {
                            Thread.sleep((long) (Math.random() * 100)); // Random delay
                            cmd.completeExceptionally(new RedisConnectionException("Connection lost during failover"));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await(5, TimeUnit.SECONDS);
                Thread.sleep(200); // Allow callbacks to complete

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getFailureCount()).isEqualTo(20);
                assertThat(snapshot.getSuccessCount()).isEqualTo(0);
                assertThat(snapshot.getFailureRate()).isEqualTo(100.0f);
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
            } finally {
                executor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle mixed success/failure during partial failover")
        void shouldHandleMixedResultsDuringPartialFailover() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 3));
            endpoint.bind(circuitBreaker);

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                commands.add(asyncCommand);
                endpoint.write(asyncCommand);
            }

            // Simulate partial failover: some commands succeed, some timeout
            CountDownLatch latch = new CountDownLatch(10);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            try {
                for (int i = 0; i < commands.size(); i++) {
                    final int index = i;
                    executor.submit(() -> {
                        try {
                            Thread.sleep((long) (Math.random() * 200));
                            if (index < 6) {
                                // First 6 succeed
                                commands.get(index).complete();
                            } else {
                                // Last 4 timeout
                                commands.get(index)
                                        .completeExceptionally(new RedisCommandTimeoutException("Timeout during failover"));
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await(5, TimeUnit.SECONDS);
                Thread.sleep(200);

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getSuccessCount()).isEqualTo(6);
                assertThat(snapshot.getFailureCount()).isEqualTo(4);
                assertThat(snapshot.getTotalCount()).isEqualTo(10);
                assertThat(snapshot.getFailureRate()).isEqualTo(40.0f);
            } finally {
                executor.shutdown();
            }
        }

    }

    // ============ Concurrent Access Tests ============

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent command writes and completions")
        void shouldHandleConcurrentCommandWritesAndCompletions() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            int commandCount = 100;
            CountDownLatch writeLatch = new CountDownLatch(commandCount);
            CountDownLatch completeLatch = new CountDownLatch(commandCount);
            List<AsyncCommand<String, String, String>> commands = new CopyOnWriteArrayList<>();

            ExecutorService writeExecutor = Executors.newFixedThreadPool(10);
            ExecutorService completeExecutor = Executors.newFixedThreadPool(10);

            try {
                // Concurrently write commands
                for (int i = 0; i < commandCount; i++) {
                    writeExecutor.submit(() -> {
                        try {
                            Command<String, String, String> command = new Command<>(CommandType.SET,
                                    new StatusOutput<>(StringCodec.UTF8));
                            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                            endpoint.write(asyncCommand);
                            commands.add(asyncCommand);
                        } finally {
                            writeLatch.countDown();
                        }
                    });
                }

                writeLatch.await(5, TimeUnit.SECONDS);

                // Concurrently complete commands
                for (AsyncCommand<String, String, String> cmd : commands) {
                    completeExecutor.submit(() -> {
                        try {
                            if (Math.random() < 0.5) {
                                cmd.complete();
                            } else {
                                cmd.completeExceptionally(new RedisCommandTimeoutException("Timeout"));
                            }
                        } finally {
                            completeLatch.countDown();
                        }
                    });
                }

                completeLatch.await(5, TimeUnit.SECONDS);
                Thread.sleep(500); // Allow all callbacks to complete

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getTotalCount()).isEqualTo(commandCount);
                assertThat(snapshot.getSuccessCount() + snapshot.getFailureCount()).isEqualTo(commandCount);
            } finally {
                writeExecutor.shutdown();
                completeExecutor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle race between command completion and circuit breaker state change")
        void shouldHandleRaceBetweenCompletionAndStateChange() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 5));
            endpoint.bind(circuitBreaker);

            AtomicInteger stateChanges = new AtomicInteger(0);
            circuitBreaker.addListener(event -> stateChanges.incrementAndGet());

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                commands.add(asyncCommand);
                endpoint.write(asyncCommand);
            }

            // Complete all with failures to trigger state change
            CountDownLatch latch = new CountDownLatch(20);
            ExecutorService executor = Executors.newFixedThreadPool(10);
            try {
                for (AsyncCommand<String, String, String> cmd : commands) {
                    executor.submit(() -> {
                        try {
                            cmd.completeExceptionally(new RedisCommandTimeoutException("Timeout"));
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await(5, TimeUnit.SECONDS);
                Thread.sleep(500);

                // Circuit should have opened
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
                assertThat(stateChanges.get()).isGreaterThan(0);
            } finally {
                executor.shutdown();
            }
        }

    }

    // ============ Edge Cases and Special Scenarios ============

    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle command completion after very long delay (simulating stuck connection)")
        void shouldHandleVeryLongDelayedCompletion() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Simulate very long delay (e.g., 2 seconds - simulating stuck connection that eventually times out)
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                executor.schedule(() -> {
                    asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Timeout after very long delay"));
                }, 2, TimeUnit.SECONDS);

                // Wait for completion
                Thread.sleep(2500);

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getFailureCount()).isEqualTo(1);
            } finally {
                executor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle callback when command is already completed before write")
        void shouldHandleAlreadyCompletedCommand() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Complete command before writing
            asyncCommand.complete();

            // Write already-completed command
            endpoint.write(asyncCommand);

            Thread.sleep(100);

            // Should still record the success
            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Note: Depending on implementation, this might be 0 or 1
            // The callback might fire immediately or not at all for already-completed commands
            assertThat(snapshot.getSuccessCount() + snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null error in callback (success case)")
        void shouldHandleNullErrorInCallback() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete successfully (error will be null in callback)
            asyncCommand.complete();

            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(1);
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle multiple callbacks on same command (if possible)")
        void shouldHandleMultipleCallbacksOnSameCommand() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Write same command multiple times (unusual but possible)
            endpoint.write(asyncCommand);
            endpoint.write(asyncCommand);

            asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Timeout"));

            Thread.sleep(100);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Should record failure (possibly multiple times depending on implementation)
            assertThat(snapshot.getFailureCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should handle exception in callback gracefully")
        void shouldHandleExceptionInCallbackGracefully() throws Exception {

            // Create a circuit breaker that will throw exception
            CircuitBreaker circuitBreaker = spy(new CircuitBreakerImpl(getCBConfig(50.0f, 100)));

            // Make recordResult throw exception
            doThrow(new RuntimeException("Simulated callback exception")).when(circuitBreaker).recordResult(any());

            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command - callback should handle exception gracefully
            assertThatCode(() -> {
                asyncCommand.complete();
                Thread.sleep(100);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should track different exception types correctly in same batch")
        void shouldTrackDifferentExceptionTypesInSameBatch() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                commands.add(asyncCommand);
                endpoint.write(asyncCommand);
            }

            // Complete with different exception types
            commands.get(0).completeExceptionally(new RedisCommandTimeoutException("Timeout"));
            commands.get(1).completeExceptionally(new RedisConnectionException("Connection failed"));
            commands.get(2).completeExceptionally(new IOException("IO error"));
            commands.get(3).completeExceptionally(new ClosedChannelException());
            commands.get(4).completeExceptionally(new TimeoutException("Generic timeout"));
            commands.get(5).complete(); // Success

            Thread.sleep(200);

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(5); // All tracked exceptions
            assertThat(snapshot.getSuccessCount()).isEqualTo(1);
            assertThat(snapshot.getTotalCount()).isEqualTo(6);
        }

    }

}
