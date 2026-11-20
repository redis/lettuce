package io.lettuce.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
 * and delivered to subscribers on an {@link EventExecutorGroup}. Each subscription is pinned to a single {@link EventExecutor},
 * which invokes its listener.
 * <p>
 * All subscribers receive events in the same order, including events published concurrently from different threads. Order
 * relative to {@link EventRecorder} is not guaranteed. Publishing never waits for another publisher: when another thread is
 * already handing events to the subscribers, a publisher only adds its event to a shared queue and returns. Listeners are never
 * invoked by the publishing thread, unless the {@link EventExecutorGroup} runs tasks on the calling thread.
 * <p>
 * A subscriber that cannot keep up drops events once its limit of events waiting or being delivered is reached, rather than
 * applying back-pressure to the publishing thread. The limit defaults to {@code maxInFlightPerSubscription}, and the newest
 * event is dropped by default. Each subscription can set its own limit and {@link OverflowStrategy} through
 * {@link EventSubscriptionOptions}. Dropped events are logged at {@code DEBUG} level.
 *
 * @author Mark Paluch
 * @author Aleksandar Todorov
 * @since 3.4
 */
public class DefaultEventBus implements EventBus {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEventBus.class);

    static final int DEFAULT_MAX_IN_FLIGHT = 1024;

    /**
     * Number of events a subscription delivers in one task before it yields its {@link EventExecutor} to other tasks.
     */
    static final int DELIVERY_BATCH_SIZE = 64;

    private static final EventSubscription[] NO_SUBSCRIPTIONS = new EventSubscription[0];

    private final EventExecutorGroup eventExecutorGroup;

    private final int maxInFlightPerSubscription;

    private final EventRecorder recorder = EventRecorder.getInstance();

    private final CopyOnWriteArrayList<EventSubscription> subscriptions = new CopyOnWriteArrayList<>();

    /**
     * Concrete event class to the subscriptions that receive it. Filled on first distribution of a class and replaced as a
     * whole after every subscribe and close.
     */
    private volatile ConcurrentHashMap<Class<?>, EventSubscription[]> receiversByType = new ConcurrentHashMap<>();

    /**
     * Published events that are not yet distributed to the subscriptions. Shared by all publishers, so its order is the one
     * order that every subscriber sees.
     */
    private final Queue<Event> published = new ConcurrentLinkedQueue<>();

    /**
     * Number of publishes not yet accounted for by a distribution pass. The publisher that moves it from {@code 0} distributes;
     * all others only add their event to {@link #published}.
     */
    private final AtomicInteger distributing = new AtomicInteger();

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
     * a subscriber once {@code maxInFlightPerSubscription} of its events are waiting or being delivered, unless the
     * subscription sets its own limit through {@link EventSubscriptionOptions}.
     *
     * @param eventExecutorGroup the executor group on which events are dispatched to subscribers, must not be {@code null}.
     * @param maxInFlightPerSubscription the default number of events that may wait or be in delivery per subscriber before
     *        events are dropped, must be greater than {@code 0}.
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

        if (subscriptions.isEmpty()) {
            return;
        }

        published.offer(event);

        if (distributing.getAndIncrement() != 0) {
            // another thread is distributing and will also distribute this event
            return;
        }

        // Subscriptions whose delivery must be started. Started only after this thread gives up distributing, so that a
        // listener
        // invoked by an executor that runs tasks on the calling thread never holds up distribution: other publishers then
        // distribute themselves, and each subscription's limit keeps applying.
        List<EventSubscription> toSchedule = null;

        int missed = 1;
        for (;;) {

            Event next;
            while ((next = published.poll()) != null) {
                toSchedule = distribute(next, toSchedule);
            }

            missed = distributing.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }

        if (toSchedule != null) {
            for (EventSubscription subscription : toSchedule) {
                subscription.schedule();
            }
        }
    }

    /**
     * Adds {@code event} to the queue of every subscription that receives it, and collects the subscriptions whose delivery
     * must be started. Never throws, so that a distribution pass always completes and {@link #distributing} returns to
     * {@code 0}.
     */
    private List<EventSubscription> distribute(Event event, List<EventSubscription> toSchedule) {

        try {
            for (EventSubscription subscription : receiversByType.computeIfAbsent(event.getClass(), this::receiversOf)) {
                if (subscription.enqueue(event)) {
                    if (toSchedule == null) {
                        toSchedule = new ArrayList<>(2);
                    }
                    toSchedule.add(subscription);
                }
            }
        } catch (Throwable t) {
            logger.warn("Event bus failed to distribute {}", event.getClass().getName(), t);
        }
        return toSchedule;
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
    public Subscription subscribe(Consumer<Event> listener, EventSubscriptionOptions options) {

        LettuceAssert.notNull(listener, "Listener must not be null");
        LettuceAssert.notNull(options, "EventSubscriptionOptions must not be null");

        return register(null, listener, options);
    }

    @Override
    public <T extends Event> Subscription subscribe(Class<T> type, Consumer<T> listener, EventSubscriptionOptions options) {

        LettuceAssert.notNull(type, "Event type must not be null");
        LettuceAssert.notNull(listener, "Listener must not be null");
        LettuceAssert.notNull(options, "EventSubscriptionOptions must not be null");

        return register(type, event -> listener.accept(type.cast(event)), options);
    }

    private Subscription register(Class<? extends Event> type, Consumer<Event> listener, EventSubscriptionOptions options) {

        EventSubscription subscription = new EventSubscription(type, listener, eventExecutorGroup.next(),
                options.maxInFlight().orElse(maxInFlightPerSubscription),
                options.overflowStrategy().orElse(OverflowStrategy.DROP_NEWEST));
        subscriptions.add(subscription);
        invalidateReceivers();
        return subscription;
    }

    /**
     * Creates the queue of one subscription. Must be safe for one writer (the distributing thread) and one reader (the delivery
     * task) at the same time. Package-private so that tests can observe the queue.
     */
    Queue<Event> newSubscriptionQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    /**
     * A single subscription: its own queue of events, bounded by its own limit and overflow strategy, delivered in queue order
     * by at most one task at a time on its {@link EventExecutor}.
     */
    private class EventSubscription implements Subscription {

        private final Class<? extends Event> type;

        private final Consumer<Event> listener;

        private final EventExecutor executor;

        private final int maxInFlight;

        private final OverflowStrategy overflowStrategy;

        private final Queue<Event> queue = newSubscriptionQueue();

        /**
         * Events waiting in {@link #queue} plus the event being delivered.
         */
        private final AtomicInteger inFlight = new AtomicInteger();

        /**
         * Number of enqueues not yet accounted for by {@link #deliver()}. The enqueue that moves it from {@code 0} schedules
         * delivery.
         */
        private final AtomicInteger scheduled = new AtomicInteger();

        private volatile boolean closed;

        EventSubscription(Class<? extends Event> type, Consumer<Event> listener, EventExecutor executor, int maxInFlight,
                OverflowStrategy overflowStrategy) {
            this.type = type;
            this.listener = listener;
            this.executor = executor;
            this.maxInFlight = maxInFlight;
            this.overflowStrategy = overflowStrategy;
        }

        boolean receives(Class<?> eventType) {
            return !closed && (type == null || type.isAssignableFrom(eventType));
        }

        /**
         * Adds {@code event} to this subscription's queue, applying the {@link OverflowStrategy} when the queue is full. Called
         * only by the distributing thread.
         *
         * @return {@code true} if the caller must {@link #schedule()} delivery, because no delivery is scheduled yet.
         */
        boolean enqueue(Event event) {

            if (closed) {
                return false;
            }

            if (overflowStrategy == OverflowStrategy.DROP_NEWEST) {
                if (inFlight.incrementAndGet() > maxInFlight) {
                    inFlight.decrementAndGet();
                    logDropped(event);
                    return false;
                }
                queue.offer(event);
            } else {
                queue.offer(event);
                if (inFlight.incrementAndGet() > maxInFlight) {
                    Event oldest = queue.poll();
                    if (oldest != null) {
                        inFlight.decrementAndGet();
                        logDropped(oldest);
                    }
                }
            }

            return scheduled.getAndIncrement() == 0;
        }

        void schedule() {
            try {
                executor.execute(this::deliver);
            } catch (RejectedExecutionException e) {

                if (executor.isShuttingDown()) {
                    // nothing can be delivered any more
                    queue.clear();
                    inFlight.set(0);
                    scheduled.set(0);
                    return;
                }

                // the executor is only full for now: keep the accepted events, they are delivered once the next event
                // for this subscription schedules delivery again
                scheduled.set(0);
                logger.warn("Event bus could not schedule delivery of {} waiting events for a subscriber: {}", queue.size(),
                        e.toString());
            }
        }

        /**
         * Delivers queued events in order. Runs as one task at a time; after {@link #DELIVERY_BATCH_SIZE} events it schedules
         * itself again so that other tasks on the same executor are not starved.
         */
        private void deliver() {

            int missed = scheduled.get();
            int delivered = 0;

            for (;;) {

                for (;;) {

                    if (delivered >= DELIVERY_BATCH_SIZE) {
                        // yield before taking the next event: keep the scheduled count above 0 so that no one else
                        // schedules, and continue in a new task. Checked before every poll, so events that arrive later
                        // cannot make this task skip the yield.
                        schedule();
                        return;
                    }

                    Event event = queue.poll();
                    if (event == null) {
                        break;
                    }

                    try {
                        if (!closed) {
                            listener.accept(event);
                        }
                    } catch (Throwable t) {
                        logger.warn("Event bus listener threw while handling {}", event.getClass().getName(), t);
                    } finally {
                        inFlight.decrementAndGet();
                    }
                    delivered++;
                }

                missed = scheduled.addAndGet(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }

        private void logDropped(Event event) {
            if (logger.isDebugEnabled()) {
                logger.debug("Dropping event {} for a slow event bus subscriber ({} in-flight events exceeded, {})",
                        event.getClass().getName(), maxInFlight, overflowStrategy);
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
