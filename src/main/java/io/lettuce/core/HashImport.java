/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Immutable template describing the shared field names of a hash import fieldset used by the {@code HIMPORT} command family.
 * <p>
 * A {@link HashImport} bundles a generated fieldset {@code name} with the ordered field names shared by many hashes. It is
 * declared once and reused across many {@code himportSet} calls: {@code himportPrepare} sends the field names to the server,
 * and each subsequent {@code himportSet} sends only the values, positionally paired to these fields.
 * <p>
 * Instances are created through {@link #of(String...)} (which auto-generates a {@code himport:<seq>} name for
 * {@link String}-keyed connections) or {@link #of(Function, Object...)} (which derives the name from a caller-supplied
 * function, for arbitrary key types). Field names are validated at creation time to be non-empty and free of duplicates.
 * <p>
 * A {@link HashImport} is safe to share across threads. Once {@link #close() closed} it is marked discarded and must not be
 * reused for further imports.
 *
 * @param <K> Key type, matching the field-name type of the connection's codec.
 * @author Redis Ltd.
 * @since 7.7
 */
@Experimental
public class HashImport<K> implements AutoCloseable {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private static final String NAME_PREFIX = "himport:";

    private final K name;

    private final K[] fields;

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
     */
    public K name() {
        return name;
    }

    /**
     * @return a copy of the ordered field names.
     */
    public K[] fields() {
        return fields.clone();
    }

    /**
     * @return the number of fields, matching the required number of values per {@code himportSet}.
     */
    public int size() {
        return fields.length;
    }

    /**
     * @return {@code true} if this fieldset has been {@link #close() closed} and must no longer be used.
     */
    public boolean isDiscarded() {
        return discarded;
    }

    /**
     * Mark this fieldset as discarded so it is no longer replayed on reconnect and can no longer be used for imports. This does
     * not itself issue a server-side {@code HIMPORT DISCARD}; use the corresponding command for that.
     */
    @Override
    public void close() {
        this.discarded = true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", fields=" + Arrays.toString(fields) + ", discarded="
                + discarded + "]";
    }

}
