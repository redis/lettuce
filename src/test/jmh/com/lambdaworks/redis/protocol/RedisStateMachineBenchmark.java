/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.protocol;

import java.nio.ByteBuffer;
import java.util.List;

import org.openjdk.jmh.annotations.*;

import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.output.ArrayOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class RedisStateMachineBenchmark {

    private final static ByteArrayCodec BYTE_ARRAY_CODEC = new ByteArrayCodec();

    private final static Command<byte[], byte[], List<Object>> byteArrayCommand = new Command<>(CommandType.GET,
            new ArrayOutput(BYTE_ARRAY_CODEC) {

                @Override
                public void set(ByteBuffer bytes) {

                }

                @Override
                public void multi(int count) {

                }

                @Override
                public void complete(int depth) {
                }

                @Override
                public void set(long integer) {
                }
            }, new CommandArgs(BYTE_ARRAY_CODEC).addKey(new byte[] { 1, 2, 3, 4 }));

    private ByteBuf masterBuffer;

    private final RedisStateMachine<byte[], byte[]> stateMachine = new RedisStateMachine<>();
    private final byte[] payload = ("*3\r\n" + //
            "$4\r\n" + //
            "LLEN\r\n" + //
            "$6\r\n" + //
            "mylist\r\n" + //
            "+QUEUED\r\n" + //
            ":12\r\n").getBytes();

    @Setup(Level.Trial)
    public void setup() {
        masterBuffer = PooledByteBufAllocator.DEFAULT.ioBuffer(32);
        masterBuffer.writeBytes(payload);
    }

    @TearDown
    public void tearDown() {
        masterBuffer.release();
    }

    @Benchmark
    public void measureDecode() {
        stateMachine.decode(masterBuffer, byteArrayCommand, byteArrayCommand.getOutput());
        masterBuffer.readerIndex(0);
    }

    public static void main(String[] args) {

        RedisStateMachineBenchmark b = new RedisStateMachineBenchmark();
        b.setup();
        while (true) {
            b.measureDecode();
        }
    }
}
