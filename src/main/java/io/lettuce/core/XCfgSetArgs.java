/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis XCFGSET command.
 * <p>
 * {@link XCfgSetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 7.3
 */
public class XCfgSetArgs implements CompositeArgument {

    private Long idmpDuration;

    private Long idmpMaxsize;

    /**
     * Builder entry points for {@link XCfgSetArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link XCfgSetArgs} and setting {@literal IDMP-DURATION}.
         *
         * @param duration duration in seconds (1-86400).
         * @return new {@link XCfgSetArgs} with {@literal IDMP-DURATION} set.
         * @see XCfgSetArgs#idmpDuration(long)
         */
        public static XCfgSetArgs idmpDuration(long duration) {
            return new XCfgSetArgs().idmpDuration(duration);
        }

        /**
         * Creates new {@link XCfgSetArgs} and sets a {@literal IDMP-MAXSIZE}.
         *
         * @param maxsize maximum number of idempotent IDs per producer (1-10000).
         * @return new {@link XCfgSetArgs} with {@literal IDMP-MAXSIZE} set.
         * @see XCfgSetArgs#idmpMaxsize(long)
         */
        public static XCfgSetArgs idmpMaxsize(long maxsize) {
            return new XCfgSetArgs().idmpMaxsize(maxsize);
        }

    }

    /**
     * Set the duration (in seconds) that Redis keeps each idempotent ID.
     *
     * @param duration duration in seconds (1-86400).
     * @return {@code this}
     */
    public XCfgSetArgs idmpDuration(long duration) {

        LettuceAssert.isTrue(duration >= 1 && duration <= 86400, "IDMP-DURATION must be between 1 and 86400 seconds");

        this.idmpDuration = duration;
        return this;
    }

    /**
     * Set the maximum number of most recent idempotent IDs kept for each producer.
     *
     * @param maxsize maximum number of idempotent IDs per producer (1-10000).
     * @return {@code this}
     */
    public XCfgSetArgs idmpMaxsize(long maxsize) {

        LettuceAssert.isTrue(maxsize >= 1 && maxsize <= 10000, "IDMP-MAXSIZE must be between 1 and 10000");

        this.idmpMaxsize = maxsize;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (idmpDuration != null) {
            args.add(CommandKeyword.IDMP_DURATION).add(idmpDuration);
        }

        if (idmpMaxsize != null) {
            args.add(CommandKeyword.IDMP_MAXSIZE).add(idmpMaxsize);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        XCfgSetArgs that = (XCfgSetArgs) o;
        return Objects.equals(idmpDuration, that.idmpDuration) && Objects.equals(idmpMaxsize, that.idmpMaxsize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idmpDuration, idmpMaxsize);
    }

}
