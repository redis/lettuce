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
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
@ChannelHandler.Sharable
public class CommandHandler<K, V> extends ChannelDuplexHandler implements RedisChannelWriter<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandHandler.class);
    protected BlockingQueue<RedisCommand<K, V, ?>> queue;
    protected ByteBuf buffer;
    protected RedisStateMachine<K, V> rsm;
    private Channel channel;
    private boolean closed;
    private RedisChannelHandler<K, V> redisChannelHandler;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     * 
     * @param queue The command queue.
     */
    public CommandHandler(BlockingQueue<RedisCommand<K, V, ?>> queue) {
        this.queue = queue;
    }

    /**
     * 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRegistered(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        buffer = ctx.alloc().heapBuffer();
        rsm = new RedisStateMachine<K, V>();
    }

    /**
     * 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf input = (ByteBuf) msg;
        try {
            if (!input.isReadable() || input.refCnt() == 0) {
                return;
            }

            if (buffer == null) {
                logger.warn("CommandHandler is closed, incoming response will be discarded.");
                return;
            }
            buffer.writeBytes(input);

            if (logger.isDebugEnabled()) {
                logger.debug("[" + channel.remoteAddress() + "] Received: " + buffer.toString(Charset.defaultCharset()).trim());
            }

            decode(ctx, buffer);

        } finally {
            input.release();
        }
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {

        while (!queue.isEmpty() && rsm.decode(buffer, queue.peek(), queue.peek().getOutput())) {
            RedisCommand<K, V, ?> cmd = queue.take();
            cmd.complete();
            buffer.discardReadBytes();
        }
    }

    /**
     * 
     * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.ChannelHandlerContext, java.lang.Object,
     *      io.netty.channel.ChannelPromise)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        final RedisCommand<K, V, ?> cmd = (RedisCommand<K, V, ?>) msg;
        ByteBuf buf = ctx.alloc().heapBuffer();
        cmd.encode(buf);
        if (logger.isDebugEnabled()) {
            logger.debug("[" + channel.remoteAddress() + "] Sent: " + buf.toString(Charset.defaultCharset()).trim());
        }

        synchronized (queue) {
            if (!queue.contains(cmd)) {
                queue.put(cmd);
            }
            ctx.write(buf, promise);
        }

    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {

        logger.debug("channelActive()");
        this.channel = ctx.channel();

        List<RedisCommand<K, V, ?>> tmp = new ArrayList<RedisCommand<K, V, ?>>(queue.size());

        tmp.addAll(queue);
        queue.clear();

        if (redisChannelHandler != null) {
            redisChannelHandler.activated();
        }

        for (RedisCommand<K, V, ?> cmd : tmp) {
            if (!cmd.isCancelled()) {
                logger.debug("Triggering command " + cmd);
                ctx.channel().writeAndFlush(cmd);
            }
        }

        tmp.clear();

    }

    /**
     * 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channelInactive()");
        try {
            this.channel = null;
            if (closed) {
                for (RedisCommand<K, V, ?> cmd : queue) {
                    if (cmd.getOutput() != null) {
                        cmd.getOutput().setError("Connection closed");
                    }
                    cmd.complete();
                }
                queue.clear();
                queue = null;

                if (redisChannelHandler != null) {
                    redisChannelHandler.deactivated();
                }
            }
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public <T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        try {

            if (closed) {
                throw new RedisException("Connection is closed");
            }

            if (channel != null) {
                channel.writeAndFlush(command);
            } else {
                synchronized (queue) {
                    queue.put(command);
                }
            }
        } catch (NullPointerException e) {
            throw new RedisException("Connection is closed");
        } catch (InterruptedException e) {
            throw new RedisCommandInterruptedException(e);
        }

        return command;
    }

    /**
     * Close the connection.
     */
    @Override
    public synchronized void close() {
        logger.debug("close()");

        if (closed) {
            logger.warn("Client is already closed");
            return;
        }

        if (buffer != null) {
            buffer.release();
            buffer = null;
        }

        if (!closed && channel != null) {
            ConnectionWatchdog watchdog = channel.pipeline().get(ConnectionWatchdog.class);
            if (watchdog != null) {
                watchdog.setReconnect(false);
            }
            closed = true;
            try {
                channel.close().sync();
            } catch (InterruptedException e) {
                throw new RedisException(e);
            }

            channel = null;
        }

    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        this.redisChannelHandler = redisChannelHandler;
    }
}
