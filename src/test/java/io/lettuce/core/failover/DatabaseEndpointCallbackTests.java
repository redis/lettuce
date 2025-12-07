package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Durations;
import org.junit.jupiter.api.*;

import io.lettuce.core.*;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;
import io.lettuce.core.failover.metrics.MetricsFactory;
import io.lettuce.core.failover.metrics.MetricsSnapshot;
import io.lettuce.core.failover.metrics.TestClock;
import io.lettuce.core.failover.metrics.TestMetricsFactory;
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
        void shouldAttachCallbackToSingleCommand() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            // Create a command
            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));

            // Track how many callbacks are registered on this command
            AtomicInteger onCompleteCallCount = new AtomicInteger(0);

            // Wrap to intercept onComplete calls
            AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                @Override
                public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                    onCompleteCallCount.incrementAndGet();
                    super.onComplete(action);
                }

            };

            // Write command - this should call completeable.onComplete(generation::recordResult) at line 72
            RedisCommand<String, String, String> result = endpoint.write(spyCommand);

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(CompleteableCommand.class);

            // Verify onComplete was called exactly once (the callback was attached)
            assertThat(onCompleteCallCount.get()).isEqualTo(1);

            // Complete the command to trigger the callback
            spyCommand.complete();

            // Verify the callback actually recorded metrics
            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should attach callbacks to multiple commands")
        void shouldAttachCallbacksToMultipleCommands() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            // Create multiple commands with onComplete tracking
            List<AsyncCommand<String, String, String>> spyCommands = new ArrayList<>();
            AtomicInteger totalOnCompleteCalls = new AtomicInteger(0);

            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                    @Override
                    public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                        totalOnCompleteCalls.incrementAndGet();
                        super.onComplete(action);
                    }

                };
                spyCommands.add(spyCommand);
            }

            // Write commands - this should call completeable.onComplete(generation::recordResult) at line 102 for each
            List<RedisCommand<String, String, ?>> results = new ArrayList<>(endpoint.write(new ArrayList<>(spyCommands)));

            assertThat(results).hasSize(5);
            results.forEach(result -> assertThat(result).isInstanceOf(CompleteableCommand.class));

            // Verify onComplete was called exactly 5 times (callbacks were attached to all commands)
            assertThat(totalOnCompleteCalls.get()).isEqualTo(5);

            // Complete all commands to trigger callbacks
            spyCommands.forEach(cmd -> cmd.complete());

            // Verify callbacks actually recorded metrics for all commands
            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should not attach callback when circuit breaker is null")
        void shouldNotAttachCallbackWhenCircuitBreakerIsNull() throws Exception {
            // Don't bind circuit breaker - this should trigger early return at line 50-51

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));

            // Track if onComplete is called
            AtomicInteger onCompleteCallCount = new AtomicInteger(0);
            AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                @Override
                public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                    onCompleteCallCount.incrementAndGet();
                    super.onComplete(action);
                }

            };

            // Write command - should NOT call onComplete since circuit breaker is null
            RedisCommand<String, String, String> result = endpoint.write(spyCommand);

            assertThat(result).isNotNull();

            // Verify onComplete was NEVER called (no callback attached)
            assertThat(onCompleteCallCount.get()).isEqualTo(0);

            // Complete the command
            spyCommand.complete();

            // Verify command completed successfully
            assertThat(spyCommand.isDone()).isTrue();
            assertThat(spyCommand.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("Should complete command exceptionally when circuit breaker is open")
        void shouldCompleteCommandExceptionallyWhenCircuitBreakerIsOpen() throws Exception {
            // Create circuit breaker and force it to OPEN state
            CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 2)); // threshold: 2 failures
            endpoint.bind(circuitBreaker);

            // Force circuit breaker to OPEN by recording failures and evaluating
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();
            circuitBreaker.evaluateMetrics(); // This triggers state transition to OPEN

            assertThat(circuitBreaker.isClosed()).isFalse(); // Verify CB is OPEN

            // Create a command
            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));

            // Track if onComplete is called
            AtomicInteger onCompleteCallCount = new AtomicInteger(0);
            AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                @Override
                public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                    onCompleteCallCount.incrementAndGet();
                    super.onComplete(action);
                }

            };

            // Write command - should trigger lines 53-55 and complete exceptionally
            RedisCommand<String, String, String> result = endpoint.write(spyCommand);

            assertThat(result).isNotNull();

            // Verify command was completed exceptionally with RedisCircuitBreakerException
            assertThat(spyCommand.isDone()).isTrue();
            assertThat(spyCommand.isCompletedExceptionally()).isTrue();

            // Verify the exception is RedisCircuitBreakerException
            assertThatThrownBy(() -> spyCommand.get()).hasCauseInstanceOf(RedisCircuitBreakerException.class);

            // Verify onComplete was NEVER called (no callback attached when CB is OPEN)
            assertThat(onCompleteCallCount.get()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should complete all commands exceptionally when circuit breaker is open for collection write")
        void shouldCompleteAllCommandsExceptionallyWhenCircuitBreakerIsOpen() throws Exception {
            // Create circuit breaker and force it to OPEN state
            CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 2)); // threshold: 2 failures
            endpoint.bind(circuitBreaker);

            // Force circuit breaker to OPEN by recording failures and evaluating
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();
            circuitBreaker.evaluateMetrics(); // This triggers state transition to OPEN

            assertThat(circuitBreaker.isClosed()).isFalse(); // Verify CB is OPEN

            // Create multiple commands with onComplete tracking
            List<AsyncCommand<String, String, String>> spyCommands = new ArrayList<>();
            AtomicInteger totalOnCompleteCalls = new AtomicInteger(0);

            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> spyCommand = new AsyncCommand<String, String, String>(command) {

                    @Override
                    public void onComplete(java.util.function.BiConsumer<? super String, Throwable> action) {
                        totalOnCompleteCalls.incrementAndGet();
                        super.onComplete(action);
                    }

                };
                spyCommands.add(spyCommand);
            }

            // Write commands - should trigger lines 82-84 and complete all exceptionally
            Collection<RedisCommand<String, String, ?>> results = endpoint.write(new ArrayList<>(spyCommands));

            assertThat(results).hasSize(5);

            // Verify all commands were completed exceptionally
            spyCommands.forEach(cmd -> {
                assertThat(cmd.isDone()).isTrue();
                assertThat(cmd.isCompletedExceptionally()).isTrue();
                assertThatThrownBy(() -> cmd.get()).hasCauseInstanceOf(RedisCircuitBreakerException.class);
            });

            // Verify onComplete was NEVER called for any command (no callbacks attached when CB is OPEN)
            assertThat(totalOnCompleteCalls.get()).isEqualTo(0);
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
                @SuppressWarnings("rawtypes")
                Future future = executor.schedule(() -> {
                    asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Delayed timeout"));
                }, 500, TimeUnit.MILLISECONDS);

                future.get(); // Wait for completion

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

                await().pollDelay(Durations.ONE_HUNDRED_MILLISECONDS).atMost(Durations.ONE_SECOND)
                        .untilAsserted(() -> assertThat(circuitBreaker.getSnapshot().getTotalCount()).isEqualTo(5));

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
        @DisplayName("Should ignore late failures from old generation after CB opens and new generation created")
        void shouldIgnoreLateFailuresFromOldGenerationAfterCBOpens() throws Exception {

            DatabaseEndpointImpl endpoint1 = new DatabaseEndpointImpl(clientOptions, clientResources);
            try {
                // Create circuit breaker with low threshold so it opens quickly
                CircuitBreaker cb1 = new CircuitBreakerImpl(getCBConfig(50.0f, 2)); // Opens after 2 failures with 50% rate

                endpoint1.bind(cb1);

                // Issue commands while CB is CLOSED
                Command<String, String, String> command1 = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand1 = new AsyncCommand<>(command1);
                endpoint1.write(asyncCommand1);

                Command<String, String, String> command2 = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand2 = new AsyncCommand<>(command2);
                endpoint1.write(asyncCommand2);

                Command<String, String, String> command3 = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand3 = new AsyncCommand<>(command3);
                endpoint1.write(asyncCommand3);

                // Capture the original generation (CLOSED state)
                CircuitBreakerGeneration originalGeneration = cb1.getGeneration();
                assertThat(cb1.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);

                // Get snapshot before any failures
                MetricsSnapshot beforeSnapshot = cb1.getSnapshot();
                assertThat(beforeSnapshot.getFailureCount()).isEqualTo(0);

                // Fail the first 2 commands to trigger CB to open
                asyncCommand1.completeExceptionally(new RedisCommandTimeoutException("Timeout 1"));
                asyncCommand2.completeExceptionally(new RedisCommandTimeoutException("Timeout 2"));

                // Verify CB has transitioned to OPEN state with a new generation
                assertThat(cb1.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
                CircuitBreakerGeneration newGeneration = cb1.getGeneration();
                assertThat(newGeneration).isNotSameAs(originalGeneration); // New generation created

                // Get snapshot after CB opened - new generation starts fresh with 0 failures
                MetricsSnapshot afterOpenSnapshot = cb1.getSnapshot();
                assertThat(afterOpenSnapshot.getFailureCount()).isEqualTo(0);
                assertThat(afterOpenSnapshot.getSuccessCount()).isEqualTo(0);

                // Now the third command (issued during CLOSED state) fails AFTER CB is already OPEN
                asyncCommand3.completeExceptionally(new RedisCommandTimeoutException("Late timeout"));

                // Verify: The late failure is IGNORED because it belongs to the old generation
                // The callback attached to asyncCommand3 points to originalGeneration, but when it fires,
                // it checks if (current != this) and returns early without recording the failure.
                // This prevents stale failures from earlier failover stages from affecting the current state.

                MetricsSnapshot finalSnapshot = cb1.getSnapshot();

                // The new generation (OPEN state) should still have 0 failures
                // The late failure from asyncCommand3 was ignored (not recorded)
                assertThat(finalSnapshot.getFailureCount()).isEqualTo(0);
                assertThat(finalSnapshot.getSuccessCount()).isEqualTo(0);

                // Verify CB is still OPEN
                assertThat(cb1.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

                // This test verifies the generation/epoch tracking behavior:
                // Late timeouts from earlier CLOSED periods don't affect the current OPEN state's metrics
            } finally {
                endpoint1.close();
            }
        }

        @Test
        @DisplayName("Should handle burst of failures during failover")
        void shouldHandleBurstOfFailuresDuringFailover() throws Exception {
            // here replace Clock with TestClock
            TestClock clock = new TestClock();
            MetricsFactory metricsFactory = new TestMetricsFactory(clock);

            // Use high minimum failures to prevent circuit breaker from opening during test
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100), metricsFactory);
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
                            clock.advance(Duration.ofMillis((long) (Math.random() * 100)));
                            cmd.completeExceptionally(new RedisConnectionException("Connection lost during failover"));

                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await(5, TimeUnit.SECONDS);

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
            // here replace Clock with TestClock
            TestClock clock = new TestClock();
            MetricsFactory metricsFactory = new TestMetricsFactory(clock);

            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 3), metricsFactory);

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
                            clock.advance(Duration.ofMillis(((long) Math.random() * 200)));
                            if (index < 6) {
                                // First 6 succeed
                                commands.get(index).complete();
                            } else {
                                // Last 4 timeout
                                commands.get(index)
                                        .completeExceptionally(new RedisCommandTimeoutException("Timeout during failover"));
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await(5, TimeUnit.SECONDS);

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
                @SuppressWarnings("rawtypes")
                Future future = executor.schedule(() -> {
                    asyncCommand.completeExceptionally(new RedisCommandTimeoutException("Timeout after very long delay"));
                }, 2, TimeUnit.SECONDS);

                future.get(); // Wait for completion

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
            CircuitBreakerGeneration generation = new CircuitBreakerGeneration() {

                @Override
                public void recordResult(Object output, Throwable error) {
                    throw new RuntimeException("Simulated callback exception");
                }

            };

            doReturn(generation).when(circuitBreaker).getGeneration();

            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command - callback should handle exception gracefully
            assertThatCode(() -> {
                asyncCommand.complete();
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

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(5); // All tracked exceptions
            assertThat(snapshot.getSuccessCount()).isEqualTo(1);
            assertThat(snapshot.getTotalCount()).isEqualTo(6);
        }

    }

}
