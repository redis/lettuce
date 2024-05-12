package io.lettuce.core.codec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

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
        ByteBuffer input = ByteBuffer.wrap(teststring.getBytes(StandardCharsets.UTF_8));

        @Setup
        public void setup(Blackhole bh) {
            blackhole = bh;
            input.flip();
        }
    }
}
