/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/cf.insert/">CF.INSERT</a> command.
 * <p>
 * {@link CfInsertArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class CfInsertArgs implements CompositeArgument {

    private Long capacity;

    private boolean noCreate;

    /**
     * Builder entry points for {@link CfInsertArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link CfInsertArgs} and sets the desired capacity of the filter.
         *
         * @return a new {@link CfInsertArgs} with capacity configured.
         */
        public static CfInsertArgs capacity(long capacity) {
            return new CfInsertArgs().capacity(capacity);
        }

        /**
         * Creates a new {@link CfInsertArgs} and sets the no create flag.
         *
         * @return a new {@link CfInsertArgs} with no create flag configured.
         */
        public static CfInsertArgs noCreate() {
            return new CfInsertArgs().noCreate();
        }

        /**
         * Creates a new {@link CfInsertArgs} with default settings.
         *
         * @return a new {@link CfInsertArgs} with default settings.
         */
        public static CfInsertArgs defaults() {
            return new CfInsertArgs();
        }

    }

    /**
     * Set the desired capacity of the filter.
     *
     * @return {@code this} {@link CfInsertArgs}.
     */
    public CfInsertArgs capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * Set the no create flag.
     *
     * @return {@code this} {@link CfInsertArgs}.
     */
    public CfInsertArgs noCreate() {
        this.noCreate = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (capacity != null) {
            args.add(CommandKeyword.CAPACITY).add(capacity);
        }
        if (noCreate) {
            args.add(CommandKeyword.NOCREATE);
        }
    }

}
