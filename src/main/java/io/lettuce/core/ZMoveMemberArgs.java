/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.NX;
import static io.lettuce.core.protocol.CommandKeyword.XX;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the destination condition of the {@code ZMOVEMEMBER} command: {@code NX} moves only members the
 * destination doesn't hold, {@code XX} only members it holds. The type holds one condition: the last call of {@link #nx()} or
 * {@link #xx()} wins. Static import the methods from {@link Builder}: {@code nx()}.
 * <p>
 * {@link ZMoveMemberArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class ZMoveMemberArgs implements CompositeArgument {

    private ProtocolKeyword condition;

    /**
     * Builder entry points for {@link ZMoveMemberArgs}.
     *
     * @since 8.0
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ZMoveMemberArgs} with {@code NX} set.
         *
         * @return new {@link ZMoveMemberArgs} with {@code NX} set.
         * @see ZMoveMemberArgs#nx()
         * @since 8.0
         */
        public static ZMoveMemberArgs nx() {
            return new ZMoveMemberArgs().nx();
        }

        /**
         * Creates new {@link ZMoveMemberArgs} with {@code XX} set.
         *
         * @return new {@link ZMoveMemberArgs} with {@code XX} set.
         * @see ZMoveMemberArgs#xx()
         * @since 8.0
         */
        public static ZMoveMemberArgs xx() {
            return new ZMoveMemberArgs().xx();
        }

    }

    /**
     * Move only members the destination doesn't hold. Replaces a condition set before.
     *
     * @return {@code this} {@link ZMoveMemberArgs}.
     * @since 8.0
     */
    public ZMoveMemberArgs nx() {
        this.condition = NX;
        return this;
    }

    /**
     * Move only members the destination already holds. Replaces a condition set before.
     *
     * @return {@code this} {@link ZMoveMemberArgs}.
     * @since 8.0
     */
    public ZMoveMemberArgs xx() {
        this.condition = XX;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (condition != null) {
            args.add(condition);
        }
    }

}
