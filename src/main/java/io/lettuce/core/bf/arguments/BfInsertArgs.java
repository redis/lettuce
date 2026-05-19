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
public class BfInsertArgs implements CompositeArgument {

    private Long capacity;
    private Double error;
    private Long expansion;
    private boolean noCreate;
    private boolean nonScaling;

    public static class Builder {

        private Builder() {
        }

        public static BfInsertArgs capacity(long capacity) {
            return new BfInsertArgs().capacity(capacity);
        }

        public static BfInsertArgs error(double error) {
            return new BfInsertArgs().error(error);
        }

        public static BfInsertArgs expansion(long expansion) {
            return new BfInsertArgs().expansion(expansion);
        }

        public static BfInsertArgs noCreate() {
            return new BfInsertArgs().noCreate();
        }

        public static BfInsertArgs nonScaling() {
            return new BfInsertArgs().nonScaling();
        }

        public static BfInsertArgs defaults() {
            return new BfInsertArgs();
        }

    }

    public BfInsertArgs capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public BfInsertArgs error(double error) {
        this.error = error;
        return this;
    }

    public BfInsertArgs expansion(long expansion) {
        this.expansion = expansion;
        return this;
    }

    public BfInsertArgs noCreate() {
        this.noCreate = true;
        return this;
    }

    public BfInsertArgs nonScaling() {
        this.nonScaling = true;
        return this;
    }

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
