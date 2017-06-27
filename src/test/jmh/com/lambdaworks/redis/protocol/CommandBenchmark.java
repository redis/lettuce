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
package com.lambdaworks.redis.protocol;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.ValueOutput;
import com.lambdaworks.redis.protocol.CommandArgs.ExperimentalByteArrayCodec;

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
    public void createCommandUsingByteArrayCodec(Blackhole blackhole) {
        blackhole.consume(createCommand(BYTE_KEY, BYTE_ARRAY_CODEC));
    }

    @Benchmark
    public void createAsyncCommandUsingByteArrayCodec(Blackhole blackhole) {
        blackhole.consume(new AsyncCommand<>(createCommand(BYTE_KEY, BYTE_ARRAY_CODEC)));
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
