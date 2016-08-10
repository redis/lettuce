// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

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
public class PubSubCommandHandler<K, V> extends CommandHandler {

    private PubSubEndpoint<K, V> endpoint;
    private RedisCodec<K, V> codec;
    private PubSubOutput<K, V, V> output;

    /**
     * Initialize a new instance.
     * 
     * @param clientResources client resources for this connection
     * @param codec Codec.
     * @param endpoint the Pub/Sub endpoint for Pub/Sub callback.
     */
    public PubSubCommandHandler(ClientResources clientResources, RedisCodec<K, V> codec, PubSubEndpoint<K, V> endpoint) {

        super(clientResources, endpoint);

        this.codec = codec;
        this.output = new PubSubOutput<>(codec);
        this.endpoint = endpoint;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {

        while (output.type() == null && !getQueue().isEmpty()) {
            CommandOutput<?, ?, ?> currentOutput = getQueue().peek().getOutput();

            if (!super.decode(buffer, currentOutput)) {
                return;
            }

            getQueue().poll().complete();

            buffer.discardReadBytes();

            if (currentOutput instanceof PubSubOutput) {
                endpoint.notifyMessage((PubSubOutput) currentOutput);
            }
        }

        while (super.decode(buffer, output)) {

            endpoint.notifyMessage(output);
            output = new PubSubOutput<>(codec);

            buffer.discardReadBytes();
        }
    }
}
