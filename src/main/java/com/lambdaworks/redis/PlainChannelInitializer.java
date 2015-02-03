package com.lambdaworks.redis;

import java.util.List;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.SettableFuture;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 02.02.15 09:36
 */
class PlainChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> implements RedisChannelInitializer {

    private List<ChannelHandler> handlers;
    protected SettableFuture<Boolean> initializedFuture = SettableFuture.create();

    public PlainChannelInitializer(List<ChannelHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                initializedFuture = SettableFuture.create();
                super.channelInactive(ctx);
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                if (!initializedFuture.isDone()) {
                    initializedFuture.set(true);
                }
                super.channelActive(ctx);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                if (!initializedFuture.isDone()) {
                    initializedFuture.setException(cause);
                }
                super.exceptionCaught(ctx, cause);
            }
        });
        for (ChannelHandler handler : handlers) {
            channel.pipeline().addLast(handler);
        }

    }

    public Future<Boolean> channelInitialized() {
        return initializedFuture;
    }
}
