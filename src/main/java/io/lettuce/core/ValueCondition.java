/*
 * Copyright 2017-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

/**
 * A compare condition to be used with commands that support conditional value checks (e.g. SET with IFEQ/IFNE/IFDEQ/IFDNE and
 * DELEX). This abstraction lets callers express value-based or digest-based comparisons without exploding method overloads.
 *
 * <p>
 * Digest-based comparisons use a 64-bit XXH3 digest represented as a 16-character lower-case hexadecimal string.
 * </p>
 *
 * @param <V> value type used for value-based comparisons
 * @since 8.x
 */
public final class ValueCondition<V> {

    /**
     * The kind of condition represented by this instance.
     */
    public enum Kind {
        /** unconditional */
        ALWAYS,
        /** key must exist */
        EXISTS,
        /** key must not exist */
        NOT_EXISTS,
        /** current value must equal provided value */
        EQUAL,
        /** current value must not equal provided value */
        NOT_EQUAL,
        /** current value's digest must equal provided digest */
        DIGEST_EQUAL,
        /** current value's digest must not equal provided digest */
        DIGEST_NOT_EQUAL
    }

    private final Kind kind;

    private final V value; // used for EQUAL/NOT_EQUAL

    private final String digestHex; // used for DIGEST_EQUAL/DIGEST_NOT_EQUAL

    private ValueCondition(Kind kind, V value, String digestHex) {
        this.kind = kind;
        this.value = value;
        this.digestHex = digestHex;
    }

    /** A condition that always applies (no comparison). */
    public static <V> ValueCondition<V> always() {
        return new ValueCondition<>(Kind.ALWAYS, null, null);

    }

    /** A condition that requires the key to exist (equivalent to XX in SET). */
    public static <V> ValueCondition<V> exists() {
        return new ValueCondition<>(Kind.EXISTS, null, null);
    }

    /** A condition that requires the key to not exist (equivalent to NX in SET). */
    public static <V> ValueCondition<V> notExists() {
        return new ValueCondition<>(Kind.NOT_EXISTS, null, null);
    }

    /** A value-based comparison: set/delete only if the current value equals {@code value}. */
    public static <V> ValueCondition<V> equal(V value) {
        if (value == null)
            throw new IllegalArgumentException("value must not be null");
        return new ValueCondition<>(Kind.EQUAL, value, null);
    }

    /** A value-based comparison: set/delete only if the current value does not equal {@code value}. */
    public static <V> ValueCondition<V> notEqual(V value) {
        if (value == null)
            throw new IllegalArgumentException("value must not be null");
        return new ValueCondition<>(Kind.NOT_EQUAL, value, null);
    }

    /** A digest-based comparison: set/delete only if the current value's digest equals {@code hex16Digest}. */
    public static <V> ValueCondition<V> digestEqualHex(String hex16Digest) {
        if (hex16Digest == null)
            throw new IllegalArgumentException("digest must not be null");
        return new ValueCondition<>(Kind.DIGEST_EQUAL, null, hex16Digest);
    }

    /** A digest-based comparison: set/delete only if the current value's digest does not equal {@code hex16Digest}. */
    public static <V> ValueCondition<V> digestNotEqualHex(String hex16Digest) {
        if (hex16Digest == null)
            throw new IllegalArgumentException("digest must not be null");
        return new ValueCondition<>(Kind.DIGEST_NOT_EQUAL, null, hex16Digest);
    }

    /** The kind of this condition. */
    public Kind kind() {
        return kind;
    }

    /** The value for value-based comparisons, or {@code null}. */
    public V value() {
        return value;
    }

    /** The 16-character lower-case hex digest for digest-based comparisons, or {@code null}. */
    public String digestHex() {
        return digestHex;
    }

    @Override
    public String toString() {
        return "ValueCondition{" + "kind=" + kind + (value != null ? ", value=" + value : "")
                + (digestHex != null ? ", digestHex='" + digestHex + '\'' : "") + '}';
    }

}
