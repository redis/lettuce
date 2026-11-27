package io.lettuce.core.event;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Subscription;
import io.lettuce.test.LoggingTestUtils;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.SingleThreadEventExecutor;

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
    void logsDroppedEventsAtDebug() throws Exception {

        try (LoggingTestUtils.CapturingAppender debug = LoggingTestUtils.attachAppenderFor(DefaultEventBus.class,
                Level.DEBUG)) {
            dropOneEvent();
            assertThat(debug.messages()).hasSize(1).allMatch(message -> message.startsWith("Dropping event"));
        }
    }

    private void dropOneEvent() throws InterruptedException {

        DefaultEventBus sut = new DefaultEventBus(group, 1);
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch firstRunning = new CountDownLatch(1);

        sut.subscribe(event -> {
            firstRunning.countDown();
            try {
                gate.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        sut.publish(new TestEvent()); // blocks the single executor thread; in-flight == 1
        assertThat(firstRunning.await(1, TimeUnit.SECONDS)).isTrue();
        sut.publish(new TestEvent()); // dropped
        gate.countDown();
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

    @Test
    void allSubscribersSeeOneOrderAcrossConcurrentPublishers() throws Exception {

        GatedEventExecutorGroup gatedGroup = new GatedEventExecutorGroup(2);
        try {
            DefaultEventBus sut = new DefaultEventBus(gatedGroup);
            LinkedBlockingQueue<Event> first = new LinkedBlockingQueue<>();
            LinkedBlockingQueue<Event> second = new LinkedBlockingQueue<>();
            AtomicReference<EventExecutor> firstExecutor = new AtomicReference<>();

            sut.subscribe(event -> {
                firstExecutor.compareAndSet(null, currentExecutor(gatedGroup));
                first.add(event);
            });
            sut.subscribe(second::add);

            // find the executor of the first subscriber, then pause the next hand-off to it
            Event probe = new TestEvent();
            sut.publish(probe);
            assertThat(first.poll(1, TimeUnit.SECONDS)).isSameAs(probe);
            assertThat(second.poll(1, TimeUnit.SECONDS)).isSameAs(probe);
            gatedGroup.arm(firstExecutor.get());

            Event e1 = new IndexedEvent(1);
            Event e2 = new IndexedEvent(2);

            // Thread 1 publishes e1 and pauses right after handing it to the first subscriber. While it is paused, thread 2
            // publishes e2. Without one order, the second subscriber would get e2 before e1.
            Thread publisher1 = new Thread(() -> sut.publish(e1));
            publisher1.start();
            assertThat(gatedGroup.entered.await(1, TimeUnit.SECONDS)).isTrue();
            Thread publisher2 = new Thread(() -> sut.publish(e2));
            publisher2.start();
            awaitWaitingOrTerminated(publisher2);
            gatedGroup.release.countDown();
            publisher1.join(TimeUnit.SECONDS.toMillis(1));
            publisher2.join(TimeUnit.SECONDS.toMillis(1));

            assertThat(Arrays.asList(first.poll(1, TimeUnit.SECONDS), first.poll(1, TimeUnit.SECONDS))).containsExactly(e1, e2);
            assertThat(Arrays.asList(second.poll(1, TimeUnit.SECONDS), second.poll(1, TimeUnit.SECONDS))).containsExactly(e1,
                    e2);
        } finally {
            gatedGroup.release.countDown();
            gatedGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    void eventWithOneReceiverDoesNotWaitForOrderedHandOff() throws Exception {

        GatedEventExecutorGroup gatedGroup = new GatedEventExecutorGroup(2);
        try {
            DefaultEventBus sut = new DefaultEventBus(gatedGroup);
            LinkedBlockingQueue<Event> all = new LinkedBlockingQueue<>();
            AtomicReference<EventExecutor> allExecutor = new AtomicReference<>();

            sut.subscribe(event -> {
                allExecutor.compareAndSet(null, currentExecutor(gatedGroup));
                all.add(event);
            });
            sut.subscribe(EventB.class, event -> {
            });

            Event probe = new TestEvent(); // received by the first subscriber only
            sut.publish(probe);
            assertThat(all.poll(1, TimeUnit.SECONDS)).isSameAs(probe);
            gatedGroup.arm(allExecutor.get());

            // EventB has two receivers: its publisher holds the order lock while paused in the hand-off
            Thread ordered = new Thread(() -> sut.publish(new EventB()));
            ordered.start();
            assertThat(gatedGroup.entered.await(1, TimeUnit.SECONDS)).isTrue();

            // EventA has one receiver: it must not wait for that lock
            Thread single = new Thread(() -> sut.publish(new EventA()));
            single.start();
            single.join(TimeUnit.SECONDS.toMillis(1));
            assertThat(single.isAlive()).isFalse();

            gatedGroup.release.countDown();
            ordered.join(TimeUnit.SECONDS.toMillis(1));
        } finally {
            gatedGroup.release.countDown();
            gatedGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    void subscribeAndCloseApplyToEventTypesPublishedBefore() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<Event> first = new ArrayBlockingQueue<>(8);
        ArrayBlockingQueue<Event> second = new ArrayBlockingQueue<>(8);

        Subscription firstSubscription = sut.subscribe(EventA.class, first::add);
        Event before = new EventA();
        sut.publish(before);
        assertThat(first.poll(1, TimeUnit.SECONDS)).isSameAs(before);

        sut.subscribe(EventA.class, second::add);
        firstSubscription.close();
        Event after = new EventA();
        sut.publish(after);

        assertThat(second.poll(1, TimeUnit.SECONDS)).isSameAs(after);
        assertThat(first.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void subscribeBySupertypeReceivesSubtypeEvents() throws Exception {

        DefaultEventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<EventA> received = new ArrayBlockingQueue<>(8);

        sut.subscribe(EventA.class, received::add);
        EventA expected = new SpecialEventA();
        sut.publish(expected);

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(expected);
    }

    private static void awaitWaitingOrTerminated(Thread thread) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.BLOCKED || state == Thread.State.TERMINATED) {
                return;
            }
            Thread.sleep(1);
        }
        throw new AssertionError("Thread did not wait or finish: " + thread.getState());
    }

    private static EventExecutor currentExecutor(EventExecutorGroup group) {

        for (EventExecutor executor : group) {
            if (executor.inEventLoop()) {
                return executor;
            }
        }
        return null;
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Executor group whose armed executor pauses once, right after accepting a task, until {@link #release} is counted down.
     */
    static class GatedEventExecutorGroup extends DefaultEventExecutorGroup {

        final AtomicBoolean armed = new AtomicBoolean();

        final CountDownLatch entered = new CountDownLatch(1);

        final CountDownLatch release = new CountDownLatch(1);

        volatile EventExecutor gated;

        GatedEventExecutorGroup(int nThreads) {
            super(nThreads);
        }

        void arm(EventExecutor executor) {
            this.gated = executor;
            this.armed.set(true);
        }

        @Override
        protected EventExecutor newChild(Executor executor, Object... args) {
            return new GatedEventExecutor(this, executor, (Integer) args[0], (RejectedExecutionHandler) args[1]);
        }

    }

    /**
     * Same run loop as netty's (final) {@code DefaultEventExecutor}, plus the pause in {@link #execute(Runnable)}.
     */
    static class GatedEventExecutor extends SingleThreadEventExecutor {

        GatedEventExecutor(EventExecutorGroup parent, Executor executor, int maxPendingTasks,
                RejectedExecutionHandler rejectedHandler) {
            super(parent, executor, true, maxPendingTasks, rejectedHandler);
        }

        @Override
        protected void run() {
            for (;;) {
                Runnable task = takeTask();
                if (task != null) {
                    runTask(task);
                    updateLastExecutionTime();
                }

                if (confirmShutdown()) {
                    break;
                }
            }
        }

        @Override
        public void execute(Runnable task) {

            super.execute(task);

            GatedEventExecutorGroup group = (GatedEventExecutorGroup) parent();
            if (group.gated == this && group.armed.compareAndSet(true, false)) {
                group.entered.countDown();
                awaitQuietly(group.release);
            }
        }

    }

    static class TestEvent implements Event {
    }

    @Test
    void listenerExceptionDoesNotCancelSubscription() throws Exception {

        EventBus sut = new DefaultEventBus(group);
        ArrayBlockingQueue<Event> received = new ArrayBlockingQueue<>(5);
        AtomicBoolean firstEvent = new AtomicBoolean(true);

        Subscription subscription = sut.subscribe(e -> {
            if (firstEvent.compareAndSet(true, false)) {
                throw new IllegalStateException("listener failure");
            }
            received.add(e);
        });

        sut.publish(new EventA()); // listener throws on this one, subscription must survive
        EventB expected = new EventB();
        sut.publish(expected); // must still be delivered

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(expected);
        subscription.close();
    }

    static class EventA implements Event {
    }

    static class EventB implements Event {
    }

    static class SpecialEventA extends EventA {
    }

    static class IndexedEvent implements Event {

        final int index;

        IndexedEvent(int index) {
            this.index = index;
        }

    }

}
