package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkState;

import java.net.InetSocketAddress;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
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
    public ChannelInitializer<Channel> build() {

        final SslContext sslCtx;
        try {
            if (redisURI.isVerifyPeer()) {
                sslCtx = SslContext.newClientContext();
            } else {
                sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            }
        } catch (SSLException e) {
            throw new RedisConnectionException("Cannot create SSL client context", e);
        }

        final List<ChannelHandler> channelHandlers = buildHandlers();

        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
                if (socketAddress == null) {
                    socketAddress = (InetSocketAddress) socketAddress();
                }
                SSLEngine sslEngine = sslCtx.newEngine(channel.alloc(), socketAddress.getHostString(), socketAddress.getPort());
                if (redisURI.isVerifyPeer()) {
                    SSLParameters sslParams = new SSLParameters();
                    sslParams.setEndpointIdentificationAlgorithm("HTTPS");
                    sslEngine.setSSLParameters(sslParams);
                }

                SslHandler sslHandler = new SslHandler(sslEngine, redisURI.isStartTls());
                channel.pipeline().addLast(sslHandler);

                for (ChannelHandler channelHandler : channelHandlers) {
                    channel.pipeline().addLast(channelHandler);
                }
            }
        };
    }
}
