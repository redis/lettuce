/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import java.nio.charset.Charset;
import java.util.Collection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for encoding commands.
 *
 * @author Mark Paluch
 */
public class CommandEncoder extends MessageToByteEncoder<Object> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandEncoder.class);

    private final boolean traceEnabled = logger.isTraceEnabled();
    private final boolean debugEnabled = logger.isDebugEnabled();

    public CommandEncoder() {
        this(true);
    }

    public CommandEncoder(boolean preferDirect) {
        super(preferDirect);
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, Object msg, boolean preferDirect) throws Exception {

        if (msg instanceof Collection) {

            if (preferDirect) {
                return ctx.alloc().ioBuffer(((Collection) msg).size() * 16);
            } else {
                return ctx.alloc().heapBuffer(((Collection) msg).size() * 16);
            }
        }

        if (preferDirect) {
            return ctx.alloc().ioBuffer();
        } else {
            return ctx.alloc().heapBuffer();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

        if (msg instanceof RedisCommand) {
            RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) msg;
            encode(ctx, out, command);
        }

        if (msg instanceof Collection) {
            Collection<RedisCommand<?, ?, ?>> commands = (Collection<RedisCommand<?, ?, ?>>) msg;
            for (RedisCommand<?, ?, ?> command : commands) {
                encode(ctx, out, command);
            }
        }
    }

    private void encode(ChannelHandlerContext ctx, ByteBuf out, RedisCommand<?, ?, ?> command) {

        try {
            out.markWriterIndex();
            command.encode(out);
        } catch (RuntimeException e) {
            out.resetWriterIndex();
            command.completeExceptionally(new EncoderException(
                    "Cannot encode command. Please close the connection as the connection state may be out of sync.",
                    e));
        }

        if (debugEnabled) {
            logger.debug("{} writing command {}", logPrefix(ctx.channel()), command);
            if (traceEnabled) {
                logger.trace("{} Sent: {}", logPrefix(ctx.channel()), out.toString(Charset.defaultCharset()).trim());
            }
        }
    }

    private String logPrefix(Channel channel) {
        StringBuilder buffer = new StringBuilder(64);
        buffer.append('[').append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return buffer.toString();
    }
}
