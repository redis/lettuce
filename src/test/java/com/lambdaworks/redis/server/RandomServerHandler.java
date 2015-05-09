package com.lambdaworks.redis.server;

import java.security.SecureRandom;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.base64.Base64;

/**
 * Handler to generate random base64 data.
 */
@ChannelHandler.Sharable
public class RandomServerHandler extends ChannelInboundHandlerAdapter {

    private SecureRandom random = new SecureRandom();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte initial[] = new byte[1];
        random.nextBytes(initial);

        byte[] response = new byte[Math.abs((int) initial[0])];
        random.nextBytes(response);

        ByteBuf buf = ctx.alloc().heapBuffer(response.length);

        ByteBuf encoded = Base64.encode(buf.writeBytes(response));
        buf.release();
        ctx.write(encoded);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}