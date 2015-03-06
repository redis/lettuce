// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

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
