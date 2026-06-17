package io.lettuce.core.event;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.lettuce.core.Subscription;
import io.lettuce.core.event.jfr.EventRecorder;
import io.lettuce.core.internal.LettuceAssert;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default implementation for an {@link EventBus}. Events are recorded through {@link EventRecorder#record(Event) EventRecorder}
 * and dispatched to subscribers on an {@link EventExecutorGroup}. Each subscription is pinned to a single {@link EventExecutor}
 * so its events are delivered in order; a subscriber that cannot keep up drops events (bounded by
 * {@code maxInFlightPerSubscription}) rather than applying back-pressure to the publishing thread.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class DefaultEventBus implements EventBus {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEventBus.class);

    static final int DEFAULT_MAX_IN_FLIGHT = 1024;

    private final EventExecutorGroup eventExecutorGroup;

    private final int maxInFlightPerSubscription;

    private final EventRecorder recorder = EventRecorder.getInstance();

    private final CopyOnWriteArrayList<EventSubscription> subscriptions = new CopyOnWriteArrayList<>();

    public DefaultEventBus(EventExecutorGroup eventExecutorGroup) {
        this(eventExecutorGroup, DEFAULT_MAX_IN_FLIGHT);
    }

    public DefaultEventBus(EventExecutorGroup eventExecutorGroup, int maxInFlightPerSubscription) {
        LettuceAssert.notNull(eventExecutorGroup, "EventExecutorGroup must not be null");
        LettuceAssert.isTrue(maxInFlightPerSubscription > 0, "maxInFlightPerSubscription must be greater than 0");
        this.eventExecutorGroup = eventExecutorGroup;
        this.maxInFlightPerSubscription = maxInFlightPerSubscription;
    }

    @Override
    public void publish(Event event) {

        recorder.record(event);

        for (EventSubscription subscription : subscriptions) {
            subscription.dispatch(event);
        }
    }

    @Override
    public Subscription subscribe(Consumer<Event> listener) {

        LettuceAssert.notNull(listener, "Listener must not be null");

        EventSubscription subscription = new EventSubscription(listener, eventExecutorGroup.next());
        subscriptions.add(subscription);
        return subscription;
    }

    /**
     * A single subscription, pinned to one {@link EventExecutor} for in-order delivery and bounded by an in-flight counter.
     */
    private class EventSubscription implements Subscription {

        private final Consumer<Event> listener;

        private final EventExecutor executor;

        private final AtomicInteger inFlight = new AtomicInteger();

        private final AtomicBoolean closed = new AtomicBoolean();

        EventSubscription(Consumer<Event> listener, EventExecutor executor) {
            this.listener = listener;
            this.executor = executor;
        }

        void dispatch(Event event) {

            if (closed.get()) {
                return;
            }

            if (inFlight.incrementAndGet() > maxInFlightPerSubscription) {
                inFlight.decrementAndGet();
                logger.warn("Dropping event {} for a slow event bus subscriber ({} in-flight events exceeded)",
                        event.getClass().getName(), maxInFlightPerSubscription);
                return;
            }

            try {
                executor.execute(() -> {
                    try {
                        if (!closed.get()) {
                            listener.accept(event);
                        }
                    } catch (Throwable t) {
                        logger.warn("Event bus listener threw while handling {}", event.getClass().getName(), t);
                    } finally {
                        inFlight.decrementAndGet();
                    }
                });
            } catch (RejectedExecutionException e) {
                inFlight.decrementAndGet();
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                subscriptions.remove(this);
            }
        }

    }

}
