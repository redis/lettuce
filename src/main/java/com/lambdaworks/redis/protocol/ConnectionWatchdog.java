// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for monitoring the channel and reconnecting when the connection is lost.
 * 
 * @author Will Glozer
 */
@ChannelHandler.Sharable
public class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionWatchdog.class);
    public static final int RETRY_TIMEOUT_MAX = 14;
    private Bootstrap bootstrap;
    private Channel channel;
    private Timer timer;
    private boolean reconnect;
    private int attempts;

    /**
     * Create a new watchdog that adds to new connections to the supplied {@link ChannelGroup} and establishes a new
     * {@link Channel} when disconnected, while reconnect is true.
     * 
     * @param bootstrap Configuration for new channels.
     * @param timer Timer used for delayed reconnect.
     */
    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer) {
        this.bootstrap = bootstrap;
        this.timer = timer;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        attempts = 0;
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (reconnect) {
            scheduleReconnect();
        }
        ctx.fireChannelInactive();
    }

    private void scheduleReconnect() {
        if (!channel.isActive()) {
            if (attempts < RETRY_TIMEOUT_MAX)
                attempts++;
            int timeout = 2 << attempts;
            timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Reconnect to the remote address that the closed channel was connected to. This creates a new {@link ChannelPipeline} with
     * the same handler instances contained in the old channel's pipeline.
     * 
     * @param timeout Timer task handle.
     * 
     * @throws Exception when reconnection fails.
     */
    @Override
    public void run(Timeout timeout) throws Exception {

        try {
            logger.info("Connecting");
            bootstrap.connect().sync();
        } catch (Exception e) {
            scheduleReconnect();
            logger.warn("Cannot connect: " + e.getMessage());
        }
    }
}
