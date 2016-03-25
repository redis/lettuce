package com.lambdaworks.redis;

import io.netty.channel.ChannelDuplexHandler;

/**
 * Channel initializer to set up the transport before a Redis connection can be used. This class is part of the internal API.
 * 
 * @author Mark Paluch
 */
public abstract class RedisChannelInitializerImpl extends ChannelDuplexHandler implements RedisChannelInitializer {
}
