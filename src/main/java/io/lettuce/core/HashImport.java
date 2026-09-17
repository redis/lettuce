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
 * Reusable set of field names shared by many hashes, imported with the {@code HIMPORT} command family.
 * <p>
 * Create a fieldset once and pass it to every {@code himportSet} call that writes those fields, supplying only the values. The
 * field names themselves are declared to the server on first use and re-declared transparently across reconnects, pooled
 * connections, and new cluster nodes, so no prepare step is ever issued by the caller.
 * <p>
 * Use {@link #of(String...)} for {@link String}-keyed connections, or {@link #of(Function, Object...)} to derive the fieldset
 * name for another key type. Field names must be non-empty and free of duplicates.
 * <p>
 * A fieldset is safe to share across threads and connections. {@link #close() Close} it when the import is finished to release
 * its server-side state; a fieldset that is never closed holds that state until the connections it was used on are closed.
 * Imports already issued when {@link #close()} is called still complete, so try-with-resources is safe with the synchronous,
 * asynchronous, and reactive APIs alike.
 *
 * <pre class="code">
 * try (HashImport&lt;String&gt; fieldset = HashImport.of("name", "email")) {
 *     redis.himportSet("user:1", fieldset, "alice", "alice@example.com");
 *     redis.himportSet("user:2", fieldset, "bob", "bob@example.com");
 * }
 * </pre>
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

    /** Connections this fieldset was declared on, held weakly so closed connections stay collectible. */
    private final Set<HashImportContext> preparedOn = Collections.newSetFromMap(new WeakHashMap<>());

    /** Imports admitted by {@link #retain()} and not yet released; cleanup waits for this to reach zero. */
    private final AtomicLong inFlight = new AtomicLong();

    /**
     * Set by {@link #close()}. Not exposed: an accessor would only invite a check-then-import that {@link #retain()} does
     * atomically.
     */
    private volatile boolean closed;

    /**
     * Set once cleanup has run; from then on no {@code PREPARE} may be sent, as nothing would discard it. Guarded by
     * {@code preparedOn}.
     */
    private boolean cleanedUp;

    private HashImport(K name, K[] fields) {
        this.name = name;
        this.fields = fields;
    }

    /**
     * Create a fieldset for {@link String}-keyed connections. The fieldset name is generated and unique per instance.
     *
     * @param fields the field names, in the order values are supplied to {@code himportSet}; must not be {@code null}, empty,
     *        or contain {@code null} or duplicate elements.
     * @return a new fieldset over {@code fields}.
     * @throws IllegalArgumentException if {@code fields} is {@code null}, empty, or contains {@code null} or duplicate
     *         elements.
     * @since 7.7
     */
    public static HashImport<String> of(String... fields) {
        return of(seq -> NAME_PREFIX + seq, fields);
    }

    /**
     * Create a fieldset for a key type other than {@link String}, naming it by applying {@code idCodec} to a generated sequence
     * number.
     * <p>
     * {@code idCodec} must map distinct sequence numbers to distinct names: two live fieldsets sharing a name also share their
     * server-side state, and closing either releases it for both.
     *
     * @param <K> Key type.
     * @param idCodec maps a generated sequence number to a fieldset name, must not be {@code null} and must produce a distinct
     *        name per invocation.
     * @param fields the field names, in the order values are supplied to {@code himportSet}; must not be {@code null}, empty,
     *        or contain {@code null} or duplicate elements.
     * @return a new fieldset over {@code fields}.
     * @throws IllegalArgumentException if {@code idCodec} is {@code null} or returns {@code null}, or if {@code fields} is
     *         {@code null}, empty, or contains {@code null} or duplicate elements.
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
     * Return the generated name identifying this fieldset on the server.
     *
     * @return the fieldset name, never {@code null}.
     * @since 7.7
     */
    public K name() {
        return name;
    }

    /**
     * Return the field names, in the order values are supplied to {@code himportSet}.
     *
     * @return a copy of the field names; modifying it does not affect this fieldset.
     * @since 7.7
     */
    public K[] fields() {
        return fields.clone();
    }

    /**
     * Return the number of fields, which is the number of values every {@code himportSet} call must supply.
     *
     * @return the number of fields, always greater than zero.
     * @since 7.7
     */
    public int size() {
        return fields.length;
    }

    /**
     * Admit one {@code himportSet}, withholding cleanup until it completes. Pair with exactly one {@link #release()}.
     *
     * @return {@code true} if the import may proceed; {@code false} if this fieldset is closed, in which case no matching
     *         {@link #release()} must be issued.
     */
    boolean retain() {

        if (closed) {
            return false;
        }

        inFlight.incrementAndGet();

        if (closed) {
            // close() may have raced in between the check and the increment; hand the reservation straight back so a
            // close() that observed a non-zero count still gets its cleanup from this release.
            release();
            return false;
        }

        return true;
    }

    /**
     * Complete one admitted {@code himportSet}, running cleanup if it was the last one outstanding on a closed fieldset.
     */
    void release() {
        if (inFlight.decrementAndGet() == 0 && closed) {
            cleanup();
        }
    }

    /**
     * Record that this fieldset is being declared on {@code context}, so cleanup can later target it.
     * <p>
     * Gates on cleanup having run rather than on the fieldset being closed, so an import admitted before {@link #close()} still
     * declares its fieldset and the {@code DISCARD} is ordered behind it.
     *
     * @param context the connection the {@code PREPARE} is about to be written on.
     * @return {@code true} if the {@code PREPARE} may be sent; {@code false} if cleanup has already run.
     */
    boolean registerConnection(HashImportContext context) {
        synchronized (preparedOn) {
            if (cleanedUp) {
                return false;
            }
            preparedOn.add(context);
            return true;
        }
    }

    /**
     * Discard this fieldset, releasing its server-side state on every connection it was used on.
     * <p>
     * Further imports are rejected from the moment this returns: a subsequent {@code himportSet} fails with an
     * {@link IllegalStateException}. Imports already issued are not cancelled and still complete, so this may return before the
     * state has actually been released.
     * <p>
     * Release is best-effort and does not report failures. The state is released in any case when a connection closes, so
     * closing a fieldset is not required for correctness — but on long-lived and pooled connections an unclosed fieldset holds
     * its state for the life of those connections. Calling this more than once has no further effect.
     *
     * @since 7.7
     */
    @Override
    public void close() {

        closed = true;

        if (inFlight.get() == 0) {
            cleanup();
        }
    }

    /**
     * Hand a {@code DISCARD} to every connection this fieldset was declared on and stop further declaration. Runs once.
     */
    private void cleanup() {

        Set<HashImportContext> targets;
        synchronized (preparedOn) {
            if (cleanedUp) {
                return;
            }
            cleanedUp = true;
            targets = new HashSet<>(preparedOn);
            preparedOn.clear();
        }

        for (HashImportContext context : targets) {
            context.discard(this);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", fields=" + Arrays.toString(fields) + ", discarded=" + closed
                + "]";
    }

}
