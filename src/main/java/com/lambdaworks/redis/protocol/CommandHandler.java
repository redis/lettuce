/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import io.netty.buffer.ByteBufAllocator;
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
    private static final AtomicLong CHANNEL_COUNTER = new AtomicLong();

    /**
     * When we encounter an unexpected IOException we look for these {@link Throwable#getMessage() messages} (because we have no
     * better way to distinguish) and log them at DEBUG rather than WARN, since they are generally caused by unclean client
     * disconnects rather than an actual problem.
     */
    private static final Set<String> SUPPRESS_IO_EXCEPTION_MESSAGES = LettuceSets.unmodifiableSet("Connection reset by peer",
            "Broken pipe", "Connection timed out");

    protected final long commandHandlerId = CHANNEL_COUNTER.incrementAndGet();
    protected final ClientOptions clientOptions;
    protected final ClientResources clientResources;
    protected final Queue<RedisCommand<K, V, ?>> queue;
    protected final AtomicLong writers = new AtomicLong();
    protected final Object stateLock = new Object();
    private final boolean latencyMetricsEnabled;

    // all access to the commandBuffer is synchronized
    protected final Deque<RedisCommand<K, V, ?>> commandBuffer = LettuceFactories.newConcurrentQueue();
    protected final Collection<RedisCommand<K, V, ?>> transportBuffer = LettuceFactories.newConcurrentCollection();
    protected final ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(8192 * 8);
    protected final RedisStateMachine<K, V> rsm = new RedisStateMachine<>();
    protected volatile Channel channel;
    private volatile ConnectionWatchdog connectionWatchdog;

    // If TRACE level logging has been enabled at startup.
    private final boolean traceEnabled;

    // If DEBUG level logging has been enabled at startup.
    private final boolean debugEnabled;

    // If WARN level logging has been enabled at startup.
    private final boolean warnEnabled;

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

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");
        LettuceAssert.notNull(queue, "Queue must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.queue = queue;
        this.traceEnabled = logger.isTraceEnabled();
        this.debugEnabled = logger.isDebugEnabled();
        this.warnEnabled = logger.isWarnEnabled();
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
        this.latencyMetricsEnabled = clientResources.commandLatencyCollector().isEnabled();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRegistered(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

        if (isClosed()) {
            logger.debug("{} Dropping register for a closed channel", logPrefix());
        }

        synchronized (stateLock) {
            channel = ctx.channel();
        }

        if (debugEnabled) {
            logPrefix = null;
            logger.debug("{} channelRegistered()", logPrefix());
        }

        setState(LifecycleState.REGISTERED);

        buffer.clear();
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

        if (debugEnabled) {
            logger.debug("{} channelUnregistered()", logPrefix());
        }

        if (channel != null && ctx.channel() != channel) {
            logger.debug("{} My channel and ctx.channel mismatch. Propagating event to other listeners", logPrefix());
            ctx.fireChannelUnregistered();
            return;
        }

        if (isClosed()) {
            cancelCommands("Connection closed");
        }

        synchronized (stateLock) {
            channel = null;
        }

        ctx.fireChannelUnregistered();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf input = (ByteBuf) msg;

        if (!input.isReadable() || input.refCnt() == 0) {
            logger.warn("{} Input not readable {}, {}", logPrefix(), input.isReadable(), input.refCnt());
            return;
        }

        if (debugEnabled) {
            logger.debug("{} Received: {} bytes, {} queued commands", logPrefix(), input.readableBytes(), queue.size());
        }

        try {
            if (buffer.refCnt() < 1) {
                logger.warn("{} Ignoring received data for closed or abandoned connection", logPrefix());
                return;
            }

            if (debugEnabled && ctx.channel() != channel) {
                logger.debug("{} Ignoring data for a non-registered channel {}", logPrefix(), ctx.channel());
                return;
            }

            if (traceEnabled) {
                logger.trace("{} Buffer: {}", logPrefix(), input.toString(Charset.defaultCharset()).trim());
            }

            buffer.writeBytes(input);

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

            if (latencyMetricsEnabled && command instanceof WithLatency) {

                WithLatency withLatency = (WithLatency) command;
                if (withLatency.getFirstResponse() == -1) {
                    withLatency.firstResponse(nanoTime());
                }

                if (!rsm.decode(buffer, command, command.getOutput())) {
                    return;
                }

                recordLatency(withLatency, command.getType());

            } else {

                if (!rsm.decode(buffer, command, command.getOutput())) {
                    return;
                }
            }

            queue.poll();

            try {
                command.complete();
            } catch (Exception e) {
                logger.warn("{} Unexpected exception during command completion: {}", logPrefix, e.toString(), e);
            }

            if (buffer.refCnt() != 0) {
                buffer.discardReadBytes();
            }
        }
    }

    private void recordLatency(WithLatency withLatency, ProtocolKeyword commandType) {

        if (withLatency != null && latencyMetricsEnabled && channel != null && remote() != null) {

            long firstResponseLatency = withLatency.getSent() - withLatency.getFirstResponse();
            long completionLatency = nanoTime() - withLatency.getSent();

            clientResources.commandLatencyCollector().recordCommandLatency(local(), remote(), commandType,
                    firstResponseLatency, completionLatency);
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

        LettuceAssert.notNull(command, "Command must not be null");

        try {
            incrementWriters();

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

            /*
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
                logger.debug("{} writeToBuffer() Completing command {} due to connection error", logPrefix(), command);
            }
            command.completeExceptionally(connectionError);

            return;
        }

        bufferCommand(command);
    }

    protected <C extends RedisCommand<K, V, T>, T> void writeToChannel(C command, Channel channel) {

        if (reliability == Reliability.AT_MOST_ONCE) {
            // cancel on exceptions and remove from queue, because there is no housekeeping
            writeAndFlush(command, channel.newPromise()).addListener(new AtMostOnceWriteListener(command, queue));
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {
            // commands are ok to stay within the queue, reconnect will retrigger them

            if (warnEnabled) {
                writeAndFlush(command, channel.newPromise()).addListener(WRITE_LOG_LISTENER);
            } else {
                writeAndFlush(command, channel.voidPromise());
            }
        }
    }

    protected void bufferCommand(RedisCommand<K, V, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() buffering command {}", logPrefix(), command);
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
            writers.decrementAndGet();
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

        if (exclusiveLockOwner == Thread.currentThread()) {
            if (writers.incrementAndGet() == 0) {
                exclusiveLockOwner = null;
            }
        }
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

    boolean isConnected() {
        return lifecycleState.ordinal() >= LifecycleState.CONNECTED.ordinal()
                && lifecycleState.ordinal() < LifecycleState.DISCONNECTED.ordinal();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void flushCommands() {

        if (debugEnabled) {
            logger.debug("{} flushCommands()", logPrefix());
        }

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

            if (debugEnabled) {
                logger.debug("{} flushCommands() Flushing {} commands", logPrefix(), queuedCommands.size());
            }

            if (reliability == Reliability.AT_MOST_ONCE) {
                // cancel on exceptions and remove from queue, because there is no housekeeping
                writeAndFlush(queuedCommands, channel.newPromise()).addListener(
                        new AtMostOnceWriteListener(queuedCommands, this.queue));
            }

            if (reliability == Reliability.AT_LEAST_ONCE) {
                // commands are ok to stay within the queue, reconnect will retrigger them

                if (warnEnabled) {
                    writeAndFlush(queuedCommands, channel.newPromise()).addListener(WRITE_LOG_LISTENER);
                } else {
                    writeAndFlush(queuedCommands, channel.voidPromise());
                }
            }
        }
    }

    private <C extends RedisCommand<K, V, ?>> ChannelFuture writeAndFlush(List<C> commands, ChannelPromise channelPromise) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush commands {}", logPrefix(), commands);
        }

        transportBuffer.addAll(commands);
        channel.writeAndFlush(commands, channelPromise);

        return channelPromise;
    }

    private <C extends RedisCommand<K, V, ?>> ChannelFuture writeAndFlush(C command, ChannelPromise channelPromise) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush command {}", logPrefix(), command);
        }

        transportBuffer.add(command);
        channel.writeAndFlush(command, channelPromise);

        return channelPromise;
    }

    /**
     * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.ChannelHandlerContext, java.lang.Object,
     *      io.netty.channel.ChannelPromise)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (debugEnabled) {
            logger.debug("{} write(ctx, {}, promise)", logPrefix(), msg);
        }

        if (msg instanceof RedisCommand) {
            writeSingleCommand(ctx, (RedisCommand<K, V, ?>) msg, promise);
            return;
        }

        if (msg instanceof Collection) {
            writeBatch(ctx, (Collection<RedisCommand<K, V, ?>>) msg, promise);
        }
    }

    private void writeSingleCommand(ChannelHandlerContext ctx, RedisCommand<K, V, ?> command, ChannelPromise promise)
            throws Exception {

        if (command.isCancelled()) {
            transportBuffer.remove(command);
            return;
        }

        queueCommand(command, promise);
        ctx.write(command, promise);
    }

    private void writeBatch(ChannelHandlerContext ctx, Collection<RedisCommand<K, V, ?>> msg, ChannelPromise promise)
            throws Exception {

        Collection<RedisCommand<K, V, ?>> commands = msg;
        Collection<RedisCommand<K, V, ?>> toWrite = commands;

        boolean cancelledCommands = false;
        for (RedisCommand<K, V, ?> command : commands) {
            if (command.isCancelled()) {
                cancelledCommands = true;
                break;
            }
        }

        if (cancelledCommands) {

            toWrite = new ArrayList<>(commands.size());

            for (RedisCommand<K, V, ?> command : commands) {

                if (command.isCancelled()) {
                    transportBuffer.remove(command);
                    continue;
                }

                toWrite.add(command);
                queueCommand(command, promise);
            }
        } else {

            for (RedisCommand<K, V, ?> command : toWrite) {
                queueCommand(command, promise);
            }
        }

        if (!toWrite.isEmpty()) {
            ctx.write(toWrite, promise);
        }
    }

    private void queueCommand(RedisCommand<K, V, ?> command, ChannelPromise promise) throws Exception {

        try {

            if (command.getOutput() == null) {
                // fire&forget commands are excluded from metrics
                command.complete();
            } else {

                if (latencyMetricsEnabled) {

                    if (command instanceof WithLatency) {

                        WithLatency withLatency = (WithLatency) command;

                        withLatency.firstResponse(-1);
                        withLatency.sent(nanoTime());

                        queue.add(command);
                    } else {

                        LatencyMeteredCommand<K, V, ?> latencyMeteredCommand = new LatencyMeteredCommand<>(command);
                        latencyMeteredCommand.firstResponse(-1);
                        latencyMeteredCommand.sent(nanoTime());

                        queue.add(latencyMeteredCommand);
                    }
                } else {
                    queue.add(command);
                }
            }
            transportBuffer.remove(command);
        } catch (Exception e) {
            command.completeExceptionally(e);
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
        connectionWatchdog = null;

        if (debugEnabled) {
            logger.debug("{} channelActive()", logPrefix());
        }

        if (ctx != null && ctx.pipeline() != null) {

            Map<String, ChannelHandler> map = ctx.pipeline().toMap();

            for (ChannelHandler handler : map.values()) {
                if (handler instanceof ConnectionWatchdog) {
                    connectionWatchdog = (ConnectionWatchdog) handler;
                }
            }
        }

        synchronized (stateLock) {
            try {
                lockWritersExclusive();
                setState(LifecycleState.CONNECTED);

                try {
                    // Move queued commands to buffer before issuing any commands because of connection activation.
                    // That's necessary to prepend queued commands first as some commands might get into the queue
                    // after the connection was disconnected. They need to be prepended to the command buffer
                    moveQueuedCommandsToCommandBuffer();
                    activateCommandHandlerAndExecuteBufferedCommands(ctx);
                } catch (Exception e) {

                    if (debugEnabled) {
                        logger.debug("{} channelActive() ran into an exception", logPrefix());
                    }

                    if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                        reset();
                    }

                    throw e;
                }
            } finally {
                unlockWritersExclusive();
            }
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

    private void moveQueuedCommandsToCommandBuffer() {

        List<RedisCommand<K, V, ?>> queuedCommands = drainCommands(queue);
        Collections.reverse(queuedCommands);

        List<RedisCommand<K, V, ?>> transportBufferCommands = drainCommands(transportBuffer);
        Collections.reverse(transportBufferCommands);

        // Queued commands first because they reached the queue before commands that are still in the transport buffer.
        queuedCommands.addAll(transportBufferCommands);

        logger.debug("{} moveQueuedCommandsToCommandBuffer {} command(s) added to buffer", logPrefix(), queuedCommands.size());
        for (RedisCommand<K, V, ?> command : queuedCommands) {
            commandBuffer.addFirst(command);
        }
    }

    private List<RedisCommand<K, V, ?>> drainCommands(Collection<RedisCommand<K, V, ?>> source) {

        List<RedisCommand<K, V, ?>> target = new ArrayList<>(source.size());
        target.addAll(source);
        source.removeAll(target);
        return target;
    }

    protected void activateCommandHandlerAndExecuteBufferedCommands(ChannelHandlerContext ctx) {

        connectionError = null;

        if (debugEnabled) {
            logger.debug("{} activateCommandHandlerAndExecuteBufferedCommands {} command(s) buffered", logPrefix(),
                    commandBuffer.size());
        }

        channel = ctx.channel();

        if (redisChannelHandler != null) {

            if (debugEnabled) {
                logger.debug("{} activating channel handler", logPrefix());
            }

            setState(LifecycleState.ACTIVATING);
            redisChannelHandler.activated();
        }
        setState(LifecycleState.ACTIVE);

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

        if (channel != null && ctx.channel() != channel) {
            logger.debug("{} My channel and ctx.channel mismatch. Propagating event to other listeners.", logPrefix());
            super.channelInactive(ctx);
            return;
        }

        synchronized (stateLock) {
            try {
                lockWritersExclusive();
                setState(LifecycleState.DISCONNECTED);

                if (redisChannelHandler != null) {

                    if (debugEnabled) {
                        logger.debug("{} deactivating channel handler", logPrefix());
                    }

                    setState(LifecycleState.DEACTIVATING);
                    redisChannelHandler.deactivated();
                }

                setState(LifecycleState.DEACTIVATED);

                // Shift all commands to the commandBuffer so the queue is empty.
                // Allows to run onConnect commands before executing buffered commands
                commandBuffer.addAll(queue);
                queue.removeAll(commandBuffer);

            } finally {
                unlockWritersExclusive();
            }
        }

        rsm.reset();

        if (debugEnabled) {
            logger.debug("{} channelInactive() done", logPrefix());
        }
        super.channelInactive(ctx);
    }

    protected void setState(LifecycleState lifecycleState) {

        if (this.lifecycleState != LifecycleState.CLOSED) {
            synchronized (stateLock) {
                this.lifecycleState = lifecycleState;
            }
        }
    }

    protected LifecycleState getState() {
        return lifecycleState;
    }

    public boolean isClosed() {
        return lifecycleState == LifecycleState.CLOSED;
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

        int size = queue.size() + commandBuffer.size();

        List<RedisCommand<K, V, ?>> toCancel = new ArrayList<>(size);

        RedisCommand<K, V, ?> c;

        while ((c = queue.poll()) != null) {
            toCancel.add(c);
        }

        while ((c = commandBuffer.poll()) != null) {
            toCancel.add(c);
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

        if (isClosed()) {
            return;
        }

        setState(LifecycleState.CLOSED);
        Channel currentChannel = this.channel;
        if (currentChannel != null) {
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.PrepareClose());
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.Close());

            ChannelFuture close = currentChannel.pipeline().close();
            if (currentChannel.isOpen()) {
                close.syncUninterruptibly();
            }
        } else if (connectionWatchdog != null) {
            connectionWatchdog.prepareClose(new ConnectionEvents.PrepareClose());
        }

        rsm.close();

        if (buffer.refCnt() > 0) {
            buffer.release();
        }
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

        rsm.reset();

        if (buffer.refCnt() > 0) {
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
        buffer.append('[').append("chid=0x").append(Long.toHexString(commandHandlerId)).append(", ")
                .append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return logPrefix = buffer.toString();
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
