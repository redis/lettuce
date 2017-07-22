/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;

import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceFactories;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Output of all commands within a MULTI block.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class MultiOutput<K, V> extends CommandOutput<K, V, List<Object>> {

    private final Queue<RedisCommand<K, V, ?>> queue;
    private Integer expectedResults = null;

    public MultiOutput(RedisCodec<K, V> codec) {
        super(codec, null);
        queue = LettuceFactories.newSpScQueue();
    }

    public void add(RedisCommand<K, V, ?> cmd) {
        queue.add(cmd);
    }

    public void cancel() {
        for (RedisCommand<K, V, ?> c : queue) {
            c.complete();
        }
    }

    @Override
    public void set(long integer) {
        RedisCommand<K, V, ?> command = queue.peek();
        if (command != null && command.getOutput() != null) {
            command.getOutput().set(integer);
        }
    }

    @Override
    public void set(ByteBuffer bytes) {
        RedisCommand<K, V, ?> command = queue.peek();
        if (command != null && command.getOutput() != null) {
            command.getOutput().set(bytes);
        }
    }

    @Override
    public void multi(int count) {

        if (output == null) {
            output = OutputFactory.newList(count);
        }

        if (expectedResults == null) {
            expectedResults = count;

            if (count == -1 && !queue.isEmpty()) {
                queue.peek().getOutput().multi(count);
            }

        } else {
            if (!queue.isEmpty()) {
                queue.peek().getOutput().multi(count);
            }
        }
    }

    @Override
    public void setError(ByteBuffer error) {
        CommandOutput<K, V, ?> output = queue.isEmpty() ? this : queue.peek().getOutput();
        output.setError(decodeAscii(error));
    }

    @Override
    public void complete(int depth) {

        if (queue.isEmpty()) {
            return;
        }

        if (depth >= 1) {
            RedisCommand<K, V, ?> cmd = queue.peek();
            cmd.getOutput().complete(depth - 1);
        }

        if (depth == 1) {
            RedisCommand<K, V, ?> cmd = queue.remove();
            CommandOutput<K, V, ?> o = cmd.getOutput();
            output.add(!o.hasError() ? o.get() : new RedisCommandExecutionException(o.getError()));
            cmd.complete();
        } else if (depth == 0 && !queue.isEmpty()) {
            for (RedisCommand<K, V, ?> cmd : queue) {
                cmd.complete();
            }
        }
    }
}
