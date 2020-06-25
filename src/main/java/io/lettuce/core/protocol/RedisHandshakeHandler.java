/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timeout;

/**
 * Handler to initialize a Redis Connection using a {@link ConnectionInitializer}.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public class RedisHandshakeHandler extends ChannelInboundHandlerAdapter {

    private final ConnectionInitializer connectionInitializer;

    private final ClientResources clientResources;

    private final Duration initializeTimeout;

    private final CompletableFuture<Void> handshakeFuture = new CompletableFuture<>();

    public RedisHandshakeHandler(ConnectionInitializer connectionInitializer, ClientResources clientResources,
            Duration initializeTimeout) {
        this.connectionInitializer = connectionInitializer;
        this.clientResources = clientResources;
        this.initializeTimeout = initializeTimeout;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

        Runnable timeoutGuard = () -> {

            if (handshakeFuture.isDone()) {
                return;
            }

            fail(ctx, ExceptionFactory.createTimeoutException("Connection initialization timed out", initializeTimeout));
        };

        Timeout timeoutHandle = clientResources.timer().newTimeout(t -> {

            if (clientResources.eventExecutorGroup().isShuttingDown()) {
                timeoutGuard.run();
                return;
            }

            clientResources.eventExecutorGroup().submit(timeoutGuard);
        }, initializeTimeout.toNanos(), TimeUnit.NANOSECONDS);

        handshakeFuture.thenAccept(ignore -> {
            timeoutHandle.cancel();
        });

        super.channelRegistered(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        if (!handshakeFuture.isDone()) {
            fail(ctx, new RedisConnectionException("Connection closed prematurely"));
        }

        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        CompletionStage<Void> future = connectionInitializer.initialize(ctx.channel());

        future.whenComplete((ignore, throwable) -> {

            if (throwable != null) {
                fail(ctx, throwable);
            } else {
                ctx.fireChannelActive();
                succeed();
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        if (!handshakeFuture.isDone()) {
            fail(ctx, cause);
        }

        super.exceptionCaught(ctx, cause);
    }

    /**
     * Complete the handshake future successfully.
     */
    protected void succeed() {
        handshakeFuture.complete(null);
    }

    /**
     * Complete the handshake future with an error and close the channel..
     */
    protected void fail(ChannelHandlerContext ctx, Throwable cause) {

        ctx.close().addListener(closeFuture -> {
            handshakeFuture.completeExceptionally(cause);
        });
    }

    /**
     * @return future to synchronize channel initialization. Returns a new future for every reconnect.
     */
    public CompletionStage<Void> channelInitialized() {
        return handshakeFuture;
    }

}
