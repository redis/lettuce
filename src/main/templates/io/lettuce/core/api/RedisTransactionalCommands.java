/*
 * Copyright 2017-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.api;

import java.util.List;
import io.lettuce.core.TransactionResult;

/**
 * ${intent} for Transactions.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisTransactionalCommands<K, V> {

    /**
     * Discard all commands issued after MULTI.
     *
     * @return String simple-string-reply always {@code OK}.
     */
    String discard();

    /**
     * Execute all commands issued after MULTI.
     *
     * @return List&lt;Object&gt; array-reply each element being the reply to each of the commands in the atomic transaction.
     *
     *         When using {@code WATCH}, {@code EXEC} can return a {@link TransactionResult#wasDiscarded discarded
     *         TransactionResult}.
     * @see TransactionResult#wasDiscarded
     */
    TransactionResult exec();

    /**
     * Mark the start of a transaction block.
     *
     * @return String simple-string-reply always {@code OK}.
     */
    String multi();

    /**
     * Watch the given keys to determine execution of the MULTI/EXEC block.
     *
     * @param keys the key.
     * @return String simple-string-reply always {@code OK}.
     */
    String watch(K... keys);

    /**
     * Forget about all watched keys.
     *
     * @return String simple-string-reply always {@code OK}.
     */
    String unwatch();

}
