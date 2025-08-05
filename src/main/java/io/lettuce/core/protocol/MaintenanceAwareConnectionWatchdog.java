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
 * @see ClientOptions#getMaintenanceEventsOptions()
 */
@ChannelHandler.Sharable
public class MaintenanceAwareConnectionWatchdog extends ConnectionWatchdog implements PushListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MaintenanceAwareConnectionWatchdog.class);

    private static final String REBIND_MESSAGE_TYPE = "MOVING";

    private static final String MIGRATING_MESSAGE_TYPE = "MIGRATING";

    private static final String MIGRATED_MESSAGE_TYPE = "MIGRATED";

    private static final String FAILING_OVER_MESSAGE_TYPE = "FAILING_OVER";

    private static final String FAILED_OVER_MESSAGE_TYPE = "FAILED_OVER";

    private static final int REBIND_ADDRESS_INDEX = 3;

    public static final AttributeKey<RebindState> REBIND_ATTRIBUTE = AttributeKey.newInstance("rebindAddress");

    private static final int MIGRATING_SHARDS_INDEX = 3;

    private static final int MIGRATED_SHARDS_INDEX = 3;

    private static final int FAILING_OVER_SHARDS_INDEX = 3;

    private static final int FAILED_OVER_SHARDS_INDEX = 3;

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
                logger.debug("Attempting to rebind to new endpoint '{}'", rebindAddress);

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
        } else if (MIGRATING_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Shard migration started");
            notifyMigrateStarted(getMigratingShards(message));
        } else if (MIGRATED_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Shard migration completed");
            notifyMigrateCompleted(getMigratedShards(message));
        } else if (FAILING_OVER_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Failover started");
            notifyFailoverStarted(getFailingOverShards(message));
        } else if (FAILED_OVER_MESSAGE_TYPE.equals(mType)) {
            logger.debug("Failover completed");
            notifyFailoverCompleted(getFailedOverShards(message));
        }
    }

    private String getMigratingShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 4))
            return null;

        return getShards(content, MIGRATING_SHARDS_INDEX, MIGRATING_MESSAGE_TYPE);
    }

    private String getMigratedShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 3))
            return null;

        return getShards(content, MIGRATED_SHARDS_INDEX, MIGRATED_MESSAGE_TYPE);
    }

    private String getFailingOverShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 3))
            return null;

        return getShards(content, FAILING_OVER_SHARDS_INDEX, FAILING_OVER_MESSAGE_TYPE);
    }

    private static boolean isInvalidMaintenanceEvent(List<Object> content, int expectedSize) {
        if (content.size() < expectedSize) {
            logger.warn("Invalid maintenance message format, expected at least {} elements, got {}", expectedSize,
                    content.size());
            return true;
        }

        return false;
    }

    private String getFailedOverShards(PushMessage message) {
        List<Object> content = message.getContent();

        if (isInvalidMaintenanceEvent(content, 3))
            return null;

        return getShards(content, FAILED_OVER_SHARDS_INDEX, FAILED_OVER_MESSAGE_TYPE);
    }

    private static String getShards(List<Object> content, int shardsIndex, String maintenanceEvent) {
        Object shardsObject = content.get(shardsIndex);

        if (!(shardsObject instanceof ByteBuffer)) {
            logger.warn("Invalid shards format, expected ByteBuffer, got {} for {} maintenance event",
                    shardsObject != null ? shardsObject.getClass() : "null", maintenanceEvent);
            return null;
        }

        return StringCodec.UTF8.decodeKey((ByteBuffer) shardsObject);
    }

    private SocketAddress getRemoteAddress(PushMessage message) {

        List<Object> content = message.getContent();
        if (content.size() != 4) {
            logger.warn("Invalid re-bind message format, expected 4 elements, got {}", content.size());
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
        this.componentListeners.forEach(MaintenanceAwareComponent::onRebindCompleted);
    }

    private void notifyRebindStarted() {
        this.componentListeners.forEach(MaintenanceAwareComponent::onRebindStarted);
    }

    private void notifyMigrateStarted(String shards) {
        this.componentListeners.forEach(component -> component.onMigrateStarted(shards));
    }

    private void notifyMigrateCompleted(String shards) {
        this.componentListeners.forEach(component -> component.onMigrateCompleted(shards));
    }

    private void notifyFailoverStarted(String shards) {
        this.componentListeners.forEach(component -> component.onFailoverStarted(shards));
    }

    private void notifyFailoverCompleted(String shards) {
        this.componentListeners.forEach(component -> component.onFailoverCompleted(shards));
    }

}
