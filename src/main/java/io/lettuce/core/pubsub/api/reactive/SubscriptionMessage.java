package io.lettuce.core.pubsub.api.reactive;

/**
 * Message payload for a subscription to a pattern or channel or shared
 *
 * @author Ko Su
 * @since 8.0
 */
public class SubscriptionMessage<K, V> {

    /**
     * Can be {@code null}.
     */
    private final K pattern;

    private final K channel;

    private final V message;

    /**
     *
     * @param pattern the pattern
     * @param channel the channel
     * @param message the message
     */
    public SubscriptionMessage(K pattern, K channel, V message) {
        this.pattern = pattern;
        this.channel = channel;
        this.message = message;
    }

    /**
     *
     * @param channel the channel
     * @param message the message
     */
    public SubscriptionMessage(K channel, V message) {
        this.pattern = null;
        this.channel = channel;
        this.message = message;
    }

    /**
     *
     * @return the pattern
     */
    public K getPattern() {
        return pattern;
    }

    /**
     *
     * @return the channel
     */
    public K getChannel() {
        return channel;
    }

    /**
     *
     * @return the message
     */
    public V getMessage() {
        return message;
    }

}
