package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.*;
import static com.lambdaworks.redis.PlainChannelInitializer.*;

import java.util.List;
import java.util.concurrent.Future;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;

import com.google.common.util.concurrent.SettableFuture;
import com.lambdaworks.redis.protocol.Command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class SslConnectionBuilder extends ConnectionBuilder {
    private RedisURI redisURI;

    public static SslConnectionBuilder sslConnectionBuilder() {
        return new SslConnectionBuilder();
    }

    public SslConnectionBuilder ssl(RedisURI redisURI) {
        this.redisURI = redisURI;
        return this;
    }

    @Override
    protected List<ChannelHandler> buildHandlers() {
        checkState(redisURI != null, "redisURI must not be null");
        checkState(redisURI.isSsl(), "redisURI is not configured for SSL (ssl is false)");

        return super.buildHandlers();
    }

    @Override
    public RedisChannelInitializer build() {

        final List<ChannelHandler> channelHandlers = buildHandlers();

        return new SslChannelInitializer(clientOptions().isPingBeforeActivateConnection(), channelHandlers, redisURI);
    }

    /**
     * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
     * @since 02.02.15 09:36
     */
    static class SslChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> implements RedisChannelInitializer {

        private boolean pingBeforeActivate;
        private List<ChannelHandler> handlers;
        private SettableFuture<Boolean> initializedFuture = SettableFuture.create();
        private RedisURI redisURI;

        public SslChannelInitializer(boolean pingBeforeActivate, List<ChannelHandler> handlers, RedisURI redisURI) {
            this.pingBeforeActivate = pingBeforeActivate;
            this.handlers = handlers;
            this.redisURI = redisURI;
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            SslContext sslContext;

            SSLParameters sslParams = new SSLParameters();

            if (redisURI.isVerifyPeer()) {
                sslContext = SslContext.newClientContext(SslProvider.JDK);
                if (JavaRuntime.AT_LEAST_JDK_7) {
                    sslParams.setEndpointIdentificationAlgorithm("HTTPS");
                }
            } else {
                sslContext = SslContext.newClientContext(SslProvider.JDK, InsecureTrustManagerFactory.INSTANCE);
            }

            SSLEngine sslEngine = sslContext.newEngine(channel.alloc(), redisURI.getHost(), redisURI.getPort());
            sslEngine.setSSLParameters(sslParams);

            removeIfExists(channel.pipeline(), SslHandler.class);
            SslHandler sslHandler = new SslHandler(sslEngine, redisURI.isStartTls());
            channel.pipeline().addLast(sslHandler);

            if (channel.pipeline().get("channelActivator") == null) {
                channel.pipeline().addLast("channelActivator", new RedisChannelInitializerImpl() {

                    private Command<?, ?, ?> pingCommand;

                    @Override
                    public Future<Boolean> channelInitialized() {
                        return initializedFuture;
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        initializedFuture = SettableFuture.create();
                        pingCommand = null;
                        super.channelInactive(ctx);
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        if (initializedFuture.isDone()) {
                            super.channelActive(ctx);
                        }
                    }

                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        if (evt instanceof SslHandshakeCompletionEvent && !initializedFuture.isDone()) {

                            SslHandshakeCompletionEvent event = (SslHandshakeCompletionEvent) evt;
                            if (event.isSuccess()) {
                                if (pingBeforeActivate) {
                                    pingCommand = INITIALIZING_CMD_BUILDER.ping();
                                    pingBeforeActivate(pingCommand, initializedFuture, ctx, handlers);
                                } else {
                                    ctx.fireChannelActive();
                                }
                            } else {
                                initializedFuture.setException(event.cause());
                            }
                        }

                        if (evt instanceof ConnectionEvents.Close) {
                            if (ctx.channel().isOpen()) {
                                ctx.channel().close();
                            }
                        }

                        if (evt instanceof ConnectionEvents.Activated) {
                            if (!initializedFuture.isDone()) {
                                initializedFuture.set(true);
                            }
                        }

                        super.userEventTriggered(ctx, evt);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

                        if (!initializedFuture.isDone()) {
                            initializedFuture.setException(cause);
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

        @Override
        public Future<Boolean> channelInitialized() {
            return initializedFuture;
        }
    }
}
