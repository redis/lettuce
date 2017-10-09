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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.ConnectionEvents.PingBeforeActivate;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceFactories;
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
 * @author Jongyeol Choi
 */
@ChannelHandler.Sharable
public class CommandHandler<K, V> extends ChannelDuplexHandler implements RedisChannelWriter<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandHandler.class);
    private static final AtomicLong CHANNEL_COUNTER = new AtomicLong();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final AtomicIntegerFieldUpdater<CommandHandler> QUEUE_SIZE = AtomicIntegerFieldUpdater.newUpdater(
            CommandHandler.class, "queueSize");

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
    protected final Queue<RedisCommand<K, V, ?>> disconnectedBuffer;
    protected final Queue<RedisCommand<K, V, ?>> commandBuffer;
    protected final AtomicLong writers = new AtomicLong();
    protected final Object stateLock = new Object();
    private final boolean latencyMetricsEnabled;
    private final boolean boundedQueue;

    protected final Deque<RedisCommand<K, V, ?>> stack = new ArrayDeque<>();
    protected final ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(8192 * 8);
    protected final RedisStateMachine<K, V> rsm = new RedisStateMachine<>();
    protected volatile Channel channel;
    private volatile ConnectionWatchdog connectionWatchdog;

    // If TRACE level logging has been enabled at startup.
    private final boolean traceEnabled;

    // If DEBUG level logging has been enabled at startup.
    private final boolean debugEnabled;

    private final Reliability reliability;

    private volatile LifecycleState lifecycleState = LifecycleState.NOT_CONNECTED;

    // access via QUEUE_SIZE
    @SuppressWarnings("unused")
    private volatile int queueSize = 0;

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
     */
    public CommandHandler(ClientOptions clientOptions, ClientResources clientResources) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.traceEnabled = logger.isTraceEnabled();
        this.debugEnabled = logger.isDebugEnabled();
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
        this.latencyMetricsEnabled = clientResources.commandLatencyCollector().isEnabled();

        this.disconnectedBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        this.commandBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        boundedQueue = clientOptions.getRequestQueueSize() != Integer.MAX_VALUE;
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

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelUnregistered(io.netty.channel.ChannelHandlerContext)
     */
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
            logger.debug("{} Received: {} bytes, {} commands in the stack", logPrefix(), input.readableBytes(), stack.size());
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

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) {

        while (canDecode(buffer)) {

            RedisCommand<K, V, ?> command = stack.peek();
            if (debugEnabled) {
                logger.debug("{} Stack contains: {} commands", logPrefix(), stack.size());
            }

            try {
                if (!decode(buffer, command)) {
                    return;
                }
            } catch (Exception e) {

                ctx.close();
                throw e;
            }

            stack.poll();

            try {
                command.complete();
            } catch (Exception e) {
                logger.warn("{} Unexpected exception during command completion: {}", logPrefix, e.toString(), e);
            }

            if (buffer.refCnt() != 0) {
                buffer.discardReadBytes();
            }

            afterComplete(ctx, command);
        }
    }

    /**
     * Hook method called after command completion.
     *
     * @param ctx
     * @param command
     */
    protected void afterComplete(ChannelHandlerContext ctx, RedisCommand<K, V, ?> command) {
    }

    protected boolean canDecode(ByteBuf buffer) {
        return !stack.isEmpty() && buffer.isReadable();
    }

    private boolean decode(ByteBuf buffer, RedisCommand<K, V, ?> command) {

        if (latencyMetricsEnabled && command instanceof WithLatency) {

            WithLatency withLatency = (WithLatency) command;
            if (withLatency.getFirstResponse() == -1) {
                withLatency.firstResponse(nanoTime());
            }

            if (!rsm.decode(buffer, command, command.getOutput())) {
                return false;
            }

            recordLatency(withLatency, command.getType());

            return true;
        }

        return rsm.decode(buffer, command, command.getOutput());
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

            validateWrite();

            RedisCommand<K, V, T> commandToSend = potentiallyWrapLatencyCommand(command);

            if (autoFlushCommands) {

                if (isConnected()) {
                    writeToChannel(commandToSend);
                } else {
                    writeToDisconnectedBuffer(commandToSend);
                }

            } else {
                bufferCommand(commandToSend);
            }
        } finally {
            decrementWriters();
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return command;
    }

    private void validateWrite() {

        if (lifecycleState == LifecycleState.CLOSED) {
            throw new RedisException("Connection is closed");
        }

        if (usesBoundedQueues()) {

            if (QUEUE_SIZE.get(this) + 1 > clientOptions.getRequestQueueSize()) {
                throw new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (disconnectedBuffer.size() + 1 > clientOptions.getRequestQueueSize()) {
                throw new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (commandBuffer.size() + 1 > clientOptions.getRequestQueueSize()) {
                throw new RedisException("Command buffer size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }
        }

        if (!isConnected() && isRejectCommand()) {
            throw new RedisException("Currently not connected. Commands are rejected.");
        }
    }

    protected <C extends RedisCommand<K, V, T>, T> void writeToDisconnectedBuffer(C command) {

        if (connectionError != null) {
            if (debugEnabled) {
                logger.debug("{} disconnectedBufferCommand() Completing command {} due to connection error", logPrefix(),
                        command);
            }
            command.completeExceptionally(connectionError);

            return;
        }

        if (debugEnabled) {
            logger.debug("{} disconnectedBufferCommand() buffering (disconnected) command {}", logPrefix(), command);
        }

        disconnectedBuffer.add(command);
    }

    @SuppressWarnings("unchecked")
    private <C extends RedisCommand<K, V, T>, T> void writeToChannel(C command) {

        QUEUE_SIZE.incrementAndGet(this);

        if (reliability == Reliability.AT_MOST_ONCE) {
            // cancel on exceptions and remove from queue, because there is no housekeeping
            writeAndFlush(command).addListener(new AtMostOnceWriteListener(command));
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {
            // commands are ok to stay within the queue, reconnect will retrigger them
            writeAndFlush(command).addListener(new RetryListener(command));
        }
    }

    private void writeToChannel(Collection<? extends RedisCommand<K, V, ?>> commands) {

        QUEUE_SIZE.addAndGet(this, commands.size());

        if (reliability == Reliability.AT_MOST_ONCE) {
            // cancel on exceptions and remove from queue, because there is no housekeeping
            writeAndFlush(commands).addListener(new AtMostOnceWriteListener(commands));
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {
            // commands are ok to stay within the queue, reconnect will retrigger them
            writeAndFlush(commands).addListener(new RetryListener(commands));
        }
    }

    private ChannelFuture writeAndFlush(RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush command {}", logPrefix(), command);
        }

        return channel.writeAndFlush(command);
    }

    private ChannelFuture writeAndFlush(Collection<? extends RedisCommand<?, ?, ?>> commands) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush commands {}", logPrefix(), commands);
        }

        return channel.writeAndFlush(commands);
    }

    private void bufferCommand(RedisCommand<K, V, ?> command) {

        if (debugEnabled) {
            logger.debug("{} bufferCommand() buffering command {}", logPrefix(), command);
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

        LifecycleState state = lifecycleState;

        return state.ordinal() >= LifecycleState.CONNECTED.ordinal() && state.ordinal() < LifecycleState.DISCONNECTED.ordinal();
    }

    @Override
    public void flushCommands() {
        flushCommands(commandBuffer);
    }

    @SuppressWarnings("unchecked")
    private void flushCommands(Queue<RedisCommand<K, V, ?>> commands) {

        if (debugEnabled) {
            logger.debug("{} flushCommands()", logPrefix());
        }

        if (channel != null && isConnected()) {
            List<RedisCommand<K, V, ?>> queuedCommands;

            synchronized (stateLock) {
                try {
                    lockWritersExclusive();

                    if (commands.isEmpty()) {
                        return;
                    }

                    queuedCommands = new ArrayList<>(commands.size());
                    drainCommands(commands, queuedCommands);
                } finally {
                    unlockWritersExclusive();
                }
            }

            if (debugEnabled) {
                logger.debug("{} flushCommands() Flushing {} commands", logPrefix(), queuedCommands.size());
            }

            writeToChannel(queuedCommands);
        }
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

        if (!isWriteable(command)) {
            return;
        }

        addToStack(command, promise);
        ctx.write(command, promise);
    }

    private void writeBatch(ChannelHandlerContext ctx, Collection<RedisCommand<K, V, ?>> batch, ChannelPromise promise)
            throws Exception {

        Collection<RedisCommand<K, V, ?>> toWrite = batch;
        int commandsToWrite = 0;

        boolean cancelledCommands = false;
        for (RedisCommand<?, ?, ?> command : batch) {

            if (!isWriteable(command)) {
                cancelledCommands = true;
                break;
            }

            commandsToWrite++;
        }

        try {
            validateWrite(commandsToWrite);
        } catch (Exception e) {

            for (RedisCommand<?, ?, ?> redisCommand : toWrite) {
                redisCommand.completeExceptionally(e);
            }

            promise.setFailure(e);
            return;
        }

        if (cancelledCommands) {

            toWrite = new ArrayList<>(batch.size());

            for (RedisCommand<K, V, ?> command : batch) {

                if (!isWriteable(command)) {
                    continue;
                }

                toWrite.add(command);
            }
        }

        for (RedisCommand<K, V, ?> command : toWrite) {
            addToStack(command, promise);
        }

        if (!toWrite.isEmpty()) {
            ctx.write(toWrite, promise);
        }
    }

    private void addToStack(RedisCommand<K, V, ?> command, ChannelPromise promise) {

        try {
            validateWrite(1);

            if (command.getOutput() == null) {
                // fire&forget commands are excluded from metrics
                command.complete();
            } else {

                if (usesBoundedQueues() && stack.size() >= clientOptions.getRequestQueueSize()) {
                    throw new RedisException("Internal stack size exceeded: " + clientOptions.getRequestQueueSize()
                            + ". Commands are not accepted until the stack size drops.");
                }

                RedisCommand<K, V, ?> commandToUse = potentiallyWrapLatencyCommand(command);

                if (stack.contains(command)) {
                    throw new RedisException("Attempting to write duplicate command that is already enqueued: " + command);
                }

                if (promise.isVoid()) {
                    stack.add(commandToUse);
                } else {
                    promise.addListener(future -> {
                        if (future.isSuccess()) {
                            stack.add(commandToUse);
                        }
                    });
                }
            }
        } catch (RuntimeException e) {
            command.completeExceptionally(e);
            promise.setFailure(e);
            throw e;
        }
    }

    private boolean usesBoundedQueues() {
        return boundedQueue;
    }

    private void validateWrite(int commands) {

        if (usesBoundedQueues()) {

            if (stack.size() + commands > clientOptions.getRequestQueueSize())
                throw new RedisException("Internal stack size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the stack size drops.");
        }
    }

    private static boolean isWriteable(RedisCommand<?, ?, ?> command) {
        return !command.isCancelled() && !command.isDone();
    }

    @SuppressWarnings("unchecked")
    private <T> RedisCommand<K, V, T> potentiallyWrapLatencyCommand(RedisCommand<K, V, T> command) {

        if (!latencyMetricsEnabled) {
            return command;
        }

        if (command instanceof WithLatency) {

            WithLatency withLatency = (WithLatency) command;

            withLatency.firstResponse(-1);
            withLatency.sent(nanoTime());

            return command;
        }

        LatencyMeteredCommand latencyMeteredCommand = new LatencyMeteredCommand(command);
        latencyMeteredCommand.firstResponse(-1);
        latencyMeteredCommand.sent(nanoTime());

        return latencyMeteredCommand;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof Reset) {

            List<RedisCommand<K, V, ?>> toCancel;
            synchronized (stateLock) {
                try {
                    lockWritersExclusive();
                    toCancel = drainCommands(stack);
                } finally {
                    unlockWritersExclusive();
                }
            }

            resetInternals();
            cancelCommands(((Reset) evt).message, toCancel);
        }

        if (evt instanceof PingBeforeActivate) {

            PingBeforeActivate pba = (PingBeforeActivate) evt;

            stack.addFirst((RedisCommand<K, V, ?>) pba.getCommand());
            ctx.writeAndFlush(pba.getCommand());
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        logPrefix = null;
        connectionWatchdog = null;

        if (debugEnabled) {
            logger.debug("{} channelActive()", logPrefix());
        }

        connectionWatchdog = getConnectionWatchdog(ctx.pipeline());

        synchronized (stateLock) {
            try {
                lockWritersExclusive();
                setState(LifecycleState.CONNECTED);

                try {
                    rebuildQueue();
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

    private static ConnectionWatchdog getConnectionWatchdog(ChannelPipeline pipeline) {

        if (pipeline != null) {

            Map<String, ChannelHandler> map = pipeline.toMap();

            for (ChannelHandler handler : map.values()) {
                if (handler instanceof ConnectionWatchdog) {
                    return (ConnectionWatchdog) handler;
                }
            }
        }

        return null;
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

                rebuildQueue();
                setState(LifecycleState.DEACTIVATED);

                channel = null;
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

    private void rebuildQueue() {

        List<RedisCommand<K, V, ?>> queuedCommands = new ArrayList<>(stack.size() + disconnectedBuffer.size());

        drainCommands(stack, queuedCommands);
        drainCommands(disconnectedBuffer, queuedCommands);

        try {
            disconnectedBuffer.addAll(queuedCommands);
        } catch (RuntimeException e) {

            if (debugEnabled) {
                logger.debug("{} rebuildQueue Queue overcommit. Cannot add all commands to buffer (disconnected).",
                        logPrefix(), queuedCommands.size());
            }
            queuedCommands.removeAll(disconnectedBuffer);

            for (RedisCommand<?, ?, ?> command : queuedCommands) {
                command.completeExceptionally(e);
            }
        }

        if (debugEnabled) {
            logger.debug("{} rebuildQueue {} command(s) added to buffer", logPrefix(), disconnectedBuffer.size());
        }
    }

    private void activateCommandHandlerAndExecuteBufferedCommands(ChannelHandlerContext ctx) {

        connectionError = null;

        if (debugEnabled) {
            logger.debug("{} activateCommandHandlerAndExecuteBufferedCommands {} command(s) buffered", logPrefix(),
                    disconnectedBuffer.size());
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

        flushCommands(disconnectedBuffer);
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

        if (channel != null) {
            channel.pipeline().fireUserEventTriggered(new Reset(message));
        } else {
            resetInternals();
        }

        cancelCommands(message, toCancel);
    }

    private void cancelCommands(String message, List<RedisCommand<K, V, ?>> toCancel) {
        for (RedisCommand<K, V, ?> cmd : toCancel) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError(message);
            }
            cmd.cancel();
        }
    }

    private void resetInternals() {
        rsm.reset();

        if (buffer.refCnt() > 0) {
            buffer.clear();
        }
    }

    protected List<RedisCommand<K, V, ?>> prepareReset() {

        int size = disconnectedBuffer.size() + commandBuffer.size();

        List<RedisCommand<K, V, ?>> toCancel = new ArrayList<>(size);

        drainCommands(disconnectedBuffer, toCancel);
        drainCommands(commandBuffer, toCancel);

        return toCancel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        InternalLogLevel logLevel = InternalLogLevel.WARN;

        if (!stack.isEmpty()) {
            RedisCommand<K, V, ?> command = stack.poll();
            if (debugEnabled) {
                logger.debug("{} Storing exception in {}", logPrefix(), command);
            }
            logLevel = InternalLogLevel.DEBUG;
            try {
                command.completeExceptionally(cause);
            } catch (Exception ex) {
                logger.warn("{} Unexpected exception during command completion exceptionally: {}", logPrefix, ex.toString(), ex);
            }
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
    }

    /**
     * Reset the command-handler to the initial not-connected state.
     */
    public void initialState() {

        setState(LifecycleState.NOT_CONNECTED);
        disconnectedBuffer.clear();
        stack.clear();

        Channel currentChannel = this.channel;
        if (currentChannel != null) {
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.PrepareClose());
            currentChannel.pipeline().fireUserEventTriggered(new ConnectionEvents.Close());
            currentChannel.pipeline().close();
        }
    }

    protected String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        StringBuffer buffer = new StringBuffer(64);
        buffer.append('[').append(ChannelLogDescriptor.logDescriptor(channel)).append(", ").append("chid=0x")
                .append(Long.toHexString(commandHandlerId)).append(']');
        return logPrefix = buffer.toString();
    }

    protected static <T> List<T> drainCommands(Queue<T> source) {

        List<T> target = new ArrayList<>(source.size());

        drainCommands(source, target);

        return target;
    }

    protected static <T> void drainCommands(Queue<T> source, Collection<T> target) {

        T element;
        while ((element = source.poll()) != null) {
            target.add(element);
        }
    }

    private static long nanoTime() {
        return System.nanoTime();
    }

    private class ListenerSupport {

        final Collection<? extends RedisCommand<K, V, ?>> sentCommands;
        final RedisCommand<K, V, ?> sentCommand;

        ListenerSupport(RedisCommand<K, V, ?> sentCommand) {
            this.sentCommand = sentCommand;
            this.sentCommands = null;
        }

        ListenerSupport(Collection<? extends RedisCommand<K, V, ?>> sentCommands) {
            this.sentCommand = null;
            this.sentCommands = sentCommands;
        }

        void dequeue() {

            if (sentCommand != null) {
                QUEUE_SIZE.decrementAndGet(CommandHandler.this);
            }

            if (sentCommands != null) {
                QUEUE_SIZE.addAndGet(CommandHandler.this, -sentCommands.size());
            }
        }

        protected void complete(Throwable t) {

            if (sentCommand != null) {
                sentCommand.completeExceptionally(t);
            }

            if (sentCommands != null) {

                for (RedisCommand<?, ?, ?> sentCommand : sentCommands) {
                    sentCommand.completeExceptionally(t);
                }
            }
        }
    }

    private class AtMostOnceWriteListener extends ListenerSupport implements ChannelFutureListener {

        AtMostOnceWriteListener(RedisCommand<K, V, ?> sentCommand) {
            super(sentCommand);
        }

        AtMostOnceWriteListener(Collection<? extends RedisCommand<K, V, ?>> sentCommands) {
            super(sentCommands);
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {

            dequeue();

            if (future.cause() != null) {
                complete(future.cause());
            }
        }
    }

    /**
     * A generic future listener which retries unsuccessful writes.
     */
    private class RetryListener extends ListenerSupport implements GenericFutureListener<Future<Void>> {

        RetryListener(RedisCommand<K, V, ?> sentCommand) {
            super(sentCommand);
        }

        RetryListener(Collection<? extends RedisCommand<K, V, ?>> sentCommands) {
            super(sentCommands);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void operationComplete(Future<Void> future) throws Exception {

            Throwable cause = future.cause();

            boolean success = future.isSuccess();
            dequeue();

            if (!success) {
                Channel channel = CommandHandler.this.channel;
                if (channel != null) {
                    channel.eventLoop().submit(this::requeueCommands);
                } else {
                    CommandHandler.this.clientResources.eventExecutorGroup().submit(this::requeueCommands);
                }
            }

            if (!success && !(cause instanceof ClosedChannelException)) {

                String message = "Unexpected exception during request: {}";
                InternalLogLevel logLevel = InternalLogLevel.WARN;

                if (cause instanceof IOException && SUPPRESS_IO_EXCEPTION_MESSAGES.contains(cause.getMessage())) {
                    logLevel = InternalLogLevel.DEBUG;
                }

                logger.log(logLevel, message, cause.toString(), cause);
            }
        }

        private void requeueCommands() {
            if (sentCommand != null) {
                try {
                    write(sentCommand);
                } catch (Exception e) {
                    complete(e);
                }
            }

            if (sentCommands != null) {

                for (RedisCommand<K, V, ?> command : sentCommands) {
                    try {
                        write(command);
                    } catch (Exception e) {
                        complete(e);
                    }
                }

            }
        }
    }

    public enum LifecycleState {
        NOT_CONNECTED, REGISTERED, CONNECTED, ACTIVATING, ACTIVE, DISCONNECTED, DEACTIVATING, DEACTIVATED, CLOSED,
    }

    private enum Reliability {
        AT_MOST_ONCE, AT_LEAST_ONCE;
    }

    private static class Reset {

        final String message;

        public Reset(String message) {
            this.message = message;
        }
    }
}
