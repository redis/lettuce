/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.pubsub.api.reactive;

/**
 * Message payload for a subscription to a pattern.
 *
 * @author Mark Paluch
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
     * @return the pattern.
     */
    public K getPattern() {
        return pattern;
    }

    /**
     * @return the channel.
     */
    public K getChannel() {
        return channel;
    }

    /**
     * @return the message.
     */
    public V getMessage() {
        return message;
    }

}
