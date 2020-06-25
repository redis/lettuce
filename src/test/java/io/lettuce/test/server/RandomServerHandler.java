/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
