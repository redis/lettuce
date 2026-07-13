package io.lettuce.core.event;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
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
 * and dispatched to subscribers on an {@link EventExecutorGroup}. Each subscription is pinned to a single
 * {@link EventExecutor}, so events published from a single thread are delivered to that subscriber in publication order;
 * ordering across concurrent publishers, and relative to {@link EventRecorder}, is not guaranteed. A subscriber that cannot
 * keep up drops events (bounded by {@code maxInFlightPerSubscription}) rather than applying back-pressure to the publishing
 * thread.
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

    /**
     * Creates a new {@link DefaultEventBus} that dispatches events on the given {@link EventExecutorGroup}, using the default
     * per-subscription in-flight bound.
     *
     * @param eventExecutorGroup the executor group on which events are dispatched to subscribers, must not be {@code null}.
     */
    public DefaultEventBus(EventExecutorGroup eventExecutorGroup) {
        this(eventExecutorGroup, DEFAULT_MAX_IN_FLIGHT);
    }

    /**
     * Creates a new {@link DefaultEventBus} that dispatches events on the given {@link EventExecutorGroup}, dropping events for
     * a subscriber once more than {@code maxInFlightPerSubscription} of its events are awaiting delivery.
     *
     * @param eventExecutorGroup the executor group on which events are dispatched to subscribers, must not be {@code null}.
     * @param maxInFlightPerSubscription the number of events that may await delivery per subscriber before further events are
     *        dropped, must be greater than {@code 0}.
     */
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

        return register(null, listener);
    }

    @Override
    public <T extends Event> Subscription subscribe(Class<T> type, Consumer<T> listener) {

        LettuceAssert.notNull(type, "Event type must not be null");
        LettuceAssert.notNull(listener, "Listener must not be null");

        return register(type, event -> listener.accept(type.cast(event)));
    }

    private Subscription register(Class<? extends Event> type, Consumer<Event> listener) {

        EventSubscription subscription = new EventSubscription(type, listener, eventExecutorGroup.next());
        subscriptions.add(subscription);
        return subscription;
    }

    /**
     * A single subscription, pinned to one {@link EventExecutor} for in-order delivery and bounded by an in-flight counter.
     */
    private class EventSubscription implements Subscription {

        private final Class<? extends Event> type;

        private final Consumer<Event> listener;

        private final EventExecutor executor;

        private final AtomicInteger inFlight = new AtomicInteger();

        private volatile boolean closed;

        EventSubscription(Class<? extends Event> type, Consumer<Event> listener, EventExecutor executor) {
            this.type = type;
            this.listener = listener;
            this.executor = executor;
        }

        void dispatch(Event event) {

            if (closed) {
                return;
            }

            if (type != null && !type.isInstance(event)) {
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
                        if (!closed) {
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
            if (closed) {
                return;
            }
            closed = true;
            subscriptions.remove(this);
        }

    }

}
