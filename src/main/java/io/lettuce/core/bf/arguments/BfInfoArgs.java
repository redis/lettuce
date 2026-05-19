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
public class BfInfoArgs implements CompositeArgument {

    private final CommandKeyword command;

    private BfInfoArgs(CommandKeyword command) {
        this.command = command;
    }

    public static BfInfoArgs capacity() {
        return new BfInfoArgs(CommandKeyword.CAPACITY);
    }

    public static BfInfoArgs size() {
        return new BfInfoArgs(CommandKeyword.SIZE);
    }

    public static BfInfoArgs filters() {
        return new BfInfoArgs(CommandKeyword.FILTERS);
    }

    public static BfInfoArgs items() {
        return new BfInfoArgs(CommandKeyword.ITEMS);
    }

    public static BfInfoArgs expansion() {
        return new BfInfoArgs(CommandKeyword.EXPANSION);
    }

    public static BfInfoArgs defaults() {
        return new BfInfoArgs(null);
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (command != null) {
            args.add(command);
        }
    }

}
