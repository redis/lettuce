package com.lambdaworks.redis.pubsub.api.rx;

/**
 * Message payload for a subscription to a pattern.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 16.06.15 10:47
 */
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
