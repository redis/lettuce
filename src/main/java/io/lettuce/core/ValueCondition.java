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

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * A compare condition to be used with commands that support conditional value checks (e.g. SET with IFEQ/IFNE/IFDEQ/IFDNE and
 * DELEX). This abstraction lets callers express value-based or digest-based comparisons without exploding method overloads.
 *
 * <p>
 * Digest-based comparisons use a 64-bit XXH3 digest represented as a 16-character lower-case hexadecimal string.
 * </p>
 *
 * @param <V> value type used for value-based comparisons
 */
@Experimental
public final class ValueCondition<V> {

    /**
     * The kind of condition represented by this instance.
     */
    public enum Kind {

        /** current value must equal provided value */
        EQUAL(CommandKeyword.IFEQ, Param.VALUE),
        /** current value must not equal provided value */
        NOT_EQUAL(CommandKeyword.IFNE, Param.VALUE),
        /** current value's digest must equal provided digest */
        DIGEST_EQUAL(CommandKeyword.IFDEQ, Param.DIGEST),
        /** current value's digest must not equal provided digest */
        DIGEST_NOT_EQUAL(CommandKeyword.IFDNE, Param.DIGEST);

        private final CommandKeyword keyword;

        private final Param param;

        Kind(CommandKeyword keyword, Param param) {
            this.keyword = keyword;
            this.param = param;
        }

        /** The protocol keyword to emit for this condition. */
        public CommandKeyword keyword() {
            return keyword;
        }

        /** Indicates whether this condition uses a value or a digest parameter. */
        public Param param() {
            return param;
        }

    }

    /** Parameter kind for condition arguments. */
    enum Param {
        VALUE, DIGEST
    }

    private final Kind kind;

    private final V value; // used for EQUAL/NOT_EQUAL

    private final String digestHex; // used for DIGEST_EQUAL/DIGEST_NOT_EQUAL

    private ValueCondition(Kind kind, V value, String digestHex) {
        this.kind = kind;
        this.value = value;
        this.digestHex = digestHex;
    }

    /**
     * Append this condition's protocol arguments to the given args.
     */
    public <K> void build(CommandArgs<K, V> args) {
        args.add(kind.keyword());
        switch (kind.param()) {
            case VALUE:
                args.addValue(value);
                break;
            case DIGEST:
                args.add(digestHex);
                break;
            default:
                break;
        }
    }

    // Factory methods for creating value- and digest-based conditions
    /** Create a value-based equality condition; succeeds only if the current value equals the given value. */
    public static <V> ValueCondition<V> valueEq(V value) {
        if (value == null)
            throw new IllegalArgumentException("value must not be null");
        return new ValueCondition<>(Kind.EQUAL, value, null);
    }

    /** Create a value-based inequality condition; succeeds only if the current value does not equal the given value. */
    public static <V> ValueCondition<V> valueNe(V value) {
        if (value == null)
            throw new IllegalArgumentException("value must not be null");
        return new ValueCondition<>(Kind.NOT_EQUAL, value, null);
    }

    /**
     * Create a digest-based equality condition; succeeds only if the current value's digest matches the given 16-character
     * lower-case hex digest.
     */
    public static <V> ValueCondition<V> digestEq(String hex16Digest) {
        if (hex16Digest == null)
            throw new IllegalArgumentException("digest must not be null");
        return new ValueCondition<>(Kind.DIGEST_EQUAL, null, hex16Digest);
    }

    /**
     * Create a digest-based inequality condition; succeeds only if the current value's digest does not match the given
     * 16-character lower-case hex digest.
     */
    public static <V> ValueCondition<V> digestNe(String hex16Digest) {
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
