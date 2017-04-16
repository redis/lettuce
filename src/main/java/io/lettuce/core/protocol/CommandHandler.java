/*
 * Copyright 2011-2016 the original author or authors.
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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceFactories;
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

    private final long commandHandlerId = COMMAND_HANDLER_COUNTER.incrementAndGet();
    private final Queue<RedisCommand<?, ?, ?>> queue = LettuceFactories.newConcurrentQueue();
    private final RedisStateMachine rsm = new RedisStateMachine();
    private final boolean traceEnabled = logger.isTraceEnabled();
    private final boolean debugEnabled = logger.isDebugEnabled();
    private final boolean latencyMetricsEnabled;
    private final BackpressureSource backpressureSource = new BackpressureSource();

    private final ClientResources clientResources;
    private final Endpoint endpoint;

    Channel channel;
    private ByteBuf buffer;
    private LifecycleState lifecycleState = LifecycleState.NOT_CONNECTED;
    private String logPrefix;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientResources client resources for this connection, must not be {@literal null}
     * @param endpoint
     */
    public CommandHandler(ClientResources clientResources, Endpoint endpoint) {

        LettuceAssert.notNull(clientResources, "ClientResources must not be null");
        LettuceAssert.notNull(endpoint, "RedisEndpoint must not be null");

        this.clientResources = clientResources;
        this.endpoint = endpoint;
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

        channel = ctx.channel();

        if (debugEnabled) {
            logPrefix = null;
            logger.debug("{} channelRegistered()", logPrefix());
        }

        setState(LifecycleState.REGISTERED);

        endpoint.registerQueue(this);

        buffer = ctx.alloc().directBuffer(8192 * 8);
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

        channel = null;
        buffer.release();

        endpoint.unregisterQueue(this);

        reset();

        setState(LifecycleState.CLOSED);
        rsm.close();

        ctx.fireChannelUnregistered();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof ConnectionEvents.Reset) {
            reset();
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        InternalLogLevel logLevel = InternalLogLevel.WARN;

        if (!queue.isEmpty()) {
            RedisCommand<?, ?, ?> command = queue.poll();
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

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        logPrefix = null;

        if (debugEnabled) {
            logger.debug("{} channelActive()", logPrefix());
        }

        setState(LifecycleState.CONNECTED);

        endpoint.notifyChannelActive(ctx.channel());

        super.channelActive(ctx);

        if (channel != null) {
            channel.eventLoop().submit(
                    (Runnable) () -> channel.pipeline().fireUserEventTriggered(new ConnectionEvents.Activated()));
        }

        if (debugEnabled) {
            logger.debug("{} channelActive() done", logPrefix());
        }
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

            RedisCommand<?, ?, ?> command = queue.peek();
            if (debugEnabled) {
                logger.debug("{} Queue contains: {} commands", logPrefix(), queue.size());
            }

            if (latencyMetricsEnabled && command instanceof WithLatency) {

                WithLatency withLatency = (WithLatency) command;
                if (withLatency.getFirstResponse() == -1) {
                    withLatency.firstResponse(nanoTime());
                }

                if (!decode(ctx, buffer, command)) {
                    return;
                }

                recordLatency(withLatency, command.getType());

            } else {

                if (!decode(ctx, buffer, command)) {
                    return;
                }

            }

            queue.poll();
            try {
                command.complete();
            } catch (Exception e) {
                logger.warn("{} Unexpected exception during request: {}", logPrefix, e.toString(), e);
            }

            if (buffer.refCnt() != 0) {
                buffer.discardReadBytes();
            }
        }
    }

    private boolean decode(ChannelHandlerContext ctx, ByteBuf buffer, RedisCommand<?, ?, ?> command) {

        if (!rsm.decode(buffer, command, command.getOutput())) {

            if (command instanceof DemandAware.Sink) {

                DemandAware.Sink sink = (DemandAware.Sink) command;
                sink.setSource(backpressureSource);

                if (!sink.hasDemand()) {
                    ctx.channel().config().setAutoRead(false);
                }
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

    private void writeSingleCommand(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command, ChannelPromise promise)
            throws Exception {

        if (command.isCancelled()) {
            return;
        }

        queueCommand(command, promise);
        ctx.write(command, promise);
    }

    private void writeBatch(ChannelHandlerContext ctx, Collection<RedisCommand<?, ?, ?>> batch, ChannelPromise promise)
            throws Exception {

        Collection<RedisCommand<?, ?, ?>> toWrite = batch;

        boolean cancelledCommands = false;
        for (RedisCommand<?, ?, ?> command : batch) {
            if (command.isCancelled()) {
                cancelledCommands = true;
                break;
            }
        }

        if (cancelledCommands) {

            toWrite = new ArrayList<>(batch.size());

            for (RedisCommand<?, ?, ?> command : batch) {

                if (command.isCancelled()) {
                    continue;
                }

                toWrite.add(command);
                queueCommand(command, promise);
            }
        } else {

            for (RedisCommand<?, ?, ?> command : toWrite) {
                queueCommand(command, promise);
            }
        }

        if (!toWrite.isEmpty()) {
            ctx.write(toWrite, promise);
        }
    }

    private void queueCommand(RedisCommand<?, ?, ?> command, ChannelPromise promise) throws Exception {

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

                        LatencyMeteredCommand<?, ?, ?> latencyMeteredCommand = new LatencyMeteredCommand<>(command);
                        latencyMeteredCommand.firstResponse(-1);
                        latencyMeteredCommand.sent(nanoTime());

                        queue.add(latencyMeteredCommand);
                    }
                } else {
                    queue.add(command);
                }
            }
        } catch (Exception e) {
            command.completeExceptionally(e);
            promise.setFailure(e);
            throw e;
        }
    }

    private long nanoTime() {
        return System.nanoTime();
    }

    protected void setState(LifecycleState lifecycleState) {

        if (this.lifecycleState != LifecycleState.CLOSED) {
            this.lifecycleState = lifecycleState;
        }
    }

    protected LifecycleState getState() {
        return lifecycleState;
    }

    public boolean isClosed() {
        return lifecycleState == LifecycleState.CLOSED;
    }

    private void reset() {

        rsm.reset();

        RedisCommand<?, ?, ?> cmd;
        while ((cmd = queue.poll()) != null) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError("Reset");
            }
            cmd.cancel();
        }

        if (buffer.refCnt() > 0) {
            buffer.clear();
        }
    }

    protected String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        StringBuilder buffer = new StringBuilder(64);
        buffer.append('[').append("chid=0x").append(Long.toHexString(commandHandlerId)).append(", ")
                .append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return logPrefix = buffer.toString();
    }

    @Override
    public Queue<RedisCommand<?, ?, ?>> getQueue() {
        return queue;
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

            if (isConnected() && !isClosed() && !channel.config().isAutoRead()) {
                channel.config().setAutoRead(true);
            }
        }
    }
}
