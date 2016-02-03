package com.lambdaworks.redis;

import io.netty.channel.ChannelDuplexHandler;

/**
 * Channel initializer to set up the transport before a Redis connection can be used. This class is part of the internal API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public abstract class RedisChannelInitializerImpl extends ChannelDuplexHandler implements RedisChannelInitializer {
}
