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
package io.lettuce.core.pubsub;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * A netty {@link ChannelHandler} responsible for writing redis pub/sub commands and reading the response stream from the
 * server.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class PubSubCommandHandler<K, V> extends CommandHandler {

    private final PubSubEndpoint<K, V> endpoint;
    private final RedisCodec<K, V> codec;
    private PubSubOutput<K, V, V> output;

    /**
     * Initialize a new instance.
     *
     * @param clientOptions client options for this connection, must not be {@literal null}
     * @param clientResources client resources for this connection
     * @param codec Codec.
     * @param endpoint the Pub/Sub endpoint for Pub/Sub callback.
     */
    public PubSubCommandHandler(ClientOptions clientOptions, ClientResources clientResources, RedisCodec<K, V> codec,
            PubSubEndpoint<K, V> endpoint) {
        super(clientOptions, clientResources, endpoint);

        this.endpoint = endpoint;
        this.codec = codec;
        this.output = new PubSubOutput<>(codec);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {

        super.decode(ctx, buffer);

        while (buffer.isReadable()) {

            if (!super.decode(buffer, output)) {
                return;
            }

            endpoint.notifyMessage(output);
            output = new PubSubOutput<>(codec);

            buffer.discardReadBytes();
        }
    }

    @Override
    protected boolean canDecode(ByteBuf buffer) {
        return super.canDecode(buffer) && output.type() == null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void afterComplete(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command) {
        if (command.getOutput() instanceof PubSubOutput) {
            endpoint.notifyMessage((PubSubOutput) command.getOutput());
        }
    }
}
