// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;

/**
 * A netty {@link ChannelHandler} responsible for writing redis commands and reading responses from the server.
 * 
 * @author Will Glozer
 */
@ChannelHandler.Sharable
public class CommandHandler<K, V> extends ChannelDuplexHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandHandler.class);
    protected BlockingQueue<Command<K, V, ?>> queue;
    protected ByteBuf buffer;
    protected RedisStateMachine<K, V> rsm;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     * 
     * @param queue The command queue.
     */
    public CommandHandler(BlockingQueue<Command<K, V, ?>> queue) {
        this.queue = queue;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        buffer = ctx.alloc().heapBuffer();
        rsm = new RedisStateMachine<K, V>();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        buffer.release();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf input = (ByteBuf) msg;
        try {
            if (!input.isReadable())
                return;

            buffer.discardReadBytes();
            buffer.writeBytes(input);

            if (logger.isDebugEnabled()) {
                logger.debug("Received: " + buffer.toString(Charset.defaultCharset()).trim());
            }

            decode(ctx, buffer);
        } finally {
            input.release();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Command<?, ?, ?> cmd = (Command<?, ?, ?>) msg;
        Channel channel = ctx.channel();
        ByteBuf buf = ctx.alloc().heapBuffer();
        cmd.encode(buf);
        if (logger.isDebugEnabled()) {
            logger.debug("Sent: " + buf.toString(Charset.defaultCharset()).trim());
        }
        ctx.write(buf, promise);
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {
        while (!queue.isEmpty() && rsm.decode(buffer, queue.peek().getOutput())) {
            Command<K, V, ?> cmd = queue.take();
            cmd.complete();
        }
    }
}
