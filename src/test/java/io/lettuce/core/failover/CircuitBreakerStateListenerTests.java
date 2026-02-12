package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import io.lettuce.TestTags;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.failover.api.CircuitBreakerConfig;
import io.lettuce.core.failover.api.CircuitBreakerStateChangeEvent;
import io.lettuce.core.failover.api.CircuitBreakerStateListener;

/**
 * Unit tests for {@link CircuitBreakerStateListener} functionality.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@Tag(TestTags.UNIT_TEST)
class CircuitBreakerStateListenerTests {

    private CircuitBreakerImpl circuitBreaker;

    private final RedisCommandTimeoutException timeoutException = new RedisCommandTimeoutException("Test Timeout");

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.builder().failureRateThreshold(50.0f).minimumNumberOfFailures(5)
                .build();
        circuitBreaker = new CircuitBreakerImpl(config);
    }

    @Test
    void shouldNotifyListenerOnStateChange() {
        // Given
        TestListener listener = new TestListener();
        circuitBreaker.addListener(listener);

        // When - trigger state change by recording failures
        for (int i = 0; i < 10; i++) {
            circuitBreaker.getGeneration().recordResult(timeoutException);
        }

        // Then
        assertThat(listener.events).hasSize(1);
        CircuitBreakerStateChangeEvent event = listener.events.get(0);
        assertThat(event.getPreviousState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(event.getNewState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(event.getCircuitBreaker()).isSameAs(circuitBreaker);
        assertThat(event.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void shouldNotifyMultipleListeners() {
        // Given
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        circuitBreaker.addListener(listener1);
        circuitBreaker.addListener(listener2);

        // When - trigger state change
        for (int i = 0; i < 10; i++) {
            circuitBreaker.getGeneration().recordResult(timeoutException);
        }

        // Then
        assertThat(listener1.events).hasSize(1);
        assertThat(listener2.events).hasSize(1);
    }

    @Test
    void shouldNotNotifyRemovedListener() {
        // Given
        TestListener listener = new TestListener();
        circuitBreaker.addListener(listener);
        circuitBreaker.removeListener(listener);

        // When - trigger state change
        for (int i = 0; i < 10; i++) {
            circuitBreaker.getGeneration().recordResult(timeoutException);
        }

        // Then
        assertThat(listener.events).isEmpty();
    }

    @Test
    void shouldNotNotifyIfStateDoesNotChange() {
        // Given
        TestListener listener = new TestListener();
        circuitBreaker.addListener(listener);

        // When - evaluate without enough failures
        circuitBreaker.getGeneration().recordResult(null);
        circuitBreaker.evaluateMetrics();

        // Then
        assertThat(listener.events).isEmpty();
    }

    @Test
    void shouldHandleListenerExceptionsGracefully() {
        // Given
        FailingListener failingListener = new FailingListener();
        TestListener normalListener = new TestListener();
        circuitBreaker.addListener(failingListener);
        circuitBreaker.addListener(normalListener);

        // When - trigger state change
        for (int i = 0; i < 10; i++) {
            circuitBreaker.getGeneration().recordResult(timeoutException);
        }

        // Then - normal listener should still receive the event
        assertThat(normalListener.events).hasSize(1);
    }

    @Test
    void shouldIncludeTimestampInEvent() throws InterruptedException {
        // Given
        TestListener listener = new TestListener();
        circuitBreaker.addListener(listener);
        long beforeTimestamp = System.currentTimeMillis();

        // When
        Thread.sleep(10); // Small delay to ensure timestamp difference
        for (int i = 0; i < 10; i++) {
            circuitBreaker.getGeneration().recordResult(timeoutException);
        }
        long afterTimestamp = System.currentTimeMillis();

        // Then
        assertThat(listener.events).hasSize(1);
        long eventTimestamp = listener.events.get(0).getTimestamp();
        assertThat(eventTimestamp).isBetween(beforeTimestamp, afterTimestamp);
    }

    /**
     * Test listener that collects all events.
     */
    private static class TestListener implements CircuitBreakerStateListener {

        final List<CircuitBreakerStateChangeEvent> events = new ArrayList<>();

        @Override
        public void onCircuitBreakerStateChange(CircuitBreakerStateChangeEvent event) {
            events.add(event);
        }

    }

    /**
     * Test listener that always throws an exception.
     */
    private static class FailingListener implements CircuitBreakerStateListener {

        @Override
        public void onCircuitBreakerStateChange(CircuitBreakerStateChangeEvent event) {
            throw new RuntimeException("Listener failure");
        }

    }

}
