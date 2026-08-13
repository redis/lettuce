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

    /**
     * Number of {@code himportSet} commands that have been admitted through {@link #retain()} and not yet {@link #release()
     * released}. Cleanup is withheld while this is positive, so imports issued before {@link #close()} still declare their
     * fieldset and complete against it.
     */
    private final AtomicLong inFlight = new AtomicLong();

    /**
     * Set synchronously by {@link #close()} so that {@link #retain()} rejects <em>new</em> imports from the moment it returns.
     * Deliberately not exposed: any accessor would only support a check-then-import that {@link #retain()} already performs
     * atomically.
     */
    private volatile boolean closed;

    /**
     * Set once {@link #cleanup()} has handed the {@code DISCARD}s to the connections and cleared {@link #preparedOn}. From that
     * point a {@code PREPARE} must no longer be sent, because nothing would discard it. Guarded by {@code preparedOn}.
     */
    private boolean cleanedUp;

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
     * Admit one {@code himportSet} for this fieldset, withholding cleanup until it completes.
     * <p>
     * Called on the caller's thread before the command is dispatched, and paired with exactly one {@link #release()} on command
     * completion. This is what makes {@code close()} order itself behind imports that were already issued: a command only
     * reaches the write path — where the {@code PREPARE} is injected — some time after {@code dispatch} returns, so a
     * {@code close()} on the calling thread would otherwise overtake it.
     *
     * @return {@code true} if the import may proceed; {@code false} if this fieldset is already closed, in which case no
     *         matching {@link #release()} must be issued.
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
     * Complete one admitted {@code himportSet}, running cleanup if this was the last one outstanding on a closed fieldset.
     * Called from the command's completion callback, hence on the connection's event loop for a command that reached the
     * server.
     */
    void release() {
        if (inFlight.decrementAndGet() == 0 && closed) {
            cleanup();
        }
    }

    /**
     * Record that this fieldset is being prepared on {@code context}, so cleanup can later target it with a {@code DISCARD}.
     * Called on the connection's event loop from the outbound write path, before the {@code PREPARE} is written, and atomically
     * with {@link #cleanup()} on {@code preparedOn}: once cleanup has run the caller must not send the {@code PREPARE} at all,
     * so no server-side state is ever left without a matching {@code DISCARD}.
     * <p>
     * Note this gates on cleanup having run, not on the fieldset being closed: an import admitted by {@link #retain()} before
     * {@link #close()} still declares its fieldset here, and the {@code DISCARD} that follows is ordered behind it.
     *
     * @param context the per-connection component the {@code HIMPORT PREPARE} is about to be injected on.
     * @return {@code true} if the connection was recorded and the {@code PREPARE} may be sent; {@code false} if cleanup has
     *         already run and preparation must be skipped.
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
     * Discard this fieldset, releasing its server-side state.
     * <p>
     * Rejects further imports from the moment this returns — a subsequent {@code himportSet} fails with an
     * {@link IllegalStateException} — and sends a best-effort {@code HIMPORT DISCARD} to every connection the fieldset was
     * prepared on. Each {@code DISCARD} is handed to that connection's {@link HashImportContext}, which writes it on the
     * channel event loop — deferring it until the current transaction ends if a {@code MULTI} is open — so a cleanup
     * {@code DISCARD} never falls inside the caller's transaction.
     * <p>
     * Imports already issued when this is called are <em>not</em> cancelled: cleanup is withheld until every outstanding
     * {@code himportSet} has completed, so the {@code DISCARD} is ordered behind them on the wire. Closing therefore does not
     * race the asynchronous APIs, and the try-with-resources idiom is safe with all execution models. The consequence is that
     * this method may return before the {@code DISCARD}s have been handed out.
     * <p>
     * Cleanup is fire-and-forget: a {@code DISCARD} that lands on a rotated connection is a harmless no-op because the
     * generated fieldset name is unique per instance. Disposal is not required for correctness — the state is also released
     * when a connection closes — but on long-lived and pooled connections a fieldset that is never closed keeps its server-side
     * state for the life of the connection. Calling this more than once has no additional effect.
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
     * Hand a {@code DISCARD} to every connection this fieldset was prepared on and stop further preparation. Runs once, either
     * directly from {@link #close()} when nothing was outstanding or from the {@link #release()} of the last outstanding
     * import.
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
