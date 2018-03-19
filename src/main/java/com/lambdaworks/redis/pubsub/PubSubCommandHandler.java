/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.pubsub;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.output.ReplayOutput;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * A netty {@link ChannelHandler} responsible for writing Redis Pub/Sub commands and reading the response stream from the
 * server. {@link PubSubCommandHandler} accounts for Pub/Sub message notification calling back
 * {@link ChannelHandlerContext#fireChannelRead(Object)}. Redis responses can be interleaved in the sense that a response
 * contains a Pub/Sub message first, then a command response. Possible interleave is introspected via
 * {@link ResponseHeaderReplayOutput} and decoding hooks.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class PubSubCommandHandler<K, V> extends CommandHandler<K, V> {

    private final RedisCodec<K, V> codec;
    private final Deque<ReplayOutput<K, V>> queue = new ArrayDeque<>();

    private ResponseHeaderReplayOutput<K, V> replay;
    private PubSubOutput<K, V, V> output;

    /**
     * Initialize a new instance.
     *
     * @param clientOptions client options for the connection
     * @param clientResources client resources for this connection
     * @param codec Codec.
     */
    public PubSubCommandHandler(ClientOptions clientOptions, ClientResources clientResources, RedisCodec<K, V> codec) {

        super(clientOptions, clientResources);

        this.codec = codec;
        this.output = new PubSubOutput<>(codec);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        replay = null;
        queue.clear();

        super.channelInactive(ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) {

        if (!getStack().isEmpty()) {
            super.decode(ctx, buffer);
        }

        ReplayOutput<K, V> replay;
        while ((replay = queue.poll()) != null) {

            replay.replay(output);
            ctx.fireChannelRead(output);
            output = new PubSubOutput<>(codec);
        }

        while (super.getStack().isEmpty() && buffer.isReadable()) {

            if (!rsm.decode(buffer, output)) {
                return;
            }

            ctx.fireChannelRead(output);
            output = new PubSubOutput<>(codec);
        }

        buffer.discardReadBytes();
    }

    @Override
    protected boolean canDecode(ByteBuf buffer) {
        return super.canDecode(buffer) && output.type() == null;
    }

    @Override
    protected boolean canComplete(RedisCommand<?, ?, ?> command) {

        if (isPubSubMessage(replay)) {

            queue.add(replay);
            replay = null;
            return false;
        }

        return super.canComplete(command);
    }

    @Override
    protected void complete(RedisCommand<?, ?, ?> command) {

        if (replay != null && command.getOutput() != null) {
            try {
                replay.replay(command.getOutput());
            } catch (Exception e) {
                command.completeExceptionally(e);
            }
            replay = null;
        }

        super.complete(command);
    }

    /**
     * Check whether {@link ResponseHeaderReplayOutput} contains a Pub/Sub message that requires Pub/Sub dispatch instead of to
     * be used as Command output.
     *
     * @param replay
     * @return
     */
    private static boolean isPubSubMessage(ResponseHeaderReplayOutput replay) {

        if (replay == null) {
            return false;
        }

        String firstElement = replay.firstElement;
        if (replay.multiCount != null && firstElement != null) {

            if (replay.multiCount == 3 && firstElement.equalsIgnoreCase(PubSubOutput.Type.message.name())) {
                return true;
            }

            if (replay.multiCount == 4 && firstElement.equalsIgnoreCase(PubSubOutput.Type.pmessage.name())) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected CommandOutput<K, V, ?> getCommandOutput(RedisCommand<K, V, ?> command) {

        if (getStack().isEmpty() || command.getOutput() == null) {
            return super.getCommandOutput(command);
        }

        if (replay == null) {
            replay = new ResponseHeaderReplayOutput<>();
        }

        return replay;
    }

    @Override
    protected void afterComplete(ChannelHandlerContext ctx, RedisCommand<K, V, ?> command) {
        if (command.getOutput() instanceof PubSubOutput) {
            ctx.fireChannelRead(command.getOutput());
        }
    }

    /**
     * Inspectable {@link ReplayOutput} to investigate the first multi and string response elements.
     *
     * @param <K>
     * @param <V>
     */
    static class ResponseHeaderReplayOutput<K, V> extends ReplayOutput<K, V> {

        Integer multiCount;
        String firstElement;

        @Override
        public void set(ByteBuffer bytes) {

            if (firstElement == null && bytes != null && bytes.remaining() > 0) {

                bytes.mark();
                firstElement = StringCodec.ASCII.decodeKey(bytes);
                bytes.reset();
            }

            super.set(bytes);
        }

        @Override
        public void multi(int count) {

            if (multiCount == null) {
                multiCount = count;
            }

            super.multi(count);
        }
    }
}
