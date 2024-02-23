package io.lettuce.core.codec;

import io.lettuce.core.protocol.CommandArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.units.qual.C;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark to measure perf gains when codec knows the exact byte size when encoding args
 *
 * @author shikharid
 */
public class ExactVsEstimatedSizeCodecBenchmark {

    @Benchmark
    public void encodeKeyExactSize(Input input, Blackhole blackhole) {
        encodeKey(input.testBytes, ByteArrayCodec.INSTANCE, input.target);
        blackhole.consume(input.target);
        input.target.clear();
    }

    @Benchmark
    public void encodeKeyEstimatedSize(Input input, Blackhole blackhole) {
        encodeKey(input.testBytes, EstimatedSizeByteArrayCodec.INSTANCE, input.target);
        blackhole.consume(input.target);
        input.target.clear();
    }

    @Benchmark
    public void encodeValueExactSize(Input input, Blackhole blackhole) {
        encodeValue(input.testBytes, ByteArrayCodec.INSTANCE, input.target);
        blackhole.consume(input.target);
        input.target.clear();
    }

    @Benchmark
    public void encodeValueEstimatedSize(Input input, Blackhole blackhole) {
        encodeValue(input.testBytes, EstimatedSizeByteArrayCodec.INSTANCE, input.target);
        blackhole.consume(input.target);
        input.target.clear();
    }

    private static void encodeKey(byte[] key, RedisCodec<byte[], byte[]> codec, ByteBuf target) {
        CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(codec);
        commandArgs.addKey(key);
        commandArgs.encode(target);
    }

    private static void encodeValue(byte[] value, RedisCodec<byte[], byte[]> codec, ByteBuf target) {
        CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(codec);
        commandArgs.addValue(value);
        commandArgs.encode(target);
    }

    @State(Scope.Thread)
    public static class Input {
        final byte[] testBytes = "some (not-so-)random bytes".getBytes();

        /*
            By default, used an Unpooled heap buffer so that "GC" specific improvements are visible in benchmark thorugh profiling

            But Using a pooled direct buffer gives us the FULL story for most real world uses
                Most usages are of a direct pooled bytebuf allocator for Netty, Which has its own jemalloc based GC

            Replace this with a pooled direct allocator to see real-world gains
                Note that GC profiling in that case won't show much diff, as we only save a couple of allocs afa heap is concerned
                But you will still see the perf gains
         */
        final ByteBuf target = Unpooled.buffer(512);
        //final ByteBuf target = PooledByteBufAllocator.DEFAULT.directBuffer(512);
    }

    // Emulates older ByteArrayCodec behaviour (no concept of exact estimates)
    public static class EstimatedSizeByteArrayCodec extends ByteArrayCodec {

        public static final EstimatedSizeByteArrayCodec INSTANCE = new EstimatedSizeByteArrayCodec();

        @Override
        public boolean isEstimateExact() {
            return false;
        }

    }
}
