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

import static io.lettuce.core.ConnectionEvents.Activated;
import static io.lettuce.core.ConnectionEvents.PingBeforeActivate;
import static io.lettuce.core.ConnectionEvents.Reset;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.resource.ClientResources;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for writing redis commands and reading responses from the server.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Jongyeol Choi
 */
public class CommandHandler extends ChannelDuplexHandler implements HasQueuedCommands {

    /**
     * When we encounter an unexpected IOException we look for these {@link Throwable#getMessage() messages} (because we have no
     * better way to distinguish) and log them at DEBUG rather than WARN, since they are generally caused by unclean client
     * disconnects rather than an actual problem.
     */
    static final Set<String> SUPPRESS_IO_EXCEPTION_MESSAGES = LettuceSets.unmodifiableSet("Connection reset by peer",
            "Broken pipe", "Connection timed out");

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandHandler.class);
    private static final AtomicLong COMMAND_HANDLER_COUNTER = new AtomicLong();

    private final ClientOptions clientOptions;
    private final ClientResources clientResources;
    private final Endpoint endpoint;

    private final Deque<RedisCommand<?, ?, ?>> stack = new ArrayDeque<>();
    private final long commandHandlerId = COMMAND_HANDLER_COUNTER.incrementAndGet();

    private final RedisStateMachine rsm = new RedisStateMachine();
    private final boolean traceEnabled = logger.isTraceEnabled();
    private final boolean debugEnabled = logger.isDebugEnabled();
    private final boolean latencyMetricsEnabled;
    private final boolean boundedQueues;
    private final BackpressureSource backpressureSource = new BackpressureSource();

    Channel channel;
    private ByteBuf buffer;
    private LifecycleState lifecycleState = LifecycleState.NOT_CONNECTED;
    private String logPrefix;
    private PristineFallbackCommand fallbackCommand;
    private boolean pristine;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection, must not be {@literal null}
     * @param clientResources client resources for this connection, must not be {@literal null}
     * @param endpoint must not be {@literal null}.
     */
    public CommandHandler(ClientOptions clientOptions, ClientResources clientResources, Endpoint endpoint) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");
        LettuceAssert.notNull(endpoint, "RedisEndpoint must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.endpoint = endpoint;
        this.latencyMetricsEnabled = clientResources.commandLatencyCollector().isEnabled();
        this.boundedQueues = clientOptions.getRequestQueueSize() != Integer.MAX_VALUE;
    }

    public Queue<RedisCommand<?, ?, ?>> getStack() {
        return stack;
    }

    protected void setState(LifecycleState lifecycleState) {

        if (this.lifecycleState != LifecycleState.CLOSED) {
            this.lifecycleState = lifecycleState;
        }
    }

    @Override
    public Collection<RedisCommand<?, ?, ?>> drainQueue() {
        return drainCommands(stack);
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

        channel = ctx.channel();

        if (debugEnabled) {
            logPrefix = null;
            logger.debug("{} channelRegistered()", logPrefix());
        }

        setState(LifecycleState.REGISTERED);

        buffer = ctx.alloc().directBuffer(8192 * 8);
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

        channel = null;
        buffer.release();

        reset();

        setState(LifecycleState.CLOSED);
        rsm.close();

        ctx.fireChannelUnregistered();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#userEventTriggered(io.netty.channel.ChannelHandlerContext, Object)
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt == EnableAutoRead.INSTANCE) {
            channel.config().setAutoRead(true);
        } else if (evt instanceof Reset) {
            reset();
        } else if (evt instanceof PingBeforeActivate) {

            PingBeforeActivate pba = (PingBeforeActivate) evt;

            stack.addFirst(pba.getCommand());
            ctx.writeAndFlush(pba.getCommand());
            return;
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        InternalLogLevel logLevel = InternalLogLevel.WARN;

        if (!stack.isEmpty()) {
            RedisCommand<?, ?, ?> command = stack.poll();
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
            endpoint.notifyException(cause);
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
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        logPrefix = null;
        pristine = true;
        fallbackCommand = null;

        if (debugEnabled) {
            logger.debug("{} channelActive()", logPrefix());
        }

        setState(LifecycleState.CONNECTED);

        endpoint.notifyChannelActive(ctx.channel());

        super.channelActive(ctx);

        if (channel != null) {
            channel.eventLoop().submit((Runnable) () -> channel.pipeline().fireUserEventTriggered(new Activated()));
        }

        if (debugEnabled) {
            logger.debug("{} channelActive() done", logPrefix());
        }
    }

    private static <T> List<T> drainCommands(Queue<T> source) {

        List<T> target = new ArrayList<>(source.size());

        T cmd;
        while ((cmd = source.poll()) != null) {
            target.add(cmd);
        }

        return target;
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

        setState(LifecycleState.DISCONNECTED);
        setState(LifecycleState.DEACTIVATING);

        endpoint.notifyChannelInactive(ctx.channel());
        endpoint.notifyDrainQueuedCommands(this);

        setState(LifecycleState.DEACTIVATED);

        PristineFallbackCommand command = this.fallbackCommand;
        if (isProtectedMode(command)) {
            onProtectedMode(command.getOutput().getError());
        }

        rsm.reset();

        if (debugEnabled) {
            logger.debug("{} channelInactive() done", logPrefix());
        }

        super.channelInactive(ctx);
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
            writeSingleCommand(ctx, (RedisCommand<?, ?, ?>) msg, promise);
            return;
        }

        if (msg instanceof Collection) {
            writeBatch(ctx, (Collection<RedisCommand<?, ?, ?>>) msg, promise);
        }
    }

    private void writeSingleCommand(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command, ChannelPromise promise)
            throws Exception {

        if (!isWriteable(command)) {
            return;
        }

        addToStack(command, promise);
        ctx.write(command, promise);
    }

    private void writeBatch(ChannelHandlerContext ctx, Collection<RedisCommand<?, ?, ?>> batch, ChannelPromise promise)
            throws Exception {

        Collection<RedisCommand<?, ?, ?>> toWrite = batch;
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

            for (RedisCommand<?, ?, ?> command : batch) {

                if (!isWriteable(command)) {
                    continue;
                }

                toWrite.add(command);
            }
        }

        for (RedisCommand<?, ?, ?> command : toWrite) {
            addToStack(command, promise);
        }

        if (!toWrite.isEmpty()) {
            ctx.write(toWrite, promise);
        }
    }

    private void addToStack(RedisCommand<?, ?, ?> command, ChannelPromise promise) throws Exception {

        try {

            validateWrite(1);

            if (command.getOutput() == null) {
                // fire&forget commands are excluded from metrics
                command.complete();
            } else {

                if (stack.contains(command)) {
                    throw new RedisException("Attempting to write duplicate command that is already enqueued: " + command);
                }
            }

            RedisCommand<?, ?, ?> redisCommand = potentiallyWrapLatencyCommand(command);

            if (promise.isVoid()) {
                stack.add(redisCommand);
            } else {
                promise.addListener(future -> {
                    if (future.isSuccess()) {
                        stack.add(redisCommand);
                    }
                });
            }
        } catch (Exception e) {
            command.completeExceptionally(e);
            promise.setFailure(e);
            throw e;
        }
    }

    private void validateWrite(int commands) {

        if (usesBoundedQueues()) {

            // number of maintenance commands (AUTH, CLIENT SETNAME, SELECT, READONLY) should be allowed on top
            // of number of user commands to ensure the driver recovers properly from a disconnect
            int maxMaintenanceCommands = 5;
            int allowedRequestQueueSize = clientOptions.getRequestQueueSize() + maxMaintenanceCommands;
            if (stack.size() + commands > allowedRequestQueueSize)

                throw new RedisException("Internal stack size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the stack size drops.");
        }
    }

    private boolean usesBoundedQueues() {
        return boundedQueues;
    }

    private static boolean isWriteable(RedisCommand<?, ?, ?> command) {
        return !command.isDone();
    }

    private RedisCommand<?, ?, ?> potentiallyWrapLatencyCommand(RedisCommand<?, ?, ?> command) {

        if (!latencyMetricsEnabled) {
            return command;
        }

        if (command instanceof WithLatency) {

            WithLatency withLatency = (WithLatency) command;

            withLatency.firstResponse(-1);
            withLatency.sent(nanoTime());

            return command;
        }

        LatencyMeteredCommand<?, ?, ?> latencyMeteredCommand = new LatencyMeteredCommand<>(command);
        latencyMeteredCommand.firstResponse(-1);
        latencyMeteredCommand.sent(nanoTime());

        return latencyMeteredCommand;
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

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {

        if (pristine && stack.isEmpty() && buffer.isReadable()) {

            if (debugEnabled) {
                logger.debug("{} Received response without a command context (empty stack)", logPrefix());
            }

            if (consumeResponse(buffer)) {
                pristine = false;
            }

            return;
        }

        while (canDecode(buffer)) {

            RedisCommand<?, ?, ?> command = stack.peek();
            if (debugEnabled) {
                logger.debug("{} Stack contains: {} commands", logPrefix(), stack.size());
            }

            pristine = false;

            try {
                if (!decode(ctx, buffer, command)) {
                    return;
                }
            } catch (Exception e) {

                ctx.close();
                throw e;
            }

            if (isProtectedMode(command)) {
                onProtectedMode(command.getOutput().getError());
            } else {

                stack.poll();

                try {
                    command.complete();
                } catch (Exception e) {
                    logger.warn("{} Unexpected exception during request: {}", logPrefix, e.toString(), e);
                }
            }

            if (buffer.refCnt() != 0) {
                buffer.discardReadBytes();
            }

            afterComplete(ctx, command);
        }
    }

    protected boolean canDecode(ByteBuf buffer) {
        return !stack.isEmpty() && buffer.isReadable();
    }

    private boolean decode(ChannelHandlerContext ctx, ByteBuf buffer, RedisCommand<?, ?, ?> command) {

        if (latencyMetricsEnabled && command instanceof WithLatency) {

            WithLatency withLatency = (WithLatency) command;
            if (withLatency.getFirstResponse() == -1) {
                withLatency.firstResponse(nanoTime());
            }

            if (!decode0(ctx, buffer, command)) {
                return false;
            }

            recordLatency(withLatency, command.getType());

            return true;
        }

        return decode0(ctx, buffer, command);
    }

    private boolean decode0(ChannelHandlerContext ctx, ByteBuf buffer, RedisCommand<?, ?, ?> command) {

        if (!decode(buffer, command, command.getOutput())) {

            if (command instanceof DemandAware.Sink) {

                DemandAware.Sink sink = (DemandAware.Sink) command;
                sink.setSource(backpressureSource);

                ctx.channel().config().setAutoRead(sink.hasDemand());
            }

            return false;
        }

        if (!ctx.channel().config().isAutoRead()) {
            ctx.channel().config().setAutoRead(true);
        }

        return true;
    }

    protected boolean decode(ByteBuf buffer, CommandOutput<?, ?, ?> output) {
        return rsm.decode(buffer, output);
    }

    protected boolean decode(ByteBuf buffer, RedisCommand<?, ?, ?> command, CommandOutput<?, ?, ?> output) {
        return rsm.decode(buffer, command, output);
    }

    /**
     * Consume a response without having a command on the stack.
     *
     * @param buffer
     * @return {@literal true} if the buffer decode was successful. {@literal false} if the buffer was not decoded.
     */
    private boolean consumeResponse(ByteBuf buffer) {

        PristineFallbackCommand command = this.fallbackCommand;

        if (command == null || !command.isDone()) {

            if (debugEnabled) {
                logger.debug("{} Consuming response using FallbackCommand", logPrefix());
            }

            if (command == null) {
                command = new PristineFallbackCommand();
                this.fallbackCommand = command;
            }

            if (!decode(buffer, command.getOutput())) {
                return false;
            }

            if (isProtectedMode(command)) {
                onProtectedMode(command.getOutput().getError());
            }
        }

        return true;
    }

    private boolean isProtectedMode(RedisCommand<?, ?, ?> command) {
        return command != null && command.getOutput() != null && command.getOutput().hasError()
                && RedisConnectionException.isProtectedMode(command.getOutput().getError());
    }

    private void onProtectedMode(String message) {

        RedisConnectionException exception = new RedisConnectionException(message);

        endpoint.notifyException(exception);

        if (channel != null) {
            channel.disconnect();
        }

        stack.forEach(cmd -> cmd.completeExceptionally(exception));
        stack.clear();
    }

    /**
     * Hook method called after command completion.
     *
     * @param ctx
     * @param command
     */
    protected void afterComplete(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command) {
    }

    private void recordLatency(WithLatency withLatency, ProtocolKeyword commandType) {

        if (withLatency != null && clientResources.commandLatencyCollector().isEnabled() && channel != null && remote() != null) {

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

    boolean isConnected() {
        return lifecycleState.ordinal() >= LifecycleState.CONNECTED.ordinal()
                && lifecycleState.ordinal() < LifecycleState.DISCONNECTED.ordinal();
    }

    private void reset() {

        resetInternals();
        cancelCommands("Reset", drainCommands(stack));
    }

    private void resetInternals() {

        rsm.reset();

        if (buffer.refCnt() > 0) {
            buffer.clear();
        }
    }

    private static void cancelCommands(String message, List<RedisCommand<?, ?, ?>> toCancel) {

        for (RedisCommand<?, ?, ?> cmd : toCancel) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError(message);
            }
            cmd.cancel();
        }
    }

    private String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        String buffer = "[" + ChannelLogDescriptor.logDescriptor(channel) + ", " + "chid=0x"
                + Long.toHexString(commandHandlerId) + ']';
        return logPrefix = buffer;
    }

    private static long nanoTime() {
        return System.nanoTime();
    }

    public enum LifecycleState {
        NOT_CONNECTED, REGISTERED, CONNECTED, ACTIVATING, ACTIVE, DISCONNECTED, DEACTIVATING, DEACTIVATED, CLOSED,
    }

    /**
     * Source for backpressure.
     */
    class BackpressureSource implements DemandAware.Source {

        @Override
        public void requestMore() {

            if (isConnected() && !isClosed()) {
                if (!channel.config().isAutoRead()) {
                    channel.pipeline().fireUserEventTriggered(EnableAutoRead.INSTANCE);
                }
            }
        }
    }

    enum EnableAutoRead {
        INSTANCE
    }
}
