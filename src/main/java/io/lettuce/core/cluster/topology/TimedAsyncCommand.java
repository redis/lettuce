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
package io.lettuce.core.cluster.topology;

import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.buffer.ByteBuf;

/**
 * Timed command that records the time at which the command was encoded and completed.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @param <T> Result type
 * @author Mark Paluch
 */
class TimedAsyncCommand<K, V, T> extends AsyncCommand<K, V, T> {

    long encodedAtNs = -1;

    long completedAtNs = -1;

    public TimedAsyncCommand(RedisCommand<K, V, T> command) {
        super(command);
    }

    @Override
    public void encode(ByteBuf buf) {
        completedAtNs = -1;
        encodedAtNs = -1;

        super.encode(buf);
        encodedAtNs = System.nanoTime();
    }

    @Override
    public void complete() {
        completedAtNs = System.nanoTime();
        super.complete();
    }

    public long duration() {
        if (completedAtNs == -1 || encodedAtNs == -1) {
            return -1;
        }
        return completedAtNs - encodedAtNs;
    }

}
