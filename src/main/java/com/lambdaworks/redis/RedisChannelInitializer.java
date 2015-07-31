package com.lambdaworks.redis;

import java.util.concurrent.Future;

import io.netty.channel.ChannelHandler;

/**
 * Channel initializer to set up the transport before a Redis connection can be used. This is part of the internal API.
 * This class is part of the internal API.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface RedisChannelInitializer extends ChannelHandler {

    /**
     * 
     * @return future to synchronize channel initialization. Returns a new future for every reconnect.
     */
    Future<Boolean> channelInitialized();
}
