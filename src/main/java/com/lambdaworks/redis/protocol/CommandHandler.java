// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisException;

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
    protected BlockingQueue<RedisCommand<K, V, ?>> commandBuffer = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();
    protected ByteBuf buffer;
    protected RedisStateMachine<K, V> rsm;
    private Channel channel;
    private boolean closed;
    private boolean connected;
    private RedisChannelHandler<K, V> redisChannelHandler;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ReentrantLock readLock = new ReentrantLock();
    private Throwable connectionError;

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
        closed = false;
        buffer = ctx.alloc().heapBuffer();
        rsm = new RedisStateMachine<K, V>();
        channel = ctx.channel();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        releaseBuffer();
        if (closed) {
            cancelCommands();
        }
        channel = null;
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

            try {
                readLock.lock();
                buffer.writeBytes(input);

                if (logger.isTraceEnabled()) {
                    logger.trace("[" + ctx.channel().remoteAddress() + "] Received: "
                            + buffer.toString(Charset.defaultCharset()).trim());
                }

                decode(ctx, buffer);
            } finally {
                readLock.unlock();
            }

        } finally {
            input.release();
        }
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {

        while (!queue.isEmpty() && rsm.decode(buffer, queue.peek(), queue.peek().getOutput())) {
            RedisCommand<K, V, ?> cmd = queue.take();
            cmd.complete();
            if (buffer != null && buffer.refCnt() != 0) {
                buffer.discardReadBytes();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!queue.isEmpty()) {
            RedisCommand<K, V, ?> command = queue.take();
            command.setException(cause);
            command.complete();
        }

        if (channel == null || !connected) {
            connectionError = cause;
            return;
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public <T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        try {

            if (closed) {
                throw new RedisException("Connection is closed");
            }

            try {
                writeLock.lock();
                if (channel != null) {
                    if (logger.isDebugEnabled()) {

                        logger.debug("[" + this + "] write() writeAndFlush Command " + command);
                    }
                    channel.writeAndFlush(command);
                } else {

                    if (connectionError != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("[" + this + "] write() completing Command " + command + " due to connection error");
                        }
                        command.setException(connectionError);
                        command.complete();
                        return command;
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("[" + this + "] write() buffering Command " + command);
                    }
                    commandBuffer.put(command);
                }
            } finally {
                writeLock.unlock();
            }

        } catch (InterruptedException e) {
            throw new RedisCommandInterruptedException(e);
        }

        return command;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        connected = true;
        closed = false;
        logger.debug("[" + this + "] channelActive()");
        List<RedisCommand<K, V, ?>> tmp = new ArrayList<RedisCommand<K, V, ?>>(queue.size() + commandBuffer.size());

        try {
            writeLock.lock();

            tmp.addAll(commandBuffer);
            tmp.addAll(queue);

            queue.clear();
            commandBuffer.clear();

            channel = ctx.channel();

            if (redisChannelHandler != null) {
                redisChannelHandler.activated();
            }

        } finally {
            writeLock.unlock();
        }

        for (RedisCommand<K, V, ?> cmd : tmp) {
            if (!cmd.isCancelled()) {

                if (logger.isDebugEnabled()) {
                    logger.debug("[" + this + "] channelActive() triggering command " + cmd);
                }
                ctx.channel().writeAndFlush(cmd);
            }
        }

        tmp.clear();

        logger.debug("[" + this + "] channelActive() done");
        super.channelActive(ctx);
    }

    /**
     * 
     * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.ChannelHandlerContext, java.lang.Object,
     *      io.netty.channel.ChannelPromise)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (msg instanceof RedisCommand) {
            final RedisCommand<K, V, ?> cmd = (RedisCommand<K, V, ?>) msg;
            ByteBuf buf = ctx.alloc().heapBuffer();
            cmd.encode(buf);

            if (logger.isTraceEnabled()) {
                logger.trace("[" + ctx.channel().remoteAddress() + "] Sent: " + buf.toString(Charset.defaultCharset()).trim());
            }

            if (cmd.getOutput() == null) {
                ctx.write(buf, promise);
                cmd.complete();
            } else {
                queue.put(cmd);
                ctx.write(buf, promise);
            }
            return;
        }
        super.write(ctx, msg, promise);

    }

    /**
     * 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("[" + this + "] channelInactive()");
        connected = false;

        if (redisChannelHandler != null) {
            redisChannelHandler.deactivated();
        }

        logger.debug("[" + this + "] channelInactive() done");
        super.channelInactive(ctx);
    }

    private void cancelCommands() {
        int size = 0;
        if (queue != null) {
            size += queue.size();
        }

        if (commandBuffer != null) {
            size += commandBuffer.size();
        }

        List<RedisCommand<K, V, ?>> toCancel = new ArrayList<RedisCommand<K, V, ?>>(size);

        if (queue != null) {
            toCancel.addAll(queue);
            queue.clear();
        }

        if (commandBuffer != null) {
            toCancel.addAll(commandBuffer);
            commandBuffer.clear();
        }

        for (RedisCommand<K, V, ?> cmd : toCancel) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError("Connection closed");
            }
            cmd.complete();
        }
    }

    /**
     * Close the connection.
     */
    @Override
    public void close() {

        logger.debug("[" + this + "] close()");

        if (closed) {
            return;
        }

        closed = true;
        Channel channel = this.channel;
        if (channel != null) {
            channel.pipeline().fireUserEventTriggered(new ConnectionEvents.PrepareClose());
            channel.pipeline().fireUserEventTriggered(new ConnectionEvents.Close());

            try {
                channel.closeFuture().await();
            } catch (InterruptedException e) {
                throw new RedisException(e);
            }
        }
    }

    private void releaseBuffer() {
        if (buffer != null) {
            try {
                readLock.lock();
                buffer.release();
            } finally {
                readLock.unlock();
            }
            buffer = null;
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
