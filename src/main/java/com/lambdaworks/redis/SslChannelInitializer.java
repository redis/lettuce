package com.lambdaworks.redis;

import java.util.List;
import java.util.concurrent.Future;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;

import com.google.common.util.concurrent.SettableFuture;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 02.02.15 09:36
 */
class SslChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> implements RedisChannelInitializer {

    private List<ChannelHandler> handlers;
    private SettableFuture<Boolean> initializedFuture = SettableFuture.create();
    private SslContext sslContext;
    private RedisURI redisURI;

    public SslChannelInitializer(List<ChannelHandler> handlers, SslContext sslContext, RedisURI redisURI) {
        this.handlers = handlers;
        this.sslContext = sslContext;
        this.redisURI = redisURI;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        SSLEngine sslEngine = sslContext.newEngine(channel.alloc(), redisURI.getHost(), redisURI.getPort());
        if (redisURI.isVerifyPeer()) {
            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            sslEngine.setSSLParameters(sslParams);
        }

        SslHandler sslHandler = new SslHandler(sslEngine, redisURI.isStartTls());
        channel.pipeline().addLast(sslHandler);
        channel.pipeline().addLast(new ChannelDuplexHandler() {

            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (!initializedFuture.isDone() && evt instanceof SslHandshakeCompletionEvent) {
                    SslHandshakeCompletionEvent event = (SslHandshakeCompletionEvent) evt;

                    if (event.isSuccess()) {
                        initializedFuture.set(true);
                    } else {
                        initializedFuture.setException(event.cause());
                    }
                }
                super.userEventTriggered(ctx, evt);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                if (!initializedFuture.isDone()
                        && (cause instanceof SSLHandshakeException || cause.getCause() instanceof SSLException)) {
                    initializedFuture.setException(cause);
                }
                super.exceptionCaught(ctx, cause);
            }
        });

        for (ChannelHandler handler : handlers) {
            channel.pipeline().addLast(handler);
        }

        for (ChannelHandler handler : handlers) {
            channel.pipeline().addLast(handler);
        }
    }

    public Future<Boolean> channelInitialized() {
        return initializedFuture;
    }
}
