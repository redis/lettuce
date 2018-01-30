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
import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import io.lettuce.core.protocol.LettuceCharsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Benchmark for {@link StringCodec}.
 *
 * @author Mark Paluch
 */
public class StringCodecBenchmark {

    @Benchmark
    public void encodeUtf8Unpooled(Input input) {
        input.blackhole.consume(input.utf8Codec.encodeKey(input.teststring));
    }

    @Benchmark
    public void encodeUtf8ToBuf(Input input) {
        input.byteBuf.clear();
        input.utf8Codec.encode(input.teststring, input.byteBuf);
    }

    @Benchmark
    public void encodeUtf8PlainStringToBuf(Input input) {
        input.byteBuf.clear();
        input.utf8Codec.encode(input.teststringPlain, input.byteBuf);
    }

    @Benchmark
    public void encodeAsciiToBuf(Input input) {
        input.byteBuf.clear();
        input.asciiCodec.encode(input.teststringPlain, input.byteBuf);
    }

    @Benchmark
    public void encodeIsoToBuf(Input input) {
        input.byteBuf.clear();
        input.isoCodec.encode(input.teststringPlain, input.byteBuf);
    }

    @Benchmark
    public void decodeUtf8Unpooled(Input input) {
        input.input.rewind();
        input.blackhole.consume(input.utf8Codec.decodeKey(input.input));
    }

    @State(Scope.Thread)
    public static class Input {

        Blackhole blackhole;
        StringCodec asciiCodec = new StringCodec(LettuceCharsets.ASCII);
        StringCodec utf8Codec = new StringCodec(LettuceCharsets.UTF8);
        StringCodec isoCodec = new StringCodec(StandardCharsets.ISO_8859_1);

        String teststring = "hello üäü~∑†®†ª€∂‚¶¢ Wørld";
        String teststringPlain = "hello uufadsfasdfadssdfadfs";
        ByteBuffer input = ByteBuffer.wrap(teststring.getBytes(LettuceCharsets.UTF8));

        ByteBuf byteBuf = Unpooled.buffer(512);

        @Setup
        public void setup(Blackhole bh) {
            blackhole = bh;
            input.flip();
        }
    }
}
