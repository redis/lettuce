/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ChannelHandler.Sharable
public class RebindAwareConnectionWatchdog extends ConnectionWatchdog implements PushListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RebindAwareConnectionWatchdog.class);

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
        commandHandler.getEndpoint().addListener(this);
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
    }

    // TODO this logic needs to be addressed according the finalized message payload
    private SocketAddress getRemoteAddress(PushMessage message) {

        if (!message.getType().equals("message")) {
            return null;
        }

        List<String> content = message.getContent().stream()
                .map(ez -> ez instanceof ByteBuffer ? StringCodec.UTF8.decodeKey((ByteBuffer) ez) : ez.toString())
                .collect(Collectors.toList());

        if (content.stream().noneMatch(c -> c.contains("type=rebind"))) {
            return null;
        }

        final String payload = content.stream().filter(c -> c.contains("to_ep")).findFirst()
                .orElse("type=rebind;from_ep=localhost:6479;to_ep=localhost:6379;until_s=10");

        final String toEndpoint = Arrays.stream(payload.split(";")).filter(c -> c.contains("to_ep")).findFirst()
                .orElse("to_ep=localhost:6479");

        final String addressAndPort = toEndpoint.split("=")[1];
        final String address = addressAndPort.split(":")[0];
        final int port = Integer.parseInt(addressAndPort.split(":")[1]);

        return new InetSocketAddress(address, port);
    }

    public void setRebindListener(RebindAwareComponent component) {
        this.componentListeners.add(component);
    }

    private void notifyRebindCompleted() {
        this.componentListeners.forEach(RebindAwareComponent::onRebindCompleted);
    }

    private void notifyRebindStarted() {
        this.componentListeners.forEach(RebindAwareComponent::onRebindStarted);
    }

}
