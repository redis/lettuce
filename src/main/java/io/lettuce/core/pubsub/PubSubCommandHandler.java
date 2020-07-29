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
package io.lettuce.core.pubsub;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.ReplayOutput;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.DecodeBufferPolicy;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for writing Redis Pub/Sub commands and reading the response stream from the
 * server. {@link PubSubCommandHandler} accounts for Pub/Sub message notification calling back
 * {@link PubSubEndpoint#notifyMessage(PubSubMessage)}. Redis responses can be interleaved in the sense that a response contains
 * a Pub/Sub message first, then a command response. Possible interleave is introspected via {@link ResponseHeaderReplayOutput}
 * and decoding hooks.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class PubSubCommandHandler<K, V> extends CommandHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PubSubCommandHandler.class);

    private final PubSubEndpoint<K, V> endpoint;

    private final RedisCodec<K, V> codec;

    private final Deque<ReplayOutput<K, V>> queue = new ArrayDeque<>();

    private final DecodeBufferPolicy decodeBufferPolicy;

    private ResponseHeaderReplayOutput<K, V> replay;

    private PubSubOutput<K, V> output;

    /**
     * Initialize a new instance.
     *
     * @param clientOptions client options for this connection, must not be {@code null}
     * @param clientResources client resources for this connection
     * @param codec Codec.
     * @param endpoint the Pub/Sub endpoint for Pub/Sub callback.
     */
    public PubSubCommandHandler(ClientOptions clientOptions, ClientResources clientResources, RedisCodec<K, V> codec,
            PubSubEndpoint<K, V> endpoint) {

        super(clientOptions, clientResources, endpoint);

        this.endpoint = endpoint;
        this.codec = codec;
        this.decodeBufferPolicy = clientOptions.getDecodeBufferPolicy();
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
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer) throws InterruptedException {

        if (output.type() != null && !output.isCompleted()) {

            if (!super.decode(buffer, output)) {
                decodeBufferPolicy.afterPartialDecode(buffer);
                return;
            }

            RedisCommand<?, ?, ?> peek = getStack().peek();
            canComplete(peek);
            doNotifyMessage(output);
            output = new PubSubOutput<>(codec);
        }

        if (!getStack().isEmpty() || isPushDecode(buffer)) {
            super.decode(ctx, buffer);
        }

        ReplayOutput<K, V> replay;
        while ((replay = queue.poll()) != null) {

            replay.replay(output);
            doNotifyMessage(output);
            output = new PubSubOutput<>(codec);
        }

        while (super.getStack().isEmpty() && buffer.isReadable()) {

            if (!super.decode(buffer, output)) {
                decodeBufferPolicy.afterPartialDecode(buffer);
                return;
            }

            doNotifyMessage(output);
            output = new PubSubOutput<>(codec);
        }

        decodeBufferPolicy.afterDecoding(buffer);
    }

    @Override
    protected boolean canDecode(ByteBuf buffer) {
        return super.canDecode(buffer) && output.type() == null;
    }

    @Override
    protected boolean canComplete(RedisCommand<?, ?, ?> command) {

        if (isResp2PubSubMessage(replay)) {

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
    private static boolean isResp2PubSubMessage(ResponseHeaderReplayOutput<?, ?> replay) {

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
    protected CommandOutput<?, ?, ?> getCommandOutput(RedisCommand<?, ?, ?> command) {

        if (getStack().isEmpty() || command.getOutput() == null) {
            return super.getCommandOutput(command);
        }

        if (replay == null) {
            replay = new ResponseHeaderReplayOutput<>();
        }

        return replay;
    }

    protected void notifyPushListeners(PushMessage notification) {

        if (PubSubOutput.Type.isPubSubType(notification.getType())) {

            PubSubOutput.Type type = PubSubOutput.Type.valueOf(notification.getType());
            RedisCommand<?, ?, ?> command = getStack().peek();

            if (command != null && shouldCompleteCommand(type, command)) {
                completeCommand(notification, command);
            }

            doNotifyMessage(toPubSubMessage(notification));
        }

        super.notifyPushListeners(notification);
    }

    private boolean shouldCompleteCommand(PubSubOutput.Type type, RedisCommand<?, ?, ?> command) {

        String commandType = command.getType().name();
        switch (type) {
            case subscribe:
                return commandType.equalsIgnoreCase("SUBSCRIBE");

            case psubscribe:
                return commandType.equalsIgnoreCase("PSUBSCRIBE");

            case unsubscribe:
                return commandType.equalsIgnoreCase("UNSUBSCRIBE");

            case punsubscribe:
                return commandType.equalsIgnoreCase("PUNSUBSCRIBE");
        }

        return false;
    }

    private void completeCommand(PushMessage notification, RedisCommand<?, ?, ?> command) {
        CommandOutput<?, ?, ?> output = command.getOutput();
        for (Object value : notification.getContent()) {

            if (value instanceof Long) {
                output.set((Long) value);
            } else {
                output.set((ByteBuffer) value);
            }
        }

        getStack().poll().complete();
    }

    private PubSubMessage<K, V> toPubSubMessage(PushMessage notification) {

        PubSubOutput<K, V> output = new PubSubOutput<>(codec);

        for (Object argument : notification.getContent()) {

            if (argument instanceof Long) {
                output.set((Long) argument);
            } else {
                output.set((ByteBuffer) argument);
            }
        }

        return output;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void afterDecode(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command) {

        super.afterDecode(ctx, command);

        if (command.getOutput() instanceof PubSubOutput) {
            doNotifyMessage((PubSubOutput) command.getOutput());
        }
    }

    private void doNotifyMessage(PubSubMessage<K, V> message) {
        try {
            endpoint.notifyMessage(message);
        } catch (Exception e) {
            logger.error("Unexpected error occurred in PubSubEndpoint.notifyMessage", e);
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
