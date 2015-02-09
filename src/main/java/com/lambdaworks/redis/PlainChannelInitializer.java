package com.lambdaworks.redis;

import java.util.List;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.SettableFuture;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

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

        if (channel.pipeline().get("channelActivator") == null) {

            channel.pipeline().addLast("channelActivator", new RedisChannelInitializerImpl() {

                @Override
                public Future<Boolean> channelInitialized() {
                    return initializedFuture;
                }

                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    initializedFuture = SettableFuture.create();
                    super.channelInactive(ctx);
                }

                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                    if (evt instanceof ConnectionEvents.Close) {
                        if (ctx.channel().isOpen()) {
                            ctx.channel().close();
                        }
                    }
                    super.userEventTriggered(ctx, evt);
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
        }

        for (ChannelHandler handler : handlers) {
            removeIfExists(channel.pipeline(), handler.getClass());
            channel.pipeline().addLast(handler);
        }
    }

    static void removeIfExists(ChannelPipeline pipeline, String name) {
        ChannelHandler channelHandler = pipeline.get(name);
        if (channelHandler != null) {
            pipeline.remove(channelHandler);
        }
    }

    static void removeIfExists(ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerClass) {
        ChannelHandler channelHandler = pipeline.get(handlerClass);
        if (channelHandler != null) {
            pipeline.remove(channelHandler);
        }
    }

    public Future<Boolean> channelInitialized() {
        return initializedFuture;
    }
}
