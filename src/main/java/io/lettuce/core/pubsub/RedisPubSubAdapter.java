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
package io.lettuce.core.pubsub;

/**
 * Convenience adapter with an empty implementation of all {@link RedisPubSubListener} callback methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class RedisPubSubAdapter<K, V> implements RedisPubSubListener<K, V> {

    @Override
    public void message(K channel, V message) {
        // empty adapter method
    }

    @Override
    public void message(K pattern, K channel, V message) {
        // empty adapter method
    }

    @Override
    public void subscribed(K channel, long count) {
        // empty adapter method
    }

    @Override
    public void psubscribed(K pattern, long count) {
        // empty adapter method
    }

    @Override
    public void unsubscribed(K channel, long count) {
        // empty adapter method
    }

    @Override
    public void punsubscribed(K pattern, long count) {
        // empty adapter method
    }

}
