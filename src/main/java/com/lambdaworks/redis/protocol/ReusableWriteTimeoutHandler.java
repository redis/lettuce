package com.lambdaworks.redis.protocol;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

/**
 * ResuableWriteTimeoutHandler is one handler per a connection, but this is marked with
 * {@link ChannelHandler.Sharable}, because it'll be reused when client is reconnected.
 */
@ChannelHandler.Sharable
public class ReusableWriteTimeoutHandler extends WriteTimeoutHandler {

    public ReusableWriteTimeoutHandler(long timeout, TimeUnit unit) {
        super(timeout, unit);
    }
}
