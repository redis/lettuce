/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import io.lettuce.core.ExceptionFactory;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Output of all commands within a MULTI block.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class MultiOutput<K, V> extends CommandOutput<K, V, TransactionResult> {

    private final Queue<RedisCommand<K, V, ?>> queue;

    private List<Object> responses = new ArrayList<>();

    private Boolean discarded;

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

        if (discarded == null) {
            discarded = count == -1;
        } else {
            if (!queue.isEmpty()) {
                queue.peek().getOutput().multi(count);
            }
        }
    }

    @Override
    public void setError(ByteBuffer error) {

        if (discarded == null) {
            super.setError(error);
            return;
        }

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
            responses.add(!o.hasError() ? o.get() : ExceptionFactory.createExecutionException(o.getError()));
            cmd.complete();
        } else if (depth == 0 && !queue.isEmpty()) {
            for (RedisCommand<K, V, ?> cmd : queue) {
                cmd.complete();
            }
        }
    }

    @Override
    public TransactionResult get() {
        return new DefaultTransactionResult(discarded == null ? false : discarded, responses);
    }

}
