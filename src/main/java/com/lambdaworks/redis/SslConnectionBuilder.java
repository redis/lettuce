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
import static com.lambdaworks.redis.PlainChannelInitializer.pingBeforeActivate;
import static com.lambdaworks.redis.PlainChannelInitializer.removeIfExists;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.net.ssl.*;

import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.resource.ClientResources;

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

    public SslConnectionBuilder ssl(RedisURI redisURI) {
        this.redisURI = redisURI;
        return this;
    }

    public static SslConnectionBuilder sslConnectionBuilder() {
        return new SslConnectionBuilder();
    }

    @Override
    protected List<ChannelHandler> buildHandlers() {
        LettuceAssert.assertState(redisURI != null, "RedisURI must not be null");
        LettuceAssert.assertState(redisURI.isSsl(), "RedisURI is not configured for SSL (ssl is false)");

        return super.buildHandlers();
    }

    @Override
    public RedisChannelInitializer build() {

        final List<ChannelHandler> channelHandlers = buildHandlers();

        return new SslChannelInitializer(getPingCommandSupplier(), channelHandlers, redisURI, clientResources(), getTimeout(),
                getTimeUnit(), clientOptions().getSslOptions());
    }

    /**
     * @author Mark Paluch
     */
    static class SslChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> implements RedisChannelInitializer {

        private final Supplier<AsyncCommand<?, ?, ?>> pingCommandSupplier;
        private final List<ChannelHandler> handlers;
        private final RedisURI redisURI;
        private final ClientResources clientResources;
        private final long timeout;
        private final TimeUnit timeUnit;
        private final SslOptions sslOptions;

        private volatile CompletableFuture<Boolean> initializedFuture = new CompletableFuture<>();

        public SslChannelInitializer(Supplier<AsyncCommand<?, ?, ?>> pingCommandSupplier, List<ChannelHandler> handlers,
                RedisURI redisURI, ClientResources clientResources, long timeout, TimeUnit timeUnit, SslOptions sslOptions) {

            this.pingCommandSupplier = pingCommandSupplier;
            this.handlers = handlers;
            this.redisURI = redisURI;
            this.clientResources = clientResources;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
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

            if (sslOptions.getKeystore() != null) {
                try (InputStream is = sslOptions.getKeystore().openStream()) {
                    sslContextBuilder.keyManager(createKeyManagerFactory(is,
                            sslOptions.getKeystorePassword().length == 0 ? null : sslOptions.getKeystorePassword()));
                }
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

            removeIfExists(channel.pipeline(), SslHandler.class);

            if (channel.pipeline().get("first") == null) {
                channel.pipeline().addFirst("first", new ChannelDuplexHandler() {

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        clientResources.eventBus().publish(new ConnectedEvent(local(ctx), remote(ctx)));
                        super.channelActive(ctx);
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        clientResources.eventBus().publish(new DisconnectedEvent(local(ctx), remote(ctx)));
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
                                if (pingCommandSupplier != PlainChannelInitializer.NO_PING) {
                                    pingCommand = pingCommandSupplier.get();
                                    pingBeforeActivate(pingCommand, initializedFuture, ctx, clientResources, timeout, timeUnit);
                                } else {
                                    ctx.fireChannelActive();
                                }
                            } else {
                                initializedFuture.completeExceptionally(event.cause());
                            }
                        }

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
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        if (cause instanceof SSLHandshakeException || cause.getCause() instanceof SSLException) {
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

        @Override
        public CompletableFuture<Boolean> channelInitialized() {
            return initializedFuture;
        }

        private static KeyManagerFactory createKeyManagerFactory(InputStream inputStream, char[] storePassword)
                throws GeneralSecurityException, IOException {

            KeyStore keyStore = getKeyStore(inputStream, storePassword);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, storePassword == null ? new char[0] : storePassword);

            return keyManagerFactory;
        }

        private static KeyStore getKeyStore(InputStream inputStream, char[] storePassword) throws KeyStoreException,
                IOException, NoSuchAlgorithmException, CertificateException {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            try {
                keyStore.load(inputStream, storePassword);
            } finally {
                inputStream.close();
            }
            return keyStore;
        }

        private static TrustManagerFactory createTrustManagerFactory(InputStream inputStream, char[] storePassword)
                throws GeneralSecurityException, IOException {

            KeyStore trustStore = getKeyStore(inputStream, storePassword);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return trustManagerFactory;
        }
    }
}
