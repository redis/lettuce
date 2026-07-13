package io.lettuce.core.event;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Subscription;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class DefaultEventBusUnitTests {

    private final EventExecutorGroup group = new DefaultEventExecutorGroup(1);

    @AfterEach
    void tearDown() {
        group.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
    }

    @Test
    void publishToSingleSubscriber() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<Event> received = new ArrayBlockingQueue<>(8);

        Subscription subscription = sut.subscribe(received::add);
        Event event = new TestEvent();
        sut.publish(event);

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(event);
        subscription.close();
    }

    @Test
    void publishToMultipleSubscribers() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<Event> first = new ArrayBlockingQueue<>(8);
        ArrayBlockingQueue<Event> second = new ArrayBlockingQueue<>(8);

        sut.subscribe(first::add);
        sut.subscribe(second::add);
        Event event = new TestEvent();
        sut.publish(event);

        assertThat(first.poll(1, TimeUnit.SECONDS)).isSameAs(event);
        assertThat(second.poll(1, TimeUnit.SECONDS)).isSameAs(event);
    }

    @Test
    void subscribeByTypeFiltersEvents() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<EventA> received = new ArrayBlockingQueue<>(8);

        sut.subscribe(EventA.class, received::add);
        sut.publish(new EventB());
        EventA expected = new EventA();
        sut.publish(expected);

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(expected);
        assertThat(received).isEmpty();
    }

    @Test
    void unsubscribeStopsDelivery() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<Event> received = new ArrayBlockingQueue<>(8);

        Subscription subscription = sut.subscribe(received::add);
        subscription.close();
        sut.publish(new TestEvent());

        assertThat(received.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void closeIsIdempotent() {

        DefaultEventBus sut = new DefaultEventBus(group);

        Subscription subscription = sut.subscribe(event -> {
        });
        subscription.close();
        subscription.close();
    }

    @Test
    void perSubscriberOrdering() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        int count = 100;
        CountDownLatch latch = new CountDownLatch(count);
        List<Integer> seen = new CopyOnWriteArrayList<>();

        sut.subscribe(event -> {
            seen.add(((IndexedEvent) event).index);
            latch.countDown();
        });
        for (int i = 0; i < count; i++) {
            sut.publish(new IndexedEvent(i));
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            expected.add(i);
        }
        assertThat(seen).containsExactlyElementsOf(expected);
    }

    @Test
    void exceptionInListenerIsIsolated() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<Event> received = new ArrayBlockingQueue<>(8);
        boolean[] threwOnce = { false };

        sut.subscribe(event -> {
            if (!threwOnce[0]) {
                threwOnce[0] = true;
                throw new RuntimeException("boom");
            }
            received.add(event);
        });
        sut.publish(new TestEvent());
        Event second = new TestEvent();
        sut.publish(second);

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(second);
    }

    @Test
    void noSubscriberIsNoOp() {

        DefaultEventBus sut = new DefaultEventBus(group);
        sut.publish(new TestEvent());
    }

    @Test
    void dropsEventsForSlowSubscriberBeyondBound() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group, 1);
        ArrayBlockingQueue<Event> received = new ArrayBlockingQueue<>(8);
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch firstRunning = new CountDownLatch(1);

        sut.subscribe(event -> {
            received.add(event);
            firstRunning.countDown();
            try {
                gate.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Event first = new TestEvent();
        sut.publish(first); // picked up, blocks the single executor thread; in-flight == 1
        assertThat(firstRunning.await(1, TimeUnit.SECONDS)).isTrue();
        sut.publish(new TestEvent()); // in-flight would be 2 > 1 -> dropped
        sut.publish(new TestEvent()); // dropped
        gate.countDown();

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(first);
        assertThat(received.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void concurrentPublishersDeliverWithoutError() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group, 100_000); // generous bound so nothing is dropped
        int threads = 8;
        int perThread = 500;
        int total = threads * perThread;
        CountDownLatch delivered = new CountDownLatch(total);
        List<Throwable> publishErrors = new CopyOnWriteArrayList<>();

        sut.subscribe(event -> delivered.countDown());

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    try {
                        for (int i = 0; i < perThread; i++) {
                            sut.publish(new TestEvent());
                        }
                    } catch (Throwable th) {
                        publishErrors.add(th);
                    }
                });
            }

            assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(publishErrors).isEmpty();
    }

    @Test
    void typedSubscriberNotStarvedByOtherEventTypes() throws Exception {

        // Tiny bound: matching events would be dropped if non-matching events consumed in-flight capacity.
        DefaultEventBus sut = new DefaultEventBus(group, 1);
        ArrayBlockingQueue<EventA> received = new ArrayBlockingQueue<>(8);

        sut.subscribe(EventA.class, received::add);

        // A burst of non-matching events must be filtered before in-flight admission and never starve the typed subscriber.
        for (int i = 0; i < 1000; i++) {
            sut.publish(new EventB());
        }
        EventA expected = new EventA();
        sut.publish(expected);

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(expected);
    }

    static class TestEvent implements Event {
    }

    static class EventA implements Event {
    }

    static class EventB implements Event {
    }

    static class IndexedEvent implements Event {

        final int index;

        IndexedEvent(int index) {
            this.index = index;
        }

    }

}
