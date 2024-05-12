package io.lettuce.test.server;

import java.util.Arrays;
import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handler to generate random base64 data.
 */
class RandomServerHandler extends ChannelInboundHandlerAdapter {

    private final int count;

    public RandomServerHandler() {

        int count;
        do {
            count = new Random().nextInt(50);
        } while (count < 1);

        this.count = count;

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        byte[] response = new byte[count];

        Arrays.fill(response, "A".getBytes()[0]);

        ByteBuf buf = ctx.alloc().heapBuffer(response.length);

        ByteBuf encoded = buf.writeBytes(response);
        ctx.writeAndFlush(encoded);
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
