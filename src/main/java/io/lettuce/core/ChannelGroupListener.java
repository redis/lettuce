package io.lettuce.core;

import static io.lettuce.core.ConnectionEventTrigger.*;

import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.protocol.CommandHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;

/**
 * A netty {@link ChannelHandler} responsible for monitoring the channel and adding/removing the channel from/to the
 * ChannelGroup.
 *
 * @author Will Glozer
 * @author Mark Paluch
 */
class ChannelGroupListener extends ChannelInboundHandlerAdapter {

    private final ChannelGroup channels;

    private final EventBus eventBus;

    public ChannelGroupListener(ChannelGroup channels, EventBus eventBus) {
        this.channels = channels;
        this.eventBus = eventBus;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        CommandHandler commandHandler = getCommandHandler(ctx);
        String epid = commandHandler.getEndpoint().getId();

        eventBus.publish(
                new ConnectedEvent(getRedisUri(ctx.channel()), epid, commandHandler.getChannelId(), local(ctx), remote(ctx)));
        channels.add(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        CommandHandler commandHandler = getCommandHandler(ctx);
        String epid = commandHandler.getEndpoint().getId();

        eventBus.publish(new DisconnectedEvent(getRedisUri(ctx.channel()), epid, commandHandler.getChannelId(), local(ctx),
                remote(ctx)));
        channels.remove(ctx.channel());
        super.channelInactive(ctx);
    }

    private static String getRedisUri(Channel channel) {

        String redisUri = null;
        if (channel.hasAttr(ConnectionBuilder.REDIS_URI)) {
            redisUri = channel.attr(ConnectionBuilder.REDIS_URI).get();
        }

        return redisUri;
    }

    private static CommandHandler getCommandHandler(ChannelHandlerContext ctx) {
        return ctx.pipeline().get(CommandHandler.class);
    }
}
