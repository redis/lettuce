/*
 * Copyright 2011-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.ContextualChannel;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.constant.DummyContextualChannelInstances;
import io.lettuce.core.context.BatchFlushEndPointContext;
import io.lettuce.core.context.ConnectionContext;
import io.lettuce.core.datastructure.queue.offerfirst.UnboundedMpscOfferFirstQueue;
import io.lettuce.core.datastructure.queue.offerfirst.impl.JcToolsUnboundedMpscOfferFirstQueue;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.utils.ExceptionUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.EncoderException;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default {@link Endpoint} implementation.
 *
 * @author Mark Paluch
 */
public class DefaultBatchFlushEndpoint implements RedisChannelWriter, BatchFlushEndpoint, PushHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BatchFlushEndpoint.class);

    private static final AtomicLong ENDPOINT_COUNTER = new AtomicLong();

    private static final AtomicReferenceFieldUpdater<DefaultBatchFlushEndpoint, ContextualChannel> CHANNEL = AtomicReferenceFieldUpdater
            .newUpdater(DefaultBatchFlushEndpoint.class, ContextualChannel.class, "channel");

    private static final AtomicIntegerFieldUpdater<DefaultBatchFlushEndpoint> QUEUE_SIZE = AtomicIntegerFieldUpdater
            .newUpdater(DefaultBatchFlushEndpoint.class, "queueSize");

    private static final AtomicIntegerFieldUpdater<DefaultBatchFlushEndpoint> STATUS = AtomicIntegerFieldUpdater
            .newUpdater(DefaultBatchFlushEndpoint.class, "status");

    private static final int ST_OPEN = 0;

    private static final int ST_CLOSED = 1;

    private static final Set<Class<? extends Throwable>> SHOULD_NOT_RETRY_EXCEPTION_TYPES = new HashSet<>();

    static {
        SHOULD_NOT_RETRY_EXCEPTION_TYPES.add(EncoderException.class);
        SHOULD_NOT_RETRY_EXCEPTION_TYPES.add(Error.class);
    }

    private static boolean isRejectCommand(ClientOptions clientOptions) {
        switch (clientOptions.getDisconnectedBehavior()) {
            case REJECT_COMMANDS:
                return true;
            case ACCEPT_COMMANDS:
                throw new UnsupportedOperationException("ACCEPT_COMMANDS is not supported");
            case DEFAULT:
                return !clientOptions.isAutoReconnect();
            default:
                throw new IllegalStateException("Unknown disconnected behavior: " + clientOptions.getDisconnectedBehavior());
        }
    }

    protected static void cancelCommandOnEndpointClose(RedisCommand<?, ?, ?> cmd) {
        if (cmd.isDone()) {
            return;
        }

        if (cmd.getOutput() != null) {
            cmd.getOutput().setError("endpoint closed");
        }
        cmd.cancel();
    }

    private final Reliability reliability;

    private final ClientOptions clientOptions;

    private final ClientResources clientResources;

    private final boolean boundedQueues;

    // access via QUEUE_SIZE
    private volatile int queueSize = 0;

    // access via STATUS
    private volatile int status = ST_OPEN;
    // access via CHANNEL

    protected volatile @Nonnull ContextualChannel channel = DummyContextualChannelInstances.CHANNEL_CONNECTING;

    private final Consumer<RedisCommand<?, ?, ?>> callbackOnClose;

    private final boolean rejectCommandsWhileDisconnected;

    private final List<PushListener> pushListeners = new CopyOnWriteArrayList<>();

    private final boolean debugEnabled = logger.isDebugEnabled();

    protected final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private String logPrefix;

    private boolean autoFlushCommands = true;

    private boolean inActivation = false;

    protected @Nullable ConnectionWatchdog connectionWatchdog;

    private ConnectionFacade connectionFacade;

    private volatile Throwable connectionError;

    private final String cachedEndpointId;

    protected final UnboundedMpscOfferFirstQueue<RedisCommand<?, ?, ?>> taskQueue;

    private final boolean canFire;

    private volatile boolean inProtectMode;

    private volatile Throwable failedToReconnectReason;

    private volatile EventLoop lastEventLoop = null;

    private final int writeSpinCount;

    private final int batchSize;

    /**
     * Create a new {@link BatchFlushEndpoint}.
     *
     * @param clientOptions client options for this connection, must not be {@code null}.
     * @param clientResources client resources for this connection, must not be {@code null}.
     */
    public DefaultBatchFlushEndpoint(ClientOptions clientOptions, ClientResources clientResources) {
        this(clientOptions, clientResources, DefaultBatchFlushEndpoint::cancelCommandOnEndpointClose);
    }

    protected DefaultBatchFlushEndpoint(ClientOptions clientOptions, ClientResources clientResources,
            Consumer<RedisCommand<?, ?, ?>> callbackOnClose) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(clientOptions, "ClientResources must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
        this.boundedQueues = clientOptions.getRequestQueueSize() != Integer.MAX_VALUE;
        this.rejectCommandsWhileDisconnected = isRejectCommand(clientOptions);
        long endpointId = ENDPOINT_COUNTER.incrementAndGet();
        this.cachedEndpointId = "0x" + Long.toHexString(endpointId);
        this.taskQueue = new JcToolsUnboundedMpscOfferFirstQueue<>();
        this.canFire = false;
        this.callbackOnClose = callbackOnClose;
        this.writeSpinCount = clientOptions.getAutoBatchFlushOptions().getWriteSpinCount();
        this.batchSize = clientOptions.getAutoBatchFlushOptions().getBatchSize();
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connectionFacade) {
        this.connectionFacade = connectionFacade;
    }

    @Override
    public ClientResources getClientResources() {
        return clientResources;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        this.autoFlushCommands = autoFlush;
    }

    @Override
    public void addListener(PushListener listener) {
        pushListeners.add(listener);
    }

    @Override
    public void removeListener(PushListener listener) {
        pushListeners.remove(listener);
    }

    @Override
    public List<PushListener> getPushListeners() {
        return pushListeners;
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        LettuceAssert.notNull(command, "Command must not be null");

        final Throwable validation = validateWrite(1);
        if (validation != null) {
            command.completeExceptionally(validation);
            return command;
        }

        try {
            if (inActivation) {
                command = processActivationCommand(command);
            }

            QUEUE_SIZE.incrementAndGet(this);
            this.taskQueue.offer(command);

            if (autoFlushCommands) {
                flushCommands();
            }

        } finally {
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {

        LettuceAssert.notNull(commands, "Commands must not be null");

        final Throwable validation = validateWrite(commands.size());
        if (validation != null) {
            commands.forEach(it -> it.completeExceptionally(validation));
            return (Collection<RedisCommand<K, V, ?>>) commands;
        }

        try {
            if (inActivation) {
                commands = processActivationCommands(commands);
            }

            for (RedisCommand<?, ?, ?> command : commands) {
                this.taskQueue.offer(command);
            }
            QUEUE_SIZE.addAndGet(this, commands.size());

            if (autoFlushCommands) {
                flushCommands();
            }
        } finally {
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return (Collection<RedisCommand<K, V, ?>>) commands;
    }

    @Override
    public void notifyChannelActive(Channel channel) {
        lastEventLoop = channel.eventLoop();

        final ContextualChannel contextualChannel = new ContextualChannel(channel, ConnectionContext.State.CONNECTED);

        this.logPrefix = null;
        this.connectionError = null;

        if (!CHANNEL.compareAndSet(this, DummyContextualChannelInstances.CHANNEL_CONNECTING, contextualChannel)) {
            channel.close();
            onUnexpectedState("notifyChannelActive", ConnectionContext.State.CONNECTING);
            return;
        }

        // Created a synchronize-before with set channel to CHANNEL_CONNECTING,
        if (isClosed()) {
            logger.info("{} Closing channel because endpoint is already closed", logPrefix());
            channel.close();

            onEndpointClosed();
            CHANNEL.set(this, DummyContextualChannelInstances.CHANNEL_ENDPOINT_CLOSED);
            return;
        }

        if (connectionWatchdog != null) {
            connectionWatchdog.arm();
        }

        try {
            if (debugEnabled) {
                logger.debug("{} activating endpoint", logPrefix());
            }

            try {
                inActivation = true;
                connectionFacade.activated();
            } finally {
                inActivation = false;
            }

            scheduleSendJobOnConnected(contextualChannel);
        } catch (Exception e) {

            if (debugEnabled) {
                logger.debug("{} channelActive() ran into an exception", logPrefix());
            }

            if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                resetInternal();
            }

            throw e;
        }
    }

    @Override
    public void notifyReconnectFailed(Throwable t) {
        this.failedToReconnectReason = t;

        if (!CHANNEL.compareAndSet(this, DummyContextualChannelInstances.CHANNEL_CONNECTING,
                DummyContextualChannelInstances.CHANNEL_RECONNECT_FAILED)) {
            onUnexpectedState("notifyReconnectFailed", ConnectionContext.State.CONNECTING);
            return;
        }

        syncAfterTerminated(() -> {
            if (isClosed()) {
                onEndpointClosed();
            } else {
                cancelCommands("reconnect failed");
            }
        });
    }

    @Override
    public void notifyChannelInactive(Channel channel) {
        if (debugEnabled) {
            logger.debug("{} deactivating endpoint handler", logPrefix());
        }

        connectionFacade.deactivated();
    }

    @Override
    public void notifyChannelInactiveAfterWatchdogDecision(Channel channel,
            Deque<RedisCommand<?, ?, ?>> retryableQueuedCommands) {
        final ContextualChannel inactiveChan = this.channel;
        if (!inactiveChan.getInitialState().isConnected() || inactiveChan.getDelegate() != channel) {
            logger.error("[unexpected][{}] notifyChannelInactive: channel not match", logPrefix());
            return;
        }

        if (inactiveChan.getContext().isChannelInactiveEventFired()) {
            logger.error("[unexpected][{}] notifyChannelInactive: already fired", logPrefix());
            return;
        }

        boolean willReconnect = connectionWatchdog != null && connectionWatchdog.willReconnect();
        RedisException exception = null;
        // Unlike DefaultEndpoint, here we don't check reliability since connectionWatchdog.willReconnect() already does it.
        if (isClosed()) {
            exception = new RedisException("endpoint closed");
            willReconnect = false;
        }

        if (willReconnect) {
            CHANNEL.set(this, DummyContextualChannelInstances.CHANNEL_WILL_RECONNECT);
            // Create a synchronize-before with this.channel = CHANNEL_WILL_RECONNECT
            if (isClosed()) {
                exception = new RedisException("endpoint closed");
                willReconnect = false;
            } else {
                exception = new RedisException("channel inactive and will reconnect");
            }
        } else if (exception == null) {
            exception = new RedisException("channel inactive and connectionWatchdog won't reconnect");
        }

        if (!willReconnect) {
            CHANNEL.set(this, DummyContextualChannelInstances.CHANNEL_ENDPOINT_CLOSED);
        }
        inactiveChan.getContext()
                .setCloseStatus(new ConnectionContext.CloseStatus(willReconnect, retryableQueuedCommands, exception));
        trySetEndpointQuiescence(inactiveChan);
    }

    @Override
    public void notifyException(Throwable t) {
        if (t instanceof RedisConnectionException && RedisConnectionException.isProtectedMode(t.getMessage())) {
            connectionError = t;
            inProtectMode = true;
        }

        final ContextualChannel curr = this.channel;
        if (!curr.getInitialState().isConnected() || !curr.isActive()) {
            connectionError = t;
        }
    }

    @Override
    public void registerConnectionWatchdog(ConnectionWatchdog connectionWatchdog) {
        this.connectionWatchdog = connectionWatchdog;
    }

    @Override
    public void flushCommands() {
        final ContextualChannel chan = this.channel;
        switch (chan.getInitialState()) {
            case ENDPOINT_CLOSED:
                syncAfterTerminated(() -> {
                    if (isClosed()) {
                        onEndpointClosed();
                    } else {
                        fulfillCommands("Connection is closed",
                                cmd -> cmd.completeExceptionally(new RedisException("Connection is closed")));
                    }
                });
                return;
            case RECONNECT_FAILED:
                syncAfterTerminated(() -> {
                    if (isClosed()) {
                        onEndpointClosed();
                    } else {
                        fulfillCommands("Reconnect failed",
                                cmd -> cmd.completeExceptionally(new RedisException("Reconnect failed")));
                    }
                });
                return;
            case WILL_RECONNECT:
            case CONNECTING:
                // command will be handled later either in notifyReconnectFailed or in notifyChannelActive
                return;
            case CONNECTED:
                scheduleSendJobIfNeeded(chan);
                return;
            default:
                throw new IllegalStateException("unexpected state: " + chan.getInitialState());
        }
    }

    /**
     * Close the connection.
     */
    @Override
    public void close() {

        if (debugEnabled) {
            logger.debug("{} close()", logPrefix());
        }

        closeAsync().join();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {

        if (debugEnabled) {
            logger.debug("{} closeAsync()", logPrefix());
        }

        if (isClosed()) {
            return closeFuture;
        }

        if (STATUS.compareAndSet(this, ST_OPEN, ST_CLOSED)) {
            if (connectionWatchdog != null) {
                connectionWatchdog.prepareClose();
            }

            final ContextualChannel chan = channel;
            if (chan.getInitialState().isConnected()) {
                // 1. STATUS.compareAndSet(this, ST_OPEN, ST_CLOSED) synchronize-before channel == CONNECTED
                // 2. channel == CONNECTED synchronize-before setting channel to WILL_RECONNECT/ENDPOINT_CLOSED
                // 3. setting channel to WILL_RECONNECT synchronize-before `isClosed()`, which will cancel all the commands.
                Futures.adapt(chan.close(), closeFuture);
            } else {
                // if is FAILED_TO_CONNECT/CLIENT_CLOSED, don't care, otherwise
                // 1. STATUS.compareAndSet(this, ST_OPEN, ST_CLOSED) synchronize-before channel == WILL_RECONNECT/CONNECTING
                // 2. channel == WILL_RECONNECT/CONNECTING synchronize-before setting channel to CONNECTED/RECONNECT_FAILED
                // 3. setting channel to CONNECTED/RECONNECT_FAILED synchronize-before `isClosed()`, which will cancel the
                // commands;
                closeFuture.complete(null);
            }
        }

        return closeFuture;
    }

    /**
     * Disconnect the channel.
     */
    public void disconnect() {

        ContextualChannel chan = this.channel;

        if (chan.getInitialState().isConnected() && chan.isOpen()) {
            chan.disconnect();
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

        final ContextualChannel chan = channel;
        if (chan.getInitialState().isConnected()) {
            chan.pipeline().fireUserEventTriggered(new ConnectionEvents.Reset());
        }
        // Unsafe to call cancelBufferedCommands() here.
        // cancelBufferedCommands("Reset");
    }

    private void resetInternal() {

        if (debugEnabled) {
            logger.debug("{} reset()", logPrefix());
        }

        ContextualChannel chan = channel;
        if (chan.getInitialState().isConnected()) {
            chan.pipeline().fireUserEventTriggered(new ConnectionEvents.Reset());
        }
        // Unsafe to call cancelBufferedCommands() here.
        cancelCommands("Reset");
    }

    /**
     * Reset the command-handler to the initial not-connected state.
     */
    @Override
    public void initialState() {

        // Thread safe since we are not connected yet.
        cancelCommands("initialState");

        ContextualChannel currentChannel = this.channel;
        if (currentChannel.getInitialState().isConnected()) {
            ChannelFuture close = currentChannel.close();
            if (currentChannel.isOpen()) {
                close.syncUninterruptibly();
            }
        }
    }

    private boolean isClosed() {
        return status == ST_CLOSED;
    }

    protected String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        String buffer = "[" + ChannelLogDescriptor.logDescriptor(channel.getDelegate()) + ", " + "epid=" + getId() + ']';
        logPrefix = buffer;
        return buffer;
    }

    @Override
    public String getId() {
        return cachedEndpointId;
    }

    private void scheduleSendJobOnConnected(final ContextualChannel chan) {
        LettuceAssert.assertState(chan.eventLoop().inEventLoop(), "must be called in event loop thread");

        // Schedule directly
        scheduleSendJobInEventLoopIfNeeded(chan);
    }

    private void scheduleSendJobIfNeeded(final ContextualChannel chan) {
        final EventLoop eventLoop = chan.eventLoop();
        if (eventLoop.inEventLoop()) {
            scheduleSendJobInEventLoopIfNeeded(chan);
            return;
        }

        if (chan.getContext().getFairEndPointContext().getHasOngoingSendLoop().tryEnterSafeGetVolatile()) {
            eventLoop.execute(() -> scheduleSendJobInEventLoopIfNeeded(chan));
        }

        // Otherwise:
        // 1. offer() (volatile write) synchronizes-before hasOngoingSendLoop.safe.get() == 1 (volatile read)
        // 2. hasOngoingSendLoop.safe.get() == 1 (volatile read) synchronizes-before
        // hasOngoingSendLoop.safe.set(0) (volatile write) in first loopSend0()
        // 3. hasOngoingSendLoop.safe.set(0) (volatile write) synchronizes-before
        // second loopSend0(), which will call poll()
    }

    private void scheduleSendJobInEventLoopIfNeeded(final ContextualChannel chan) {
        // Guarantee only 1 send loop.
        if (chan.getContext().getFairEndPointContext().getHasOngoingSendLoop().tryEnterUnsafe()) {
            loopSend(chan);
        }
    }

    private void loopSend(final ContextualChannel chan) {
        final ConnectionContext connectionContext = chan.getContext();
        final BatchFlushEndPointContext batchFlushEndPointContext = connectionContext.getFairEndPointContext();
        if (connectionContext.isChannelInactiveEventFired() || batchFlushEndPointContext.hasRetryableFailedToSendTasks()) {
            return;
        }

        LettuceAssert.assertState(channel == chan, "unexpected: channel not match but closeStatus == null");
        loopSend0(batchFlushEndPointContext, chan, writeSpinCount, true);
    }

    private void loopSend0(final BatchFlushEndPointContext batchFlushEndPointContext, final ContextualChannel chan,
            int remainingSpinnCount, final boolean firstCall) {
        do {
            final int count = pollBatch(batchFlushEndPointContext, batchSize, chan);
            if (count < 0) {
                return;
            }
            if (count == 0 || (firstCall && count < batchSize)) {
                // queue was empty
                break;
            }
        } while (--remainingSpinnCount > 0);

        if (remainingSpinnCount <= 0) {
            chan.eventLoop().execute(() -> loopSend(chan));
            return;
        }

        // QPSPattern is low and we have drained all tasks.
        if (firstCall) {
            // Don't setUnsafe here because loopSend0() may result in a delayed loopSend() call.
            batchFlushEndPointContext.getHasOngoingSendLoop().exitSafe();
            // Guarantee thread-safety: no dangling tasks in the queue.
            loopSend0(batchFlushEndPointContext, chan, remainingSpinnCount, false);
        } else {
            // In low qps pattern, the send job will be triggered later when a new task is added,
            batchFlushEndPointContext.getHasOngoingSendLoop().exitUnsafe();
        }
    }

    private int pollBatch(final BatchFlushEndPointContext batchFlushEndPointContext, final int maxBatchSize,
            ContextualChannel chan) {
        int count = 0;
        for (; count < maxBatchSize; count++) {
            final RedisCommand<?, ?, ?> cmd = this.taskQueue.poll(); // relaxed poll is faster and we wil retry later anyway.
            if (cmd == null) {
                break;
            }
            channelWrite(chan, cmd).addListener(future -> {
                QUEUE_SIZE.decrementAndGet(this);
                batchFlushEndPointContext.done(1);

                final Throwable retryableErr = checkSendResult(future, chan, cmd);
                if (retryableErr != null && batchFlushEndPointContext.addRetryableFailedToSendTask(cmd, retryableErr)) {
                    // Close connection on first transient write failure
                    internalCloseConnectionIfNeeded(chan, retryableErr);
                }

                trySetEndpointQuiescence(chan);
            });
        }

        if (count > 0) {
            batchFlushEndPointContext.add(count);

            channelFlush(chan);
            if (batchFlushEndPointContext.hasRetryableFailedToSendTasks()) {
                // Wait for onConnectionClose event()
                return -1;
            }
        }
        return count;
    }

    /**
     * Check write result.
     *
     * @param sendFuture The future to check.
     * @param contextualChannel The channel instance associated with the future.
     * @param cmd The task.
     * @return The cause of the failure if is a retryable failed task, otherwise null.
     */
    private Throwable checkSendResult(Future<?> sendFuture, ContextualChannel contextualChannel, RedisCommand<?, ?, ?> cmd) {
        if (cmd.isDone()) {
            ExceptionUtils.logUnexpectedDone(logger, logPrefix(), cmd);
            return null;
        }

        final ConnectionContext.CloseStatus closeStatus = contextualChannel.getContext().getCloseStatus();
        if (closeStatus != null) {
            logger.warn("[checkSendResult][interesting][{}] callback called after onClose() event, close status: {}",
                    logPrefix(), contextualChannel.getContext().getCloseStatus());
            final Throwable err = sendFuture.isSuccess() ? closeStatus.getErr() : sendFuture.cause();
            if (!closeStatus.isWillReconnect() || shouldNotRetry(err, cmd)) {
                cmd.completeExceptionally(err);
                return null;
            } else {
                return err;
            }
        }

        if (sendFuture.isSuccess()) {
            return null;
        }

        final Throwable cause = sendFuture.cause();
        ExceptionUtils.maybeLogSendError(logger, cause);
        if (shouldNotRetry(cause, cmd)) {
            cmd.completeExceptionally(cause);
            return null;
        }

        return cause;
    }

    private boolean shouldNotRetry(Throwable cause, RedisCommand<?, ?, ?> cmd) {
        return reliability == Reliability.AT_MOST_ONCE || ActivationCommand.isActivationCommand(cmd)
                || ExceptionUtils.oneOf(cause, SHOULD_NOT_RETRY_EXCEPTION_TYPES);
    }

    private void trySetEndpointQuiescence(ContextualChannel chan) {
        final EventLoop chanEventLoop = chan.eventLoop();
        LettuceAssert.isTrue(chanEventLoop.inEventLoop(), "unexpected: not in event loop");
        LettuceAssert.isTrue(chanEventLoop == lastEventLoop, "unexpected: event loop not match");

        final ConnectionContext connectionContext = chan.getContext();
        final @Nullable ConnectionContext.CloseStatus closeStatus = connectionContext.getCloseStatus();
        final BatchFlushEndPointContext batchFlushEndPointContext = connectionContext.getFairEndPointContext();
        if (batchFlushEndPointContext.isDone() && closeStatus != null) {
            if (closeStatus.isWillReconnect()) {
                onWillReconnect(closeStatus, batchFlushEndPointContext);
            } else {
                onWontReconnect(closeStatus, batchFlushEndPointContext);
            }

            if (chan.getContext().setChannelQuiescentOnce()) {
                onEndpointQuiescence();
            } else {
                ExceptionUtils.maybeFire(logger, canFire, "unexpected: setEndpointQuiescenceOncePerConnection() failed");
            }
        }
    }

    private void onEndpointQuiescence() {
        if (channel.getInitialState() == ConnectionContext.State.ENDPOINT_CLOSED) {
            return;
        }

        // Create happens-before with channelActive()
        if (!CHANNEL.compareAndSet(this, DummyContextualChannelInstances.CHANNEL_WILL_RECONNECT,
                DummyContextualChannelInstances.CHANNEL_CONNECTING)) {
            onUnexpectedState("onEndpointQuiescence", ConnectionContext.State.WILL_RECONNECT);
            return;
        }

        // neither connectionWatchdog nor doReconnectOnEndpointQuiescence could be null
        connectionWatchdog.reconnectOnEndpointQuiescence();
    }

    private void onWillReconnect(@Nonnull final ConnectionContext.CloseStatus closeStatus,
            final BatchFlushEndPointContext batchFlushEndPointContext) {
        final @Nullable Deque<RedisCommand<?, ?, ?>> retryableFailedToSendTasks = batchFlushEndPointContext
                .getAndClearRetryableFailedToSendTasks();
        if (retryableFailedToSendTasks != null) {
            // Save retryable failed tasks
            logger.info(
                    "[onWillReconnect][{}] compensate {} retryableFailedToSendTasks (write failure) for retrying on reconnecting, first write error: {}",
                    logPrefix(), retryableFailedToSendTasks.size(),
                    batchFlushEndPointContext.getFirstDiscontinueReason().getMessage());
            offerFirstAll(retryableFailedToSendTasks);
        }

        LettuceAssert.assertState(reliability != Reliability.AT_MOST_ONCE, "unexpected: reliability is AT_MOST_ONCE");
        final Deque<RedisCommand<?, ?, ?>> retryablePendingCommands = closeStatus.getAndClearRetryablePendingCommands();
        if (retryablePendingCommands != null) {
            // Save uncompletedTasks for later retry.
            logger.info(
                    "[onWillReconnect][{}] compensate {} retryable pending commands (write success) for retrying on reconnecting",
                    logPrefix(), retryablePendingCommands.size());
            offerFirstAll(retryablePendingCommands);
        }

        // follow the same logic as DefaultEndpoint
        if (inProtectMode) {
            cancelCommands("inProtectMode");
        }
    }

    private void onWontReconnect(@Nonnull final ConnectionContext.CloseStatus closeStatus,
            final BatchFlushEndPointContext batchFlushEndPointContext) {
        // No need to use syncAfterTerminated() since we are already in the event loop.
        if (isClosed()) {
            onEndpointClosed(closeStatus.getAndClearRetryablePendingCommands(),
                    batchFlushEndPointContext.getAndClearRetryableFailedToSendTasks());
        } else {
            fulfillCommands("onConnectionClose called and won't reconnect",
                    it -> it.completeExceptionally(closeStatus.getErr()), closeStatus.getAndClearRetryablePendingCommands(),
                    batchFlushEndPointContext.getAndClearRetryableFailedToSendTasks());
        }
    }

    private void offerFirstAll(Deque<RedisCommand<?, ?, ?>> commands) {
        commands.forEach(cmd -> {
            if (cmd instanceof DemandAware.Sink) {
                ((DemandAware.Sink) cmd).removeSource();
            }
        });
        this.taskQueue.offerFirstAll(commands);
    }

    private void internalCloseConnectionIfNeeded(ContextualChannel toCloseChan, Throwable reason) {
        if (toCloseChan.getContext().isChannelInactiveEventFired() || !toCloseChan.isActive()) {
            return;
        }

        logger.error("[internalCloseConnectionIfNeeded][attention][{}] close the connection due to write error, reason: '{}'",
                logPrefix(), reason.getMessage(), reason);
        toCloseChan.eventLoop().schedule(() -> {
            if (toCloseChan.isActive()) {
                toCloseChan.close();
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void cancelCommands(String message) {
        fulfillCommands(message, RedisCommand::cancel);
    }

    @SafeVarargs
    private final void onEndpointClosed(Queue<RedisCommand<?, ?, ?>>... queues) {
        fulfillCommands("endpoint closed", callbackOnClose, queues);
    }

    @SafeVarargs
    private final void fulfillCommands(String message, Consumer<RedisCommand<?, ?, ?>> commandConsumer,
            Queue<RedisCommand<?, ?, ?>>... queues) {
        int totalCancelledTaskNum = 0;
        for (Queue<RedisCommand<?, ?, ?>> queue : queues) {
            while (true) {
                RedisCommand<?, ?, ?> cmd = queue.poll();
                if (cmd == null) {
                    break;
                }
                if (cmd.getOutput() != null) {
                    cmd.getOutput().setError(message);
                }
                commandConsumer.accept(cmd);

                totalCancelledTaskNum++;
            }
        }

        while (true) {
            RedisCommand<?, ?, ?> cmd = this.taskQueue.poll();
            if (cmd == null) {
                break;
            }
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError(message);
            }
            commandConsumer.accept(cmd);
            totalCancelledTaskNum++;
        }

        if (totalCancelledTaskNum > 0) {
            logger.error("cancel {} pending tasks, reason: '{}'", totalCancelledTaskNum, message);
        }
    }

    private <K, V, T> RedisCommand<K, V, T> processActivationCommand(RedisCommand<K, V, T> command) {

        if (!ActivationCommand.isActivationCommand(command)) {
            return new ActivationCommand<>(command);
        }

        return command;
    }

    private <K, V> Collection<RedisCommand<K, V, ?>> processActivationCommands(
            Collection<? extends RedisCommand<K, V, ?>> commands) {

        Collection<RedisCommand<K, V, ?>> commandsToReturn = new ArrayList<>(commands.size());

        for (RedisCommand<K, V, ?> command : commands) {

            if (!ActivationCommand.isActivationCommand(command)) {
                command = new ActivationCommand<>(command);
            }

            commandsToReturn.add(command);
        }

        return commandsToReturn;
    }

    private Throwable validateWrite(@SuppressWarnings("unused") int commands) {
        if (isClosed()) {
            return new RedisException("Connection is closed");
        }

        final Throwable localConnectionErr = connectionError;
        if (localConnectionErr != null /* different logic of DefaultEndpoint */) {
            return localConnectionErr;
        }

        if (boundedQueues && queueSize + commands > clientOptions.getRequestQueueSize()) {
            return new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                    + ". Commands are not accepted until the queue size drops.");
        }

        final ContextualChannel chan = this.channel;
        switch (chan.getInitialState()) {
            case ENDPOINT_CLOSED:
                return new RedisException("Connection is closed");
            case RECONNECT_FAILED:
                return failedToReconnectReason;
            case WILL_RECONNECT:
            case CONNECTING:
                return rejectCommandsWhileDisconnected ? new RedisException("Currently not connected. Commands are rejected.")
                        : null;
            case CONNECTED:
                return !chan.isActive() && rejectCommandsWhileDisconnected ? new RedisException("Connection is closed") : null;
            default:
                throw new IllegalStateException("unexpected state: " + chan.getInitialState());
        }
    }

    private void onUnexpectedState(String caller, ConnectionContext.State exp) {
        final ConnectionContext.State actual = this.channel.getInitialState();
        logger.error("[{}][unexpected] {}: unexpected state: exp '{}' got '{}'", caller, logPrefix(), exp, actual);
        cancelCommands(String.format("%s: state not match: expect '%s', got '%s'", caller, exp, actual));
    }

    private void channelFlush(Channel channel) {
        if (debugEnabled) {
            logger.debug("{} write() channelFlush", logPrefix());
        }

        channel.flush();
    }

    private ChannelFuture channelWrite(Channel channel, RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() channelWrite command {}", logPrefix(), command);
        }

        return channel.write(command);
    }

    /*
     * Synchronize after the endpoint is terminated. This is to ensure only one thread can access the task queue after endpoint
     * is terminated (state is RECONNECT_FAILED/ENDPOINT_CLOSED)
     */
    private void syncAfterTerminated(Runnable runnable) {
        final EventLoop localLastEventLoop = lastEventLoop;
        LettuceAssert.notNull(localLastEventLoop, "lastEventLoop must not be null after terminated");
        if (localLastEventLoop.inEventLoop()) {
            runnable.run();
        } else {
            localLastEventLoop.execute(() -> {
                runnable.run();
                LettuceAssert.isTrue(lastEventLoop == localLastEventLoop, "lastEventLoop must not be changed after terminated");
            });
        }
    }

    private enum Reliability {
        AT_MOST_ONCE, AT_LEAST_ONCE
    }

}
