// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
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
    protected Queue<RedisCommand<K, V, ?>> queue;
    protected Queue<RedisCommand<K, V, ?>> commandBuffer = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();
    protected ByteBuf buffer;
    protected RedisStateMachine<K, V> rsm;

    private Channel channel;
    private boolean closed;
    private boolean connected;
    private RedisChannelHandler<K, V> redisChannelHandler;
    private final ReentrantLock writeLock = new ReentrantLock();
    private Throwable connectionError;
    private String logPrefix;

    /**
     * If TRACE level logging has been enabled at startup.
     */
    private final boolean traceEnabled;

    /**
     * If DEBUG level logging has been enabled at startup.
     */
    private final boolean debugEnabled;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection
     * @param queue The command queue
     */
    public CommandHandler(ClientOptions clientOptions, Queue<RedisCommand<K, V, ?>> queue) {
        this.clientOptions = clientOptions;
        this.queue = queue;
        traceEnabled = logger.isTraceEnabled();
        debugEnabled = logger.isDebugEnabled();
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

        if (!input.isReadable() || input.refCnt() == 0 || buffer == null) {
            return;
        }

        try {
            buffer.writeBytes(input);

            if (traceEnabled) {
                logger.trace("{} Received: {}", logPrefix(), buffer.toString(Charset.defaultCharset()).trim());
            }

            decode(ctx, buffer);
        } finally {
            input.release();
        }
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {

        while (!queue.isEmpty() && rsm.decode(buffer, queue.peek(), queue.peek().getOutput())) {
            RedisCommand<K, V, ?> cmd = queue.poll();
            cmd.complete();
            if (buffer != null && buffer.refCnt() != 0) {
                buffer.discardReadBytes();
            }
        }
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C write(C command) {

        if (closed) {
            throw new RedisException("Connection is closed");
        }
        try {
            writeLock.lock();
            Channel channel = this.channel;
            if (channel != null && connected) {
                if (debugEnabled) {
                    logger.debug("{} write() writeAndFlush Command {}", logPrefix(), command);
                }
                channel.writeAndFlush(command);
            } else {

                if (connectionError != null) {
                    if (debugEnabled) {
                        logger.debug("{} write() completing Command {} due to connection error", logPrefix(), command);
                    }
                    command.completeExceptionally(connectionError);
                    return command;
                }

                if (debugEnabled) {
                    logger.debug("{} write() buffering Command {}", logPrefix(), command);
                }
                commandBuffer.add(command);
            }
        } finally {
            writeLock.unlock();
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return command;
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

        if (cmd.getOutput() == null) {
            cmd.complete();
        } else {
            queue.add(cmd);
        }

        ctx.write(cmd, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logPrefix = null;
        if (debugEnabled) {
            logger.debug("{} channelActive()", logPrefix());
        }
        connected = true;
        closed = false;

        try {
            executeQueuedCommands(ctx);
        } catch (Exception e) {
            if (debugEnabled) {
                logger.debug("{} channelActive() ran into an exception", logPrefix());
            }
            if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                reset();
            }
            throw e;
        }

        super.channelActive(ctx);
        if (debugEnabled) {
            logger.debug("{} channelActive() done", logPrefix());
        }
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

            if (debugEnabled) {
                logger.debug("{} executeQueuedCommands {} command(s) queued", logPrefix(), queue.size());
            }
            channel = ctx.channel();

            if (redisChannelHandler != null) {
                if (debugEnabled) {
                    logger.debug("{} activating channel handler", logPrefix());
                }
                redisChannelHandler.activated();
            }

            tmp.stream().filter(cmd -> !cmd.isCancelled()).forEach(cmd -> {

                if (debugEnabled) {
                    logger.debug("{} channelActive() triggering command {}", logPrefix(), cmd);
                }
                ctx.channel().write(cmd);
            });

            ctx.channel().flush();

            tmp.clear();

        } finally {
            writeLock.unlock();
        }

    }

    /**
     * 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (debugEnabled) {
            logger.debug("{} channelInactive()", logPrefix());
        }
        connected = false;

        if (redisChannelHandler != null) {
            if (debugEnabled) {
                logger.debug("{} deactivating channel handler", logPrefix());
            }
            redisChannelHandler.deactivated();
        }

        if (debugEnabled) {
            logger.debug("{} channelInactive() done", logPrefix());
        }
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!queue.isEmpty()) {
            RedisCommand<K, V, ?> command = queue.poll();
            command.completeExceptionally(cause);
        }

        if (channel == null || !connected) {
            connectionError = cause;
            return;
        }
        super.exceptionCaught(ctx, cause);
    }

    /**
     * Close the connection.
     */
    @Override
    public void close() {

        if (debugEnabled) {
            logger.debug("{} close()", logPrefix());
        }

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
            buffer.release();
            buffer = null;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Reset the writer state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection.
     */
    @Override
    public void reset() {
        if (debugEnabled) {
            logger.debug("{} reset()", logPrefix());
        }
        try {
            writeLock.lock();
            cancelCommands("Reset");
        } finally {
            writeLock.unlock();
        }

        if (buffer != null) {
            rsm.reset();
            buffer.clear();
        }
    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        this.redisChannelHandler = redisChannelHandler;
    }

    private String logPrefix() {
        if (logPrefix != null) {
            return logPrefix;
        }
        StringBuffer buffer = new StringBuffer(16);
        buffer.append('[');
        if (channel != null) {
            buffer.append(channel.remoteAddress());
        } else {
            buffer.append("not connected");
        }
        buffer.append(']');
        return logPrefix = buffer.toString();
    }

}
