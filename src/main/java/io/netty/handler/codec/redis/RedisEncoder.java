/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.netty.handler.codec.redis;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;

public class RedisEncoder extends MessageToMessageEncoder<RedisMessage> {

    private static final int TYPE_LENGTH = 1;
    private static final byte[] CRLF = { '\r', '\n' };
    private static final byte[] NULL_BULK_STRING = { '-', '1' };

    @Override
    protected void encode(ChannelHandlerContext ctx, RedisMessage msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.alloc().buffer(getBufferLength(msg));
        out.add(writeRedisMessage(buf, msg));
    }

    private static ByteBuf writeRedisMessage(ByteBuf buf, RedisMessage msg) {
        RedisMessageType type = msg.type();
        if (type.isInline()) {
            writeInlineMessage(buf, msg);
        } else if (type.isBulkString()) {
            writeBulkStringsMessage(buf, msg);
        } else if (type.isArray()) {
            writeArrays(buf, msg);
        } else {
            throw new Error("bad message type: " + type);
        }
        return buf;
    }

    private static ByteBuf writeInlineMessage(ByteBuf buf, RedisMessage msg) {
        buf.writeByte(msg.type().value());
        buf.writeBytes(msg.content());
        buf.writeBytes(CRLF);
        return buf;
    }

    private static ByteBuf writeBulkStringsMessage(ByteBuf buf, RedisMessage msg) {
        ByteBuf content = msg.content();
        buf.writeByte(msg.type().value());
        if (content == null) {
            buf.writeBytes(NULL_BULK_STRING);
        } else {
            buf.writeBytes(Integer.toString(content.readableBytes()).getBytes(CharsetUtil.UTF_8));
            buf.writeBytes(CRLF);
            buf.writeBytes(content);
        }
        buf.writeBytes(CRLF);
        return buf;
    }

    private static ByteBuf writeArrays(ByteBuf buf, RedisMessage msg) {
        buf.writeByte(msg.type().value());
        if (msg.children() == null) {
            buf.writeBytes(NULL_BULK_STRING);
            buf.writeBytes(CRLF);
        } else {
            buf.writeBytes(Integer.toString(msg.children().size()).getBytes(CharsetUtil.UTF_8));
            buf.writeBytes(CRLF);
            for (RedisMessage child : msg.children()) {
                writeRedisMessage(buf, child);
            }
        }
        return buf;
    }

    private static int getBufferLength(RedisMessage msg) {
        final RedisMessageType type = msg.type();
        if (type.isInline()) {
            return TYPE_LENGTH + msg.content().readableBytes() + CRLF.length;
        } else if (type.isBulkString()) {
            if (msg.content() == null) {
                return TYPE_LENGTH + NULL_BULK_STRING.length + CRLF.length;
            } else {
                final int bulkStringLength = msg.content().readableBytes();
                return TYPE_LENGTH + Integer.toString(bulkStringLength).length() + CRLF.length + bulkStringLength + CRLF.length;
            }
        } else if (type.isArray()) {
            if (msg.children() == null) {
                return TYPE_LENGTH + NULL_BULK_STRING.length + CRLF.length;
            } else {
                int length = TYPE_LENGTH + Integer.toString(msg.children().size()).length() + CRLF.length;
                for (RedisMessage child : msg.children()) {
                    length += getBufferLength(child);
                }
                return length;
            }
        } else {
            throw new Error("bad type: " + type);
        }
    }

}
