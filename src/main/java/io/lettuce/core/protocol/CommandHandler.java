/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.core.ConnectionEvents.*;
import static io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionBuilder;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.datastructure.queue.HashIndexedQueue;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.metrics.EndpointQueueMonitor;
import io.lettuce.core.metrics.EndpointQueueMonitor.QueueId;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.PushOutput;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.TraceContext;
import io.lettuce.core.tracing.TraceContextProvider;
import io.lettuce.core.tracing.Tracer;
import io.lettuce.core.tracing.Tracing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.local.LocalAddress;
import io.netty.util.Recycler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for writing redis commands and reading responses from the server.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Jongyeol Choi
 * @author Grzegorz Szpak
 * @author Daniel Albuquerque
 * @author Gavin Cook
 * @author Anuraag Agrawal
 */
public class CommandHandler extends ChannelDuplexHandler implements HasQueuedCommands {

    /**
     * When we encounter an unexpected {@link IOException} we look for these {@link Throwable#getMessage() messages} (because we
     * have no better way to distinguish) and log them at DEBUG rather than WARN, since they are generally caused by unclean
     * client disconnects rather than an actual problem.
     */
    static final Set<String> SUPPRESS_IO_EXCEPTION_MESSAGES = LettuceSets.unmodifiableSet("Connection reset by peer",
            "Broken pipe", "Connection timed out");

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandHandler.class);

    private static final AtomicLong COMMAND_HANDLER_COUNTER = new AtomicLong();

    private final ClientOptions clientOptions;

    private final ClientResources clientResources;

    private final Endpoint endpoint;

    private final Queue<RedisCommand<?, ?, ?>> stack;

    private final long commandHandlerId = COMMAND_HANDLER_COUNTER.incrementAndGet();

    private final boolean traceEnabled = logger.isTraceEnabled();

    private final boolean debugEnabled = logger.isDebugEnabled();

    private final CommandLatencyRecorder commandLatencyRecorder;

    private final boolean latencyMetricsEnabled;

    private final boolean tracingEnabled;

    private final DecodeBufferPolicy decodeBufferPolicy;

    private final boolean boundedQueues;

    private final BackpressureSource backpressureSource = new BackpressureSource();

    private RedisStateMachine rsm;

    private Channel channel;

    private ByteBuf readBuffer;

    private boolean hasDecodeProgress;

    private PushOutput<ByteBuffer, ByteBuffer> pushOutput;

    private LifecycleState lifecycleState = LifecycleState.NOT_CONNECTED;

    private String logPrefix;

    private PristineFallbackCommand fallbackCommand;

    private boolean pristine;

    private Tracing.Endpoint tracedEndpoint;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection, must not be {@code null}
     * @param clientResources client resources for this connection, must not be {@code null}
     * @param endpoint must not be {@code null}.
     */
    public CommandHandler(ClientOptions clientOptions, ClientResources clientResources, Endpoint endpoint) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");
        LettuceAssert.notNull(endpoint, "RedisEndpoint must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.endpoint = endpoint;
        this.commandLatencyRecorder = clientResources.commandLatencyRecorder();
        this.latencyMetricsEnabled = commandLatencyRecorder.isEnabled();
        this.boundedQueues = clientOptions.getRequestQueueSize() != Integer.MAX_VALUE;
        this.stack = clientOptions.isUseHashIndexedQueue() ? new HashIndexedQueue<>() : new ArrayDeque<>();

        Tracing tracing = clientResources.tracing();

        this.tracingEnabled = tracing.isEnabled();

        this.decodeBufferPolicy = clientOptions.getDecodeBufferPolicy();

        EndpointQueueMonitor endpointQueueMonitor = clientResources.endpointQueueMonitor();
        if (endpointQueueMonitor != null) {
            endpointQueueMonitor.observeQueueSize(QueueId.create("lettuce.command.handler.queue", endpoint.getId()),
                    stack::size);
        }
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Queue<RedisCommand<?, ?, ?>> getStack() {
        return stack;
    }

    protected void setState(LifecycleState lifecycleState) {

        if (this.lifecycleState != LifecycleState.CLOSED) {
            this.lifecycleState = lifecycleState;
        }
    }

    void setReadBuffer(ByteBuf readBuffer) {
        this.readBuffer = readBuffer;
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

        logPrefix = null;
        pristine = true;
        fallbackCommand = null;

        setState(LifecycleState.REGISTERED);

        readBuffer = ctx.alloc().buffer(8192 * 8);
        rsm = new RedisStateMachine();
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

        if (readBuffer != null) {
            readBuffer.release();
        }

        if (rsm != null) {
            rsm.close();
        }
        rsm = null;

        reset();

        setState(LifecycleState.CLOSED);

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
                logger.warn("{} Unexpected exception during command completion exceptionally: {}", logPrefix, ex.toString(),
                        ex);
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

        if (debugEnabled) {
            logger.debug("{} channelActive()", logPrefix());
        }

        setState(LifecycleState.CONNECTED);

        tracedEndpoint = clientResources.tracing().createEndpoint(ctx.channel().remoteAddress());

        endpoint.notifyChannelActive(ctx.channel());
        super.channelActive(ctx);

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

        tracedEndpoint = null;
        setState(LifecycleState.DISCONNECTED);
        setState(LifecycleState.DEACTIVATING);

        endpoint.notifyChannelInactive(ctx.channel());
        endpoint.notifyDrainQueuedCommands(this);

        setState(LifecycleState.DEACTIVATED);

        PristineFallbackCommand command = this.fallbackCommand;
        if (isProtectedMode(command)) {
            onProtectedMode(command.getOutput().getError());
        }

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

        if (msg instanceof List) {

            List<RedisCommand<?, ?, ?>> batch = (List<RedisCommand<?, ?, ?>>) msg;

            if (batch.size() == 1) {

                writeSingleCommand(ctx, batch.get(0), promise);
                return;
            }

            writeBatch(ctx, batch, promise);
            return;
        }

        if (msg instanceof Collection) {
            writeBatch(ctx, (Collection<RedisCommand<?, ?, ?>>) msg, promise);
        }
    }

    private void writeSingleCommand(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command, ChannelPromise promise) {

        if (!isWriteable(command)) {
            promise.trySuccess();
            return;
        }

        addToStack(command, promise);

        attachTracing(ctx, command);

        ctx.write(command, promise);
    }

    private void writeBatch(ChannelHandlerContext ctx, Collection<RedisCommand<?, ?, ?>> batch, ChannelPromise promise) {

        Collection<RedisCommand<?, ?, ?>> deduplicated = new LinkedHashSet<>(batch.size(), 1);

        for (RedisCommand<?, ?, ?> command : batch) {

            if (isWriteable(command) && !deduplicated.add(command)) {
                deduplicated.remove(command);
                command.completeExceptionally(
                        new RedisException("Attempting to write duplicate command that is already enqueued: " + command));
            }
        }

        try {
            validateWrite(deduplicated.size());
        } catch (Exception e) {

            for (RedisCommand<?, ?, ?> redisCommand : deduplicated) {
                redisCommand.completeExceptionally(e);
            }

            throw e;
        }

        for (RedisCommand<?, ?, ?> command : deduplicated) {
            attachTracing(ctx, command);
            addToStack(command, promise);
        }

        if (!deduplicated.isEmpty()) {
            ctx.write(deduplicated, promise);
        } else {
            promise.trySuccess();
        }
    }

    private void attachTracing(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command) {

        if (!tracingEnabled || !(command instanceof CompleteableCommand)) {
            return;
        }

        TracedCommand<?, ?, ?> traced = CommandWrapper.unwrap(command, TracedCommand.class);
        TraceContextProvider provider = (traced == null ? clientResources.tracing().initialTraceContextProvider() : traced);
        Tracer tracer = clientResources.tracing().getTracerProvider().getTracer();

        if (provider != null) {

            TraceContext context = provider.getTraceContext();

            Tracer.Span span = tracer.nextSpan(context);
            span.name(command.getType().toString());

            if (channel.hasAttr(ConnectionBuilder.REDIS_URI)) {
                String redisUriStr = channel.attr(ConnectionBuilder.REDIS_URI).get();
                RedisURI redisURI = RedisURI.create(redisUriStr);
                span.tag("server.address", redisURI.toString());
                span.tag("db.namespace", String.valueOf(redisURI.getDatabase()));
                span.tag("user.name", Optional.ofNullable(redisURI.getCredentialsProvider().resolveCredentials().block())
                        .map(RedisCredentials::getUsername).orElse(""));
            }

            if (tracedEndpoint != null) {
                span.remoteEndpoint(tracedEndpoint);
            } else {
                span.remoteEndpoint(clientResources.tracing().createEndpoint(ctx.channel().remoteAddress()));
            }

            span.start(command);

            if (traced != null) {
                traced.setSpan(span);
            }
        }
    }

    private void addToStack(RedisCommand<?, ?, ?> command, ChannelPromise promise) {

        try {

            if (!ActivationCommand.isActivationCommand(command)) {
                validateWrite(1);
            }

            if (command.getOutput() == null) {
                // fire&forget commands are excluded from metrics and replies
                complete(command);
            }

            RedisCommand<?, ?, ?> redisCommand = potentiallyWrapLatencyCommand(command);

            stack.add(redisCommand);
            if (!promise.isVoid()) {
                promise.addListener(AddToStack.newInstance(stack, redisCommand));
            }
        } catch (Exception e) {
            command.completeExceptionally(e);
            throw e;
        }
    }

    private void validateWrite(int commands) {

        if (usesBoundedQueues()) {

            // number of maintenance commands (AUTH, CLIENT SETNAME, SELECT, READONLY) should be allowed on top
            // of number of user commands to ensure the driver recovers properly from a disconnect

            if (stack.size() + commands > clientOptions.getRequestQueueSize())

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
        input.touch("CommandHandler.read(…)");

        if (!input.isReadable() || input.refCnt() == 0) {
            logger.warn("{} Input not readable {}, {}", logPrefix(), input.isReadable(), input.refCnt());
            return;
        }

        if (debugEnabled) {
            logger.debug("{} Received: {} bytes, {} commands in the stack", logPrefix(), input.readableBytes(), stack.size());
        }

        try {
            if (readBuffer == null || readBuffer.refCnt() < 1) {
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

            readBuffer.touch("CommandHandler.read(…)");
            readBuffer.writeBytes(input);

            decode(ctx, readBuffer);
        } finally {
            input.release();
        }
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {
        final boolean rebindInProgress = ctx.channel().hasAttr(REBIND_ATTRIBUTE)
                && ctx.channel().attr(REBIND_ATTRIBUTE).get() != null
                && ctx.channel().attr(REBIND_ATTRIBUTE).get().equals(RebindState.STARTED);
        if (debugEnabled && rebindInProgress) {
            logger.debug("{} Processing command while rebind is in progress, stack has {} more to process", logPrefix(),
                    stack.size());
        }

        if (pristine) {

            if (stack.isEmpty() && buffer.isReadable() && !isPushDecode(buffer)) {

                if (debugEnabled) {
                    logger.debug("{} Received response without a command context (empty stack)", logPrefix());
                }

                if (consumeResponse(buffer)) {
                    pristine = false;
                }

                return;
            }
        }

        while (canDecode(buffer)) {

            if (isPushDecode(buffer)) {

                if (pushOutput == null) {
                    pushOutput = new PushOutput<>(ByteBufferCopyCodec.INSTANCE);
                }

                try {
                    if (!decode(ctx, buffer, pushOutput)) {
                        hasDecodeProgress = true;
                        decodeBufferPolicy.afterPartialDecode(buffer);
                        return;
                    }

                } catch (Exception e) {

                    ctx.close();
                    throw e;
                }

                hasDecodeProgress = false;
                PushOutput<ByteBuffer, ByteBuffer> output = pushOutput;
                pushOutput = null;
                notifyPushListeners(output);
            } else {

                RedisCommand<?, ?, ?> command = stack.peek();
                if (debugEnabled) {
                    logger.debug("{} Stack contains: {} commands", logPrefix(), stack.size());
                }

                pristine = false;

                try {

                    if (!decode(ctx, buffer, command)) {
                        hasDecodeProgress = true;
                        decodeBufferPolicy.afterPartialDecode(buffer);
                        return;
                    }
                } catch (Exception e) {

                    ctx.close();
                    throw e;
                }

                hasDecodeProgress = false;
                if (isProtectedMode(command)) {
                    onProtectedMode(command.getOutput().getError());
                } else {

                    if (canComplete(command)) {
                        stack.poll();

                        try {
                            if (debugEnabled) {
                                logger.debug("{} Completing command {}", logPrefix(), command);
                            }
                            complete(command);
                        } catch (Exception e) {
                            logger.warn("{} Unexpected exception during request: {}", logPrefix, e.toString(), e);
                        }
                    }
                }
                afterDecode(ctx, command);
            }
        }

        if (rebindInProgress && stack.isEmpty()) {
            logger.info("{} Rebind completed at {}", logPrefix(), LocalTime.now());
            ctx.channel().attr(REBIND_ATTRIBUTE).set(RebindState.COMPLETED);
        }

        decodeBufferPolicy.afterDecoding(buffer);
    }

    protected void notifyPushListeners(PushMessage notification) {

        Collection<PushListener> pushListeners = endpoint.getPushListeners();

        try {
            pushListeners.forEach(pushListener -> {
                pushListener.onPushMessage(notification);
            });
        } catch (Exception e) {
            logger.warn("PushListener.onPushMessage failed with " + e.toString(), e);
        }
    }

    /**
     * Decoding hook: Can the buffer be decoded to a command.
     *
     * @param buffer
     * @return
     */
    protected boolean canDecode(ByteBuf buffer) {
        return buffer.isReadable() && (isMessageDecode() || isPushDecode(buffer));
    }

    private boolean isPushMessage(ByteBuf buffer) {
        return buffer.getByte(buffer.readerIndex()) == RedisStateMachine.State.Type.PUSH.marker;
    }

    protected boolean isPushDecode(ByteBuf buffer) {
        return (!hasDecodeProgress && isPushMessage(buffer)) || pushOutput != null;
    }

    private boolean isMessageDecode() {
        return !stack.isEmpty();
    }

    /**
     * Decoding hook: Can the command be completed.
     *
     * @param command
     * @return
     */
    protected boolean canComplete(RedisCommand<?, ?, ?> command) {
        return true;
    }

    /**
     * Decoding hook: Complete a command.
     *
     * @param command
     * @see RedisCommand#complete()
     */
    protected void complete(RedisCommand<?, ?, ?> command) {
        command.complete();
    }

    /**
     * Decode a command.
     *
     * @param ctx
     * @param buffer
     * @param command
     * @return
     */
    private boolean decode(ChannelHandlerContext ctx, ByteBuf buffer, RedisCommand<?, ?, ?> command) {

        if (latencyMetricsEnabled && command instanceof WithLatency) {

            WithLatency withLatency = (WithLatency) command;
            if (withLatency.getFirstResponse() == -1) {
                withLatency.firstResponse(nanoTime());
            }

            if (!decode0(ctx, buffer, command)) {
                return false;
            }

            recordLatency(withLatency, command);

            return true;
        }

        return decode0(ctx, buffer, command);
    }

    /**
     * Decode to a {@link CommandOutput}.
     *
     * @param ctx
     * @param buffer
     * @param output
     * @return
     */
    private boolean decode(ChannelHandlerContext ctx, ByteBuf buffer, CommandOutput<?, ?, ?> output) {
        return decode0(ctx, buffer, output);
    }

    private boolean decode0(ChannelHandlerContext ctx, ByteBuf buffer, RedisCommand<?, ?, ?> command) {

        if (!decode(buffer, command, getCommandOutput(command))) {

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

    private boolean decode0(ChannelHandlerContext ctx, ByteBuf buffer, CommandOutput<?, ?, ?> pushOutput) {

        if (rsm != null && !rsm.decode(buffer, pushOutput, ctx::fireExceptionCaught)) {
            return false;
        }

        if (!ctx.channel().config().isAutoRead()) {
            ctx.channel().config().setAutoRead(true);
        }

        return true;
    }

    /**
     * Decoding hook: Retrieve {@link CommandOutput} for {@link RedisCommand} decoding.
     *
     * @param command
     * @return
     * @see RedisCommand#getOutput()
     */
    protected CommandOutput<?, ?, ?> getCommandOutput(RedisCommand<?, ?, ?> command) {
        return command.getOutput();
    }

    protected boolean decode(ByteBuf buffer, CommandOutput<?, ?, ?> output) {
        return rsm != null && rsm.decode(buffer, output);
    }

    protected boolean decode(ByteBuf buffer, RedisCommand<?, ?, ?> command, CommandOutput<?, ?, ?> output) {
        return rsm != null && rsm.decode(buffer, output, command::completeExceptionally);
    }

    /**
     * Consume a response without having a command on the stack.
     *
     * @param buffer
     * @return {@code true} if the buffer decode was successful. {@code false} if the buffer was not decoded.
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

        stack.forEach(cmd -> cmd.completeExceptionally(exception));
        stack.clear();

        if (channel != null) {
            channel.disconnect();
        }
    }

    /**
     * Hook method called after command completion.
     *
     * @param ctx
     * @param command
     */
    protected void afterDecode(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command) {
        decodeBufferPolicy.afterCommandDecoded(readBuffer);
    }

    private void recordLatency(WithLatency withLatency, RedisCommand<?, ?, ?> command) {

        if (withLatency != null && latencyMetricsEnabled && channel != null && remote() != null) {

            long firstResponseLatency = withLatency.getFirstResponse() - withLatency.getSent();
            long completionLatency = nanoTime() - withLatency.getSent();

            commandLatencyRecorder.recordCommandLatency(local(), remote(), command, firstResponseLatency, completionLatency);
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

        if (rsm != null) {
            rsm.reset();
        }

        if (readBuffer != null && readBuffer.refCnt() > 0) {
            readBuffer.clear();
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

    /**
     * @return the channel Id.
     * @since 6.1
     */
    public String getChannelId() {
        return channel == null ? "unknown" : ChannelLogDescriptor.getId(channel);
    }

    private String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        String buffer = "[" + ChannelLogDescriptor.logDescriptor(channel) + ", epid=" + endpoint.getId() + ", chid=0x"
                + getCommandHandlerId() + ']';
        return logPrefix = buffer;
    }

    private String getCommandHandlerId() {
        return Long.toHexString(commandHandlerId);
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

    /**
     * Add to stack listener. This listener is pooled and must be {@link #recycle() recycled after usage}.
     */
    static class AddToStack implements GenericFutureListener<Future<Void>> {

        private static final Recycler<AddToStack> RECYCLER = new Recycler<AddToStack>() {

            @Override
            protected AddToStack newObject(Handle<AddToStack> handle) {
                return new AddToStack(handle);
            }

        };

        private final Recycler.Handle<AddToStack> handle;

        private Queue<RedisCommand<?, ?, ?>> stack;

        private RedisCommand<?, ?, ?> command;

        AddToStack(Recycler.Handle<AddToStack> handle) {
            this.handle = handle;
        }

        /**
         * Allocate a new instance.
         *
         * @param stack
         * @param command
         * @return
         */
        @SuppressWarnings("unchecked")
        static AddToStack newInstance(Queue<RedisCommand<?, ?, ?>> stack, RedisCommand<?, ?, ?> command) {

            AddToStack entry = RECYCLER.get();

            entry.stack = stack;
            entry.command = command;

            return entry;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void operationComplete(Future<Void> future) {

            try {
                if (!future.isSuccess()) {
                    stack.remove(command);
                }
            } finally {
                recycle();
            }
        }

        private void recycle() {

            this.stack = null;
            this.command = null;

            handle.recycle(this);
        }

    }

}
