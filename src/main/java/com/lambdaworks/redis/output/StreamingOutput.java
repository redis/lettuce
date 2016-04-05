package com.lambdaworks.redis.output;

/**
 * Implementors of this class support a streaming {@link CommandOutput} while the command is still processed. The receiving
 * {@link Subscriber} receives {@link Subscriber#onNext(Object)} calls while the command is active.
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
    interface Subscriber<T> {

        /**
         * Data notification sent by the {@link StreamingOutput}.
         *
         * @param t element
         */
        void onNext(T t);
    }

}
