package io.lettuce.core.protocol;

import io.lettuce.core.ConnectionEvents;
import org.openjdk.jmh.annotations.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.EmptyStatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ValueOutput;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Benchmark for {@link DefaultEndpoint}.
 * <p>
 * Test cases:
 * <ul>
 * <li>user command writes</li>
 * </ul>
 *
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class RedisEndpointBenchmark {

    private static final ByteArrayCodec CODEC = new ByteArrayCodec();
    private static final ClientOptions CLIENT_OPTIONS = ClientOptions.create();
    private static final byte[] KEY = "key".getBytes();
    private static final ChannelFuture EMPTY = new EmptyFuture();

    private DefaultEndpoint defaultEndpoint;
    private Command command;

    @Setup
    public void setup() {

        defaultEndpoint = new DefaultEndpoint(CLIENT_OPTIONS, EmptyClientResources.INSTANCE);
        command = new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));

        defaultEndpoint.setConnectionFacade(EmptyStatefulRedisConnection.INSTANCE);
        defaultEndpoint.notifyChannelActive(new MyLocalChannel());
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        defaultEndpoint.channel.pipeline().fireUserEventTriggered(new ConnectionEvents.Reset());
    }

    @Benchmark
    public void measureUserWrite() {
        defaultEndpoint.write(command);
    }

    private static final class MyLocalChannel extends EmbeddedChannel {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public ChannelFuture write(Object msg) {
            return EMPTY;
        }

        @Override
        public ChannelFuture write(Object msg, ChannelPromise promise) {
            return promise;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            return EMPTY;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            return promise;
        }
    }

}
