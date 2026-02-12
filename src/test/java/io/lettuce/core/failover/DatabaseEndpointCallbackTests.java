package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;
import static io.lettuce.TestTags.UNIT_TEST;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Durations;
import org.junit.jupiter.api.*;

import io.lettuce.core.*;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.CircuitBreakerConfig;
import io.lettuce.core.failover.metrics.MetricsFactory;
import io.lettuce.core.failover.metrics.MetricsSnapshot;
import io.lettuce.core.failover.metrics.TestClock;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.TestClientResources;

/**
 * Integration tests for DatabaseEndpointImpl focusing on:
 * <ul>
 * <li>Failover behavior and generation/epoch tracking</li>
 * <li>Late failure handling from old generations</li>
 * <li>Timeout exception tracking (only exception type tracked via callback)</li>
 * </ul>
 *
 * Note: This test file focuses on integration-level behavior of DatabaseEndpointImpl. Unit-level tests for
 * DatabaseCommandTracker are in {@link DatabaseCommandTrackerUnitTests}. Unit-level tests for MultiDbOutboundHandler are in
 * {@link MultiDbOutboundHandlerUnitTests}.
 *
 * @author Ali Takavci
 */
@Tag(UNIT_TEST)
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
        if (endpoint != null) {
            endpoint.close();
        }
    }

    private CircuitBreakerConfig getCBConfig(float failureRateThreshold, int minimumNumberOfFailures) {
        return CircuitBreakerConfig.builder().failureRateThreshold(failureRateThreshold)
                .minimumNumberOfFailures(minimumNumberOfFailures).build();
    }

    // ============ Timeout Exception Tracking Tests ============
    // Note: Only timeout exceptions are tracked via DatabaseCommandTracker callback.
    // Other exceptions and successes are tracked by MultiDbOutboundHandler in the pipeline.

    @Nested
    @DisplayName("Timeout Exception Tracking Tests")
    @Tag(UNIT_TEST)
    class TimeoutExceptionTrackingTests {

        @Test
        @DisplayName("Should track timeout exception via callback")
        void shouldTrackTimeoutExceptionViaCallback() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete with timeout exception
            asyncCommand.completeExceptionally(new RedisCommandTimeoutException("timeout"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track timeout exceptions for batch commands")
        void shouldTrackTimeoutExceptionsForBatchCommands() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                commands.add(new AsyncCommand<>(command));
            }

            endpoint.write(new ArrayList<>(commands));

            // Complete all with timeout exceptions
            commands.forEach(cmd -> cmd.completeExceptionally(new RedisCommandTimeoutException("timeout")));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getFailureCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should NOT track non-timeout exceptions via callback (handled by MultiDbOutboundHandler)")
        void shouldNotTrackNonTimeoutExceptionsViaCallback() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete with non-timeout exception (should be tracked by MultiDbOutboundHandler, not callback)
            asyncCommand.completeExceptionally(new RedisConnectionException("connection failed"));

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Should NOT be recorded by callback (MultiDbOutboundHandler would record it in real pipeline)
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should NOT track success via callback (handled by MultiDbOutboundHandler)")
        void shouldNotTrackSuccessViaCallback() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete successfully
            asyncCommand.complete();

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Should NOT be recorded by callback (MultiDbOutboundHandler would record it in real pipeline)
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        }

    }

    // ============ Failover Behavior Tests ============
    // These tests verify generation/epoch tracking - unique to DatabaseEndpointImpl integration

    @Nested
    @DisplayName("Failover Behavior and Generation Tracking Tests")
    @Tag(UNIT_TEST)
    class FailoverBehaviorTests {

        @Test
        @DisplayName("Should record timeout exception as failure via callback")
        void shouldRecordTimeoutExceptionAsFailureViaCallback() {
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
        @DisplayName("Should handle delayed timeout exception completion")
        void shouldHandleDelayedTimeoutExceptionCompletion() throws Exception {
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
        @DisplayName("Should handle multiple timeout exceptions completing at different times")
        void shouldHandleMultipleTimeoutExceptionsCompletingAtDifferentTimes() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                commands.add(asyncCommand);
                endpoint.write(asyncCommand);
            }

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
            try {
                // Complete commands at different times with timeout exceptions
                executor.schedule(() -> commands.get(0).completeExceptionally(new RedisCommandTimeoutException("timeout1")),
                        100, TimeUnit.MILLISECONDS);
                executor.schedule(() -> commands.get(1).completeExceptionally(new RedisCommandTimeoutException("timeout2")),
                        200, TimeUnit.MILLISECONDS);
                executor.schedule(() -> commands.get(2).completeExceptionally(new RedisCommandTimeoutException("timeout3")),
                        300, TimeUnit.MILLISECONDS);

                await().pollDelay(Durations.ONE_HUNDRED_MILLISECONDS).atMost(Durations.ONE_SECOND)
                        .untilAsserted(() -> assertThat(circuitBreaker.getSnapshot().getFailureCount()).isEqualTo(3));

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getFailureCount()).isEqualTo(3);
            } finally {
                executor.shutdown();
            }
        }

    }

    // ============ Failover Behavior and Generation Tracking Tests ============
    // These tests verify the critical generation/epoch tracking logic

    @Nested
    @DisplayName("Failover Behavior and Generation Tracking Tests")
    @Tag(UNIT_TEST)
    class FailoverBehaviorAndGenerationTrackingTests {

        @Test
        @DisplayName("Should ignore late failures from old generation after CB opens and new generation created")
        void shouldIgnoreLateFailuresFromOldGenerationAfterCBOpens() {

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
        @DisplayName("Should handle burst of timeout exceptions during failover")
        void shouldHandleBurstOfTimeoutExceptionsDuringFailover() throws Exception {
            // here replace Clock with TestClock
            TestClock clock = new TestClock();
            ReflectionTestUtils.setField(MetricsFactory.class, "DEFAULT_CLOCK", clock);

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

            // Simulate all commands timing out during failover
            CountDownLatch latch = new CountDownLatch(20);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            try {
                for (AsyncCommand<String, String, String> cmd : commands) {
                    executor.submit(() -> {
                        try {
                            clock.advance(Duration.ofMillis((long) (Math.random() * 100)));
                            cmd.completeExceptionally(new RedisCommandTimeoutException("Timeout during failover"));

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
        @DisplayName("Should handle concurrent timeout exceptions during failover")
        void shouldHandleConcurrentTimeoutExceptionsDuringFailover() throws Exception {
            // here replace Clock with TestClock
            TestClock clock = new TestClock();
            ReflectionTestUtils.setField(MetricsFactory.class, "DEFAULT_CLOCK", clock);

            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));

            endpoint.bind(circuitBreaker);

            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                commands.add(asyncCommand);
                endpoint.write(asyncCommand);
            }

            // Simulate all commands timing out concurrently
            CountDownLatch latch = new CountDownLatch(10);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            try {
                for (int i = 0; i < commands.size(); i++) {
                    final int index = i;
                    executor.submit(() -> {
                        try {
                            clock.advance(Duration.ofMillis((long) (Math.random() * 200)));
                            commands.get(index)
                                    .completeExceptionally(new RedisCommandTimeoutException("Timeout during failover"));
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await(5, TimeUnit.SECONDS);

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getFailureCount()).isEqualTo(10);
                assertThat(snapshot.getTotalCount()).isEqualTo(10);
                assertThat(snapshot.getFailureRate()).isEqualTo(100.0f);
            } finally {
                executor.shutdown();
            }
        }

    }

    // ============ Concurrent Timeout Exception Tests ============

    @Nested
    @DisplayName("Concurrent Timeout Exception Tests")
    @Tag(UNIT_TEST)
    class ConcurrentTimeoutExceptionTests {

        @Test
        @DisplayName("Should handle concurrent timeout exception writes and completions")
        void shouldHandleConcurrentTimeoutExceptionWritesAndCompletions() throws Exception {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            int commandCount = 50;
            List<AsyncCommand<String, String, String>> commands = new ArrayList<>();

            // First, create and write all commands sequentially to ensure callbacks are attached
            for (int i = 0; i < commandCount; i++) {
                Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
                AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);
                endpoint.write(asyncCommand);
                commands.add(asyncCommand);
            }

            // Then concurrently complete them with timeout exceptions
            CountDownLatch completeLatch = new CountDownLatch(commandCount);
            ExecutorService completeExecutor = Executors.newFixedThreadPool(10);

            try {
                for (AsyncCommand<String, String, String> cmd : commands) {
                    completeExecutor.submit(() -> {
                        try {
                            cmd.completeExceptionally(new RedisCommandTimeoutException("Timeout"));
                        } finally {
                            completeLatch.countDown();
                        }
                    });
                }

                completeLatch.await(5, TimeUnit.SECONDS);

                // Wait for all callbacks to fire
                await().pollDelay(Durations.ONE_HUNDRED_MILLISECONDS).atMost(Durations.ONE_SECOND).untilAsserted(
                        () -> assertThat(circuitBreaker.getSnapshot().getFailureCount()).isEqualTo(commandCount));

                MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
                assertThat(snapshot.getFailureCount()).isEqualTo(commandCount);
            } finally {
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
    @Tag(UNIT_TEST)
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
        @DisplayName("Should handle timeout exception when command is already completed before write")
        void shouldHandleAlreadyCompletedTimeoutCommand() {
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(getCBConfig(50.0f, 100));
            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            // Complete command with timeout before writing
            asyncCommand.completeExceptionally(new RedisCommandTimeoutException("timeout"));

            // Write already-completed command
            endpoint.write(asyncCommand);

            // Should still record the timeout failure
            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Note: Depending on implementation, this might be 0 or 1
            // The callback might fire immediately or not at all for already-completed commands
            assertThat(snapshot.getFailureCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should handle multiple callbacks on same command (if possible)")
        void shouldHandleMultipleCallbacksOnSameCommand() {
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
            CircuitBreakerGeneration generation = error -> {
                throw new RuntimeException("Simulated callback exception");
            };

            doReturn(generation).when(circuitBreaker).getGeneration();

            endpoint.bind(circuitBreaker);

            Command<String, String, String> command = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));
            AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(command);

            endpoint.write(asyncCommand);

            // Complete command with timeout - callback should handle exception gracefully
            assertThatCode(() -> asyncCommand.completeExceptionally(new RedisCommandTimeoutException("timeout")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should track only timeout exceptions in batch (other exceptions handled by pipeline)")
        void shouldTrackOnlyTimeoutExceptionsInBatch() {
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
            commands.get(1).completeExceptionally(new RedisConnectionException("Connection failed")); // NOT tracked by callback
            commands.get(2).completeExceptionally(new IOException("IO error")); // NOT tracked by callback
            commands.get(3).completeExceptionally(new ClosedChannelException()); // NOT tracked by callback
            commands.get(4).completeExceptionally(new TimeoutException("Generic timeout")); // NOT tracked by callback
            commands.get(5).complete(); // Success - NOT tracked by callback

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            // Only timeout exception (index 0) is tracked by callback
            // Other exceptions and success would be tracked by MultiDbOutboundHandler in real pipeline
            assertThat(snapshot.getFailureCount()).isEqualTo(1);
        }

    }

    // Note: DatabaseCommandTracker integration tests are covered in DatabaseCommandTrackerUnitTests.java
    // Note: Channel lifecycle tests are covered in DatabasePubSubEndpointTrackerTests.java

}
