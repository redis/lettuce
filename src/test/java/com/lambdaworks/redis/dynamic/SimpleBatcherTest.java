/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.dynamic.batch.CommandBatching;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleBatcherTest {

    @Mock
    private StatefulConnection<Object, Object> connection;

    @Test
    public void shouldBatchWithDefaultSize() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();
        RedisCommand<Object, Object, Object> c4 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 2);

        assertThat(batcher.batch(c1, null)).isEqualTo(BatchTasks.EMPTY);
        verifyZeroInteractions(connection);

        BatchTasks batch = batcher.batch(c2, null);
        verify(connection).dispatch(Arrays.asList(c1, c2));
        assertThat(batch).contains(c1, c2);

        batcher.batch(c3, null);
        verifyZeroInteractions(connection);

        batcher.batch(c4, null);
        verify(connection).dispatch(Arrays.asList(c3, c4));
    }

    @Test
    public void shouldBatchWithoutSize() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, -1);

        batcher.batch(c1, null);

        verify(connection).dispatch(c1);

        batcher.batch(c2, null);

        verify(connection).dispatch(c2);
    }

    @Test
    public void shouldBatchWithBatchControlQueue() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();
        RedisCommand<Object, Object, Object> c4 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 2);

        batcher.batch(c1, CommandBatching.queue());
        batcher.batch(c2, CommandBatching.queue());
        verifyZeroInteractions(connection);

        batcher.batch(c3, null);

        verify(connection).dispatch(Arrays.asList(c1, c2));
    }

    @Test
    public void shouldBatchWithBatchControlQueueOverqueue() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();
        RedisCommand<Object, Object, Object> c4 = createCommand();
        RedisCommand<Object, Object, Object> c5 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 2);

        batcher.batch(c1, CommandBatching.queue());
        batcher.batch(c2, CommandBatching.queue());
        batcher.batch(c3, CommandBatching.queue());
        batcher.batch(c4, CommandBatching.queue());
        verifyZeroInteractions(connection);

        batcher.batch(c5, null);

        verify(connection).dispatch(Arrays.asList(c1, c2));
        verify(connection).dispatch(Arrays.asList(c3, c4));
    }

    @Test
    public void shouldBatchWithBatchControlFlush() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 4);

        batcher.batch(c1, null);
        batcher.batch(c2, CommandBatching.flush());

        verify(connection).dispatch(Arrays.asList(c1, c2));
    }

    private static RedisCommand<Object, Object, Object> createCommand() {
        return new AsyncCommand<>(new Command<>(CommandType.COMMAND, null, null));
    }
}