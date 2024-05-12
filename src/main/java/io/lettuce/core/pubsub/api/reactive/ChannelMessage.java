package io.lettuce.core.pubsub.api.reactive;

/**
 * Message payload for a subscription to a channel.
 *
 * @author Mark Paluch
 */
public class ChannelMessage<K, V> {

    private final K channel;

    private final V message;

    /**
     *
     * @param channel the channel
     * @param message the message
     */
    public ChannelMessage(K channel, V message) {
        this.channel = channel;
        this.message = message;
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
