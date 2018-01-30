/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic;

import io.lettuce.core.dynamic.batch.CommandBatching;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Command batcher to enqueue commands and flush a batch once a flush is requested or a configured command threshold is reached.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see SimpleBatcher
 */
public interface Batcher {

    /**
     * Batcher that does not support batching.
     */
    Batcher NONE = (command, batching) -> {
        throw new UnsupportedOperationException();
    };

    /**
     * Add command to the {@link Batcher}.
     *
     * @param command the command to batch.
     * @param batching invocation-specific {@link CommandBatching} control. May be {@literal null} to use default batching
     *        settings.
     * @return result of the batching. Either an {@link BatchTasks#EMPTY empty} result or a result containing the batched
     *         commands.
     */
    BatchTasks batch(RedisCommand<Object, Object, Object> command, CommandBatching batching);

    /**
     * Force-flush the batch. Has no effect if the queue is empty.
     */
    default BatchTasks flush() {
        return BatchTasks.EMPTY;
    }
}
