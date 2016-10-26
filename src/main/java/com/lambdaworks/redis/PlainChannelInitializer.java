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
package com.lambdaworks.redis;

import static com.lambdaworks.redis.ConnectionEventTrigger.local;
import static com.lambdaworks.redis.ConnectionEventTrigger.remote;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;
import com.lambdaworks.redis.protocol.AsyncCommand;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * @author Mark Paluch
 */
class PlainChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> implements RedisChannelInitializer {

    static final RedisCommandBuilder<String, String> INITIALIZING_CMD_BUILDER = new RedisCommandBuilder<>(
            new Utf8StringCodec());

    protected final char[] password;

    private boolean pingBeforeActivate;
    private CompletableFuture<Boolean> initializedFuture = new CompletableFuture<>();
    private final List<ChannelHandler> handlers;
    private final EventBus eventBus;

    public PlainChannelInitializer(boolean pingBeforeActivateConnection, char[] password, List<ChannelHandler> handlers,
            EventBus eventBus) {
        this.pingBeforeActivate = pingBeforeActivateConnection;
        this.password = password;
        this.handlers = handlers;
        this.eventBus = eventBus;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        if (channel.pipeline().get("channelActivator") == null) {

            channel.pipeline().addLast("channelActivator", new RedisChannelInitializerImpl() {

                private AsyncCommand<?, ?, ?> pingCommand;

                @Override
                public CompletableFuture<Boolean> channelInitialized() {
                    return initializedFuture;
                }

                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    eventBus.publish(new DisconnectedEvent(local(ctx), remote(ctx)));
                    initializedFuture = new CompletableFuture<>();
                    pingCommand = null;
                    super.channelInactive(ctx);
                }

                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                    if (evt instanceof ConnectionEvents.Close) {
                        if (ctx.channel().isOpen()) {
                            ctx.channel().close();
                        }
                    }

                    if (evt instanceof ConnectionEvents.Activated) {
                        if (!initializedFuture.isDone()) {
                            initializedFuture.complete(true);
                            eventBus.publish(new ConnectionActivatedEvent(local(ctx), remote(ctx)));
                        }
                    }
                    super.userEventTriggered(ctx, evt);
                }

                @Override
                public void channelActive(final ChannelHandlerContext ctx) throws Exception {
                    eventBus.publish(new ConnectedEvent(local(ctx), remote(ctx)));
                    if (pingBeforeActivate) {
                        if (password != null && password.length != 0) {
                            pingCommand = new AsyncCommand<>(INITIALIZING_CMD_BUILDER.auth(new String(password)));
                        } else {
                            pingCommand = new AsyncCommand<>(INITIALIZING_CMD_BUILDER.ping());
                        }
                        pingBeforeActivate(pingCommand, initializedFuture, ctx, handlers);
                    } else {
                        super.channelActive(ctx);
                    }
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                    if (!initializedFuture.isDone()) {
                        initializedFuture.completeExceptionally(cause);
                    }
                    super.exceptionCaught(ctx, cause);
                }
            });
        }

        for (ChannelHandler handler : handlers) {
            removeIfExists(channel.pipeline(), handler.getClass());
            channel.pipeline().addLast(handler);
        }
    }

    static void pingBeforeActivate(final AsyncCommand<?, ?, ?> cmd, final CompletableFuture<Boolean> initializedFuture,
            final ChannelHandlerContext ctx, final List<ChannelHandler> handlers) throws Exception {
        cmd.handle((o, throwable) -> {
            if (throwable == null) {
                initializedFuture.complete(true);
                ctx.fireChannelActive();
            } else {
                initializedFuture.completeExceptionally(throwable);
            }
            return null;
        });

        ctx.channel().writeAndFlush(cmd);
    }

    static void removeIfExists(ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerClass) {
        ChannelHandler channelHandler = pipeline.get(handlerClass);
        if (channelHandler != null) {
            pipeline.remove(channelHandler);
        }
    }

    @Override
    public CompletableFuture<Boolean> channelInitialized() {
        return initializedFuture;
    }

}
