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
import io.lettuce.core.context.AutoBatchFlushEndPointContext;
import io.lettuce.core.context.ConnectionContext;
import io.lettuce.core.datastructure.queue.offerfirst.UnboundedOfferFirstQueue;
import io.lettuce.core.datastructure.queue.offerfirst.impl.ConcurrentLinkedOfferFirstQueue;
import io.lettuce.core.datastructure.queue.offerfirst.impl.JcToolsUnboundedMpscOfferFirstQueue;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.utils.ExceptionUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Recycler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default {@link Endpoint} implementation.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("DuplicatedCode")
public class DefaultAutoBatchFlushEndpoint implements RedisChannelWriter, AutoBatchFlushEndpoint, PushHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AutoBatchFlushEndpoint.class);

    private static final AtomicLong ENDPOINT_COUNTER = new AtomicLong();

    private static final AtomicReferenceFieldUpdater<DefaultAutoBatchFlushEndpoint, ContextualChannel> CHANNEL = AtomicReferenceFieldUpdater
            .newUpdater(DefaultAutoBatchFlushEndpoint.class, ContextualChannel.class, "channel");

    private static final AtomicIntegerFieldUpdater<DefaultAutoBatchFlushEndpoint> QUEUE_SIZE = AtomicIntegerFieldUpdater
            .newUpdater(DefaultAutoBatchFlushEndpoint.class, "queueSize");

    private static final AtomicIntegerFieldUpdater<DefaultAutoBatchFlushEndpoint> STATUS = AtomicIntegerFieldUpdater
            .newUpdater(DefaultAutoBatchFlushEndpoint.class, "status");

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

    private volatile Throwable failedToReconnectReason;

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

    private final String cachedEndpointId;

    protected final UnboundedOfferFirstQueue<Object> taskQueue;

    private final boolean canFire;

    private volatile EventLoop lastEventLoop = null;

    private volatile Throwable connectionError;

    private volatile boolean inProtectMode;

    private final int writeSpinCount;

    private final int batchSize;

    private final boolean usesMpscQueue;

    /**
     * Create a new {@link AutoBatchFlushEndpoint}.
     *
     * @param clientOptions client options for this connection, must not be {@code null}.
     * @param clientResources client resources for this connection, must not be {@code null}.
     */
    public DefaultAutoBatchFlushEndpoint(ClientOptions clientOptions, ClientResources clientResources) {
        this(clientOptions, clientResources, DefaultAutoBatchFlushEndpoint::cancelCommandOnEndpointClose);
    }

    protected DefaultAutoBatchFlushEndpoint(ClientOptions clientOptions, ClientResources clientResources,
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
        this.usesMpscQueue = clientOptions.getAutoBatchFlushOptions().usesMpscQueue();
        this.taskQueue = usesMpscQueue ? new JcToolsUnboundedMpscOfferFirstQueue<>() : new ConcurrentLinkedOfferFirstQueue<>();
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
        final ContextualChannel chan = this.channel;
        final Throwable validation = validateWrite(chan, 1, inActivation);
        if (validation != null) {
            command.completeExceptionally(validation);
            return command;
        }

        try {
            if (inActivation) {
                // needs write and flush activation command immediately, cannot queue it.
                command = processActivationCommand(command);
                writeAndFlushActivationCommand(chan, command);
            } else {
                this.taskQueue.offer(command);
                QUEUE_SIZE.incrementAndGet(this);

                if (autoFlushCommands) {
                    flushCommands();
                }
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

        final ContextualChannel chan = this.channel;
        final Throwable validation = validateWrite(chan, commands.size(), inActivation);
        if (validation != null) {
            commands.forEach(it -> it.completeExceptionally(validation));
            return (Collection<RedisCommand<K, V, ?>>) commands;
        }

        try {
            if (inActivation) {
                // needs write and flush activation commands immediately, cannot queue it.
                commands = processActivationCommands(commands);
                writeAndFlushActivationCommands(chan, commands);
            } else {
                this.taskQueue.offer(commands);
                QUEUE_SIZE.addAndGet(this, commands.size());

                if (autoFlushCommands) {
                    flushCommands();
                }
            }
        } finally {
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return (Collection<RedisCommand<K, V, ?>>) commands;
    }

    private <V, K> void writeAndFlushActivationCommand(ContextualChannel chan, RedisCommand<K, V, ?> command) {
        channelWrite(chan, command).addListener(WrittenToChannel.newInstance(this, chan, command, true));
        channelFlush(chan);
    }

    private <V, K> void writeAndFlushActivationCommands(ContextualChannel chan,
            Collection<? extends RedisCommand<K, V, ?>> commands) {
        for (RedisCommand<?, ?, ?> command : commands) {
            channelWrite(chan, command).addListener(WrittenToChannel.newInstance(this, chan, command, true));
        }
        channelFlush(chan);
    }

    @Override
    public void notifyChannelActive(Channel channel) {
        final ContextualChannel contextualChannel = new ContextualChannel(channel, ConnectionContext.State.CONNECTED);
        if (!CHANNEL.compareAndSet(this, DummyContextualChannelInstances.CHANNEL_CONNECTING, contextualChannel)) {
            channel.close();
            onUnexpectedState("notifyChannelActive", ConnectionContext.State.CONNECTING);
            return;
        }

        this.lastEventLoop = channel.eventLoop();
        this.connectionError = null;
        this.inProtectMode = false;
        this.logPrefix = null;

        // Created a synchronize-before with set channel to CHANNEL_CONNECTING,
        if (isClosed()) {
            logger.info("{} Closing channel because endpoint is already closed", logPrefix());
            channel.close();
            // Cleaning will be done later in notifyChannelInactiveAfterWatchdogDecision, we are happy so far.
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
        this.logPrefix = null;

        if (!CHANNEL.compareAndSet(this, DummyContextualChannelInstances.CHANNEL_CONNECTING,
                DummyContextualChannelInstances.CHANNEL_RECONNECT_FAILED)) {
            onUnexpectedState("notifyReconnectFailed", ConnectionContext.State.CONNECTING);
            return;
        }

        syncAfterTerminated(() -> {
            if (isClosed()) {
                onEndpointClosed();
            } else {
                onReconnectFailed();
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
            Deque<RedisCommand<?, ?, ?>> retryablePendingCommands) {
        final ContextualChannel inactiveChan = this.channel;
        if (!inactiveChan.context.initialState.isConnected()) {
            logger.error("[unexpected][{}] notifyChannelInactive: channel initial state not connected", logPrefix());
            onUnexpectedState("notifyChannelInactiveAfterWatchdogDecision", ConnectionContext.State.CONNECTED);
            return;
        }

        if (inactiveChan.getDelegate() != channel) {
            logger.error("[unexpected][{}] notifyChannelInactive: channel not match", logPrefix());
            onUnexpectedState("notifyChannelInactiveAfterWatchdogDecision: channel not match",
                    ConnectionContext.State.CONNECTED);
            return;
        }

        if (inactiveChan.context.isChannelInactiveEventFired()) {
            logger.error("[unexpected][{}] notifyChannelInactive: already fired", logPrefix());
            return;
        }

        boolean willReconnect = connectionWatchdog != null
                && connectionWatchdog.willReconnectOnAutoBatchFlushEndpointQuiescence();
        RedisException exception = null;
        // Unlike DefaultEndpoint, here we don't check reliability since connectionWatchdog.willReconnect() already does it.
        if (isClosed()) {
            exception = new RedisException("endpoint closed");
            willReconnect = false;
        }

        if (willReconnect) {
            this.logPrefix = null;
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
            this.logPrefix = null;
            CHANNEL.set(this, DummyContextualChannelInstances.CHANNEL_ENDPOINT_CLOSED);
        }
        inactiveChan.context
                .setCloseStatus(new ConnectionContext.CloseStatus(willReconnect, retryablePendingCommands, exception));
        trySetEndpointQuiescence(inactiveChan);
    }

    @Override
    public void notifyException(Throwable t) {
        if (t instanceof RedisConnectionException && RedisConnectionException.isProtectedMode(t.getMessage())) {
            connectionError = t;
            inProtectMode = true;
        }

        final ContextualChannel curr = this.channel;
        if (!curr.context.initialState.isConnected() || !curr.isActive()) {
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
        switch (chan.context.initialState) {
            case ENDPOINT_CLOSED:
                syncAfterTerminated(this::onEndpointClosed);
                return;
            case RECONNECT_FAILED:
                syncAfterTerminated(() -> {
                    if (isClosed()) {
                        onEndpointClosed();
                    } else {
                        onReconnectFailed();
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
                throw new IllegalStateException("unexpected state: " + chan.context.initialState);
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
    @SuppressWarnings("java:S125" /* The comments are necessary to prove the correctness code */)
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
            if (chan.context.initialState.isConnected()) {
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

        if (chan.context.initialState.isConnected() && chan.isOpen()) {
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
        if (chan.context.initialState.isConnected()) {
            chan.pipeline().fireUserEventTriggered(new ConnectionEvents.Reset());
        }
        if (!usesMpscQueue) {
            cancelCommands("reset");
        }
        // Otherwise, unsafe to call cancelBufferedCommands() here.
    }

    private void resetInternal() {
        if (debugEnabled) {
            logger.debug("{} reset()", logPrefix());
        }

        ContextualChannel chan = channel;
        if (chan.context.initialState.isConnected()) {
            chan.pipeline().fireUserEventTriggered(new ConnectionEvents.Reset());
        }
        LettuceAssert.assertState(lastEventLoop.inEventLoop(), "must be called in lastEventLoop thread");
        cancelCommands("resetInternal");
    }

    /**
     * Reset the command-handler to the initial not-connected state.
     */
    @Override
    public void initialState() {
        if (!usesMpscQueue) {
            cancelCommands("initialState");
        }
        // Otherwise, unsafe to call cancelBufferedCommands() here.
        ContextualChannel currentChannel = this.channel;
        if (currentChannel.context.initialState.isConnected()) {
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

        final ContextualChannel chan = this.channel;
        if (!chan.context.initialState.isConnected()) {
            final String buffer = "[" + chan.context.initialState + ", " + "epid=" + getId() + ']';
            logPrefix = buffer;
            return buffer;
        }

        final String buffer = "[CONNECTED, " + ChannelLogDescriptor.logDescriptor(chan.getDelegate()) + ", " + "epid=" + getId()
                + ']';
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
        loopSend(chan, false);
    }

    private void scheduleSendJobIfNeeded(final ContextualChannel chan) {
        final EventLoop eventLoop = chan.eventLoop();
        if (eventLoop.inEventLoop()) {
            // Possible in reactive() mode.
            loopSend(chan, false);
            return;
        }

        if (chan.context.autoBatchFlushEndPointContext.hasOngoingSendLoop.tryEnter()) {
            // Benchmark result:
            // Redis:
            // engine: 7.1.0
            // server: AWS elasticcache cache.r7g.large
            // Client: EC2-c5n.2xlarge
            // Test Model:
            // multi-thread sync exists (./bench-multi-thread-exists.sh -b 32 -s 10 -n 80000 -t 64)
            // Test Parameter:
            // thread num: 64, loop num: 80000, batch size: 32, write spin count: 10
            //
            // With tryEnter():
            // Avg latency: 0.64917373203125ms
            // Avg QPS: 196037.67991971457/s
            // Without tryEnter():
            // Avg latency: 0.6618976359375001ms
            // Avg QPS: 192240.1301551348/s
            eventLoop.execute(() -> loopSend(chan, true));
        }

        // Otherwise:
        // 1. offer() (volatile write of producerIndex) synchronizes-before hasOngoingSendLoop.safe.get() == 1 (volatile read)
        // 2. hasOngoingSendLoop.safe.get() == 1 (volatile read) synchronizes-before
        // hasOngoingSendLoop.safe.set(0) (volatile write) in first loopSend0()
        // 3. hasOngoingSendLoop.safe.set(0) (volatile write) synchronizes-before
        // taskQueue.isEmpty() (volatile read of producerIndex), which guarantees to see the offered task.
    }

    private void loopSend(final ContextualChannel chan, boolean entered) {
        final ConnectionContext connectionContext = chan.context;
        final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext = connectionContext.autoBatchFlushEndPointContext;
        if (connectionContext.isChannelInactiveEventFired()
                || autoBatchFlushEndPointContext.hasRetryableFailedToSendCommands()) {
            return;
        }

        LettuceAssert.assertState(channel == chan, "unexpected: channel not match but closeStatus == null");
        loopSend0(autoBatchFlushEndPointContext, chan, writeSpinCount, entered);
    }

    private void loopSend0(final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext, final ContextualChannel chan,
            int remainingSpinnCount, final boolean entered) {
        do {
            final int count = DefaultAutoBatchFlushEndpoint.this.pollBatch(autoBatchFlushEndPointContext, chan);
            if (count == 0) {
                break;
            }
            if (count < 0) {
                return;
            }
        } while (--remainingSpinnCount > 0);

        if (remainingSpinnCount <= 0) {
            // Don't need to exitUnsafe since we still have an ongoing consume tasks in this thread.
            chan.eventLoop().execute(() -> loopSend(chan, entered));
            return;
        }

        if (entered) {
            // queue was empty
            // The send loop will be triggered later when a new task is added,
            autoBatchFlushEndPointContext.hasOngoingSendLoop.exit();
            // Guarantee thread-safety: no dangling tasks in the queue, see scheduleSendJobIfNeeded()
            if (!taskQueue.isEmpty()) {
                loopSend0(autoBatchFlushEndPointContext, chan, remainingSpinnCount, false);
            }
        }
    }

    private int pollBatch(final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext, ContextualChannel chan) {
        int count = 0;
        while (count < batchSize) {
            final Object o = this.taskQueue.poll();
            if (o == null) {
                break;
            }

            if (o instanceof RedisCommand<?, ?, ?>) {
                RedisCommand<?, ?, ?> cmd = (RedisCommand<?, ?, ?>) o;
                channelWrite(chan, cmd).addListener(WrittenToChannel.newInstance(this, chan, cmd, false));
                count++;
            } else {
                @SuppressWarnings("unchecked")
                Collection<? extends RedisCommand<?, ?, ?>> commands = (Collection<? extends RedisCommand<?, ?, ?>>) o;
                for (RedisCommand<?, ?, ?> cmd : commands) {
                    channelWrite(chan, cmd).addListener(WrittenToChannel.newInstance(this, chan, cmd, false));
                }
                count += commands.size();
            }
        }

        if (count > 0) {
            autoBatchFlushEndPointContext.add(count);

            channelFlush(chan);
            if (autoBatchFlushEndPointContext.hasRetryableFailedToSendCommands()) {
                // Wait for onConnectionClose event()
                return -1;
            }
        }
        return count;
    }

    private void trySetEndpointQuiescence(ContextualChannel chan) {
        final EventLoop eventLoop = chan.eventLoop();
        LettuceAssert.isTrue(eventLoop.inEventLoop(), "unexpected: not in event loop");
        LettuceAssert.isTrue(eventLoop == lastEventLoop, "unexpected: lastEventLoop not match");

        final ConnectionContext connectionContext = chan.context;
        final @Nullable ConnectionContext.CloseStatus closeStatus = connectionContext.getCloseStatus();
        if (closeStatus == null) {
            return;
        }

        final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext = connectionContext.autoBatchFlushEndPointContext;
        if (!autoBatchFlushEndPointContext.isDone()) {
            return;
        }

        if (closeStatus.isWillReconnect()) {
            onWillReconnect(closeStatus, autoBatchFlushEndPointContext);
        } else {
            onWontReconnect(closeStatus, autoBatchFlushEndPointContext);
        }

        if (chan.context.setChannelQuiescentOnce()) {
            onEndpointQuiescence();
        } else {
            ExceptionUtils.maybeFire(logger, canFire, "unexpected: setEndpointQuiescenceOncePerConnection() failed");
        }
    }

    private void onWillReconnect(@Nonnull final ConnectionContext.CloseStatus closeStatus,
            final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext) {
        final @Nullable Deque<RedisCommand<?, ?, ?>> retryableFailedToSendTasks = autoBatchFlushEndPointContext
                .getAndClearRetryableFailedToSendCommands();
        if (retryableFailedToSendTasks != null) {
            // Save retryable failed tasks
            logger.info(
                    "[onWillReconnect][{}] compensate {} retryableFailedToSendTasks (write failure) for retrying on reconnecting, first write error: {}",
                    logPrefix(), retryableFailedToSendTasks.size(),
                    autoBatchFlushEndPointContext.getFirstDiscontinueReason().getMessage());
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
            final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext) {
        // No need to use syncAfterTerminated() since we are already in the event loop.
        if (isClosed()) {
            onEndpointClosed(closeStatus.getAndClearRetryablePendingCommands(),
                    autoBatchFlushEndPointContext.getAndClearRetryableFailedToSendCommands());
        } else {
            fulfillCommands("onConnectionClose called and won't reconnect",
                    it -> it.completeExceptionally(closeStatus.getErr()), closeStatus.getAndClearRetryablePendingCommands(),
                    autoBatchFlushEndPointContext.getAndClearRetryableFailedToSendCommands());
        }
    }

    private void onEndpointQuiescence() {
        if (channel.context.initialState == ConnectionContext.State.ENDPOINT_CLOSED) {
            return;
        }

        this.logPrefix = null;
        // Create happens-before with channelActive()
        if (!CHANNEL.compareAndSet(this, DummyContextualChannelInstances.CHANNEL_WILL_RECONNECT,
                DummyContextualChannelInstances.CHANNEL_CONNECTING)) {
            onUnexpectedState("onEndpointQuiescence", ConnectionContext.State.WILL_RECONNECT);
            return;
        }

        // notify connectionWatchDog that it is safe to reconnect now.
        // neither connectionWatchdog nor doReconnectOnEndpointQuiescence could be null
        // noinspection DataFlowIssue
        connectionWatchdog.reconnectOnAutoBatchFlushEndpointQuiescence();
    }

    private void offerFirstAll(Deque<RedisCommand<?, ?, ?>> commands) {
        commands.forEach(cmd -> {
            if (cmd instanceof DemandAware.Sink) {
                ((DemandAware.Sink) cmd).removeSource();
            }
        });
        this.taskQueue.offerFirstAll(commands);
        QUEUE_SIZE.addAndGet(this, commands.size());
    }

    private void cancelCommands(String message) {
        fulfillCommands(message, RedisCommand::cancel);
    }

    @SafeVarargs
    private final void onEndpointClosed(Queue<RedisCommand<?, ?, ?>>... queues) {
        fulfillCommands("endpoint closed", callbackOnClose, queues);
    }

    private final void onReconnectFailed() {
        fulfillCommands("reconnect failed", cmd -> cmd.completeExceptionally(getFailedToReconnectReason()));
    }

    @SafeVarargs
    @SuppressWarnings("java:S3776" /* Suppress cognitive complexity warning */)
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

        int cancelledTaskNumInTaskQueue = 0;
        while (true) {
            Object o = this.taskQueue.poll();
            if (o == null) {
                break;
            }

            if (o instanceof RedisCommand<?, ?, ?>) {
                RedisCommand<?, ?, ?> cmd = (RedisCommand<?, ?, ?>) o;
                if (cmd.getOutput() != null) {
                    cmd.getOutput().setError(message);
                }
                commandConsumer.accept(cmd);
                cancelledTaskNumInTaskQueue++;
                totalCancelledTaskNum++;
            } else {
                @SuppressWarnings("unchecked")
                Collection<? extends RedisCommand<?, ?, ?>> commands = (Collection<? extends RedisCommand<?, ?, ?>>) o;
                for (RedisCommand<?, ?, ?> cmd : commands) {
                    if (cmd.getOutput() != null) {
                        cmd.getOutput().setError(message);
                    }
                    commandConsumer.accept(cmd);
                }
                cancelledTaskNumInTaskQueue += commands.size();
                totalCancelledTaskNum += commands.size();
            }
        }

        QUEUE_SIZE.addAndGet(this, -cancelledTaskNumInTaskQueue);
        if (totalCancelledTaskNum > 0) {
            logger.error("{} cancel {} pending tasks, reason: '{}'", logPrefix(), totalCancelledTaskNum, message);
        }
    }

    private Throwable getFailedToReconnectReason() {
        Throwable t = failedToReconnectReason;
        if (t != null) {
            return t;
        }
        return new Throwable("reconnect failed");
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

    private Throwable validateWrite(ContextualChannel chan, int commands, boolean isActivationCommand) {
        if (isClosed()) {
            return new RedisException("Endpoint is closed");
        }

        final Throwable localConnectionErr = connectionError;
        if (localConnectionErr != null /* attention: different logic of DefaultEndpoint */) {
            return localConnectionErr;
        }

        if (!isActivationCommand /* activation command should never be excluded due to queue full */ && boundedQueues
                && queueSize + commands > clientOptions.getRequestQueueSize()) {
            return new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                    + ". Commands are not accepted until the queue size drops.");
        }

        final ConnectionContext.State initialState = chan.context.initialState;
        final boolean rejectCommandsWhileDisconnectedLocal = this.rejectCommandsWhileDisconnected || isActivationCommand;
        final String rejectDesc = isActivationCommand ? "isActivationCommand" : "rejectCommandsWhileDisconnected";
        switch (initialState) {
            case ENDPOINT_CLOSED:
                return new RedisException("Connection is closed");
            case RECONNECT_FAILED:
                return getFailedToReconnectReason();
            case WILL_RECONNECT:
            case CONNECTING:
                return rejectCommandsWhileDisconnectedLocal ? new RedisException("Currently not connected and " + rejectDesc)
                        : null;
            case CONNECTED:
                return !chan.isActive() && rejectCommandsWhileDisconnectedLocal
                        ? new RedisException("Channel is inactive and " + rejectDesc)
                        : null;
            default:
                throw new IllegalStateException("unexpected state: " + initialState);
        }
    }

    private void onUnexpectedState(String caller, ConnectionContext.State exp) {
        final ConnectionContext.State actual = this.channel.context.initialState;
        logger.error("{}[{}][unexpected] : unexpected state: exp '{}' got '{}'", logPrefix(), caller, exp, actual);
        syncAfterTerminated(
                () -> cancelCommands(String.format("%s: state not match: expect '%s', got '%s'", caller, exp, actual)));
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

    /**
     * Add to stack listener. This listener is pooled and must be {@link #recycle() recycled after usage}.
     */
    static class WrittenToChannel implements GenericFutureListener<Future<Void>> {

        private static final Recycler<WrittenToChannel> RECYCLER = new Recycler<WrittenToChannel>() {

            @Override
            protected WrittenToChannel newObject(Recycler.Handle<WrittenToChannel> handle) {
                return new WrittenToChannel(handle);
            }

        };

        private final Recycler.Handle<WrittenToChannel> handle;

        private DefaultAutoBatchFlushEndpoint endpoint;

        private RedisCommand<?, ?, ?> cmd;

        private boolean isActivationCommand;

        private ContextualChannel chan;

        private WrittenToChannel(Recycler.Handle<WrittenToChannel> handle) {
            this.handle = handle;
        }

        /**
         * Allocate a new instance.
         *
         * @return new instance
         */
        static WrittenToChannel newInstance(DefaultAutoBatchFlushEndpoint endpoint, ContextualChannel chan,
                RedisCommand<?, ?, ?> command, boolean isActivationCommand) {

            WrittenToChannel entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.chan = chan;
            entry.cmd = command;
            entry.isActivationCommand = isActivationCommand;

            LettuceAssert.assertState(isActivationCommand == ActivationCommand.isActivationCommand(command),
                    "unexpected: isActivationCommand not match");

            return entry;
        }

        @Override
        public void operationComplete(Future<Void> future) {
            try {
                if (isActivationCommand) {
                    if (!future.isSuccess()) {
                        cmd.completeExceptionally(future.cause());
                    }
                    return;
                }

                final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext = chan.context.autoBatchFlushEndPointContext;
                QUEUE_SIZE.decrementAndGet(endpoint);
                autoBatchFlushEndPointContext.done(1);

                final Throwable retryableErr = checkSendResult(future);
                if (retryableErr != null && autoBatchFlushEndPointContext.addRetryableFailedToSendCommand(cmd, retryableErr)) {
                    // Close connection on first transient write failure
                    internalCloseConnectionIfNeeded(retryableErr);
                }

                endpoint.trySetEndpointQuiescence(chan);
            } finally {
                recycle();
            }
        }

        /**
         * Check write result.
         *
         * @param sendFuture The future to check.
         * @return The cause of the failure if is a retryable failed task, otherwise null.
         */
        private Throwable checkSendResult(Future<?> sendFuture) {
            if (cmd.isDone()) {
                ExceptionUtils.logUnexpectedDone(logger, endpoint.logPrefix(), cmd);
                return null;
            }

            final ConnectionContext.CloseStatus closeStatus = chan.context.getCloseStatus();
            if (closeStatus != null) {
                logger.warn("[checkSendResult][interesting][{}] callback called after onClose() event, close status: {}",
                        endpoint.logPrefix(), chan.context.getCloseStatus());
                final Throwable err = sendFuture.isSuccess() ? closeStatus.getErr() : sendFuture.cause();
                if (!closeStatus.isWillReconnect() || shouldNotRetry(err)) {
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
            if (shouldNotRetry(cause)) {
                cmd.completeExceptionally(cause);
                return null;
            }

            return cause;
        }

        private boolean shouldNotRetry(Throwable cause) {
            return endpoint.reliability == Reliability.AT_MOST_ONCE
                    || ExceptionUtils.oneOf(cause, SHOULD_NOT_RETRY_EXCEPTION_TYPES);
        }

        private void internalCloseConnectionIfNeeded(Throwable reason) {
            final ContextualChannel chanLocal = this.chan; // the value may be changed in the future, so save it on stack.
            if (chanLocal.context.isChannelInactiveEventFired() || !chanLocal.isActive()) {
                return;
            }

            logger.error(
                    "[internalCloseConnectionIfNeeded][interesting][{}] close the connection due to write error, reason: '{}'",
                    endpoint.logPrefix(), reason.getMessage(), reason);
            chanLocal.eventLoop().schedule(() -> {
                if (chanLocal.isActive()) {
                    chanLocal.close();
                }
            }, 1, TimeUnit.SECONDS);
        }

        private void recycle() {
            this.endpoint = null;
            this.chan = null;
            this.cmd = null;
            this.isActivationCommand = false;

            handle.recycle(this);
        }

    }

}
