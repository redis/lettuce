// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
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

    protected ClientOptions clientOptions;
    protected BlockingQueue<RedisCommand<K, V, ?>> queue;
    protected BlockingQueue<RedisCommand<K, V, ?>> commandBuffer = new LinkedBlockingQueue<>();
    protected ByteBuf buffer;
    protected RedisStateMachine<K, V> rsm;

    private Channel channel;
    private boolean closed;
    private boolean connected;
    private RedisChannelHandler<K, V> redisChannelHandler;
    private Throwable connectionError;

    private final ReentrantLock writeLock = new ReentrantLock();
    private final ReentrantLock readLock = new ReentrantLock();

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection
     * @param queue The command queue
     */
    public CommandHandler(ClientOptions clientOptions, BlockingQueue<RedisCommand<K, V, ?>> queue) {
        this.clientOptions = clientOptions;
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
        rsm = new RedisStateMachine<>();
        channel = ctx.channel();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        releaseBuffer();
        if (closed) {
            cancelCommands("Connection closed");
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
            if (!input.isReadable() || input.refCnt() == 0 || buffer == null) {
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
            command.completeExceptionally(cause);
        }

        if (channel == null || !connected) {
            connectionError = cause;
            return;
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C write(C command) {

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        try {

            writeLock.lock();
            if (channel != null && connected) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[" + this + "] write() writeAndFlush Command " + command);
                }
                channel.writeAndFlush(command);
            } else {

                if (connectionError != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[" + this + "] write() completing Command " + command + " due to connection error");
                    }
                    command.completeExceptionally(connectionError);
                    return command;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("[" + this + "] write() buffering Command " + command);
                }
                commandBuffer.add(command);
            }
        } finally {
            writeLock.unlock();
        }

        return command;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        connected = true;
        closed = false;
        logger.debug("[" + this + "] channelActive()");
        try {
            executeQueuedCommands(ctx);
            logger.debug("[" + this + "] channelActive() done");

        } catch (Exception e) {
            logger.debug("[" + this + "] channelActive() ran into an exception");

            if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                reset();
            }
            throw e;
        }

        super.channelActive(ctx);
    }

    protected void executeQueuedCommands(ChannelHandlerContext ctx) {
        List<RedisCommand<K, V, ?>> tmp = new ArrayList<>(queue.size() + commandBuffer.size());

        try {
            writeLock.lock();
            connectionError = null;

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

        tmp.stream().filter(cmd -> !cmd.isCancelled()).forEach(cmd -> {

            if (logger.isDebugEnabled()) {
                logger.debug("[" + this + "] channelActive() triggering command " + cmd);
            }
            ctx.channel().writeAndFlush(cmd);
        });

        tmp.clear();
    }

    /**
     * 
     * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.ChannelHandlerContext, java.lang.Object,
     *      io.netty.channel.ChannelPromise)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        RedisCommand<K, V, ?> cmd = (RedisCommand<K, V, ?>) msg;
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

    private void cancelCommands(String message) {
        int size = 0;
        if (queue != null) {
            size += queue.size();
        }

        if (commandBuffer != null) {
            size += commandBuffer.size();
        }

        List<RedisCommand<K, V, ?>> toCancel = new ArrayList<>(size);

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
                cmd.getOutput().setError(message);
            }
            cmd.cancel();
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
        Channel currentChannel = this.channel;
        if (currentChannel != null) {
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.PrepareClose());
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.Close());
            currentChannel.closeFuture().syncUninterruptibly();
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

    /**
     * Reset the writer state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection.
     */
    @Override
    public void reset() {
        logger.debug("[" + this + "] reset()");
        try {
            writeLock.lock();
            cancelCommands("Reset");
        } finally {
            writeLock.unlock();
        }

        if (buffer != null) {
            try {
                readLock.lock();
                rsm.reset();
                buffer.clear();
            } finally {
                readLock.unlock();
            }
        }
    }
}
