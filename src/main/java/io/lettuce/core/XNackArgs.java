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
 * Argument list builder for the Redis <a href="https://redis.io/commands/xnack">XNACK</a> command. Static import the methods
 * from {@link Builder} and call the methods: {@code retryCount(…)} , {@code force()} .
 * <p>
 * {@link XNackArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 7.6
 */
public class XNackArgs implements CompositeArgument {

    private Long retryCount;

    private boolean force;

    /**
     * Builder entry points for {@link XNackArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link XNackArgs} and setting {@literal RETRYCOUNT}.
         *
         * @param count delivery counter to set, must be {@code >= 0}.
         * @return new {@link XNackArgs} with {@literal RETRYCOUNT} set.
         * @see XNackArgs#retryCount(long)
         */
        public static XNackArgs retryCount(long count) {
            return new XNackArgs().retryCount(count);
        }

        /**
         * Creates new {@link XNackArgs} and setting {@literal FORCE}.
         *
         * @return new {@link XNackArgs} with {@literal FORCE} set.
         * @see XNackArgs#force()
         */
        public static XNackArgs force() {
            return new XNackArgs().force();
        }

    }

    /**
     * Overrides the mode's implicit delivery counter adjustment with an exact value. Useful for AOF rewrite, which must restore
     * the precise counter of a NACKed entry, and for cases where explicit control is desired.
     *
     * @param count delivery counter to set, must be {@code >= 0}.
     * @return {@code this}
     */
    public XNackArgs retryCount(long count) {

        LettuceAssert.isTrue(count >= 0, "RETRYCOUNT must be greater than or equal to 0");

        this.retryCount = count;
        return this;
    }

    /**
     * Creates a new unowned PEL entry for any ID not already in the group's PEL, rather than silently skipping it. Intended
     * primarily for AOF rewrite.
     *
     * @return {@code this}
     */
    public XNackArgs force() {
        this.force = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (retryCount != null) {
            args.add(CommandKeyword.RETRYCOUNT).add(retryCount);
        }

        if (force) {
            args.add(CommandKeyword.FORCE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        XNackArgs that = (XNackArgs) o;
        return force == that.force && Objects.equals(retryCount, that.retryCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(retryCount, force);
    }

}
