/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.bf.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/bf.insert/">BF.INSERT</a> command.
 * <p>
 * {@link BfInsertArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class BfInsertArgs implements CompositeArgument {

    private Long capacity;

    private Double error;

    private Long expansion;

    private boolean noCreate;

    private boolean nonScaling;

    /**
     * Builder entry points for {@link BfInsertArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link BfInsertArgs} and sets the desired capacity of the filter.
         *
         * @return a new {@link BfInsertArgs} with capacity configured.
         */
        public static BfInsertArgs capacity(long capacity) {
            return new BfInsertArgs().capacity(capacity);
        }

        /**
         * Creates a new {@link BfInsertArgs} and sets the desired error rate of the filter.
         *
         * @return a new {@link BfInsertArgs} with error rate configured.
         */
        public static BfInsertArgs error(double error) {
            return new BfInsertArgs().error(error);
        }

        /**
         * Creates a new {@link BfInsertArgs} and sets the expansion rate of the filter.
         *
         * @return a new {@link BfInsertArgs} with expansion rate configured.
         */
        public static BfInsertArgs expansion(long expansion) {
            return new BfInsertArgs().expansion(expansion);
        }

        /**
         * Creates a new {@link BfInsertArgs} and sets the no create flag.
         *
         * @return a new {@link BfInsertArgs} with no create flag configured.
         */
        public static BfInsertArgs noCreate() {
            return new BfInsertArgs().noCreate();
        }

        /**
         * Creates a new {@link BfInsertArgs} and sets the non scaling flag.
         *
         * @return a new {@link BfInsertArgs} with non scaling flag configured.
         */
        public static BfInsertArgs nonScaling() {
            return new BfInsertArgs().nonScaling();
        }

        /**
         * Creates a new {@link BfInsertArgs} with default settings.
         *
         * @return a new {@link BfInsertArgs} with default settings.
         */
        public static BfInsertArgs defaults() {
            return new BfInsertArgs();
        }

    }

    /**
     * Set the desired capacity of the filter.
     *
     * @return {@code this} {@link BfInsertArgs}.
     */
    public BfInsertArgs capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * Set the desired error rate of the filter.
     *
     * @return {@code this} {@link BfInsertArgs}.
     */
    public BfInsertArgs error(double error) {
        this.error = error;
        return this;
    }

    /**
     * Set the expansion rate of the filter.
     *
     * @return {@code this} {@link BfInsertArgs}.
     */
    public BfInsertArgs expansion(long expansion) {
        this.expansion = expansion;
        return this;
    }

    /**
     * Set the no create flag.
     *
     * @return {@code this} {@link BfInsertArgs}.
     */
    public BfInsertArgs noCreate() {
        this.noCreate = true;
        return this;
    }

    /**
     * Set the non scaling flag.
     *
     * @return {@code this} {@link BfInsertArgs}.
     */
    public BfInsertArgs nonScaling() {
        this.nonScaling = true;
        return this;
    }

    /**
     * Set the default settings.
     *
     * @return {@code this} {@link BfInsertArgs}.
     */
    public BfInsertArgs defaults() {
        this.capacity = null;
        this.error = null;
        this.expansion = null;
        this.noCreate = false;
        this.nonScaling = false;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (capacity != null) {
            args.add(CommandKeyword.CAPACITY).add(capacity);
        }
        if (error != null) {
            args.add(CommandKeyword.ERROR).add(error);
        }
        if (expansion != null) {
            args.add(CommandKeyword.EXPANSION).add(expansion);
        }
        if (noCreate) {
            args.add(CommandKeyword.NOCREATE);
        }
        if (nonScaling) {
            args.add(CommandKeyword.NONSCALING);
        }
    }

}
