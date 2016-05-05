// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceFactories;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.internal.LettuceSets;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for writing redis commands and reading responses from the server.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
@ChannelHandler.Sharable
public class CommandHandler<K, V> extends ChannelDuplexHandler implements RedisChannelWriter<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandHandler.class);
    private static final WriteLogListener WRITE_LOG_LISTENER = new WriteLogListener();

    /**
     * When we encounter an unexpected IOException we look for these {@link Throwable#getMessage() messages} (because we have no
     * better way to distinguish) and log them at DEBUG rather than WARN, since they are generally caused by unclean client
     * disconnects rather than an actual problem.
     */
    private static final Set<String> SUPPRESS_IO_EXCEPTION_MESSAGES = LettuceSets.unmodifiableSet("Connection reset by peer", "Broken pipe",
            "Connection timed out");

    protected final ClientOptions clientOptions;
    protected final ClientResources clientResources;
    protected final Queue<RedisCommand<K, V, ?>> queue;
    protected final AtomicLong writers = new AtomicLong();
    protected final Object stateLock = new Object();

    // all access to the commandBuffer is synchronized
    protected final Queue<RedisCommand<K, V, ?>> commandBuffer = LettuceFactories.newConcurrentQueue();
    protected ByteBuf buffer;
    protected RedisStateMachine<K, V> rsm;
    protected Channel channel;

    // If TRACE level logging has been enabled at startup.
    private final boolean traceEnabled;

    // If DEBUG level logging has been enabled at startup.
    private final boolean debugEnabled;
    private final Reliability reliability;

    private volatile LifecycleState lifecycleState = LifecycleState.NOT_CONNECTED;
    private Thread exclusiveLockOwner;
    private RedisChannelHandler<K, V> redisChannelHandler;
    private Throwable connectionError;
    private String logPrefix;
    private boolean autoFlushCommands = true;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection, must not be {@literal null}
     * @param clientResources client resources for this connection, must not be {@literal null}
     * @param queue The command queue, must not be {@literal null}
     */
    public CommandHandler(ClientOptions clientOptions, ClientResources clientResources, Queue<RedisCommand<K, V, ?>> queue) {

        LettuceAssert.notNull(clientOptions, "clientOptions must not be null");
        LettuceAssert.notNull(clientResources, "clientResources must not be null");
        LettuceAssert.notNull(queue, "queue must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.queue = queue;
        this.traceEnabled = logger.isTraceEnabled();
        this.debugEnabled = logger.isDebugEnabled();
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRegistered(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

        setState(LifecycleState.REGISTERED);
        buffer = ctx.alloc().directBuffer(8192 * 8);
        rsm = new RedisStateMachine<K, V>();

        synchronized (stateLock) {
            channel = ctx.channel();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

        releaseBuffer();
        releaseStateMachine();

        if (lifecycleState == LifecycleState.CLOSED) {
            cancelCommands("Connection closed");
        }
        synchronized (stateLock) {
            channel = null;
        }
    }

    /**
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

        while (!queue.isEmpty()) {

            RedisCommand<K, V, ?> command = queue.peek();

            if (debugEnabled) {
                logger.debug("{} Queue contains: {} commands", logPrefix(), queue.size());
            }

            WithLatency withLatency = null;

            if (clientResources.commandLatencyCollector().isEnabled()) {
                RedisCommand<K, V, ?> unwrappedCommand = CommandWrapper.unwrap(command);
                if (unwrappedCommand instanceof WithLatency) {
                    withLatency = (WithLatency) unwrappedCommand;
                    if (withLatency.getFirstResponse() == -1) {
                        withLatency.firstResponse(nanoTime());
                    }
                }
            }

            if (!rsm.decode(buffer, command, command.getOutput())) {
                return;
            }

            command = queue.poll();
            recordLatency(withLatency, command.getType());

            command.complete();

            if (buffer != null && buffer.refCnt() != 0) {
                buffer.discardReadBytes();
            }
        }
    }

    private void recordLatency(WithLatency withLatency, ProtocolKeyword commandType) {

        if (withLatency != null && clientResources.commandLatencyCollector().isEnabled() && channel != null
                && remote() != null) {

            long firstResponseLatency = nanoTime() - withLatency.getFirstResponse();
            long completionLatency = nanoTime() - withLatency.getSent();

            clientResources.commandLatencyCollector().recordCommandLatency(local(), remote(), commandType, firstResponseLatency,
                    completionLatency);
        }
    }

    private SocketAddress remote() {
        return channel.remoteAddress();
    }

    private SocketAddress local() {
        if (channel.localAddress() != null) {
            return channel.localAddress();
        }
        return LocalAddress.ANY;
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C write(C command) {

        LettuceAssert.notNull(command, "command must not be null");

        if (lifecycleState == LifecycleState.CLOSED) {
            throw new RedisException("Connection is closed");
        }

        if (clientOptions.getRequestQueueSize() != Integer.MAX_VALUE
                && commandBuffer.size() + queue.size() >= clientOptions.getRequestQueueSize()) {
            throw new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                    + ". Commands are not accepted until the queue size drops.");
        }

        if ((channel == null || !isConnected()) && isRejectCommand()) {
            throw new RedisException("Currently not connected. Commands are rejected.");
        }

        try {
            incrementWriters();

            /**
             * This lock causes safety for connection activation and somehow netty gets more stable and predictable performance
             * than without a lock and all threads are hammering towards writeAndFlush.
             */
            Channel channel = this.channel;
            if (autoFlushCommands) {

                if (channel != null && isConnected() && channel.isActive()) {
                    writeToChannel(command, channel);
                } else {
                    writeToBuffer(command);
                }

            } else {
                bufferCommand(command);
            }
        } finally {
            decrementWriters();
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return command;
    }

    protected <C extends RedisCommand<K, V, T>, T> void writeToBuffer(C command) {

        if (commandBuffer.contains(command) || queue.contains(command)) {
            return;
        }

        if (connectionError != null) {
            if (debugEnabled) {
                logger.debug("{} write() completing Command {} due to connection error", logPrefix(), command);
            }
            command.completeExceptionally(connectionError);

            return;
        }

        bufferCommand(command);
    }

    protected <C extends RedisCommand<K, V, T>, T> void writeToChannel(C command, Channel channel) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush Command {}", logPrefix(), command);
        }

        if (reliability == Reliability.AT_MOST_ONCE) {
            // cancel on exceptions and remove from queue, because there is no housekeeping
            channel.writeAndFlush(command).addListener(new AtMostOnceWriteListener(command, queue));
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {
            // commands are ok to stay within the queue, reconnect will retrigger them
            channel.writeAndFlush(command).addListener(WRITE_LOG_LISTENER);
        }
    }

    protected void bufferCommand(RedisCommand<K, V, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() buffering Command {}", logPrefix(), command);
        }

        commandBuffer.add(command);
    }

    /**
     * Wait for stateLock and increment writers. Will wait if stateLock is locked and if writer counter is negative.
     */
    protected void incrementWriters() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            return;
        }

        synchronized (stateLock) {
            for (;;) {

                if (writers.get() >= 0) {
                    writers.incrementAndGet();
                    return;
                }
            }
        }
    }

    /**
     * Decrement writers without any wait.
     */
    protected void decrementWriters() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            return;
        }

        writers.decrementAndGet();
    }

    /**
     * Wait for stateLock and no writers. Must be used in an outer {@code synchronized} block to prevent interleaving with other
     * methods using writers. Sets writers to a negative value to create a lock for {@link #incrementWriters()}.
     */
    protected void lockWritersExclusive() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            return;
        }

        synchronized (stateLock) {
            for (;;) {

                if (writers.compareAndSet(0, -1)) {
                    exclusiveLockOwner = Thread.currentThread();
                    return;
                }
            }
        }
    }

    /**
     * Unlock writers.
     */
    protected void unlockWritersExclusive() {
        exclusiveLockOwner = null;
        writers.set(0);
    }

    private boolean isRejectCommand() {

        if (clientOptions == null) {
            return false;
        }

        switch (clientOptions.getDisconnectedBehavior()) {
            case REJECT_COMMANDS:
                return true;

            case ACCEPT_COMMANDS:
                return false;

            default:
            case DEFAULT:
                if (!clientOptions.isAutoReconnect()) {
                    return true;
                }

                return false;
        }
    }

    private boolean isConnected() {
        return lifecycleState.ordinal() >= LifecycleState.CONNECTED.ordinal()
                && lifecycleState.ordinal() <= LifecycleState.DISCONNECTED.ordinal();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void flushCommands() {

        if (channel != null && isConnected()) {
            List<RedisCommand<K, V, ?>> queuedCommands;

            synchronized (stateLock) {
                try {
                    lockWritersExclusive();

                    if (commandBuffer.isEmpty()) {
                        return;
                    }

                    queuedCommands = new ArrayList<>(commandBuffer.size());
                    RedisCommand<K, V, ?> cmd;
                    while ((cmd = commandBuffer.poll()) != null) {
                        queuedCommands.add(cmd);
                    }
                } finally {
                    unlockWritersExclusive();
                }
            }

            if (reliability == Reliability.AT_MOST_ONCE) {
                // cancel on exceptions and remove from queue, because there is no housekeeping
                channel.writeAndFlush(queuedCommands).addListener(new AtMostOnceWriteListener(queuedCommands, this.queue));
            }

            if (reliability == Reliability.AT_LEAST_ONCE) {
                // commands are ok to stay within the queue, reconnect will retrigger them
                channel.writeAndFlush(queuedCommands).addListener(WRITE_LOG_LISTENER);
            }
        }
    }

    /**
     * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.ChannelHandlerContext, java.lang.Object,
     *      io.netty.channel.ChannelPromise)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (msg instanceof Collection) {
            Collection<RedisCommand<K, V, ?>> commands = (Collection<RedisCommand<K, V, ?>>) msg;
            for (RedisCommand<K, V, ?> command : commands) {
                queueCommand(promise, command);
            }
            ctx.write(commands, promise);
            return;
        }

        RedisCommand<K, V, ?> cmd = (RedisCommand<K, V, ?>) msg;
        queueCommand(promise, cmd);
        ctx.write(cmd, promise);
    }

    private void queueCommand(ChannelPromise promise, RedisCommand<K, V, ?> cmd) throws Exception {

        if (cmd.isCancelled()) {
            return;
        }

        try {

            if (cmd.getOutput() == null) {

                // fire&forget commands are excluded from metrics
                cmd.complete();
            } else {
                queue.add(cmd);
            }
        } catch (Exception e) {
            cmd.completeExceptionally(e);
            promise.setFailure(e);
            throw e;
        }
    }

    private long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        logPrefix = null;

        if (debugEnabled) {
            logger.debug("{} channelActive()", logPrefix());
        }

        setStateIfNotClosed(LifecycleState.CONNECTED);

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
        if (channel != null) {
            channel.eventLoop().submit(new Runnable() {
                @Override
                public void run() {
                    channel.pipeline().fireUserEventTriggered(new ConnectionEvents.Activated());
                }
            });
        }

        if (debugEnabled) {
            logger.debug("{} channelActive() done", logPrefix());
        }
    }

    protected void executeQueuedCommands(ChannelHandlerContext ctx) {

        synchronized (stateLock) {
            try {
                lockWritersExclusive();

                connectionError = null;

                commandBuffer.addAll(queue);
                queue.removeAll(commandBuffer);

                if (debugEnabled) {
                    logger.debug("{} executeQueuedCommands {} command(s) queued", logPrefix(), commandBuffer.size());
                }

                synchronized (stateLock) {
                    channel = ctx.channel();
                }

                if (redisChannelHandler != null) {
                    if (debugEnabled) {
                        logger.debug("{} activating channel handler", logPrefix());
                    }
                    setStateIfNotClosed(LifecycleState.ACTIVATING);
                    redisChannelHandler.activated();
                }
                setStateIfNotClosed(LifecycleState.ACTIVE);
            } finally {
                unlockWritersExclusive();
            }
        }

        flushCommands();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (debugEnabled) {
            logger.debug("{} channelInactive()", logPrefix());
        }
        setStateIfNotClosed(LifecycleState.DISCONNECTED);

        if (redisChannelHandler != null) {
            if (debugEnabled) {
                logger.debug("{} deactivating channel handler", logPrefix());
            }
            setStateIfNotClosed(LifecycleState.DEACTIVATING);
            redisChannelHandler.deactivated();
        }
        setStateIfNotClosed(LifecycleState.DEACTIVATED);

        if (buffer != null) {
            rsm.reset();
            buffer.clear();
        }

        if (debugEnabled) {
            logger.debug("{} channelInactive() done", logPrefix());
        }
        super.channelInactive(ctx);
    }

    protected void setStateIfNotClosed(LifecycleState lifecycleState) {
        if (this.lifecycleState != LifecycleState.CLOSED) {
            setState(lifecycleState);
        }
    }

    protected void setState(LifecycleState lifecycleState) {
        synchronized (stateLock) {
            this.lifecycleState = lifecycleState;
        }
    }

    protected LifecycleState getState() {
        return lifecycleState;
    }

    private void cancelCommands(String message) {

        List<RedisCommand<K, V, ?>> toCancel;
        synchronized (stateLock) {
            try {
                lockWritersExclusive();
                toCancel = prepareReset();
            } finally {
                unlockWritersExclusive();
            }
        }

        for (RedisCommand<K, V, ?> cmd : toCancel) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError(message);
            }
            cmd.cancel();
        }
    }

    protected List<RedisCommand<K, V, ?>> prepareReset() {

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
        return toCancel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        InternalLogLevel logLevel = InternalLogLevel.WARN;

        if (!queue.isEmpty()) {
            RedisCommand<K, V, ?> command = queue.poll();
            if (debugEnabled) {
                logger.debug("{} Storing exception in {}", logPrefix(), command);
            }
            logLevel = InternalLogLevel.DEBUG;
            command.completeExceptionally(cause);
        }

        if (channel == null || !channel.isActive() || !isConnected()) {
            if (debugEnabled) {
                logger.debug("{} Storing exception in connectionError", logPrefix());
            }
            logLevel = InternalLogLevel.DEBUG;
            connectionError = cause;
        }

        if (cause instanceof IOException && logLevel.ordinal() > InternalLogLevel.INFO.ordinal()) {
            logLevel = InternalLogLevel.INFO;
            if (SUPPRESS_IO_EXCEPTION_MESSAGES.contains(cause.getMessage())) {
                logLevel = InternalLogLevel.DEBUG;
            }
        }

        logger.log(logLevel, "{} Unexpected exception during request: {}", logPrefix, cause.toString(), cause);
    }

    /**
     * Close the connection.
     */
    @Override
    public void close() {

        if (debugEnabled) {
            logger.debug("{} close()", logPrefix());
        }

        if (lifecycleState == LifecycleState.CLOSED) {
            return;
        }

        setStateIfNotClosed(LifecycleState.CLOSED);
        Channel currentChannel = this.channel;
        if (currentChannel != null) {
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.PrepareClose());
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.Close());

            ChannelFuture close = currentChannel.pipeline().close();
            if (currentChannel.isOpen()) {
                close.syncUninterruptibly();
            }
        }
    }

    public boolean isClosed() {
        return lifecycleState == LifecycleState.CLOSED;
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

        cancelCommands("Reset");

        if (buffer != null) {
            rsm.reset();
            buffer.clear();
        }
    }

    /**
     * Reset the command-handler to the initial not-connected state.
     */
    public void initialState() {

        setState(LifecycleState.NOT_CONNECTED);
        queue.clear();
        commandBuffer.clear();

        Channel currentChannel = this.channel;
        if (currentChannel != null) {
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.PrepareClose());
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.Close());
            currentChannel.pipeline().close();
        }
    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        this.redisChannelHandler = redisChannelHandler;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        synchronized (stateLock) {
            this.autoFlushCommands = autoFlush;
        }
    }

    protected String logPrefix() {
        if (logPrefix != null) {
            return logPrefix;
        }
        StringBuffer buffer = new StringBuffer(64);
        buffer.append('[').append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return logPrefix = buffer.toString();
    }

    private void releaseBuffer() {

        if (buffer != null) {
            buffer.release();
            buffer = null;
        }
    }

    private void releaseStateMachine() {

        if (rsm != null) {
            rsm.close();
            rsm = null;
        }
    }

    public enum LifecycleState {
        NOT_CONNECTED, REGISTERED, CONNECTED, ACTIVATING, ACTIVE, DISCONNECTED, DEACTIVATING, DEACTIVATED, CLOSED,
    }

    private enum Reliability {
        AT_MOST_ONCE, AT_LEAST_ONCE;
    }

    private static class AtMostOnceWriteListener<K, V, T> implements ChannelFutureListener {

        private final Collection<RedisCommand<K, V, T>> sentCommands;
        private final Queue<?> queue;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public AtMostOnceWriteListener(RedisCommand<K, V, T> sentCommand, Queue<?> queue) {
            this((Collection) LettuceLists.newList(sentCommand), queue);
        }

        public AtMostOnceWriteListener(Collection<RedisCommand<K, V, T>> sentCommand, Queue<?> queue) {
            this.sentCommands = sentCommand;
            this.queue = queue;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            future.await();
            if (future.cause() != null) {

                for (RedisCommand<?, ?, ?> sentCommand : sentCommands) {
                    sentCommand.completeExceptionally(future.cause());
                }

                queue.removeAll(sentCommands);
            }
        }
    }

    /**
     * A generic future listener which logs unsuccessful writes.
     */
    static class WriteLogListener implements GenericFutureListener<Future<Void>> {

        @Override
        public void operationComplete(Future<Void> future) throws Exception {
            Throwable cause = future.cause();
            if (!future.isSuccess() && !(cause instanceof ClosedChannelException)) {

                String message = "Unexpected exception during request: {}";
                InternalLogLevel logLevel = InternalLogLevel.WARN;

                if (cause instanceof IOException && SUPPRESS_IO_EXCEPTION_MESSAGES.contains(cause.getMessage())) {
                    logLevel = InternalLogLevel.DEBUG;
                }

                logger.log(logLevel, message, cause.toString(), cause);
            }
        }

    }

}
