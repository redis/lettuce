package com.lambdaworks.redis;

import io.netty.channel.ChannelHandler;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface RedisChannelInitializer extends ChannelHandler {

    Future<Boolean> channelInitialized();
}
