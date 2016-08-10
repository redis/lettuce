package com.lambdaworks.redis.protocol;

import static com.lambdaworks.redis.protocol.CommandHandler.SUPPRESS_IO_EXCEPTION_MESSAGES;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceFactories;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
public class DefaultEndpoint implements RedisChannelWriter, Endpoint, HasQueuedCommands {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEndpoint.class);
    private static final AtomicLong ENDPOINT_COUNTER = new AtomicLong();

    private final long endpointId = ENDPOINT_COUNTER.incrementAndGet();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Deque<RedisCommand<?, ?, ?>> commandBuffer = LettuceFactories.newConcurrentQueue();

    private final SharedLock sharedLock = new SharedLock();

    private final Reliability reliability;
    private final ClientOptions clientOptions;

    private final QueuedCommands queuedCommands = new QueuedCommands();

    // If TRACE level logging has been enabled at startup.
    private final boolean traceEnabled;

    // If DEBUG level logging has been enabled at startup.
    private final boolean debugEnabled;

    protected volatile Channel channel;
    private String logPrefix;
    private boolean autoFlushCommands = true;

    private ConnectionWatchdog connectionWatchdog;
    private ConnectionFacade connectionFacade;

    private Throwable connectionError;

    /**
     * Create a new {@link DefaultEndpoint}.
     * 
     * @param clientOptions client options for this connection, must not be {@literal null}
     */
    public DefaultEndpoint(ClientOptions clientOptions) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");

        this.clientOptions = clientOptions;
        this.traceEnabled = logger.isTraceEnabled();
        this.debugEnabled = logger.isDebugEnabled();
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
        this.queuedCommands.register(this);
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        LettuceAssert.notNull(command, "Command must not be null");

        try {
            sharedLock.incrementWriters();

            if (isClosed()) {
                throw new RedisException("Connection is closed");
            }

            if (queuedCommands.exceedsLimit(clientOptions.getRequestQueueSize())) {
                throw new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if ((channel == null || !isConnected()) && isRejectCommand()) {
                throw new RedisException("Currently not connected. Commands are rejected.");
            }

            if (autoFlushCommands) {

                if (isConnected()) {
                    writeToChannel(command);
                } else {
                    writeToBuffer(command);
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

    private <C extends RedisCommand<?, ?, T>, T> void writeToBuffer(C command) {

        if (commandBuffer.contains(command)) {
            return;
        }

        bufferCommand(command);
    }

    private <C extends RedisCommand<?, ?, T>, T> void writeToChannel(C command) {

        if (reliability == Reliability.AT_MOST_ONCE) {
            // cancel on exceptions and remove from queue, because there is no housekeeping
            writeAndFlush(command).addListener(new AtMostOnceWriteListener(command, queuedCommands));
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {
            // commands are ok to stay within the queue, reconnect will retrigger them
            writeAndFlush(command).addListener(new RetryListener(command));
        }
    }

    protected void bufferCommand(RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() buffering command {}", logPrefix(), command);
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
    public void registerQueue(HasQueuedCommands queueHolder) {
        queuedCommands.register(queueHolder);
    }

    @Override
    public void unregisterQueue(HasQueuedCommands queueHolder) {
        queuedCommands.unregister(queueHolder);
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
                            commandBuffer.size());
                }

                if (debugEnabled) {
                    logger.debug("{} activating endpoint", logPrefix());
                }

                connectionFacade.activated();

                flushCommands();
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

        if (!isConnected()) {
            connectionError = t;
        }
    }

    @Override
    public void registerConnectionWatchdog(Optional<ConnectionWatchdog> connectionWatchdog) {
        this.connectionWatchdog = connectionWatchdog.orElse(null);
    }

    @Override
    public Queue<RedisCommand<?, ?, ?>> getQueue() {
        return commandBuffer;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void flushCommands() {

        if (debugEnabled) {
            logger.debug("{} flushCommands()", logPrefix());
        }

        if (isConnected()) {

            List<RedisCommand<?, ?, ?>> commands = sharedLock.doExclusive(() -> {

                if (commandBuffer.isEmpty()) {
                    return Collections.<RedisCommand<?, ?, ?>> emptyList();
                }

                return drainCommands(commandBuffer);
            });

            if (debugEnabled) {
                logger.debug("{} flushCommands() Flushing {} commands", logPrefix(), commands.size());
            }

            if (!commands.isEmpty()) {

                if (reliability == Reliability.AT_MOST_ONCE) {
                    // cancel on exceptions and remove from queue, because there is no housekeeping
                    writeAndFlush(commands).addListener(new AtMostOnceWriteListener(commands, queuedCommands));
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

    @Override
    public void setConnectionFacade(ConnectionFacade connectionFacade) {
        this.connectionFacade = connectionFacade;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        this.autoFlushCommands = autoFlush;
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
            cancelCommands("Connection closed", queuedCommands.getQueue());
            return;
        }

        sharedLock.doExclusive(() -> {

            List<RedisCommand<?, ?, ?>> commands = drainCommands(queuedCommands.getQueue());
            Collections.reverse(commands);

            logger.debug("{} notifyQueuedCommands {} command(s) added to buffer", logPrefix(), commands.size());

            for (RedisCommand<?, ?, ?> command : commands) {
                if (!commandBuffer.contains(command)) {
                    commandBuffer.addFirst(command);
                }
            }

            if (isConnected()) {
                flushCommands();
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

    private ChannelFuture writeAndFlush(List<? extends RedisCommand<?, ?, ?>> commands) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush commands {}", logPrefix(), commands);
        }

        return channel.writeAndFlush(commands);
    }

    private ChannelFuture writeAndFlush(RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush command {}", logPrefix(), command);
        }

        return channel.writeAndFlush(command);
    }

    private List<RedisCommand<?, ?, ?>> drainCommands(Queue<? extends RedisCommand<?, ?, ?>> source) {

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>(source.size());

        RedisCommand<?, ?, ?> cmd;
        while ((cmd = source.poll()) != null) {
            target.add(cmd);
        }

        return target;
    }

    private void cancelBufferedCommands(String message) {

        List<RedisCommand<?, ?, ?>> toCancel = sharedLock.doExclusive(queuedCommands::drainCommands);
        cancelCommands(message, toCancel);
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
        return channel != null && channel.isActive();
    }

    protected String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        StringBuffer buffer = new StringBuffer(64);
        buffer.append('[').append("epid=0x").append(Long.toHexString(endpointId)).append(", ")
                .append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return logPrefix = buffer.toString();
    }

    private static class AtMostOnceWriteListener implements ChannelFutureListener {

        private final Collection<RedisCommand<?, ?, ?>> sentCommands;
        private final RedisCommand<?, ?, ?> sentCommand;
        private final QueuedCommands queuedCommands;

        AtMostOnceWriteListener(RedisCommand<?, ?, ?> sentCommand, QueuedCommands queuedCommands) {
            this.sentCommand = sentCommand;
            this.sentCommands = null;
            this.queuedCommands = queuedCommands;
        }

        AtMostOnceWriteListener(Collection<RedisCommand<?, ?, ?>> sentCommands, QueuedCommands queuedCommands) {
            this.sentCommand = null;
            this.sentCommands = sentCommands;
            this.queuedCommands = queuedCommands;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            future.await();

            if (future.cause() != null) {

                if (sentCommand != null) {

                    sentCommand.completeExceptionally(future.cause());
                    queuedCommands.remove(sentCommand);
                }

                if (sentCommands != null) {

                    for (RedisCommand<?, ?, ?> sentCommand : sentCommands) {
                        sentCommand.completeExceptionally(future.cause());
                    }
                    queuedCommands.removeAll(sentCommands);
                }
            }
        }
    }

    /**
     * A generic future listener which retries unsuccessful writes.
     */
    private class RetryListener implements GenericFutureListener<Future<Void>> {

        private final Collection<RedisCommand<?, ?, ?>> sentCommands;
        private final RedisCommand<?, ?, ?> sentCommand;

        RetryListener(Collection<RedisCommand<?, ?, ?>> sentCommands) {
            this.sentCommands = sentCommands;
            this.sentCommand = null;
        }

        RetryListener(RedisCommand<?, ?, ?> sentCommand) {
            this.sentCommands = null;
            this.sentCommand = sentCommand;
        }

        @Override
        public void operationComplete(Future<Void> future) throws Exception {

            Throwable cause = future.cause();

            if (!future.isSuccess()) {
                if (sentCommand != null) {
                    if (!sentCommand.isCancelled() && !sentCommand.isDone()) {
                        write(sentCommand);
                    }
                }

                if (sentCommands != null) {
                    for (RedisCommand<?, ?, ?> command : sentCommands) {
                        if (!command.isCancelled() && !command.isDone()) {
                            write(command);
                        }
                    }
                }
            }

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

    private enum Reliability {
        AT_MOST_ONCE, AT_LEAST_ONCE;
    }

}
