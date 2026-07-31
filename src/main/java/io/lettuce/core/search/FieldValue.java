/*
 * Copyright 2026-present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A single field value from a Redis search reply, retained in the exact shape returned by the server.
 * <p>
 * A search reply carries no per-field type information, so the client cannot know whether a value is textual (a
 * {@code TEXT}/{@code NUMERIC} field or an aggregation expression) or binary (a vector embedding returned through
 * {@code RETURN} or {@code LOAD}). {@code FieldValue} keeps the exact bytes and lets the caller decide how to read them:
 * {@link #asString()} for text and numbers, {@link #asBytes()} for binary content where UTF-8 decoding would corrupt the value.
 * <p>
 * Most field values are scalars, but aggregation reducers such as {@code FT.AGGREGATE REDUCE COLLECT} produce nested column
 * values. A {@code FieldValue} therefore has a {@link Kind}: {@link Kind#SCALAR} for raw bytes, {@link Kind#ARRAY} for a list
 * of nested values, {@link Kind#MAP} for named nested values, and {@link Kind#NULL} for a server-returned null. A collected
 * column is an {@link Kind#ARRAY} with one element per collected entry; read it via {@link #asList()} and read each entry via
 * {@link #asMap()}, which normalizes the protocol-specific entry shape (RESP3 returns each entry as a map, RESP2 as a flat
 * key/value array).
 * <p>
 * The server can also return a field with a null value (for example a JSON {@code null} loaded through {@code RETURN} or
 * {@code LOAD}). Such a field is kept with its key present and reported by {@link #isNull()}; all accessors return {@code null}
 * for it. The {@code FieldValue} itself is never {@code null} in {@link SearchReply.SearchResult#getFields()}; a field that was
 * not returned at all is represented by the absence of its key. Reading a non-null value through an accessor of a different
 * kind (for example {@link #asString()} on an {@link Kind#ARRAY}) throws {@link IllegalStateException}.
 *
 * @author Viktoriya Kutsarova
 * @since 7.7
 */
public final class FieldValue {

    /**
     * The shape of a {@link FieldValue}.
     *
     * @since 7.7
     */
    public enum Kind {

        /**
         * A scalar value, kept as the raw bytes returned by the server.
         */
        SCALAR,

        /**
         * An array of nested values, for example a collected aggregation column.
         */
        ARRAY,

        /**
         * A map of nested values keyed by name, for example a collected entry under RESP3.
         */
        MAP,

        /**
         * A null value returned by the server.
         */
        NULL

    }

    /**
     * Shared instance representing a field that the server returned with a null value.
     */
    static final FieldValue NULL = new FieldValue(Kind.NULL, null);

    private final Kind kind;

    private final Object value;

