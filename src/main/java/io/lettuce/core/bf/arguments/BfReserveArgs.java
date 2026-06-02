/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.bf.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/bf.reserve/">BF.RESERVE</a> command.
 * <p>
 * {@link BfReserveArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class BfReserveArgs implements CompositeArgument {

    private Long expansion;

    private boolean nonScaling;

    /**
     * Builder entry points for {@link BfReserveArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link BfReserveArgs} and sets the expansion rate of the filter.
         *
         * @return a new {@link BfReserveArgs} with expansion rate configured.
         */
        public static BfReserveArgs expansion(long expansion) {
            return new BfReserveArgs().expansion(expansion);
        }

        /**
         * Creates a new {@link BfReserveArgs} and sets the non scaling flag.
         *
         * @return a new {@link BfReserveArgs} with non scaling flag configured.
         */
        public static BfReserveArgs nonScaling() {
            return new BfReserveArgs().nonScaling();
        }

    }

    /**
     * Sets the expansion rate of the filter.
     *
     * @return this
     */
    public BfReserveArgs expansion(long expansion) {
        this.expansion = expansion;
        return this;
    }

    /**
     * Sets the non scaling flag.
     *
     * @return this
     */
    public BfReserveArgs nonScaling() {
        this.nonScaling = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (expansion != null) {
            args.add(CommandKeyword.EXPANSION).add(expansion);
        }
        if (nonScaling) {
            args.add(CommandKeyword.NONSCALING);
        }
    }

}
