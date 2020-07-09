/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.ClientResources;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * Connection builder for SSL connections. This class is part of the internal API.
 *
 * @author Mark Paluch
 * @author Amin Mohtashami
 * @author Felipe Ruiz
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
    public ChannelInitializer<Channel> build(SocketAddress socketAddress) {
        return new SslChannelInitializer(this::buildHandlers, toHostAndPort(socketAddress), redisURI.isVerifyPeer(),
                redisURI.isStartTls(), clientResources(), clientOptions().getSslOptions());
    }

    static HostAndPort toHostAndPort(SocketAddress socketAddress) {

        if (socketAddress instanceof InetSocketAddress) {

            InetSocketAddress isa = (InetSocketAddress) socketAddress;

            return HostAndPort.of(isa.getHostString(), isa.getPort());
        }

        return null;
    }

    static class SslChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> {

        private final Supplier<List<ChannelHandler>> handlers;

        private final HostAndPort hostAndPort;

        private final boolean verifyPeer;

        private final boolean startTls;

        private final ClientResources clientResources;

        private final SslOptions sslOptions;

        public SslChannelInitializer(Supplier<List<ChannelHandler>> handlers, HostAndPort hostAndPort, boolean verifyPeer,
                boolean startTls, ClientResources clientResources, SslOptions sslOptions) {

            this.handlers = handlers;
            this.hostAndPort = hostAndPort;
            this.verifyPeer = verifyPeer;
            this.startTls = startTls;
            this.clientResources = clientResources;
            this.sslOptions = sslOptions;
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {

            SSLEngine sslEngine = initializeSSLEngine(channel.alloc());
            SslHandler sslHandler = new SslHandler(sslEngine, startTls);
            Duration sslHandshakeTimeout = sslOptions.getHandshakeTimeout();
            sslHandler.setHandshakeTimeoutMillis(sslHandshakeTimeout.toMillis());

            channel.pipeline().addLast(sslHandler);

            for (ChannelHandler handler : handlers.get()) {
                channel.pipeline().addLast(handler);
            }

            clientResources.nettyCustomizer().afterChannelInitialized(channel);
        }

        private SSLEngine initializeSSLEngine(ByteBufAllocator alloc) throws IOException, GeneralSecurityException {

            SSLParameters sslParams = sslOptions.createSSLParameters();
            SslContextBuilder sslContextBuilder = sslOptions.createSslContextBuilder();

            if (verifyPeer) {
                sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            } else {
                sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }

            SslContext sslContext = sslContextBuilder.build();

            SSLEngine sslEngine = hostAndPort != null
                    ? sslContext.newEngine(alloc, hostAndPort.getHostText(), hostAndPort.getPort())
                    : sslContext.newEngine(alloc);
            sslEngine.setSSLParameters(sslParams);

            return sslEngine;
        }

    }

}
