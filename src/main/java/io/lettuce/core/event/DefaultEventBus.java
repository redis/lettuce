package io.lettuce.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
 * and handed, on the publishing thread, to the subscriptions that receive the event's type. Each subscription is pinned to a
 * single {@link EventExecutor} of the {@link EventExecutorGroup}, which invokes its listener. All subscribers receive events in
 * the same order, including events published concurrently from different threads; publishers of an event that two or more
 * subscriptions receive briefly wait for each other to hand it off. Order relative to {@link EventRecorder} is not guaranteed.
 * <p>
 * A subscriber that cannot keep up drops events (bounded by {@code maxInFlightPerSubscription}) rather than applying
 * back-pressure to the publishing thread. Dropped events are logged at {@code DEBUG} level.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class DefaultEventBus implements EventBus {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEventBus.class);

    static final int DEFAULT_MAX_IN_FLIGHT = 1024;

    private static final EventSubscription[] NO_SUBSCRIPTIONS = new EventSubscription[0];

    private final EventExecutorGroup eventExecutorGroup;

    private final int maxInFlightPerSubscription;

    private final EventRecorder recorder = EventRecorder.getInstance();

    private final CopyOnWriteArrayList<EventSubscription> subscriptions = new CopyOnWriteArrayList<>();

    /**
     * Concrete event class to the subscriptions that receive it. Filled on first publish of a class and replaced as a whole
     * after every subscribe and close. The arrays are never modified, so a publisher uses one array for both the order decision
     * and the hand-off.
     */
    private volatile ConcurrentHashMap<Class<?>, EventSubscription[]> receiversByType = new ConcurrentHashMap<>();

    /**
     * Serializes the hand-off of events that two or more subscriptions receive, so that those subscriptions see them in one
     * order.
     */
    private final Lock orderLock = new ReentrantLock();

    /**
     * Creates a new {@link DefaultEventBus} that dispatches events on the given {@link EventExecutorGroup}, using the default
     * per-subscription in-flight bound.
     *
     * @param eventExecutorGroup the executor group on which events are dispatched to subscribers, must not be {@code null}.
     * @since 8.0
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
     * @since 8.0
     */
    public DefaultEventBus(EventExecutorGroup eventExecutorGroup, int maxInFlightPerSubscription) {
        LettuceAssert.notNull(eventExecutorGroup, "EventExecutorGroup must not be null");
        LettuceAssert.isTrue(maxInFlightPerSubscription > 0, "maxInFlightPerSubscription must be greater than 0");
        this.eventExecutorGroup = eventExecutorGroup;
        this.maxInFlightPerSubscription = maxInFlightPerSubscription;
    }

    @Override
    public void publish(Event event) {

        LettuceAssert.notNull(event, "Event must not be null");

        recorder.record(event);

        EventSubscription[] receivers = receiversByType.computeIfAbsent(event.getClass(), this::receiversOf);

        if (receivers.length < 2) {
            // zero or one receiver: there is no second subscriber that could see a different order
            dispatch(receivers, event);
            return;
        }

        orderLock.lock();
        try {
            dispatch(receivers, event);
        } finally {
            orderLock.unlock();
        }
    }

    private static void dispatch(EventSubscription[] receivers, Event event) {
        for (EventSubscription subscription : receivers) {
            subscription.dispatch(event);
        }
    }

    /**
     * Collects the subscriptions that receive events of {@code eventType}, in subscription order.
     */
    private EventSubscription[] receiversOf(Class<?> eventType) {

        List<EventSubscription> receivers = new ArrayList<>();
        for (EventSubscription subscription : subscriptions) {
            if (subscription.receives(eventType)) {
                receivers.add(subscription);
            }
        }
        return receivers.toArray(NO_SUBSCRIPTIONS);
    }

    /**
     * Drops all cached receiver arrays. Called after every change to {@link #subscriptions}, so that the next lookup is built
     * from the changed list.
     */
    private void invalidateReceivers() {
        receiversByType = new ConcurrentHashMap<>();
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
        invalidateReceivers();
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

        boolean receives(Class<?> eventType) {
            return !closed && (type == null || type.isAssignableFrom(eventType));
        }

        /**
         * Hands {@code event} to this subscription's executor. Callers pass only events whose type this subscription receives.
         */
        void dispatch(Event event) {

            if (closed) {
                return;
            }

            if (inFlight.incrementAndGet() > maxInFlightPerSubscription) {
                inFlight.decrementAndGet();
                if (logger.isDebugEnabled()) {
                    logger.debug("Dropping event {} for a slow event bus subscriber ({} in-flight events exceeded)",
                            event.getClass().getName(), maxInFlightPerSubscription);
                }
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
            invalidateReceivers();
        }

    }

}
