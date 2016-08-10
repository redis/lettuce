package com.lambdaworks.redis;

import static com.lambdaworks.redis.ConnectionEventTrigger.local;
import static com.lambdaworks.redis.ConnectionEventTrigger.remote;
import static com.lambdaworks.redis.PlainChannelInitializer.INITIALIZING_CMD_BUILDER;
import static com.lambdaworks.redis.PlainChannelInitializer.pingBeforeActivate;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.net.ssl.*;

import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.AsyncCommand;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * Connection builder for SSL connections. This class is part of the internal API.
 * 
 * @author Mark Paluch
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
        LettuceAssert.assertState(redisURI != null, "RedisURI must not be null");
        LettuceAssert.assertState(redisURI.isSsl(), "RedisURI is not configured for SSL (ssl is false)");

        return super.buildHandlers();
    }

    @Override
    public RedisChannelInitializer build() {

        return new SslChannelInitializer(clientOptions().isPingBeforeActivateConnection(), this::buildHandlers, redisURI,
                clientResources().eventBus(), clientOptions().getSslOptions());
    }

    /**
     * @author Mark Paluch
     */
    static class SslChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> implements RedisChannelInitializer {

        private final boolean pingBeforeActivate;
        private final Supplier<List<ChannelHandler>> handlers;
        private final RedisURI redisURI;
        private final EventBus eventBus;
        private final SslOptions sslOptions;
        private CompletableFuture<Boolean> initializedFuture = new CompletableFuture<>();

        public SslChannelInitializer(boolean pingBeforeActivate, Supplier<List<ChannelHandler>> handlers, RedisURI redisURI,
                EventBus eventBus, SslOptions sslOptions) {
            this.pingBeforeActivate = pingBeforeActivate;
            this.handlers = handlers;
            this.redisURI = redisURI;
            this.eventBus = eventBus;
            this.sslOptions = sslOptions;
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {

            SSLParameters sslParams = new SSLParameters();

            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient().sslProvider(sslOptions.getSslProvider());
            if (redisURI.isVerifyPeer()) {
                sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            } else {
                sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }

            if (sslOptions.getTruststore() != null) {
                try (InputStream is = sslOptions.getTruststore().openStream()) {
                    sslContextBuilder.trustManager(createTrustManagerFactory(is,
                            sslOptions.getTruststorePassword().length == 0 ? null : sslOptions.getTruststorePassword()));
                }
            }

            SslContext sslContext = sslContextBuilder.build();

            SSLEngine sslEngine = sslContext.newEngine(channel.alloc(), redisURI.getHost(), redisURI.getPort());
            sslEngine.setSSLParameters(sslParams);

            if (channel.pipeline().get("first") == null) {
                channel.pipeline().addFirst("first", new ChannelDuplexHandler() {

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        eventBus.publish(new ConnectedEvent(local(ctx), remote(ctx)));
                        super.channelActive(ctx);
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        eventBus.publish(new DisconnectedEvent(local(ctx), remote(ctx)));
                        super.channelInactive(ctx);
                    }
                });
            }

            SslHandler sslHandler = new SslHandler(sslEngine, redisURI.isStartTls());
            channel.pipeline().addLast(sslHandler);

            if (channel.pipeline().get("channelActivator") == null) {
                channel.pipeline().addLast("channelActivator", new RedisChannelInitializerImpl() {

                    private AsyncCommand<?, ?, ?> pingCommand;

                    @Override
                    public CompletableFuture<Boolean> channelInitialized() {
                        return initializedFuture;
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        initializedFuture = new CompletableFuture<>();
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
                                    if (redisURI.getPassword() != null && redisURI.getPassword().length != 0) {
                                        pingCommand = new AsyncCommand<>(
                                                INITIALIZING_CMD_BUILDER.auth(new String(redisURI.getPassword())));
                                    } else {
                                        pingCommand = new AsyncCommand<>(INITIALIZING_CMD_BUILDER.ping());
                                    }
                                    pingBeforeActivate(pingCommand, initializedFuture, ctx);
                                } else {
                                    ctx.fireChannelActive();
                                }
                            } else {
                                initializedFuture.completeExceptionally(event.cause());
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
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        if (cause instanceof SSLHandshakeException || cause.getCause() instanceof SSLException) {
                            initializedFuture.completeExceptionally(cause);
                        }
                        super.exceptionCaught(ctx, cause);
                    }
                });
            }

            for (ChannelHandler handler : handlers.get()) {
                channel.pipeline().addLast(handler);
            }
        }

        @Override
        public CompletableFuture<Boolean> channelInitialized() {
            return initializedFuture;
        }

        private static TrustManagerFactory createTrustManagerFactory(InputStream inputStream, char[] storePassword)
                throws GeneralSecurityException, IOException {

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

            try {
                trustStore.load(inputStream, storePassword);
            } finally {
                inputStream.close();
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return trustManagerFactory;
        }

    }
}
