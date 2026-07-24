/*
 * Copyright 2026-present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A single field value from a Redis search reply, retained as the raw bytes returned by the server.
 * <p>
 * A search reply carries no per-field type information, so the client cannot know whether a value is textual (a
 * {@code TEXT}/{@code NUMERIC} field or an aggregation expression) or binary (a vector embedding returned through
 * {@code RETURN} or {@code LOAD}). {@code FieldValue} keeps the exact bytes and lets the caller decide how to read them:
 * {@link #asString()} for text and numbers, {@link #asBytes()} for binary content where UTF-8 decoding would corrupt the value.
 * <p>
 * The server can also return a field with a null value (for example a JSON {@code null} loaded through {@code RETURN} or
 * {@code LOAD}). Such a field is kept with its key present and reported by {@link #isNull()}; {@link #asString()} and
 * {@link #asBytes()} return {@code null} for it. The {@code FieldValue} itself is never {@code null} in
 * {@link SearchReply.SearchResult#getFields()}; a field that was not returned at all is represented by the absence of its key.
 *
 * @author Viktoriya Kutsarova
 * @since 7.7
 */
public final class FieldValue {

    /**
     * Shared instance representing a field that the server returned with a null value.
     */
    static final FieldValue NULL = new FieldValue(null);

    private final byte[] value;

    private FieldValue(byte[] value) {
        this.value = value;
    }

    /**
     * Wraps the raw bytes of a field value.
     *
     * @param value the raw field value exactly as returned by the server. Must not be {@code null}.
     * @return a {@link FieldValue} view over the given bytes
     */
    public static FieldValue of(byte[] value) {
        LettuceAssert.notNull(value, "Field value must not be null");
        return new FieldValue(value);
    }

    /**
     * Gets the raw field bytes, exactly as returned by the server. Use this accessor for binary fields such as vector
     * embeddings, where UTF-8 decoding would corrupt the value.
     *
     * @return the raw field bytes, or {@code null} if the server returned a null value (see {@link #isNull()}). This is the
     *         backing array and must not be modified.
     */
    public byte[] asBytes() {
        return value;
    }

    /**
     * Gets the field value decoded as UTF-8 text. This suits textual and numeric fields. Binary values (for example vector
     * embeddings) are not valid UTF-8 and are corrupted by this view; read those via {@link #asBytes()}.
     *
     * @return the field value decoded as UTF-8, or {@code null} if the server returned a null value (see {@link #isNull()})
     */
    public String asString() {
        return asString(StandardCharsets.UTF_8);
    }

    /**
     * Gets the field value decoded as text using the given charset.
     *
     * @param charset the charset to decode with
     * @return the decoded field value, or {@code null} if the server returned a null value (see {@link #isNull()})
     */
    public String asString(Charset charset) {
        return value == null ? null : new String(value, charset);
    }

    /**
     * Reports whether the server returned this field with a null value. When {@code true}, {@link #asString()} and
     * {@link #asBytes()} return {@code null}.
     *
     * @return {@code true} if this field value is null
     */
    public boolean isNull() {
        return value == null;
    }

}
