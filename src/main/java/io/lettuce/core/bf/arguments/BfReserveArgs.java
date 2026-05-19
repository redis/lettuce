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
 *
 *
 * @author Yordan Tsintsov
 * @since 7.6
 */
public class BfReserveArgs implements CompositeArgument {

    private Long expansion;
    private boolean nonScaling;

    public static class Builder {

        private Builder() {
        }

        public static BfReserveArgs expansion(long expansion) {
            return new BfReserveArgs().expansion(expansion);
        }

        public static BfReserveArgs nonScaling() {
            return new BfReserveArgs().nonScaling();
        }

    }

    public BfReserveArgs expansion(long expansion) {
        this.expansion = expansion;
        return this;
    }

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
