// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.internal;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link io.netty.channel.ChannelHandler} responsible for monitoring the channel and adding/removing the channel
 * from/to the ChannelGroup.
 * 
 * @author Will Glozer
 */
@ChannelHandler.Sharable
public class ChannelGroupListener extends ChannelInboundHandlerAdapter {

    private ChannelGroup channels;

    public ChannelGroupListener(ChannelGroup channels) {
        this.channels = channels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
    }

}
