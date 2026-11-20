package io.lettuce.core.event;

import java.util.function.Consumer;

import io.lettuce.core.Subscription;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Interface for an EventBus. Events can be published over the bus that are delivered to the subscribers.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public interface EventBus {

    /**
     * Publishes an {@link Event} to the bus.
     *
     * @param event the event to publish
     */
    void publish(Event event);

    /**
     * Subscribes to all {@link Event}s published to the bus, with delivery settings for this subscription. The {@code listener}
     * is invoked for every event until the returned {@link Subscription} is {@link Subscription#close() closed}. When the
     * {@code listener} cannot keep up, events are dropped for this subscription as {@code options} specify, instead of slowing
     * down the publisher. Settings that {@code options} leaves unset use the defaults of the bus.
     * <p>
     * Exceptions thrown by the {@code listener} are caught and logged so that a single failing invocation does not cancel the
     * subscription or affect delivery to other subscribers. All other {@code subscribe} methods delegate to this one.
     *
     * @param listener callback invoked with each published event, must not be {@code null}.
     * @param options delivery settings for this subscription, must not be {@code null}.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 8.0
     */
    Subscription subscribe(Consumer<Event> listener, EventSubscriptionOptions options);

    /**
     * Subscribes to {@link Event}s of a given {@code type} published to the bus, with delivery settings for this subscription.
     * The {@code listener} is invoked for every event assignable to {@code type} until the returned {@link Subscription} is
     * {@link Subscription#close() closed}. Settings that {@code options} leaves unset use the defaults of the bus.
     * <p>
     * Implementations that bound or drop events per subscriber should override this method to filter by {@code type} before
     * admission, so that non-matching events do not consume a subscriber's delivery capacity.
     *
     * @param type the event type to receive, must not be {@code null}.
     * @param listener callback invoked with each matching event, must not be {@code null}.
     * @param options delivery settings for this subscription, must not be {@code null}.
     * @param <T> the event type.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 8.0
     */
    default <T extends Event> Subscription subscribe(Class<T> type, Consumer<T> listener, EventSubscriptionOptions options) {
        LettuceAssert.notNull(type, "Event type must not be null");
        LettuceAssert.notNull(listener, "Listener must not be null");
        return subscribe(event -> {
            if (type.isInstance(event)) {
                listener.accept(type.cast(event));
            }
        }, options);
    }

    /**
     * Subscribes to all {@link Event}s published to the bus, using the default delivery settings of the bus. Same as
     * {@link #subscribe(Consumer, EventSubscriptionOptions)} with {@link EventSubscriptionOptions#defaults()}.
     *
     * @param listener callback invoked with each published event, must not be {@code null}.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 7.8
     */
    default Subscription subscribe(Consumer<Event> listener) {
        return subscribe(listener, EventSubscriptionOptions.defaults());
    }

    /**
     * Subscribes to {@link Event}s of a given {@code type} published to the bus, using the default delivery settings of the
     * bus. Same as {@link #subscribe(Class, Consumer, EventSubscriptionOptions)} with
     * {@link EventSubscriptionOptions#defaults()}.
     *
     * @param type the event type to receive, must not be {@code null}.
     * @param listener callback invoked with each matching event, must not be {@code null}.
     * @param <T> the event type.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 7.8
     */
    default <T extends Event> Subscription subscribe(Class<T> type, Consumer<T> listener) {
        return subscribe(type, listener, EventSubscriptionOptions.defaults());
    }

}
