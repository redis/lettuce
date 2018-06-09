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
package io.lettuce.core.protocol;

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
        defaultEndpoint.reset();
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
