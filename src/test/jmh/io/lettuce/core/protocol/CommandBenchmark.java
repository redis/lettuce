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
package io.lettuce.core.protocol;

import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.ValueOutput;

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
    private final static Utf8StringCodec OLD_STRING_CODEC = new Utf8StringCodec();
    private final static StringCodec NEW_STRING_CODEC = new StringCodec(StandardCharsets.UTF_8);
    private final static EmptyByteBuf DUMMY_BYTE_BUF = new EmptyByteBuf();

    private final static String KEY = "key";
    private final static byte[] BYTE_KEY = "key".getBytes();

    @Benchmark
    public void createCommandUsingByteArrayCodec() {
        createCommand(BYTE_KEY, BYTE_ARRAY_CODEC);
    }

    @Benchmark
    public void createCommandUsingStringCodec() {
        createCommand(KEY, OLD_STRING_CODEC);
    }

    @Benchmark
    public void encodeCommandUsingByteArrayCodec() {
        createCommand(BYTE_KEY, BYTE_ARRAY_CODEC).encode(DUMMY_BYTE_BUF);
    }

    @Benchmark
    public void encodeCommandUsingOldStringCodec() {
        createCommand(KEY, OLD_STRING_CODEC).encode(DUMMY_BYTE_BUF);
    }

    @Benchmark
    public void encodeCommandUsingNewStringCodec() {
        createCommand(KEY, NEW_STRING_CODEC).encode(DUMMY_BYTE_BUF);
    }

    private <K, V, T> Command<K, V, T> createCommand(K key, RedisCodec<K, V> codec) {
        Command command = new Command(CommandType.GET, new ValueOutput<>(codec), new CommandArgs(codec).addKey(key));
        return command;
    }

}
