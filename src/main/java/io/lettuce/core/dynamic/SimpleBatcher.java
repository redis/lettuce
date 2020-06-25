/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.dynamic.batch.CommandBatching;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Simple threadsafe {@link Batcher} that flushes queued command when either:
 * <ul>
 * <li>Reaches the configured {@link #batchSize}</li>
 * <li>Encounters a {@link CommandBatching#flush() force flush}</li>
 * </ul>
 *
 * @author Mark Paluch
 */
class SimpleBatcher implements Batcher {

    private final StatefulConnection<Object, Object> connection;

    private final int batchSize;

    private final BlockingQueue<RedisCommand<Object, Object, Object>> queue = new LinkedBlockingQueue<>();

    private final AtomicBoolean flushing = new AtomicBoolean();

    public SimpleBatcher(StatefulConnection<Object, Object> connection, int batchSize) {

        LettuceAssert.isTrue(batchSize == -1 || batchSize > 1, "Batch size must be greater zero or -1");
        this.connection = connection;
        this.batchSize = batchSize;
    }

    @Override
    public BatchTasks batch(RedisCommand<Object, Object, Object> command, CommandBatching batching) {

        queue.add(command);

        if (batching == CommandBatching.queue()) {
            return BatchTasks.EMPTY;
        }

        boolean forcedFlush = batching == CommandBatching.flush();

        boolean defaultFlush = false;

        if (!forcedFlush) {
            if (queue.size() >= batchSize) {
                defaultFlush = true;
            }
        }

        if (defaultFlush || forcedFlush) {
            return flush(forcedFlush);
        }

        return BatchTasks.EMPTY;
    }

    @Override
    public BatchTasks flush() {
        return flush(true);
    }

    protected BatchTasks flush(boolean forcedFlush) {

        boolean defaultFlush = false;

        List<RedisCommand<?, ?, ?>> commands = new ArrayList<>(Math.max(batchSize, 10));

        while (flushing.compareAndSet(false, true)) {

            try {

                int consume = -1;

                if (!forcedFlush) {
                    long queuedItems = queue.size();
                    if (queuedItems >= batchSize) {
                        consume = batchSize;
                        defaultFlush = true;
                    }
                }

                List<? extends RedisCommand<?, ?, ?>> batch = doFlush(forcedFlush, defaultFlush, consume);
                if (batch != null) {
                    commands.addAll(batch);
                }

                if (defaultFlush && !queue.isEmpty() && queue.size() > batchSize) {
                    continue;
                }

                return new BatchTasks(commands);

            } finally {
                flushing.set(false);
            }
        }

        return BatchTasks.EMPTY;
    }

    private List<? extends RedisCommand<?, ?, ?>> doFlush(boolean forcedFlush, boolean defaultFlush, int consume) {

        List<RedisCommand<Object, Object, Object>> commands = null;
        if (forcedFlush) {
            commands = prepareForceFlush();
        } else if (defaultFlush) {
            commands = prepareDefaultFlush(consume);
        }

        if (commands != null && !commands.isEmpty()) {
            if (commands.size() == 1) {
                connection.dispatch(commands.get(0));
            } else {
                connection.dispatch(commands);
            }

            return commands;
        }
        return Collections.emptyList();
    }

    private List<RedisCommand<Object, Object, Object>> prepareForceFlush() {

        List<RedisCommand<Object, Object, Object>> batch = new ArrayList<>(Math.max(batchSize, 10));

        do {
            RedisCommand<Object, Object, Object> poll = queue.poll();

            assert poll != null;
            batch.add(poll);
        } while (!queue.isEmpty());

        return batch;
    }

    private List<RedisCommand<Object, Object, Object>> prepareDefaultFlush(int consume) {

        List<RedisCommand<Object, Object, Object>> batch = new ArrayList<>(Math.max(consume, 10));

        while ((batch.size() < consume || consume == -1) && !queue.isEmpty()) {

            RedisCommand<Object, Object, Object> poll = queue.poll();

            assert poll != null;
            batch.add(poll);
        }

        return batch;
    }

}
