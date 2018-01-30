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
package io.lettuce.core.output;

import java.nio.ByteBuffer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import io.lettuce.core.codec.ByteArrayCodec;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class ValueListOutputBenchmark {

    private static final ByteArrayCodec CODEC = ByteArrayCodec.INSTANCE;
    private final ByteBuffer BUFFER = ByteBuffer.wrap(new byte[0]);

    @Benchmark
    public void measureZeroElement() {

        ValueListOutput<byte[], byte[]> output = new ValueListOutput<>(CODEC);
        output.multi(0);
        output.complete(1);
    }

    @Benchmark
    public void measureSingleElement() {

        ValueListOutput<byte[], byte[]> output = new ValueListOutput<>(CODEC);
        output.multi(1);
        output.set(BUFFER);
        output.complete(1);
    }

    @Benchmark
    public void measure16Elements() {

        ValueListOutput<byte[], byte[]> output = new ValueListOutput<>(CODEC);
        output.multi(16);
        for (int i = 0; i < 16; i++) {
            output.set(BUFFER);
        }
        output.complete(1);
    }

    @Benchmark
    public void measure16ElementsWithResizeElement() {

        ValueListOutput<byte[], byte[]> output = new ValueListOutput<>(CODEC);
        output.multi(10);
        for (int i = 0; i < 16; i++) {
            output.set(BUFFER);
        }
        output.complete(1);
    }

    @Benchmark
    public void measure100Elements() {

        ValueListOutput<byte[], byte[]> output = new ValueListOutput<>(CODEC);
        output.multi(100);
        for (int i = 0; i < 100; i++) {
            output.set(BUFFER);
        }
        output.complete(1);
    }

    @Benchmark
    public void measure100ElementsWithResizeElement() {

        ValueListOutput<byte[], byte[]> output = new ValueListOutput<>(CODEC);
        output.multi(10);
        for (int i = 0; i < 100; i++) {
            output.set(BUFFER);
        }
        output.complete(1);
    }
}
