package com.lambdaworks.redis;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 16.05.14 21:18
 */
@ChannelHandler.Sharable
class ConnectionEventTrigger extends ChannelInboundHandlerAdapter {
    private ConnectionEvents connectionEvents;
    private RedisChannelHandler<?, ?> connection;

    public ConnectionEventTrigger(ConnectionEvents connectionEvents, RedisChannelHandler<?, ?> connection) {
        this.connectionEvents = connectionEvents;
        this.connection = connection;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connectionEvents.fireEventRedisConnected(connection);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionEvents.fireEventRedisDisconnected(connection);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        connectionEvents.fireEventRedisExceptionCaught(connection, cause);
        super.exceptionCaught(ctx, cause);
    }
}
