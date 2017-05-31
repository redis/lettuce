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
package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * A netty {@link ChannelHandler} responsible for writing redis pub/sub commands and reading the response stream from the
 * server.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class PubSubCommandHandler<K, V> extends CommandHandler<K, V> {

    private final RedisCodec<K, V> codec;
    private PubSubOutput<K, V, V> output;

    /**
     * Initialize a new instance.
     *
     * @param clientOptions client options for the connection
     * @param clientResources client resources for this connection
     * @param codec Codec.
     */
    public PubSubCommandHandler(ClientOptions clientOptions, ClientResources clientResources, RedisCodec<K, V> codec) {
        super(clientOptions, clientResources);
        this.codec = codec;
        this.output = new PubSubOutput<>(codec);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) {

        while (output.type() == null && canDecode(buffer)) {
            CommandOutput<K, V, ?> currentOutput = stack.peek().getOutput();
            if (!rsm.decode(buffer, currentOutput)) {
                return;
            }
            stack.poll().complete();
            buffer.discardReadBytes();
            if (currentOutput instanceof PubSubOutput) {
                ctx.fireChannelRead(currentOutput);
            }
        }

        while (rsm.decode(buffer, output)) {
            ctx.fireChannelRead(output);
            output = new PubSubOutput<>(codec);
            buffer.discardReadBytes();
        }
    }

}
