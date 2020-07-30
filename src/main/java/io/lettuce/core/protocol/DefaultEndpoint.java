/*
 * Copyright 2011-2020 the original author or authors.
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

import static io.lettuce.core.protocol.CommandHandler.SUPPRESS_IO_EXCEPTION_MESSAGES;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.lettuce.core.*;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Recycler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default {@link Endpoint} implementation.
 *
 * @author Mark Paluch
 */
public class DefaultEndpoint implements RedisChannelWriter, Endpoint, PushHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEndpoint.class);

    private static final AtomicLong ENDPOINT_COUNTER = new AtomicLong();

    private static final AtomicIntegerFieldUpdater<DefaultEndpoint> QUEUE_SIZE = AtomicIntegerFieldUpdater
            .newUpdater(DefaultEndpoint.class, "queueSize");

    private static final AtomicIntegerFieldUpdater<DefaultEndpoint> STATUS = AtomicIntegerFieldUpdater
            .newUpdater(DefaultEndpoint.class, "status");

    private static final int ST_OPEN = 0;

    private static final int ST_CLOSED = 1;

    protected volatile Channel channel;

    private final Reliability reliability;

    private final ClientOptions clientOptions;

    private final ClientResources clientResources;

    private final Queue<RedisCommand<?, ?, ?>> disconnectedBuffer;

    private final Queue<RedisCommand<?, ?, ?>> commandBuffer;

    private final boolean boundedQueues;

    private final boolean rejectCommandsWhileDisconnected;

    private final long endpointId = ENDPOINT_COUNTER.incrementAndGet();

    private final List<PushListener> pushListeners = new CopyOnWriteArrayList<>();

    private final SharedLock sharedLock = new SharedLock();

    private final boolean debugEnabled = logger.isDebugEnabled();

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private String logPrefix;

    private boolean autoFlushCommands = true;

    private boolean inActivation = false;

    private ConnectionWatchdog connectionWatchdog;

    private ConnectionFacade connectionFacade;

    private volatile Throwable connectionError;

    // access via QUEUE_SIZE
    @SuppressWarnings("unused")
    private volatile int queueSize = 0;

    // access via STATUS
    @SuppressWarnings("unused")
    private volatile int status = ST_OPEN;

    /**
     * Create a new {@link DefaultEndpoint}.
     *
     * @param clientOptions client options for this connection, must not be {@code null}.
     * @param clientResources client resources for this connection, must not be {@code null}.
     */
    public DefaultEndpoint(ClientOptions clientOptions, ClientResources clientResources) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(clientOptions, "ClientResources must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
        this.disconnectedBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        this.commandBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        this.boundedQueues = clientOptions.getRequestQueueSize() != Integer.MAX_VALUE;
        this.rejectCommandsWhileDisconnected = isRejectCommand(clientOptions);
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

        RedisException validation = validateWrite(1);
        if (validation != null) {
            command.completeExceptionally(validation);
            return command;
        }

        try {
            sharedLock.incrementWriters();

            if (inActivation) {
                command = processActivationCommand(command);
            }

            if (autoFlushCommands) {

                if (isConnected()) {
                    writeToChannelAndFlush(command);
                } else {
                    writeToDisconnectedBuffer(command);
                }

            } else {
                writeToBuffer(command);
            }
        } finally {
            sharedLock.decrementWriters();
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

        RedisException validation = validateWrite(commands.size());

        if (validation != null) {
            commands.forEach(it -> it.completeExceptionally(validation));
            return (Collection<RedisCommand<K, V, ?>>) commands;
        }

        try {
            sharedLock.incrementWriters();

            if (inActivation) {
                commands = processActivationCommands(commands);
            }

            if (autoFlushCommands) {

                if (isConnected()) {
                    writeToChannelAndFlush(commands);
                } else {
                    writeToDisconnectedBuffer(commands);
                }

            } else {
                writeToBuffer(commands);
            }
        } finally {
            sharedLock.decrementWriters();
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return (Collection<RedisCommand<K, V, ?>>) commands;
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

    private RedisException validateWrite(int commands) {

        if (isClosed()) {
            return new RedisException("Connection is closed");
        }

        if (usesBoundedQueues()) {

            boolean connected = isConnected();

            if (QUEUE_SIZE.get(this) + commands > clientOptions.getRequestQueueSize()) {
                return new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (!connected && disconnectedBuffer.size() + commands > clientOptions.getRequestQueueSize()) {
                return new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (connected && commandBuffer.size() + commands > clientOptions.getRequestQueueSize()) {
                return new RedisException("Command buffer size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }
        }

        if (!isConnected() && rejectCommandsWhileDisconnected) {
            return new RedisException("Currently not connected. Commands are rejected.");
        }

        return null;
    }

    private boolean usesBoundedQueues() {
        return boundedQueues;
    }

    private void writeToBuffer(Iterable<? extends RedisCommand<?, ?, ?>> commands) {

        for (RedisCommand<?, ?, ?> command : commands) {
            writeToBuffer(command);
        }
    }

    private void writeToDisconnectedBuffer(Collection<? extends RedisCommand<?, ?, ?>> commands) {
        for (RedisCommand<?, ?, ?> command : commands) {
            writeToDisconnectedBuffer(command);
        }
    }

    private void writeToDisconnectedBuffer(RedisCommand<?, ?, ?> command) {

        if (connectionError != null) {
            if (debugEnabled) {
                logger.debug("{} writeToDisconnectedBuffer() Completing command {} due to connection error", logPrefix(),
                        command);
            }
            command.completeExceptionally(connectionError);

            return;
        }

        if (debugEnabled) {
            logger.debug("{} writeToDisconnectedBuffer() buffering (disconnected) command {}", logPrefix(), command);
        }

        disconnectedBuffer.add(command);
    }

    protected <C extends RedisCommand<?, ?, T>, T> void writeToBuffer(C command) {

        if (debugEnabled) {
            logger.debug("{} writeToBuffer() buffering command {}", logPrefix(), command);
        }

        if (connectionError != null) {

            if (debugEnabled) {
                logger.debug("{} writeToBuffer() Completing command {} due to connection error", logPrefix(), command);
            }
            command.completeExceptionally(connectionError);

            return;
        }

        commandBuffer.add(command);
    }

    private void writeToChannelAndFlush(RedisCommand<?, ?, ?> command) {

        QUEUE_SIZE.incrementAndGet(this);

        ChannelFuture channelFuture = channelWriteAndFlush(command);

        if (reliability == Reliability.AT_MOST_ONCE) {
            // cancel on exceptions and remove from queue, because there is no housekeeping
            channelFuture.addListener(AtMostOnceWriteListener.newInstance(this, command));
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {
            // commands are ok to stay within the queue, reconnect will retrigger them
            channelFuture.addListener(RetryListener.newInstance(this, command));
        }
    }

    private void writeToChannelAndFlush(Collection<? extends RedisCommand<?, ?, ?>> commands) {

        QUEUE_SIZE.addAndGet(this, commands.size());

        if (reliability == Reliability.AT_MOST_ONCE) {

            // cancel on exceptions and remove from queue, because there is no housekeeping
            for (RedisCommand<?, ?, ?> command : commands) {
                channelWrite(command).addListener(AtMostOnceWriteListener.newInstance(this, command));
            }
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {

            // commands are ok to stay within the queue, reconnect will retrigger them
            for (RedisCommand<?, ?, ?> command : commands) {
                channelWrite(command).addListener(RetryListener.newInstance(this, command));
            }
        }

        channelFlush();
    }

    private void channelFlush() {

        if (debugEnabled) {
            logger.debug("{} write() channelFlush", logPrefix());
        }

        channel.flush();
    }

    private ChannelFuture channelWrite(RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() channelWrite command {}", logPrefix(), command);
        }

        return channel.write(command);
    }

    private ChannelFuture channelWriteAndFlush(RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush command {}", logPrefix(), command);
        }

        return channel.writeAndFlush(command);
    }

    @Override
    public void notifyChannelActive(Channel channel) {

        this.logPrefix = null;
        this.channel = channel;
        this.connectionError = null;

        if (isClosed()) {

            logger.info("{} Closing channel because endpoint is already closed", logPrefix());
            channel.close();
            return;
        }

        if (connectionWatchdog != null) {
            connectionWatchdog.arm();
        }

        sharedLock.doExclusive(() -> {

            try {
                // Move queued commands to buffer before issuing any commands because of connection activation.
                // That's necessary to prepend queued commands first as some commands might get into the queue
                // after the connection was disconnected. They need to be prepended to the command buffer

                if (debugEnabled) {
                    logger.debug("{} activateEndpointAndExecuteBufferedCommands {} command(s) buffered", logPrefix(),
                            disconnectedBuffer.size());
                }

                if (debugEnabled) {
                    logger.debug("{} activating endpoint", logPrefix());
                }

                try {
                    inActivation = true;
                    connectionFacade.activated();
                } finally {
                    inActivation = false;
                }

                flushCommands(disconnectedBuffer);
            } catch (Exception e) {

                if (debugEnabled) {
                    logger.debug("{} channelActive() ran into an exception", logPrefix());
                }

                if (clientOptions.isCancelCommandsOnReconnectFailure()) {
                    reset();
                }

                throw e;
            }
        });
    }

    @Override
    public void notifyChannelInactive(Channel channel) {

        if (isClosed()) {
            RedisException closed = new RedisException("Connection closed");
            cancelCommands("Connection closed", drainCommands(), it -> it.completeExceptionally(closed));
        }

        sharedLock.doExclusive(() -> {

            if (debugEnabled) {
                logger.debug("{} deactivating endpoint handler", logPrefix());
            }

            connectionFacade.deactivated();
        });

        if (this.channel == channel) {
            this.channel = null;
        }
    }

    @Override
    public void notifyException(Throwable t) {

        if (t instanceof RedisConnectionException && RedisConnectionException.isProtectedMode(t.getMessage())) {

            connectionError = t;

            if (connectionWatchdog != null) {
                connectionWatchdog.setListenOnChannelInactive(false);
                connectionWatchdog.setReconnectSuspended(false);
            }

            doExclusive(this::drainCommands).forEach(cmd -> cmd.completeExceptionally(t));
        }

        if (!isConnected()) {
            connectionError = t;
        }
    }

    @Override
    public void registerConnectionWatchdog(ConnectionWatchdog connectionWatchdog) {
        this.connectionWatchdog = connectionWatchdog;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void flushCommands() {
        flushCommands(commandBuffer);
    }

    private void flushCommands(Queue<RedisCommand<?, ?, ?>> queue) {

        if (debugEnabled) {
            logger.debug("{} flushCommands()", logPrefix());
        }

        if (isConnected()) {

            List<RedisCommand<?, ?, ?>> commands = sharedLock.doExclusive(() -> {

                if (queue.isEmpty()) {
                    return Collections.emptyList();
                }

                return drainCommands(queue);
            });

            if (debugEnabled) {
                logger.debug("{} flushCommands() Flushing {} commands", logPrefix(), commands.size());
            }

            if (!commands.isEmpty()) {
                writeToChannelAndFlush(commands);
            }
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

            cancelBufferedCommands("Close");

            Channel channel = getOpenChannel();

            if (channel != null) {
                Futures.adapt(channel.close(), closeFuture);
            } else {
                closeFuture.complete(null);
            }
        }

        return closeFuture;
    }

    /**
     * Disconnect the channel.
     */
    public void disconnect() {

        Channel channel = this.channel;

        if (channel != null && channel.isOpen()) {
            channel.disconnect();
        }
    }

    private Channel getOpenChannel() {

        Channel currentChannel = this.channel;

        if (currentChannel != null) {
            return currentChannel;
        }

        return null;
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

        if (channel != null) {
            channel.pipeline().fireUserEventTriggered(new ConnectionEvents.Reset());
        }
        cancelBufferedCommands("Reset");
    }

    /**
     * Reset the command-handler to the initial not-connected state.
     */
    public void initialState() {

        commandBuffer.clear();

        Channel currentChannel = this.channel;
        if (currentChannel != null) {

            ChannelFuture close = currentChannel.close();
            if (currentChannel.isOpen()) {
                close.syncUninterruptibly();
            }
        }
    }

    @Override
    public void notifyDrainQueuedCommands(HasQueuedCommands queuedCommands) {

        if (isClosed()) {

            RedisException closed = new RedisException("Connection closed");
            cancelCommands(closed.getMessage(), queuedCommands.drainQueue(), it -> it.completeExceptionally(closed));
            cancelCommands(closed.getMessage(), drainCommands(), it -> it.completeExceptionally(closed));
            return;
        } else if (reliability == Reliability.AT_MOST_ONCE && rejectCommandsWhileDisconnected) {

            RedisException disconnected = new RedisException("Connection disconnected");
            cancelCommands(disconnected.getMessage(), queuedCommands.drainQueue(),
                    it -> it.completeExceptionally(disconnected));
            cancelCommands(disconnected.getMessage(), drainCommands(), it -> it.completeExceptionally(disconnected));
            return;
        }

        sharedLock.doExclusive(() -> {

            Collection<RedisCommand<?, ?, ?>> commands = queuedCommands.drainQueue();

            if (debugEnabled) {
                logger.debug("{} notifyQueuedCommands adding {} command(s) to buffer", logPrefix(), commands.size());
            }

            commands.addAll(drainCommands(disconnectedBuffer));

            for (RedisCommand<?, ?, ?> command : commands) {

                if (command instanceof DemandAware.Sink) {
                    ((DemandAware.Sink) command).removeSource();
                }
            }

            try {
                disconnectedBuffer.addAll(commands);
            } catch (RuntimeException e) {

                if (debugEnabled) {
                    logger.debug("{} notifyQueuedCommands Queue overcommit. Cannot add all commands to buffer (disconnected).",
                            logPrefix(), commands.size());
                }
                commands.removeAll(disconnectedBuffer);

                for (RedisCommand<?, ?, ?> command : commands) {
                    command.completeExceptionally(e);
                }
            }

            if (isConnected()) {
                flushCommands(disconnectedBuffer);
            }
        });
    }

    public boolean isClosed() {
        return STATUS.get(this) == ST_CLOSED;
    }

    /**
     * Execute a {@link Supplier} callback guarded by an exclusive lock.
     *
     * @param supplier
     * @param <T>
     * @return
     */
    protected <T> T doExclusive(Supplier<T> supplier) {
        return sharedLock.doExclusive(supplier);
    }

    protected List<RedisCommand<?, ?, ?>> drainCommands() {

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>(disconnectedBuffer.size() + commandBuffer.size());

        target.addAll(drainCommands(disconnectedBuffer));
        target.addAll(drainCommands(commandBuffer));

        return target;
    }

    /**
     * Drain commands from a queue and return only active commands.
     *
     * @param source the source queue.
     * @return List of commands.
     */
    private static List<RedisCommand<?, ?, ?>> drainCommands(Queue<? extends RedisCommand<?, ?, ?>> source) {

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>(source.size());

        RedisCommand<?, ?, ?> cmd;
        while ((cmd = source.poll()) != null) {

            if (!cmd.isDone() && !ActivationCommand.isActivationCommand(cmd)) {
                target.add(cmd);
            }
        }

        return target;
    }

    private void cancelBufferedCommands(String message) {
        cancelCommands(message, doExclusive(this::drainCommands), RedisCommand::cancel);
    }

    private void cancelCommands(String message, Iterable<? extends RedisCommand<?, ?, ?>> toCancel,
            Consumer<RedisCommand<?, ?, ?>> commandConsumer) {

        for (RedisCommand<?, ?, ?> cmd : toCancel) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError(message);
            }
            commandConsumer.accept(cmd);
        }
    }

    private boolean isConnected() {

        Channel channel = this.channel;
        return channel != null && channel.isActive();
    }

    protected String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        String buffer = "[" + ChannelLogDescriptor.logDescriptor(channel) + ", " + "epid=0x" + Long.toHexString(endpointId)
                + ']';
        return logPrefix = buffer;
    }

    private static boolean isRejectCommand(ClientOptions clientOptions) {

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

    static class ListenerSupport {

        Collection<? extends RedisCommand<?, ?, ?>> sentCommands;

        RedisCommand<?, ?, ?> sentCommand;

        DefaultEndpoint endpoint;

        void dequeue() {

            if (sentCommand != null) {
                QUEUE_SIZE.decrementAndGet(endpoint);
            } else {
                QUEUE_SIZE.addAndGet(endpoint, -sentCommands.size());
            }
        }

        protected void complete(Throwable t) {

            if (sentCommand != null) {
                sentCommand.completeExceptionally(t);
            } else {
                for (RedisCommand<?, ?, ?> sentCommand : sentCommands) {
                    sentCommand.completeExceptionally(t);
                }
            }
        }

    }

    static class AtMostOnceWriteListener extends ListenerSupport implements ChannelFutureListener {

        private static final Recycler<AtMostOnceWriteListener> RECYCLER = new Recycler<AtMostOnceWriteListener>() {

            @Override
            protected AtMostOnceWriteListener newObject(Handle<AtMostOnceWriteListener> handle) {
                return new AtMostOnceWriteListener(handle);
            }

        };

        private final Recycler.Handle<AtMostOnceWriteListener> handle;

        AtMostOnceWriteListener(Recycler.Handle<AtMostOnceWriteListener> handle) {
            this.handle = handle;
        }

        static AtMostOnceWriteListener newInstance(DefaultEndpoint endpoint, RedisCommand<?, ?, ?> command) {

            AtMostOnceWriteListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommand = command;

            return entry;
        }

        static AtMostOnceWriteListener newInstance(DefaultEndpoint endpoint,
                Collection<? extends RedisCommand<?, ?, ?>> commands) {

            AtMostOnceWriteListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommands = commands;

            return entry;
        }

        @Override
        public void operationComplete(ChannelFuture future) {

            try {

                dequeue();

                if (!future.isSuccess() && future.cause() != null) {
                    complete(future.cause());
                }
            } finally {
                recycle();
            }
        }

        private void recycle() {

            this.endpoint = null;
            this.sentCommand = null;
            this.sentCommands = null;

            handle.recycle(this);
        }

    }

    /**
     * A generic future listener which retries unsuccessful writes.
     */
    static class RetryListener extends ListenerSupport implements GenericFutureListener<Future<Void>> {

        private static final Recycler<RetryListener> RECYCLER = new Recycler<RetryListener>() {

            @Override
            protected RetryListener newObject(Handle<RetryListener> handle) {
                return new RetryListener(handle);
            }

        };

        private final Recycler.Handle<RetryListener> handle;

        RetryListener(Recycler.Handle<RetryListener> handle) {
            this.handle = handle;
        }

        static RetryListener newInstance(DefaultEndpoint endpoint, RedisCommand<?, ?, ?> command) {

            RetryListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommand = command;

            return entry;
        }

        static RetryListener newInstance(DefaultEndpoint endpoint, Collection<? extends RedisCommand<?, ?, ?>> commands) {

            RetryListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommands = commands;

            return entry;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void operationComplete(Future<Void> future) {

            try {
                doComplete(future);
            } finally {
                recycle();
            }
        }

        private void doComplete(Future<Void> future) {

            Throwable cause = future.cause();

            boolean success = future.isSuccess();
            dequeue();

            if (success) {
                return;
            }

            if (cause instanceof EncoderException || cause instanceof Error || cause.getCause() instanceof Error) {
                complete(cause);
                return;
            }

            Channel channel = endpoint.channel;

            // Capture values before recycler clears these.
            RedisCommand<?, ?, ?> sentCommand = this.sentCommand;
            Collection<? extends RedisCommand<?, ?, ?>> sentCommands = this.sentCommands;
            potentiallyRequeueCommands(channel, sentCommand, sentCommands);

            if (!(cause instanceof ClosedChannelException)) {

                String message = "Unexpected exception during request: {}";
                InternalLogLevel logLevel = InternalLogLevel.WARN;

                if (cause instanceof IOException && SUPPRESS_IO_EXCEPTION_MESSAGES.contains(cause.getMessage())) {
                    logLevel = InternalLogLevel.DEBUG;
                }

                logger.log(logLevel, message, cause.toString(), cause);
            }
        }

        /**
         * Requeue command/commands
         *
         * @param channel
         * @param sentCommand
         * @param sentCommands
         */
        private void potentiallyRequeueCommands(Channel channel, RedisCommand<?, ?, ?> sentCommand,
                Collection<? extends RedisCommand<?, ?, ?>> sentCommands) {

            if (sentCommand != null && sentCommand.isDone()) {
                return;
            }

            if (sentCommands != null) {

                boolean foundToSend = false;

                for (RedisCommand<?, ?, ?> command : sentCommands) {
                    if (!command.isDone()) {
                        foundToSend = true;
                        break;
                    }
                }

                if (!foundToSend) {
                    return;
                }
            }

            if (channel != null) {
                DefaultEndpoint endpoint = this.endpoint;
                channel.eventLoop().submit(() -> {
                    requeueCommands(sentCommand, sentCommands, endpoint);
                });
            } else {
                requeueCommands(sentCommand, sentCommands, endpoint);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void requeueCommands(RedisCommand<?, ?, ?> sentCommand,
                Collection<? extends RedisCommand<?, ?, ?>> sentCommands, DefaultEndpoint endpoint) {

            if (sentCommand != null) {
                try {
                    endpoint.write(sentCommand);
                } catch (Exception e) {
                    sentCommand.completeExceptionally(e);
                }
            } else {
                try {
                    endpoint.write((Collection) sentCommands);
                } catch (Exception e) {
                    for (RedisCommand<?, ?, ?> command : sentCommands) {
                        command.completeExceptionally(e);
                    }
                }
            }
        }

        private void recycle() {

            this.endpoint = null;
            this.sentCommand = null;
            this.sentCommands = null;

            handle.recycle(this);
        }

    }

    private enum Reliability {
        AT_MOST_ONCE, AT_LEAST_ONCE
    }

    static class ActivationCommand<K, V, T> extends CommandWrapper<K, V, T> {

        public ActivationCommand(RedisCommand<K, V, T> command) {
            super(command);
        }

        public static boolean isActivationCommand(RedisCommand<?, ?, ?> command) {

            if (command instanceof ActivationCommand) {
                return true;
            }

            while (command instanceof CommandWrapper) {
                command = ((CommandWrapper<?, ?, ?>) command).getDelegate();

                if (command instanceof ActivationCommand) {
                    return true;
                }
            }

            return false;
        }

    }

}
