/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

/**
 * Test listener that routes each received pub/sub message into a per-channel {@link BlockingQueue}, preserving the
 * channel/pattern/message tuple per delivery. This is the alternative to {@link PubSubTestListener} for tests that need to
 * assert which message arrived on which channel — typically multi-channel pattern subscriptions, exact-match channel routing,
 * or binary-channel assertions where {@link PubSubTestListener}'s independent {@code channels}/{@code messages} queues lose the
 * pairing.
 * <p>
 * Per-channel queues also enable cheap negative assertions such as {@link #expectNoMessageOn(Object, long, TimeUnit)}.
 * <p>
 * For {@code byte[]} key types the per-channel map keys are wrapped in {@link ByteBuffer} so equality is content-based.
 *
 * @param <K> Key type. Typically {@link String} or {@code byte[]}.
 * @param <V> Value type. Typically {@link String} or {@code byte[]}.
 * @author Aleksandar Todorov
 */
public class CapturingPubSubListener<K, V> extends RedisPubSubAdapter<K, V> {

    /** Default per-call timeout for {@link #expectMessageOn(Object)}. */
    public static final long DEFAULT_AWAIT_MILLIS = 10_000;

    private final ConcurrentMap<Object, BlockingQueue<Notification<K, V>>> byChannel = new ConcurrentHashMap<>();

    @Override
    public void message(K channel, V message) {
        queueFor(channel).add(new Notification<>(null, channel, message));
    }

    @Override
    public void message(K pattern, K channel, V message) {
        queueFor(channel).add(new Notification<>(pattern, channel, message));
    }

    /** Wait for the next message on {@code channel}, up to {@link #DEFAULT_AWAIT_MILLIS} ms. */
    public Notification<K, V> expectMessageOn(K channel) {
        return expectMessageOn(channel, DEFAULT_AWAIT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /** Wait for the next message on {@code channel}, up to {@code timeout} {@code unit}. */
    public Notification<K, V> expectMessageOn(K channel, long timeout, TimeUnit unit) {
        try {
            Notification<K, V> n = queueFor(channel).poll(timeout, unit);
            if (n == null) {
                throw new AssertionError(
                        "did not receive notification on channel '" + channel + "' within " + timeout + " " + unit);
            }
            return n;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for notification on channel '" + channel + "'", e);
        }
    }

    /** Assert that no message arrives on {@code channel} within {@code timeout} {@code unit}. */
    public void expectNoMessageOn(K channel, long timeout, TimeUnit unit) {
        try {
            Notification<K, V> n = queueFor(channel).poll(timeout, unit);
            if (n != null) {
                throw new AssertionError(
                        "expected no message on channel '" + channel + "' but received message: " + n.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for absence of notification on channel '" + channel + "'", e);
        }
    }

    /** Drop all captured notifications. */
    public void clear() {
        byChannel.clear();
    }

    private BlockingQueue<Notification<K, V>> queueFor(K channel) {
        return byChannel.computeIfAbsent(keyOf(channel), k -> LettuceFactories.newBlockingQueue());
    }

    private static Object keyOf(Object channel) {
        if (channel instanceof byte[]) {
            return ByteBuffer.wrap((byte[]) channel);
        }
        return channel;
    }

    /** Captured pub/sub event carrying the originating pattern (when any), channel, and message together. */
    public static final class Notification<K, V> {

        private final K pattern;

        private final K channel;

        private final V message;

        Notification(K pattern, K channel, V message) {
            this.pattern = pattern;
            this.channel = channel;
            this.message = message;
        }

        public K getPattern() {
            return pattern;
        }

        public K getChannel() {
            return channel;
        }

        public V getMessage() {
            return message;
        }

    }

}
