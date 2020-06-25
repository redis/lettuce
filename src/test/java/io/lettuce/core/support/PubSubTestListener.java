/*
 * Copyright 2016-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
