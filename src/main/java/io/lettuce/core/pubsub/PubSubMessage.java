package io.lettuce.core.pubsub;

/**
 * Represents a Pub/Sub notification message.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface PubSubMessage<K, V> {

    /**
     * @return the {@link PubSubOutput.Type} message type.
     */
    PubSubOutput.Type type();

    /**
     * @return name of the channel to which this notification belongs to.
     */
    K channel();

    /**
     * @return pattern that applies if the message was received through a pattern subscription. Can be {@code null}.
     */
    K pattern();

    /**
     * @return the subscriber count if applicable.
     */
    long count();

    /**
     * @return the message body, if applicable. Can be {@code null}.
     */
    V body();

}
