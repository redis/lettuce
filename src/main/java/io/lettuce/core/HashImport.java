/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.AsyncCommand;

import io.netty.channel.Channel;

/**
 * Immutable template describing the shared field names of a hash import fieldset used by the {@code HIMPORT} command family.
 * <p>
 * A {@link HashImport} bundles a generated fieldset {@code name} with the ordered field names shared by many hashes. It is
 * created once and reused across many {@code himportSet} calls, each of which sends only the values, positionally paired to
 * these fields. The field names are declared to the server transparently on first use per physical connection, so callers never
 * issue a prepare step themselves.
 * <p>
 * Instances are created through {@link #of(String...)} (which auto-generates a {@code himport:<seq>} name for
 * {@link String}-keyed connections) or {@link #of(Function, Object...)} (which derives the name from a caller-supplied
 * function, for arbitrary key types). Field names are validated at creation time to be non-empty and free of duplicates.
 * <p>
 * A {@link HashImport} is safe to share across threads. Closing it (via {@link #close()} or try-with-resources) sends a
 * best-effort {@code HIMPORT DISCARD} to every connection it was prepared on and marks it discarded so it can no longer be used
 * for further imports.
 *
 * @param <K> Key type, matching the field-name type of the connection's codec.
 * @author Aleksandar Todorov
 * @since 7.7
 */
@Experimental
public class HashImport<K> implements AutoCloseable {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private static final String NAME_PREFIX = "himport:";

    private final K name;

    private final K[] fields;

    /**
     * The connections this fieldset has been prepared on, tracked weakly so idle/closed connections are collectible. Populated
     * on the write path when {@code HIMPORT PREPARE} is injected; consulted only by {@link #close()} to target cleanup. The
     * value is the codec of the connection, used to encode the {@code DISCARD}.
     */
    private final Map<Channel, RedisCodec<K, ?>> preparedOn = new WeakHashMap<>();

    private volatile boolean discarded;

    private HashImport(K name, K[] fields) {
        this.name = name;
        this.fields = fields;
    }

    /**
     * Create a {@link HashImport} for {@link String}-keyed connections with an auto-generated {@code himport:<seq>} name.
     *
     * @param fields the ordered field names shared by the imported hashes, must not be {@code null}, empty, or contain
     *        duplicates.
     * @return a new {@link HashImport}.
     * @since 7.7
     */
    public static HashImport<String> of(String... fields) {
        return of(seq -> NAME_PREFIX + seq, fields);
    }

    /**
     * Create a {@link HashImport} for arbitrary key types, deriving the fieldset name from {@code idCodec} applied to a
     * generated sequence number.
     *
     * @param idCodec function mapping a generated sequence number to a fieldset name of key type {@code K}, must not be
     *        {@code null}.
     * @param fields the ordered field names shared by the imported hashes, must not be {@code null}, empty, or contain
     *        duplicates.
     * @param <K> Key type.
     * @return a new {@link HashImport}.
     * @since 7.7
     */
    @SafeVarargs
    public static <K> HashImport<K> of(Function<Long, K> idCodec, K... fields) {

        LettuceAssert.notNull(idCodec, "Id codec must not be null");
        LettuceAssert.notNull(fields, "Fields must not be null");
        LettuceAssert.isTrue(fields.length > 0, "Fields must not be empty");

        Set<K> seen = new HashSet<>(fields.length);
        for (K field : fields) {
            LettuceAssert.notNull(field, "Field must not be null");
            if (!seen.add(field)) {
                throw new IllegalArgumentException("Fields must not contain duplicates: " + field);
            }
        }

        K name = idCodec.apply(SEQUENCE.incrementAndGet());
        LettuceAssert.notNull(name, "Generated fieldset name must not be null");

        return new HashImport<>(name, fields.clone());
    }

    /**
     * @return the generated fieldset name sent to the server.
     * @since 7.7
     */
    public K name() {
        return name;
    }

    /**
     * @return a copy of the ordered field names.
     * @since 7.7
     */
    public K[] fields() {
        return fields.clone();
    }

    /**
     * @return the number of fields, matching the required number of values per {@code himportSet}.
     * @since 7.7
     */
    public int size() {
        return fields.length;
    }

    /**
     * @return {@code true} if this fieldset has been {@link #close() closed} and must no longer be used.
     * @since 7.7
     */
    public boolean isDiscarded() {
        return discarded;
    }

    /**
     * Record that this fieldset was prepared on {@code channel}, so {@link #close()} can target it with a {@code DISCARD}.
     * Called on the connection's event loop from the outbound write path. No-op once discarded.
     *
     * @param channel the connection the {@code HIMPORT PREPARE} was injected on.
     * @param codec the connection's codec, used to encode the eventual {@code DISCARD}.
     */
    void registerConnection(Channel channel, RedisCodec<K, ?> codec) {
        synchronized (preparedOn) {
            if (!discarded) {
                preparedOn.put(channel, codec);
            }
        }
    }

    /**
     * Discard this fieldset: send a best-effort {@code HIMPORT DISCARD} to every still-active connection it was prepared on and
     * mark it discarded so it can no longer be used for imports. Cleanup is fire-and-forget — failures are ignored and a
     * {@code DISCARD} that lands on a rotated connection is a harmless no-op, since the fieldset name is globally unique. It is
     * not required for correctness (server-side state also dies with the connection) but releases {@code maxmemory-clients}
     * pressure promptly on long-lived and pooled connections. Idempotent: a second call does nothing.
     *
     * @since 7.7
     */
    @Override
    public void close() {

        Map<Channel, RedisCodec<K, ?>> targets;
        synchronized (preparedOn) {
            if (discarded) {
                return;
            }
            discarded = true;
            targets = new IdentityHashMap<>(preparedOn);
            preparedOn.clear();
        }

        for (Map.Entry<Channel, RedisCodec<K, ?>> target : targets.entrySet()) {
            Channel channel = target.getKey();
            if (channel != null && channel.isActive()) {
                safeDiscard(channel, target.getValue());
            }
        }
    }

    private <V> void safeDiscard(Channel channel, RedisCodec<K, V> codec) {
        try {
            RedisCommandBuilder<K, V> commandBuilder = new RedisCommandBuilder<>(codec);
            channel.writeAndFlush(new AsyncCommand<>(commandBuilder.himportDiscard(this)));
        } catch (RuntimeException ignore) {
            // best-effort cleanup: the fieldset dies with the connection regardless
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", fields=" + Arrays.toString(fields) + ", discarded="
                + discarded + "]";
    }

}
