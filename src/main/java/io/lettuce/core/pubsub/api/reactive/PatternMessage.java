package io.lettuce.core.pubsub.api.reactive;

/**
 * Message payload for a subscription to a pattern.
 *
 * @author Mark Paluch
 *
 * @deprecated use {@link io.lettuce.core.pubsub.api.reactive.SubscriptionMessage} instead.
 */
@Deprecated
public class PatternMessage<K, V> {

    private final K pattern;

    private final K channel;

    private final V message;

    /**
     *
     * @param pattern the pattern
     * @param channel the channel
     * @param message the message
     */
    public PatternMessage(K pattern, K channel, V message) {
        this.pattern = pattern;
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
