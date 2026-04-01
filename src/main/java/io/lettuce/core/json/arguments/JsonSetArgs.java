/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/json.set/">JSON.SET</a> command.
 * <p>
 * {@link JsonSetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 6.5
 */
public class JsonSetArgs implements CompositeArgument {

    private boolean nx;

    private boolean xx;

    private FphaType fphaType;

    /**
     * Floating-point half-precision array type for the {@literal FPHA} block of the {@literal JSON.SET} command.
     *
     * @since 7.6
     */
    public enum FphaType {

        FP16(CommandKeyword.FP16), BF16(CommandKeyword.BF16), FP32(CommandKeyword.FP32), FP64(CommandKeyword.FP64);

        private final CommandKeyword keyword;

        FphaType(CommandKeyword keyword) {
            this.keyword = keyword;
        }

    }

    /**
     * Builder entry points for {@link JsonSetArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link JsonSetArgs} and sets {@literal NX}.
         *
         * @return new {@link JsonSetArgs} with {@literal NX} set.
         */
        public static JsonSetArgs nx() {
            return new JsonSetArgs().nx();
        }

        /**
         * Creates new {@link JsonSetArgs} and sets {@literal XX}.
         *
         * @return new {@link JsonSetArgs} with {@literal XX} set.
         */
        public static JsonSetArgs xx() {
            return new JsonSetArgs().xx();
        }

        /**
         * Creates new {@link JsonSetArgs} and sets the {@literal FPHA} type.
         *
         * @param fphaType the floating-point half-precision array type.
         * @return new {@link JsonSetArgs} with {@literal FPHA} type set.
         * @since 7.6
         */
        public static JsonSetArgs fpha(FphaType fphaType) {
            return new JsonSetArgs().fpha(fphaType);
        }

        /**
         * Creates new empty {@link JsonSetArgs}
         *
         * @return new {@link JsonSetArgs} with nothing set.
         */
        public static JsonSetArgs defaults() {
            return new JsonSetArgs().defaults();
        }

    }

    /**
     * Set the key only if it does not already exist.
     *
     * @return {@code this}.
     */
    public JsonSetArgs nx() {

        this.nx = true;
        return this;
    }

    /**
     * Set the key only if it already exists.
     *
     * @return {@code this}.
     */
    public JsonSetArgs xx() {

        this.xx = true;
        return this;
    }

    /**
     * Set the floating-point half-precision array type for encoding numeric arrays.
     *
     * @param fphaType the floating-point half-precision array type.
     * @return {@code this}.
     * @since 7.6
     */
    public JsonSetArgs fpha(FphaType fphaType) {

        this.fphaType = fphaType;
        return this;
    }

    /**
     * Set the key only if it already exists.
     *
     * @return {@code this}.
     */
    public JsonSetArgs defaults() {

        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (xx) {
            args.add(CommandKeyword.XX);
        } else if (nx) {
            args.add(CommandKeyword.NX);
        }

        if (fphaType != null) {
            args.add(CommandKeyword.FPHA);
            args.add(fphaType.keyword);
        }
    }

}
