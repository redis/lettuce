/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.codec;

import java.nio.ByteBuffer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import io.lettuce.core.protocol.LettuceCharsets;

/**
 * Benchmark for {@link Utf8StringCodec}.
 *
 * @author Mark Paluch
 */
public class Utf8StringCodecBenchmark {

    @Benchmark
    public void encodeUnpooled(Input input) {
        input.blackhole.consume(input.codec.encodeKey(input.teststring));
    }

    @Benchmark
    public void decodeUnpooled(Input input) {
        input.input.rewind();
        input.blackhole.consume(input.codec.decodeKey(input.input));
    }

    @State(Scope.Thread)
    public static class Input {

        Blackhole blackhole;
        Utf8StringCodec codec = new Utf8StringCodec();

        String teststring = "hello üäü~∑†®†ª€∂‚¶¢ Wørld";
        ByteBuffer input = ByteBuffer.wrap(teststring.getBytes(LettuceCharsets.UTF8));

        @Setup
        public void setup(Blackhole bh) {
            blackhole = bh;
            input.flip();
        }
    }
}
