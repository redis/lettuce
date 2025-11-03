/*
 * Copyright 2025, Redis Ltd. and Contributors
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

package io.lettuce.core.datastructure.queue;

import io.lettuce.core.protocol.RedisCommand;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

/**
 * A queue wrapper that filters out completed {@link RedisCommand} instances.
 * When peeking or polling, this queue skips over commands where {@link RedisCommand#isDone()}
 * returns {@code true} and returns the first incomplete command.
 *
 * @author Tihomir Mateev
 * @since 6.2.2-uber-0.5
 */
public class IncompleteQueue extends AbstractQueue<RedisCommand<?, ?, ?>> {

    private final Queue<RedisCommand<?, ?, ?>> queue;

    public IncompleteQueue(Queue<RedisCommand<?, ?, ?>> queue) {
        this.queue = queue;
    }

    @Override
    public boolean offer(RedisCommand<?, ?, ?> e) {
        return queue.offer(e);
    }

    @Override
    public RedisCommand<?, ?, ?> poll() {
        RedisCommand<?, ?, ?> cmd = queue.peek();

        while (cmd != null && cmd.getOutput().hasError()) {
            RedisCommand<?, ?, ?> discarded = queue.remove();
            cmd = queue.peek();
        }
        return queue.poll();
    }

    @Override
    public RedisCommand<?, ?, ?> peek() {
        RedisCommand<?, ?, ?> cmd = queue.peek();

        while (cmd != null && cmd.getOutput().hasError()) {
            RedisCommand<?, ?, ?> discarded = queue.remove();
            cmd = queue.peek();
        }
        return cmd;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @NotNull
    @Override
    public Iterator<RedisCommand<?, ?, ?>> iterator() {
        return queue.iterator();
    }
}

