/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.protocol;

import static io.lettuce.core.protocol.CommandHandler.SUPPRESS_IO_EXCEPTION_MESSAGES;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.lettuce.core.*;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceFactories;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.EncoderException;
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
public class DefaultEndpoint implements RedisChannelWriter, Endpoint {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEndpoint.class);
    private static final AtomicLong ENDPOINT_COUNTER = new AtomicLong();
    private static final AtomicIntegerFieldUpdater<DefaultEndpoint> QUEUE_SIZE = AtomicIntegerFieldUpdater.newUpdater(
            DefaultEndpoint.class, "queueSize");

    protected volatile Channel channel;

    private final Reliability reliability;
    private final ClientOptions clientOptions;
    private final Queue<RedisCommand<?, ?, ?>> disconnectedBuffer;
    private final Queue<RedisCommand<?, ?, ?>> commandBuffer;
    private final boolean boundedQueues;

    private final long endpointId = ENDPOINT_COUNTER.incrementAndGet();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final SharedLock sharedLock = new SharedLock();
    private final boolean debugEnabled = logger.isDebugEnabled();

    private String logPrefix;
    private boolean autoFlushCommands = true;

    private ConnectionWatchdog connectionWatchdog;
    private ConnectionFacade connectionFacade;

    private volatile Throwable connectionError;

    // access via QUEUE_SIZE
    @SuppressWarnings("unused")
    private volatile int queueSize = 0;

    /**
     * Create a new {@link DefaultEndpoint}.
     *
     * @param clientOptions client options for this connection, must not be {@literal null}
     */
    public DefaultEndpoint(ClientOptions clientOptions) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");

        this.clientOptions = clientOptions;
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
        this.disconnectedBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        this.commandBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        this.boundedQueues = clientOptions.getRequestQueueSize() != Integer.MAX_VALUE;
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connectionFacade) {
        this.connectionFacade = connectionFacade;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        this.autoFlushCommands = autoFlush;
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        LettuceAssert.notNull(command, "Command must not be null");

        try {
            sharedLock.incrementWriters();

            validateWrite(1);

            if (autoFlushCommands) {

                if (isConnected()) {
                    writeToChannel(command);
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

        try {
            sharedLock.incrementWriters();

            validateWrite(commands.size());

            if (autoFlushCommands) {

                if (isConnected()) {
                    writeToChannel(commands);
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

    private void validateWrite(int commands) {

        if (isClosed()) {
            throw new RedisException("Connection is closed");
        }

        if (usesBoundedQueues()) {

            boolean connected = isConnected();

            if (QUEUE_SIZE.get(this) + commands > clientOptions.getRequestQueueSize()) {
                throw new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (!connected && disconnectedBuffer.size() + commands > clientOptions.getRequestQueueSize()) {
                throw new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (connected && commandBuffer.size() + commands > clientOptions.getRequestQueueSize()) {
                throw new RedisException("Command buffer size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }
        }

        if (!isConnected() && isRejectCommand()) {
            throw new RedisException("Currently not connected. Commands are rejected.");
        }
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

    private void writeToChannel(RedisCommand<?, ?, ?> command) {

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

    private void writeToChannel(Collection<? extends RedisCommand<?, ?, ?>> commands) {

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

                connectionFacade.activated();

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
            cancelBufferedCommands("Connection closed");
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
                    return Collections.<RedisCommand<?, ?, ?>> emptyList();
                }

                return drainCommands(queue);
            });

            if (debugEnabled) {
                logger.debug("{} flushCommands() Flushing {} commands", logPrefix(), commands.size());
            }

            if (!commands.isEmpty()) {

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

        if (isClosed()) {
            return;
        }

        if (closed.compareAndSet(false, true)) {

            if (connectionWatchdog != null) {
                connectionWatchdog.prepareClose();
            }

            cancelBufferedCommands("Close");

            Channel currentChannel = this.channel;
            if (currentChannel != null) {

                ChannelFuture close = currentChannel.close();
                if (currentChannel.isOpen()) {
                    close.syncUninterruptibly();
                }
            }
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
            cancelCommands("Connection closed", queuedCommands.drainQueue());
            cancelCommands("Connection closed", drainCommands());
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
        return closed.get();
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

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>();

        target.addAll(drainCommands(disconnectedBuffer));
        target.addAll(drainCommands(commandBuffer));

        return target;
    }

    private static List<RedisCommand<?, ?, ?>> drainCommands(Queue<? extends RedisCommand<?, ?, ?>> source) {

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>(source.size());

        RedisCommand<?, ?, ?> cmd;
        while ((cmd = source.poll()) != null) {
            target.add(cmd);
        }

        return target;
    }

    private void cancelBufferedCommands(String message) {
        cancelCommands(message, doExclusive(this::drainCommands));
    }

    private void cancelCommands(String message, Iterable<? extends RedisCommand<?, ?, ?>> toCancel) {

        for (RedisCommand<?, ?, ?> cmd : toCancel) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError(message);
            }
            cmd.cancel();
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

    private class ListenerSupport {

        final Collection<? extends RedisCommand<?, ?, ?>> sentCommands;
        final RedisCommand<?, ?, ?> sentCommand;

        ListenerSupport(RedisCommand<?, ?, ?> sentCommand) {
            this.sentCommand = sentCommand;
            this.sentCommands = null;
        }

        ListenerSupport(Collection<? extends RedisCommand<?, ?, ?>> sentCommands) {
            this.sentCommand = null;
            this.sentCommands = sentCommands;
        }

        void dequeue() {
            if (sentCommand != null) {
                QUEUE_SIZE.decrementAndGet(DefaultEndpoint.this);
            }

            if (sentCommands != null) {
                QUEUE_SIZE.addAndGet(DefaultEndpoint.this, -sentCommands.size());
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

        AtMostOnceWriteListener(RedisCommand<?, ?, ?> sentCommand) {
            super(sentCommand);
        }

        AtMostOnceWriteListener(Collection<? extends RedisCommand<?, ?, ?>> sentCommands) {
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

        RetryListener(RedisCommand<?, ?, ?> sentCommand) {
            super(sentCommand);
        }

        RetryListener(Collection<? extends RedisCommand<?, ?, ?>> sentCommands) {
            super(sentCommands);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void operationComplete(Future<Void> future) throws Exception {

            Throwable cause = future.cause();

            boolean success = future.isSuccess();
            dequeue();

            if (!success) {

                if (cause instanceof EncoderException || cause instanceof Error || cause.getCause() instanceof Error) {
                    complete(cause);
                    return;
                }

                Channel channel = DefaultEndpoint.this.channel;
                if (channel != null) {
                    channel.eventLoop().submit(this::requeueCommands);
                } else {
                    requeueCommands();
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
                try {
                    write((Collection) sentCommands);
                } catch (Exception e) {
                    complete(e);
                }
            }
        }
    }

    private enum Reliability {
        AT_MOST_ONCE, AT_LEAST_ONCE
    }

}
