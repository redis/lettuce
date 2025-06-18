/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.resource.Delay;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An extension to {@link ConnectionWatchdog} that supports re-bind events.
 *
 * @author Tihomir Mateev
 * @since 6.7
 * @see ClientOptions#isProactiveRebindEnabled()
 */
@ChannelHandler.Sharable
public class RebindAwareConnectionWatchdog extends ConnectionWatchdog implements PushListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RebindAwareConnectionWatchdog.class);

    private static final String REBIND_MESSAGE_TYPE = "MOVING";

    private static final String MIGRATING_MESSAGE_TYPE = "MIGRATING";

    private static final String MIGRATED_MESSAGE_TYPE = "MIGRATED";

    private static final int REBIND_ADDRESS_INDEX = 2;

    private static final int MIGRATED_DELAY_RELAX_TIMEOUT_INDEX = 1;

    public static final AttributeKey<RebindState> REBIND_ATTRIBUTE = AttributeKey.newInstance("rebindAddress");

    private Channel channel;

    private final Set<RebindAwareComponent> componentListeners = new HashSet<>();

    public RebindAwareConnectionWatchdog(Delay reconnectDelay, ClientOptions clientOptions, Bootstrap bootstrap, Timer timer,
            EventExecutorGroup reconnectWorkers, Mono<SocketAddress> socketAddressSupplier,
            ReconnectionListener reconnectionListener, ConnectionFacade connectionFacade, EventBus eventBus,
            Endpoint endpoint) {

        super(reconnectDelay, clientOptions, bootstrap, timer, reconnectWorkers, socketAddressSupplier, reconnectionListener,
                connectionFacade, eventBus, endpoint);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.channel = ctx.channel();

        ChannelPipeline pipeline = ctx.channel().pipeline();
        CommandHandler commandHandler = pipeline.get(CommandHandler.class);

        if (!commandHandler.getEndpoint().getPushListeners().contains(this)) {
            commandHandler.getEndpoint().addListener(this);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel() != null && ctx.channel().isActive() && ctx.channel().hasAttr(REBIND_ATTRIBUTE)
                && ctx.channel().attr(REBIND_ATTRIBUTE).get() == RebindState.COMPLETED) {
            logger.debug("Disconnecting at {}", LocalTime.now());
            ctx.channel().close().awaitUninterruptibly();
            notifyRebindCompleted();
        }

        super.channelReadComplete(ctx);
    }

    @Override
    public void onPushMessage(PushMessage message) {
        String mType = message.getType();

        if (REBIND_MESSAGE_TYPE.equals(mType)) {
            final SocketAddress rebindAddress = getRemoteAddress(message);
            if (rebindAddress != null) {
                logger.info("Attempting to rebind to new endpoint '{}'", rebindAddress);

                channel.attr(REBIND_ATTRIBUTE).set(RebindState.STARTED);
                this.reconnectionHandler.setSocketAddressSupplier(rebindAddress);

                ChannelPipeline pipeline = channel.pipeline();
                CommandHandler commandHandler = pipeline.get(CommandHandler.class);
                if (commandHandler.getStack().isEmpty()) {
                    channel.close().awaitUninterruptibly();
                    channel.attr(REBIND_ATTRIBUTE).set(RebindState.COMPLETED);
                } else {
                    notifyRebindStarted();
                }
            }
        } else if (MIGRATING_MESSAGE_TYPE.equals(mType) || isMigratingMessage(message)) {
            logger.info("Shard migration started");
            notifyMigrateStarted();
        } else if (MIGRATED_MESSAGE_TYPE.equals(mType) || isMigratedMessage(message)) {
            logger.info("Shard migration completed");
            Long relaxedTimeoutGracePeriod = getRelaxedTimeoutGracePeriod(message);
            notifyMigrateCompleted(relaxedTimeoutGracePeriod);
        }
    }

    private boolean isMigratingMessage(PushMessage message) {

        if (!message.getType().equals("message")) {
            return false;
        }

        List<Object> content = message.getContent();
        if (content.size() != 3) {
            logger.warn("Invalid MIGRATING message format, expected 1 elements, got {}", content.size());
            return false;
        }

        Object msg = content.get(2);
        if (!(msg instanceof ByteBuffer)) {
            logger.warn("Invalid MIGRATING message format, expected 1rd element to be a ByteBuffer, got {}", msg.getClass());
            return false;
        }

        String decodedMsg = StringCodec.UTF8.decodeKey((ByteBuffer) msg);
        if (MIGRATING_MESSAGE_TYPE.equals(decodedMsg.split(";")[0].split("=")[1])) {
            return true;
        }

        return false;
    }

    private boolean isMigratedMessage(PushMessage message) {

        if (!message.getType().equals("message")) {
            return false;
        }

        List<Object> content = message.getContent();
        if (content.size() == 3) {
            logger.warn("Invalid MIGRATING message format, expected 1 elements, got {}", content.size());
            return false;
        }

        Object msg = content.get(2);
        if (!(msg instanceof ByteBuffer)) {
            logger.warn("Invalid MIGRATING message format, expected 1rd element to be a ByteBuffer, got {}", msg.getClass());
            return false;
        }

        String decodedMsg = StringCodec.UTF8.decodeKey((ByteBuffer) msg);
        if (MIGRATED_MESSAGE_TYPE.equals(decodedMsg.split(";")[0].split("=")[1])) {
            return true;
        }

        return false;
    }

    private Long getRelaxedTimeoutGracePeriod(PushMessage message) {

        List<Object> content = message.getContent();
        if (content.size() != 2) {
            logger.warn("Invalid MIGRATED message format, expected 2 elements, got {}", content.size());
            return null;
        }

        Object delayedObject = content.get(MIGRATED_DELAY_RELAX_TIMEOUT_INDEX);
        if (!(delayedObject instanceof Long)) {
            logger.warn("Invalid MIGRATED message format, expected 2nd element to be a Long, got {}", delayedObject.getClass());
            return null;
        }

        return (Long) delayedObject;
    }

    private SocketAddress getRemoteAddress(PushMessage message) {

        List<Object> content = message.getContent();
        if (content.size() != 3) {
            logger.warn("Invalid re-bind message format, expected 3 elements, got {}", content.size());
            return null;
        }

        Object addressObject = content.get(REBIND_ADDRESS_INDEX);
        if (!(addressObject instanceof ByteBuffer)) {
            logger.warn("Invalid re-bind message format, expected 3rd element to be a ByteBuffer, got {}",
                    addressObject.getClass());
            return null;
        }

        String addressAndPort = StringCodec.UTF8.decodeKey((ByteBuffer) addressObject);
        try {
            String[] parts = addressAndPort.split(":");
            String address = parts[0];
            int port = Integer.parseInt(parts[1]);
            return new InetSocketAddress(address, port);
        } catch (Exception e) {
            logger.error("Failed to parse address and port from '{}'", addressAndPort, e);
            return null;
        }
    }

    /**
     * Register a component that is aware of re-bind events. Such a component is going to be notified of re-bind events by
     * calling their {@code onRebindStarted} and {@code onRebindCompleted} methods.
     *
     * @param component the component to register
     */
    public void setRebindListener(RebindAwareComponent component) {
        this.componentListeners.add(component);
    }

    private void notifyRebindCompleted() {
        this.componentListeners.forEach(RebindAwareComponent::onRebindCompleted);
    }

    private void notifyRebindStarted() {
        this.componentListeners.forEach(RebindAwareComponent::onRebindStarted);
    }

    private void notifyMigrateStarted() {
        this.componentListeners.forEach(RebindAwareComponent::onMigrateStarted);
    }

    private void notifyMigrateCompleted(Long relaxedTimeoutGracePeriod) {
        this.componentListeners.forEach(component -> component.onMigrateCompleted(relaxedTimeoutGracePeriod));
    }

}
