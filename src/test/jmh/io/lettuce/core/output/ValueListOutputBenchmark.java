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
