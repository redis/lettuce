package com.lambdaworks.redis;

import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 02.02.15 09:36
 */
class RedisChannelInitializer extends io.netty.channel.ChannelInitializer<Channel> {

    private List<ChannelHandler> handlers;

    public RedisChannelInitializer(List<ChannelHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        for (ChannelHandler handler : handlers) {
            channel.pipeline().addLast(handler);
        }
    }
}
