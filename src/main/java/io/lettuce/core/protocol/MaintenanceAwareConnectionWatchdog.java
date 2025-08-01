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
 * An extension to {@link ConnectionWatchdog} that intercepts maintenance events.
 *
 * @author Tihomir Mateev
 * @since 7.0
 * @see ClientOptions#supportsMaintenanceEvents()
 */
@ChannelHandler.Sharable
public class MaintenanceAwareConnectionWatchdog extends ConnectionWatchdog implements PushListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MaintenanceAwareConnectionWatchdog.class);

    private static final String REBIND_MESSAGE_TYPE = "MOVING";

    private static final String MIGRATING_MESSAGE_TYPE = "MIGRATING";

    private static final String MIGRATED_MESSAGE_TYPE = "MIGRATED";

    private static final String FAILING_OVER_MESSAGE_TYPE = "FAILING_OVER";

    private static final String FAILED_OVER_MESSAGE_TYPE = "FAILED_OVER";

    private static final int REBIND_ADDRESS_INDEX = 2;

    public static final AttributeKey<RebindState> REBIND_ATTRIBUTE = AttributeKey.newInstance("rebindAddress");

    private static final int FAILING_OVER_SHARDS_INDEX = 2;

    private static final int FAILED_OVER_SHARDS_INDEX = 1;

    private Channel channel;

    private final Set<MaintenanceAwareComponent> componentListeners = new HashSet<>();

    public MaintenanceAwareConnectionWatchdog(Delay reconnectDelay, ClientOptions clientOptions, Bootstrap bootstrap,
            Timer timer, EventExecutorGroup reconnectWorkers, Mono<SocketAddress> socketAddressSupplier,
            ReconnectionListener reconnectionListener, ConnectionFacade connectionFacade, EventBus eventBus,
            Endpoint endpoint) {

        super(reconnectDelay, clientOptions, bootstrap, timer, reconnectWorkers, socketAddressSupplier, reconnectionListener,
                connectionFacade, eventBus, endpoint);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.channel = ctx.channel();
        logger.info("*** WATCHDOG: channelActive() called - registering as PushListener ***");

        ChannelPipeline pipeline = ctx.channel().pipeline();
        CommandHandler commandHandler = pipeline.get(CommandHandler.class);

        boolean alreadyRegistered = commandHandler.getEndpoint().getPushListeners().contains(this);
        logger.info("*** WATCHDOG: Already registered as PushListener: {} ***", alreadyRegistered);

        if (!alreadyRegistered) {
            commandHandler.getEndpoint().addListener(this);
            logger.info("*** WATCHDOG: Successfully registered as PushListener ***");
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("*** WATCHDOG: we go IN channelReadComplete ***");

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
                logger.debug("Attempting to rebind to new endpoint '{}'", rebindAddress);

                channel.attr(REBIND_ATTRIBUTE).set(RebindState.STARTED);
                this.reconnectionHandler.setSocketAddressSupplier(rebindAddress);

                ChannelPipeline pipeline = channel.pipeline();
                CommandHandler commandHandler = pipeline.get(CommandHandler.class);
                boolean stackEmpty = commandHandler.getStack().isEmpty();
                int stackSize = commandHandler.getStack().size();
                logger.info("*** WATCHDOG: MOVING notification - stack.isEmpty()={}, stack.size()={} ***", stackEmpty,
                        stackSize);

                if (stackEmpty) {
                    logger.info("*** WATCHDOG: Stack is empty - closing channel and setting COMPLETED ***");
                    channel.close().awaitUninterruptibly();
                    channel.attr(REBIND_ATTRIBUTE).set(RebindState.COMPLETED);
                } else {
                    logger.info("*** WATCHDOG: Stack is NOT empty - calling notifyRebindStarted() ***");
                    notifyRebindStarted();
                }
            }
        } else if (MIGRATING_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Shard migration started");
            notifyMigrateStarted();
        } else if (MIGRATED_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Shard migration completed");
            notifyMigrateCompleted();
        } else if (FAILING_OVER_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Failover started");
            notifyFailoverStarted(getFailingOverShards(message));
        } else if (FAILED_OVER_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Failover completed");
            notifyFailoverCompleted(getFailedOverShards(message));
        }
    }

    private String getFailingOverShards(PushMessage message) {
        List<Object> content = message.getContent(StringCodec.UTF8::decodeValue);

        if (content.size() < 3) {
            logger.warn("Invalid failing over message format, expected at least 3 elements, got {}", content.size());
            return null;
        }

        Object shardsObject = content.get(FAILING_OVER_SHARDS_INDEX);

        if (!(shardsObject instanceof String)) {
            logger.warn("Invalid failing over message format, expected 3rd element to be a List, got {}",
                    shardsObject != null ? shardsObject.getClass() : "null");
            return null;
        }

        @SuppressWarnings("unchecked")
        String shards = (String) shardsObject;
        return shards;
    }

    private String getFailedOverShards(PushMessage message) {
        List<Object> content = message.getContent(StringCodec.UTF8::decodeValue);

        if (content.size() < 2) {
            logger.warn("Invalid failed over message format, expected at least 2 elements, got {}", content.size());
            return null;
        }

        Object shardsObject = content.get(FAILED_OVER_SHARDS_INDEX);

        if (!(shardsObject instanceof String)) {
            logger.warn("Invalid failed over message format, expected 2rd element to be a String, got {}",
                    shardsObject != null ? shardsObject.getClass() : "null");
            return null;
        }

        // expected to be a list of strings ["1","2"]

        @SuppressWarnings("unchecked")
        String shards = (String) shardsObject;
        return shards;
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
    public void setMaintenanceEventListener(MaintenanceAwareComponent component) {
        this.componentListeners.add(component);
    }

    private void notifyRebindCompleted() {
        logger.info("*** WATCHDOG: notifyRebindCompleted() called - notifying {} component listeners ***",
                this.componentListeners.size());
        this.componentListeners.forEach(MaintenanceAwareComponent::onRebindCompleted);
        logger.info("*** WATCHDOG: notifyRebindCompleted() completed ***");
    }

    private void notifyRebindStarted() {
        logger.info("*** WATCHDOG: notifyRebindStarted() called - notifying {} component listeners ***",
                this.componentListeners.size());
        this.componentListeners.forEach(MaintenanceAwareComponent::onRebindStarted);
        logger.info("*** WATCHDOG: notifyRebindStarted() completed ***");
    }

    private void notifyMigrateStarted() {
        logger.info("*** WATCHDOG: notifyMigrateStarted() called - notifying {} component listeners ***",
                this.componentListeners.size());
        this.componentListeners.forEach(MaintenanceAwareComponent::onMigrateStarted);
        logger.info("*** WATCHDOG: notifyMigrateStarted() completed ***");
    }

    private void notifyMigrateCompleted() {
        logger.info("*** WATCHDOG: notifyMigrateCompleted() called - notifying {} component listeners ***",
                this.componentListeners.size());
        this.componentListeners.forEach(MaintenanceAwareComponent::onMigrateCompleted);
        logger.info("*** WATCHDOG: notifyMigrateCompleted() completed ***");
    }

    private void notifyFailoverStarted(String shards) {
        logger.info("*** WATCHDOG: notifyFailoverStarted() called for shards {} - notifying {} component listeners ***", shards,
                this.componentListeners.size());
        this.componentListeners.forEach(component -> component.onFailoverStarted(shards));
        logger.info("*** WATCHDOG: notifyFailoverStarted() completed ***");
    }

    private void notifyFailoverCompleted(String shards) {
        logger.info("*** WATCHDOG: notifyFailoverCompleted() called for shards {} - notifying {} component listeners ***",
                shards, this.componentListeners.size());
        this.componentListeners.forEach(component -> component.onFailoverCompleted(shards));
        logger.info("*** WATCHDOG: notifyFailoverCompleted() completed ***");
    }

}
