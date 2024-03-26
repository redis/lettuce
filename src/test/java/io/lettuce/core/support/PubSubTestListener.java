package io.lettuce.core.support;

import java.util.concurrent.BlockingQueue;

import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.pubsub.RedisPubSubListener;

/**
 * @author Mark Paluch
 */
public class PubSubTestListener implements RedisPubSubListener<String, String> {

    private BlockingQueue<String> channels = LettuceFactories.newBlockingQueue();
    private BlockingQueue<String> patterns = LettuceFactories.newBlockingQueue();
    private BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
    private BlockingQueue<Long> counts = LettuceFactories.newBlockingQueue();

    // RedisPubSubListener implementation

    @Override
    public void message(String channel, String message) {
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        patterns.add(pattern);
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void subscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }

    @Override
    public void unsubscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }

    public BlockingQueue<String> getChannels() {
        return channels;
    }

    public BlockingQueue<String> getPatterns() {
        return patterns;
    }

    public BlockingQueue<String> getMessages() {
        return messages;
    }

    public BlockingQueue<Long> getCounts() {
        return counts;
    }
}
