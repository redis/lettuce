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
package io.lettuce.core.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.netty.buffer.ByteBuf;

/**
 * Generic fallback command to collect arbitrary Redis responses in a {@link List} represented as String. Used as buffer when
 * received a Redis response without a command to correlate.
 *
 * @author Mark Paluch
 * @since 4.5
 */
class PristineFallbackCommand implements RedisCommand<String, String, List<String>> {

    private final CommandOutput<String, String, List<String>> output;
    private volatile boolean complete;

    PristineFallbackCommand() {
        this.output = new FallbackOutput();
    }

    @Override
    public CommandOutput<String, String, List<String>> getOutput() {
        return output;
    }

    @Override
    public void complete() {
        complete = true;
    }

    @Override
    public void cancel() {
        complete = true;
    }

    @Override
    public CommandArgs<String, String> getArgs() {
        return null;
    }

    @Override
    public boolean completeExceptionally(Throwable throwable) {
        return false;
    }

    @Override
    public ProtocolKeyword getType() {
        return null;
    }

    @Override
    public void encode(ByteBuf buf) {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return complete;
    }

    @Override
    public void setOutput(CommandOutput<String, String, List<String>> output) {
    }

    static class FallbackOutput extends CommandOutput<String, String, List<String>> {

        FallbackOutput() {
            super(StringCodec.ASCII, new ArrayList<>());
        }

        @Override
        public void set(ByteBuffer bytes) {
            output.add(bytes != null ? codec.decodeKey(bytes) : null);
        }

        @Override
        public void set(long integer) {
            output.add("" + integer);
        }
    }
}
