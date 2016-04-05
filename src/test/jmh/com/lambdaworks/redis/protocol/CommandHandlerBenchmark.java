package com.lambdaworks.redis.protocol;

import java.util.ArrayDeque;

import org.openjdk.jmh.annotations.*;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.output.ValueOutput;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Benchmark for {@link Command}. Test cases:
 * <ul>
 * <li>user command writes</li>
 * <li>netty (in-eventloop) writes</li>
 * </ul>
 * 
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class CommandHandlerBenchmark {

    private final static ByteArrayCodec CODEC = new ByteArrayCodec();
    private final static ClientOptions CLIENT_OPTIONS = ClientOptions.create();
    private final static EmptyContext CHANNEL_HANDLER_CONTEXT = new EmptyContext();
    private final static byte[] KEY = "key".getBytes();
    private final static ChannelFuture EMPTY = new EmptyFuture();

    private CommandHandler commandHandler;
    private Command command;

    @Setup
    public void setup() {

        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE, new ArrayDeque<>(512));
        command = new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));

        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);

        commandHandler.channel = new MyLocalChannel();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        commandHandler.reset();
    }

    @Benchmark
    public void measureUserWrite() {
        commandHandler.write(command);
    }

    @Benchmark
    public void measureNettyWrite() throws Exception {
        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, null);
    }

    private final static class MyLocalChannel extends EmbeddedChannel {
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
