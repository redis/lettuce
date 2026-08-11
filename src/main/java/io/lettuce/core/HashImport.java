/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Template describing the shared field names of a hash import fieldset used by the {@code HIMPORT} command family.
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
     * The per-connection {@link HashImportContext} components this fieldset has been prepared on, tracked weakly so idle/closed
     * connections are collectible. Populated on the write path when {@code HIMPORT PREPARE} is injected; consulted only by
     * {@link #close()} to target cleanup. Each context builds and sends the transaction-safe {@code DISCARD} for its
     * connection.
     */
    private final Set<HashImportContext> preparedOn = Collections.newSetFromMap(new WeakHashMap<>());

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
     * @throws IllegalArgumentException if {@code fields} is {@code null}, empty, or contains {@code null} or duplicate
     *         elements.
     * @since 7.7
     */
    public static HashImport<String> of(String... fields) {
        return of(seq -> NAME_PREFIX + seq, fields);
    }

    /**
     * Create a {@link HashImport} for arbitrary key types, deriving the fieldset name from {@code idCodec} applied to a
     * generated sequence number.
     *
     * @param <K> Key type.
     * @param idCodec function mapping a generated sequence number to a fieldset name of key type {@code K}, must not be
     *        {@code null}.
     * @param fields the ordered field names shared by the imported hashes, must not be {@code null}, empty, or contain
     *        duplicates.
     * @return a new {@link HashImport}.
     * @throws IllegalArgumentException if {@code idCodec} is {@code null}, or {@code fields} is {@code null}, empty, or
     *         contains {@code null} or duplicate elements.
     * @since 7.7
     */
    @SafeVarargs
    public static <K> HashImport<K> of(Function<Long, K> idCodec, K... fields) {

        LettuceAssert.notNull(idCodec, "Id codec must not be null");
        LettuceAssert.notNull(fields, "Fields must not be null");
        LettuceAssert.isTrue(fields.length > 0, "Fields must not be empty");

        Set<Object> seen = new HashSet<>(fields.length);
        for (K field : fields) {
            LettuceAssert.notNull(field, "Field must not be null");
            Object key = (field instanceof byte[]) ? ByteBuffer.wrap((byte[]) field) : field;
            if (!seen.add(key)) {
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
     * Record that this fieldset is being prepared on {@code context}, so {@link #close()} can later target it with a
     * {@code DISCARD}. Called on the connection's event loop from the outbound write path, before the {@code PREPARE} is
     * written, and atomically with {@link #close()} on {@code preparedOn}: if the fieldset is already discarded the caller must
     * not send the {@code PREPARE} at all, so no server-side state is ever left without a matching {@code DISCARD}.
     *
     * @param context the per-connection component the {@code HIMPORT PREPARE} is about to be injected on.
     * @return {@code true} if the connection was recorded and the {@code PREPARE} may be sent; {@code false} if the fieldset
     *         has already been closed and preparation must be skipped.
     */
    boolean registerConnection(HashImportContext context) {
        synchronized (preparedOn) {
            if (discarded) {
                return false;
            }
            preparedOn.add(context);
            return true;
        }
    }

    /**
     * Discard this fieldset, releasing its server-side state.
     * <p>
     * Sends a best-effort {@code HIMPORT DISCARD} to every connection the fieldset was prepared on and marks it discarded so it
     * can no longer be used for imports. Each {@code DISCARD} is handed to that connection's {@link HashImportContext}, which
     * writes it on the channel event loop — deferring it until the current transaction ends if a {@code MULTI} is open — so a
     * cleanup {@code DISCARD} never falls inside the caller's transaction. Cleanup is fire-and-forget: a {@code DISCARD} that
     * lands on a rotated connection is a harmless no-op because the generated fieldset name is unique per instance. Disposal is
     * not required for correctness — the state is also released when a connection closes — but frees it promptly on long-lived
     * and pooled connections. Calling this more than once has no additional effect.
     *
     * @since 7.7
     */
    @Override
    public void close() {

        Set<HashImportContext> targets;
        synchronized (preparedOn) {
            if (discarded) {
                return;
            }
            discarded = true;
            targets = new HashSet<>(preparedOn);
            preparedOn.clear();
        }

        for (HashImportContext context : targets) {
            context.discard(this);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", fields=" + Arrays.toString(fields) + ", discarded="
                + discarded + "]";
    }

}
