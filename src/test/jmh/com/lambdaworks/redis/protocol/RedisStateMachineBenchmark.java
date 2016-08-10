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

    private final RedisStateMachine stateMachine = new RedisStateMachine();
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
        stateMachine.decode(masterBuffer.duplicate(), byteArrayCommand, byteArrayCommand.getOutput());
    }

    public static void main(String[] args) {

        RedisStateMachineBenchmark b = new RedisStateMachineBenchmark();
        b.setup();
        while (true) {
            b.measureDecode();
        }
    }
}
