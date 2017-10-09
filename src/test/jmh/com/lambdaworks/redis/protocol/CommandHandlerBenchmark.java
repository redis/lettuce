/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.protocol;

import java.util.Collection;

import org.openjdk.jmh.annotations.*;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.output.ValueOutput;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Benchmark for {@link CommandHandler}. Test cases:
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
    private final static ClientOptions CLIENT_OPTIONS = ClientOptions.builder().build();
    private final static EmptyContext CHANNEL_HANDLER_CONTEXT = new EmptyContext();
    private final static byte[] KEY = "key".getBytes();
    private final static EmptyPromise EMPTY = new EmptyPromise();

    private CommandHandler commandHandler;
    private Collection<?> stack;
    private Command command;

    @Setup
    public void setup() {

        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE) {
            @Override
            protected void setState(LifecycleState lifecycleState) {
                CommandHandlerBenchmark.this.stack = super.stack;
                super.setState(lifecycleState);
            }
        };

        command = new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));

        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);

        commandHandler.channel = new MyLocalChannel();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        commandHandler.reset();
        stack.clear();
    }

    @Benchmark
    public void measureUserWrite() {
        commandHandler.write(command);
    }

    @Benchmark
    public void measureNettyWrite() throws Exception {
        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, EMPTY);
        stack.remove(command);
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
