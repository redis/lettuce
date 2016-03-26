// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.resource.ClientResources;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.redis.RedisFrame;
import io.netty.util.ReferenceCounted;

import java.util.Queue;

/**
 * A netty {@link ChannelHandler} responsible for writing redis pub/sub commands and reading the response stream from the
 * server.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * 
 * @author Will Glozer
 */
@ChannelHandler.Sharable
public class PubSubCommandHandler<K, V> extends CommandHandler<K, V> {
    private RedisCodec<K, V> codec;
    private PubSubOutput<K, V, V> output;

    /**
     * Initialize a new instance.
     * 
     * @param clientOptions client options for the connection
     * @param clientResources client resources for this connection
     * @param queue Command queue.
     * @param codec Codec.
     */
    public PubSubCommandHandler(ClientOptions clientOptions, ClientResources clientResources,
            Queue<RedisCommand<K, V, ?>> queue, RedisCodec<K, V> codec) {
        super(clientOptions, clientResources, queue);
        this.codec = codec;
        this.output = new PubSubOutput<>(codec);
    }
    
    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // TODO: Cleanup quickhack to use RedisSegment
        if (msg instanceof RedisFrame) {

            if (output.type() == null && !queue.isEmpty()) {
                CommandOutput<K, V, ?> currentOutput = queue.peek().getOutput();
                if (!segmentStateMachine.decode((RedisFrame) msg, currentOutput)) {
                    return;
                }

                queue.poll().complete();
                if (currentOutput instanceof PubSubOutput) {
                    ctx.fireChannelRead(currentOutput);
                }
                return;
            }

            if (segmentStateMachine.decode((RedisFrame) msg, output)) {
                if (msg instanceof ReferenceCounted) {
                    ((ReferenceCounted) msg).release();
                }
                ctx.fireChannelRead(output);
                output = new PubSubOutput<K, V, V>(codec);
                buffer.discardReadBytes();
            }

            return;
        }
        
        super.channelRead(ctx, msg);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {
        
        while (output.type() == null && !queue.isEmpty()) {
            CommandOutput<K, V, ?> currentOutput = queue.peek().getOutput();
            if (!rsm.decode(buffer, currentOutput)) {
                return;
            }
            queue.poll().complete();
            buffer.discardReadBytes();
            if (currentOutput instanceof PubSubOutput) {
                ctx.fireChannelRead(currentOutput);
            }
        }

        while (rsm.decode(buffer, output)) {
            ctx.fireChannelRead(output);
            output = new PubSubOutput<K, V, V>(codec);
            buffer.discardReadBytes();
        }
    }

}
