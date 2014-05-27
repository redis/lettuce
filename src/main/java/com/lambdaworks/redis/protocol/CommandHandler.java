// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.internal.RedisChannelWriter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for writing redis commands and reading responses from the server.
 * 
 * @author Will Glozer
 */
@ChannelHandler.Sharable
public class CommandHandler<K, V> extends ChannelDuplexHandler implements RedisChannelWriter<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandHandler.class);
    protected BlockingQueue<Command<K, V, ?>> queue;
    protected ByteBuf buffer;
    protected RedisStateMachine<K, V> rsm;
    private Command<K, V, ?> authentication;
    private Command<K, V, ?> database;
    private Channel channel;
    private boolean closed;
    private RedisChannelHandler<K, V> redisChannelHandler;

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

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channelActive()");
        this.channel = ctx.channel();

        List<Command<K, V, ?>> tmp = new ArrayList<Command<K, V, ?>>(queue.size() + 2);

        tmp.addAll(queue);
        queue.clear();

        if (redisChannelHandler != null) {
            redisChannelHandler.activated();
        }

        for (Command<K, V, ?> cmd : tmp) {
            if (!cmd.isCancelled()) {
                queue.add(cmd);
                channel.writeAndFlush(cmd);
            }
        }

        tmp.clear();

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channelInactive()");
        if (closed) {
            for (Command<K, V, ?> cmd : queue) {
                if (cmd.getOutput() != null) {
                    cmd.getOutput().setError("Connection closed");
                }
                cmd.complete();
            }
            queue.clear();
            queue = null;
            this.channel = null;
        }

        if (redisChannelHandler != null) {
            redisChannelHandler.deactivated();
        }
    }

    @Override
    public void write(Command<K, V, ?> command) {
        try {

            if (closed) {
                throw new RedisException("Connection is closed");
            }

            queue.put(command);

            if (channel != null) {
                channel.writeAndFlush(command);
            }
        } catch (NullPointerException e) {
            throw new RedisException("Connection is closed");
        } catch (InterruptedException e) {
            throw new RedisCommandInterruptedException(e);
        }

    }

    /**
     * Close the connection.
     */
    public synchronized void close() {
        logger.debug("close()");

        if (closed) {
            logger.warn("Client is already closed");
            return;
        }

        if (!closed && channel != null) {
            ConnectionWatchdog watchdog = channel.pipeline().get(ConnectionWatchdog.class);
            if (watchdog != null) {
                watchdog.setReconnect(false);
            }
            closed = true;
            channel.close();

            channel = null;
        }

    }

    public boolean isClosed() {
        return closed;
    }

    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        this.redisChannelHandler = redisChannelHandler;
    }
}
