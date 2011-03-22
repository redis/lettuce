// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

import java.util.concurrent.BlockingQueue;

/**
 * A netty {@link ChannelHandler} responsible for writing redis pub/sub commands
 * and reading the response stream from the server.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class PubSubCommandHandler<K, V> extends CommandHandler {
    private RedisCodec<K, V> codec;

    /**
     * Initialize a new instance.
     *
     * @param queue Command queue.
     * @param codec Codec.
     */
    public PubSubCommandHandler(BlockingQueue<Command<?>> queue, RedisCodec<K, V> codec) {
        super(queue);
        this.codec = codec;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ChannelBuffer buffer) throws InterruptedException {
        while (!queue.isEmpty()) {
            CommandOutput<?> output = queue.peek().getOutput();
            if (!rsm.decode(buffer, output)) return;
            queue.take().complete();
            if (output instanceof PubSubOutput) Channels.fireMessageReceived(ctx, output);
        }

        PubSubOutput<V> output = new PubSubOutput<V>(codec);
        while (rsm.decode(buffer, output)) {
            Channels.fireMessageReceived(ctx, output);
            output = new PubSubOutput<V>(codec);
        }
    }

}
