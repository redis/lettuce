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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

import com.lambdaworks.redis.protocol.CommandArgs.ExperimentalByteArrayCodec;
import org.openjdk.jmh.annotations.*;

import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.ValueOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;

/**
 * Benchmark for {@link Command}. Test cases:
 * <ul>
 * <li>Create commands using String and ByteArray codecs</li>
 * <li>Encode commands using String and ByteArray codecs</li>
 * </ul>
 *
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class CommandBenchmark {

    private final static ByteArrayCodec BYTE_ARRAY_CODEC = new ByteArrayCodec();
    private final static ExperimentalByteArrayCodec BYTE_ARRAY_CODEC2 = ExperimentalByteArrayCodec.INSTANCE;
    private final static Utf8StringCodec STRING_CODEC = new Utf8StringCodec();
    private final static EmptyByteBuf DUMMY_BYTE_BUF = new EmptyByteBuf();

    private final static String KEY = "key";
    private final static byte[] BYTE_KEY = "key".getBytes();

    @Benchmark
    public void createCommandUsingByteArrayCodec() {
        createCommand(BYTE_KEY, BYTE_ARRAY_CODEC);
    }

    @Benchmark
    public void createCommandUsingStringCodec() {
        createCommand(KEY, STRING_CODEC);
    }

    @Benchmark
    public void encodeCommandUsingByteArrayCodec() {
        createCommand(BYTE_KEY, BYTE_ARRAY_CODEC).encode(DUMMY_BYTE_BUF);
    }

    @Benchmark
    public void encodeCommandUsingByteArrayCodec2() {
        createCommand(BYTE_KEY, BYTE_ARRAY_CODEC2).encode(DUMMY_BYTE_BUF);
    }

    @Benchmark
    public void encodeCommandUsingStringCodec() {
        createCommand(KEY, STRING_CODEC).encode(DUMMY_BYTE_BUF);
    }

    private <K, V, T> Command<K, V, T> createCommand(K key, RedisCodec<K, V> codec) {
        Command command = new Command(CommandType.GET, new ValueOutput<>(codec), new CommandArgs(codec).addKey(key));
        return command;
    }


}
