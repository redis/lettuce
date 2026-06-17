package io.lettuce.core.event;

import java.util.function.Consumer;

import io.lettuce.core.Subscription;
import io.lettuce.core.internal.LettuceAssert;
import reactor.core.publisher.Flux;

/**
 * Interface for an EventBus. Events can be published over the bus that are delivered to the subscribers.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public interface EventBus {

    /**
     * Subscribe to the event bus and {@link Event}s. The {@link Flux} drops events on backpressure to avoid contention.
     *
     * @return the observable to obtain events.
     * @deprecated since 7.7, use {@link #subscribe(Consumer)} or {@link #subscribe(Class, Consumer)} instead; scheduled for
     *             removal in Lettuce 8.0 (when {@code reactor-core} becomes optional). To obtain a {@link Flux} from the
     *             callback API, bridge it yourself:
     *
     *             <pre class="code">
     *             Flux.create(sink -&gt; {
     *             Subscription s = eventBus.subscribe(sink::next);
     *             sink.onDispose(s::close);
     *             }, FluxSink.OverflowStrategy.DROP);
     *             </pre>
     */
    @Deprecated
    Flux<Event> get();

    /**
     * Publish a {@link Event} to the bus.
     *
     * @param event the event to publish
     */
    void publish(Event event);

    /**
     * Subscribe to all {@link Event}s published to the bus. The {@code listener} is invoked for every event until the returned
     * {@link Subscription} is {@link Subscription#close() closed}. Events are dropped on backpressure to avoid contention.
     *
     * @param listener callback invoked with each published event, must not be {@code null}.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 7.7
     */
    default Subscription subscribe(Consumer<Event> listener) {
        LettuceAssert.notNull(listener, "Listener must not be null");
        reactor.core.Disposable disposable = get().subscribe(listener);
        return disposable::dispose;
    }

    /**
     * Subscribe to {@link Event}s of a given {@code type} published to the bus. The {@code listener} is invoked for every event
     * assignable to {@code type} until the returned {@link Subscription} is {@link Subscription#close() closed}.
     *
     * @param type the event type to receive, must not be {@code null}.
     * @param listener callback invoked with each matching event, must not be {@code null}.
     * @param <T> the event type.
     * @return a {@link Subscription} that stops delivery when closed.
     * @since 7.7
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
