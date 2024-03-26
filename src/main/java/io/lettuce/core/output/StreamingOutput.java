package io.lettuce.core.output;

import java.util.Collection;

/**
 * Implementors of this class support a streaming {@link CommandOutput} while the command is still processed. The receiving
 * {@link Subscriber} receives {@link Subscriber#onNext(Collection, Object)} calls while the command is active.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public interface StreamingOutput<T> {

    /**
     * Sets the {@link Subscriber}.
     *
     * @param subscriber
     */
    void setSubscriber(Subscriber<T> subscriber);

    /**
     * Retrieves the {@link Subscriber}.
     *
     * @return
     */
    Subscriber<T> getSubscriber();

    /**
     * Subscriber to a {@link StreamingOutput}.
     *
     * @param <T>
     */
    abstract class Subscriber<T> {

        /**
         * Data notification sent by the {@link StreamingOutput}.
         *
         * @param t element
         */
        public abstract void onNext(T t);

        /**
         * Data notification sent by the {@link StreamingOutput}.
         *
         * @param outputTarget target
         * @param t element
         */
        public void onNext(Collection<T> outputTarget, T t) {
            onNext(t);
        }

    }

}
