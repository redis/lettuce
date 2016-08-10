package com.lambdaworks.redis;

import java.net.SocketAddress;

import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectionDeactivatedEvent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.local.LocalAddress;

/**
 * @author Mark Paluch
 * @since 3.0
 */
class ConnectionEventTrigger extends ChannelInboundHandlerAdapter {
    private final ConnectionEvents connectionEvents;
    private final RedisChannelHandler<?, ?> connection;
    private final EventBus eventBus;

    ConnectionEventTrigger(ConnectionEvents connectionEvents, RedisChannelHandler<?, ?> connection, EventBus eventBus) {
        this.connectionEvents = connectionEvents;
        this.connection = connection;
        this.eventBus = eventBus;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connectionEvents.fireEventRedisConnected(connection);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionEvents.fireEventRedisDisconnected(connection);
        eventBus.publish(new ConnectionDeactivatedEvent(local(ctx), remote(ctx)));
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        connectionEvents.fireEventRedisExceptionCaught(connection, cause);
        super.exceptionCaught(ctx, cause);
    }

    static SocketAddress remote(ChannelHandlerContext ctx) {
        if (ctx.channel() != null && ctx.channel().remoteAddress() != null) {
            return ctx.channel().remoteAddress();
        }
        return new LocalAddress("unknown");
    }

    static SocketAddress local(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (channel != null && channel.localAddress() != null) {
            return channel.localAddress();
        }
        return LocalAddress.ANY;
    }

}
