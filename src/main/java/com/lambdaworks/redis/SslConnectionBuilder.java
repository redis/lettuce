package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class SslConnectionBuilder extends ConnectionBuilder {
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

        final SslContext sslCtx;
        try {
            if (redisURI.isVerifyPeer()) {
                sslCtx = SslContext.newClientContext(SslProvider.JDK);
            } else {
                sslCtx = SslContext.newClientContext(SslProvider.JDK, InsecureTrustManagerFactory.INSTANCE);
            }
        } catch (SSLException e) {
            throw new RedisConnectionException("Cannot create SSL client context", e);
        }

        final List<ChannelHandler> channelHandlers = buildHandlers();

        return new SslChannelInitializer(channelHandlers, sslCtx, redisURI);
    }
}
