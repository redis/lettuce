/*
 * Copyright 2011-2017 the original author or authors.
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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.lambdaworks.redis.ConnectionEvents.PingBeforeActivate;
import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.Timeout;

/**
 * @author Mark Paluch
 */
class PlainChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> implements RedisChannelInitializer {

    final static Supplier<AsyncCommand<?, ?, ?>> NO_PING = () -> null;

    private final List<ChannelHandler> handlers;
    private final Supplier<AsyncCommand<?, ?, ?>> pingCommandSupplier;
    private final ClientResources clientResources;
    private final long timeout;
    private final TimeUnit timeUnit;

    private volatile CompletableFuture<Boolean> initializedFuture = new CompletableFuture<>();

    PlainChannelInitializer(Supplier<AsyncCommand<?, ?, ?>> pingCommandSupplier, List<ChannelHandler> handlers,
            ClientResources clientResources, long timeout, TimeUnit timeUnit) {

        this.pingCommandSupplier = pingCommandSupplier;
        this.handlers = handlers;
        this.clientResources = clientResources;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
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

                    clientResources.eventBus().publish(new DisconnectedEvent(local(ctx), remote(ctx)));
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
                            clientResources.eventBus().publish(new ConnectionActivatedEvent(local(ctx), remote(ctx)));
                        }
                    }
                    super.userEventTriggered(ctx, evt);
                }

                @Override
                public void channelActive(final ChannelHandlerContext ctx) throws Exception {

                    clientResources.eventBus().publish(new ConnectedEvent(local(ctx), remote(ctx)));

                    if (pingCommandSupplier != NO_PING) {
                        pingCommand = pingCommandSupplier.get();
                        pingBeforeActivate(pingCommand, initializedFuture, ctx, clientResources, timeout, timeUnit);
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

        clientResources.nettyCustomizer().afterChannelInitialized(channel);
    }

    static void pingBeforeActivate(AsyncCommand<?, ?, ?> cmd, CompletableFuture<Boolean> initializedFuture,
            ChannelHandlerContext ctx, ClientResources clientResources, long timeout, TimeUnit timeUnit) throws Exception {

        ctx.fireUserEventTriggered(new PingBeforeActivate(cmd));

        Runnable timeoutGuard = () -> {

            if (cmd.isDone() || initializedFuture.isDone()) {
                return;
            }

            initializedFuture.completeExceptionally(new RedisCommandTimeoutException(String.format(
                    "Cannot initialize channel (PING before activate) within %d %s", timeout, timeUnit)));
        };

        Timeout timeoutHandle = clientResources.timer().newTimeout(t -> {

            if (clientResources.eventExecutorGroup().isShuttingDown()) {
                timeoutGuard.run();
                return;
            }

            clientResources.eventExecutorGroup().submit(timeoutGuard);
        }, timeout, timeUnit);

        cmd.whenComplete((o, throwable) -> {

            timeoutHandle.cancel();

            if (throwable == null) {
                ctx.fireChannelActive();
                initializedFuture.complete(true);
            } else {
                initializedFuture.completeExceptionally(throwable);
            }
        });

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