    private FieldValue(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    /**
     * Wraps the raw bytes of a scalar field value.
     *
     * @param value the raw field value exactly as returned by the server. Must not be {@code null}.
     * @return a {@link Kind#SCALAR} {@link FieldValue} view over the given bytes
     */
    public static FieldValue of(byte[] value) {
        LettuceAssert.notNull(value, "Field value must not be null");
        return new FieldValue(Kind.SCALAR, value);
    }

    /**
     * Creates an array field value from the given elements.
     *
     * @param elements the array elements. Must not be {@code null} and must not contain {@code null} elements (use
     *        {@link #nullValue()} for a null element).
     * @return a {@link Kind#ARRAY} {@link FieldValue} holding a copy of the given elements
     * @since 7.7
     */
    public static FieldValue array(List<FieldValue> elements) {
        LettuceAssert.notNull(elements, "Field value elements must not be null");
        LettuceAssert.noNullElements(elements, "Field value elements must not contain null elements");
        return new FieldValue(Kind.ARRAY, Collections.unmodifiableList(new ArrayList<>(elements)));
    }

    /**
     * Creates a map field value from the given entries, preserving their iteration order.
     *
     * @param entries the map entries. Must not be {@code null} and must not contain {@code null} keys or values (use
     *        {@link #nullValue()} for a null value).
     * @return a {@link Kind#MAP} {@link FieldValue} holding a copy of the given entries
     * @since 7.7
     */
    public static FieldValue map(Map<String, FieldValue> entries) {
        LettuceAssert.notNull(entries, "Field value entries must not be null");
        Map<String, FieldValue> copy = new LinkedHashMap<>(entries.size());
        entries.forEach((key, value) -> {
            LettuceAssert.notNull(key, "Field value entry keys must not be null");
            LettuceAssert.notNull(value, "Field value entry values must not be null");
            copy.put(key, value);
        });
        return new FieldValue(Kind.MAP, Collections.unmodifiableMap(copy));
    }

    /**
     * Returns the {@link FieldValue} representing a server-returned null value.
     *
     * @return the shared {@link Kind#NULL} instance
     * @since 7.7
     */
    public static FieldValue nullValue() {
        return NULL;
    }

    /**
     * Returns the {@link Kind} of this field value.
     *
     * @return the kind, never {@code null}
     * @since 7.7
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Gets the raw field bytes, exactly as returned by the server. Use this accessor for binary fields such as vector
     * embeddings, where UTF-8 decoding would corrupt the value.
     *
     * @return the raw field bytes, or {@code null} if the server returned a null value (see {@link #isNull()}). This is the
     *         backing array and must not be modified.
     * @throws IllegalStateException if this value is an {@link Kind#ARRAY} or a {@link Kind#MAP}
     */
    public byte[] asBytes() {
        if (kind == Kind.NULL) {
            return null;
        }
        if (kind != Kind.SCALAR) {
            throw wrongKind(Kind.SCALAR);
        }
        return (byte[]) value;
    }

    /**
     * Gets the field value decoded as UTF-8 text. This suits textual and numeric fields. Binary values (for example vector
     * embeddings) are not valid UTF-8 and are corrupted by this view; read those via {@link #asBytes()}.
     *
     * @return the field value decoded as UTF-8, or {@code null} if the server returned a null value (see {@link #isNull()})
     * @throws IllegalStateException if this value is an {@link Kind#ARRAY} or a {@link Kind#MAP}
     */
    public String asString() {
        return asString(StandardCharsets.UTF_8);
    }

    /**
     * Gets the field value decoded as text using the given charset.
     *
     * @param charset the charset to decode with
     * @return the decoded field value, or {@code null} if the server returned a null value (see {@link #isNull()})
     * @throws IllegalStateException if this value is an {@link Kind#ARRAY} or a {@link Kind#MAP}
     */
    public String asString(Charset charset) {
        byte[] bytes = asBytes();
        return bytes == null ? null : new String(bytes, charset);
    }

    /**
     * Gets the elements of an array field value, for example the entries of a collected aggregation column.
     *
     * @return an unmodifiable list of the array elements, or {@code null} if the server returned a null value (see
     *         {@link #isNull()})
     * @throws IllegalStateException if this value is a {@link Kind#SCALAR} or a {@link Kind#MAP}
     * @since 7.7
     */
    @SuppressWarnings("unchecked")
    public List<FieldValue> asList() {
        if (kind == Kind.NULL) {
            return null;
        }
        if (kind != Kind.ARRAY) {
            throw wrongKind(Kind.ARRAY);
        }
        return (List<FieldValue>) value;
    }

    /**
     * Gets the field value as a map of named nested values.
     * <p>
     * A {@link Kind#MAP} value is returned directly. An {@link Kind#ARRAY} value is interpreted as a flat list of key/value
     * pairs — the shape RESP2 uses for a collected aggregation entry — where each key must be a scalar and is decoded as UTF-8;
     * an array of odd length, or one whose key positions hold non-scalar values, does not represent key/value pairs and is
     * rejected. This makes a collected entry readable the same way on RESP2 and RESP3. Note that any even-length array of
     * scalars is accepted by this interpretation, including values (for example a {@code TOLIST} column) that the server did
     * not produce as key/value pairs — it is the caller's responsibility to apply this view only to fields that hold pairs.
     *
     * @return an unmodifiable, ordered map of the named nested values, or {@code null} if the server returned a null value (see
     *         {@link #isNull()})
     * @throws IllegalStateException if this value is a {@link Kind#SCALAR}, or an {@link Kind#ARRAY} that does not represent
     *         key/value pairs
     * @since 7.7
     */
    @SuppressWarnings("unchecked")
    public Map<String, FieldValue> asMap() {
        if (kind == Kind.NULL) {
            return null;
        }
        if (kind == Kind.MAP) {
            return (Map<String, FieldValue>) value;
        }
        if (kind == Kind.ARRAY) {
            return pairsToMap((List<FieldValue>) value);
        }
        throw wrongKind(Kind.MAP);
    }

    /**
     * Reports whether the server returned this field with a null value. When {@code true}, all accessors return {@code null}.
     *
     * @return {@code true} if this field value is null
     */
    public boolean isNull() {
        return kind == Kind.NULL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FieldValue)) {
            return false;
        }
        FieldValue other = (FieldValue) o;
        if (kind != other.kind) {
            return false;
        }
        if (kind == Kind.SCALAR) {
            return Arrays.equals((byte[]) value, (byte[]) other.value);
        }
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        int valueHash = kind == Kind.SCALAR ? Arrays.hashCode((byte[]) value) : Objects.hashCode(value);
        return 31 * kind.hashCode() + valueHash;
    }

    @Override
    public String toString() {
        switch (kind) {
            case NULL:
                return "null";
            case SCALAR:
                return new String((byte[]) value, StandardCharsets.UTF_8);
            default:
                return value.toString();
        }
    }

    private static Map<String, FieldValue> pairsToMap(List<FieldValue> pairs) {
        if (pairs.size() % 2 != 0) {
            throw new IllegalStateException(
                    "Array field value of size " + pairs.size() + " does not represent key/value pairs");
        }
        Map<String, FieldValue> map = new LinkedHashMap<>(pairs.size() / 2);
        for (int i = 0; i < pairs.size(); i += 2) {
            FieldValue key = pairs.get(i);
            if (key.getKind() != Kind.SCALAR) {
                throw new IllegalStateException(
                        "Array field value does not represent key/value pairs, key at index " + i + " is " + key.getKind());
            }
            map.put(key.asString(), pairs.get(i + 1));
        }
        return Collections.unmodifiableMap(map);
    }

    private IllegalStateException wrongKind(Kind requested) {
        return new IllegalStateException("Field value is " + kind + ", not " + requested);
    }

}
