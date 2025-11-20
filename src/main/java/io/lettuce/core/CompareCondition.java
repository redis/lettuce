/*
 * Copyright 2017-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * A compare condition to be used with commands that support conditional value checks (e.g. SET with IFEQ/IFNE/IFDEQ/IFDNE and
 * DELEX). This abstraction lets callers express value-based or digest-based comparisons.
 *
 * <p>
 * Digest-based comparisons use a 64-bit XXH3 digest represented as a 16-character lower-case hexadecimal string.
 * </p>
 *
 * @param <V> value type used for value-based comparisons
 *
 * @author Aleksandar Todorov
 *
 * @since 7.1
 */
@Experimental
public final class CompareCondition<V> {

    /**
     * The kind of condition represented by this instance.
     */
    public enum Condition {

        /** current value must equal provided value */
        VALUE_EQUAL(CommandKeyword.IFEQ),
        /** current value must not equal provided value */
        VALUE_NOT_EQUAL(CommandKeyword.IFNE),
        /** current value's digest must equal provided digest */
        DIGEST_EQUAL(CommandKeyword.IFDEQ),
        /** current value's digest must not equal provided digest */
        DIGEST_NOT_EQUAL(CommandKeyword.IFDNE);

        private final CommandKeyword keyword;

        Condition(CommandKeyword keyword) {
            this.keyword = keyword;
        }

        /** The protocol keyword to emit for this condition. */
        public CommandKeyword getKeyword() {
            return keyword;
        }

    }

    private final Condition condition;

    private final V value; // used for EQUAL/NOT_EQUAL

    private final String digest; // used for DIGEST_EQUAL/DIGEST_NOT_EQUAL

    private CompareCondition(Condition condition, V value, String digestHex) {
        this.condition = condition;
        this.value = value;
        this.digest = digestHex;
    }

    /**
     * Append this condition's protocol arguments to the given args.
     */
    public <K> void build(CommandArgs<K, V> args) {
        args.add(condition.getKeyword());

        if (condition.equals(Condition.DIGEST_EQUAL) || condition.equals(Condition.DIGEST_NOT_EQUAL)) {
            args.add(digest);
        } else if (condition.equals(Condition.VALUE_EQUAL) || condition.equals(Condition.VALUE_NOT_EQUAL)) {
            args.addValue(value);
        }
    }

    // Factory methods for creating value- and digest-based conditions
    /** Create a value-based equality condition; succeeds only if the current value equals the given value. */
    public static <V> CompareCondition<V> valueEq(V value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return new CompareCondition<>(Condition.VALUE_EQUAL, value, null);
    }

    /** Create a value-based inequality condition; succeeds only if the current value does not equal the given value. */
    public static <V> CompareCondition<V> valueNe(V value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return new CompareCondition<>(Condition.VALUE_NOT_EQUAL, value, null);
    }

    /**
     * Create a digest-based equality condition; succeeds only if the current value's digest matches the given 16-character
     * lower-case hex digest.
     */
    public static <V> CompareCondition<V> digestEq(String hex16Digest) {
        if (hex16Digest == null) {
            throw new IllegalArgumentException("digest must not be null");
        }
        return new CompareCondition<>(Condition.DIGEST_EQUAL, null, hex16Digest);
    }

    /**
     * Create a digest-based inequality condition; succeeds only if the current value's digest does not match the given
     * 16-character lower-case hex digest.
     */
    public static <V> CompareCondition<V> digestNe(String hex16Digest) {
        if (hex16Digest == null) {
            throw new IllegalArgumentException("digest must not be null");
        }
        return new CompareCondition<>(Condition.DIGEST_NOT_EQUAL, null, hex16Digest);
    }

    /** The kind of this condition. */
    public Condition getCondition() {
        return condition;
    }

    /** The value for value-based comparisons, or {@code null}. */
    public V getValue() {
        return value;
    }

    /** The 16-character lower-case hex digest for digest-based comparisons, or {@code null}. */
    public String getDigest() {
        return digest;
    }

    @Override
    public String toString() {
        return "ValueCondition{" + "kind=" + condition + (value != null ? ", value=" + value : "")
                + (digest != null ? ", digestHex='" + digest + '\'' : "") + '}';
    }

}
