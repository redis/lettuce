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
     * Publish a {@link Event} to the bus.
     *
     * @param event the event to publish
     */
    void publish(Event event);

    /**
     * Subscribe to all {@link Event}s published to the bus. The {@code listener} is invoked for every event until the returned
     * {@link Subscription} is {@link Subscription#close() closed}. Events are dropped for a subscriber that cannot keep up, to
     * avoid contention.
     *
     * @param listener callback invoked with each published event, must not be {@code null}.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 8.0
     */
    Subscription subscribe(Consumer<Event> listener);

    /**
     * Subscribe to {@link Event}s of a given {@code type} published to the bus. The {@code listener} is invoked for every event
     * assignable to {@code type} until the returned {@link Subscription} is {@link Subscription#close() closed}.
     *
     * @param type the event type to receive, must not be {@code null}.
     * @param listener callback invoked with each matching event, must not be {@code null}.
     * @param <T> the event type.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 8.0
     */
    default <T extends Event> Subscription subscribe(Class<T> type, Consumer<T> listener) {
        LettuceAssert.notNull(type, "Event type must not be null");
        LettuceAssert.notNull(listener, "Listener must not be null");
        return subscribe(event -> {
            if (type.isInstance(event)) {
                listener.accept(type.cast(event));
            }
        });
    }

}
