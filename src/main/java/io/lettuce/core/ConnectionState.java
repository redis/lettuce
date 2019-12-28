/*
 * Copyright 2019 the original author or authors.
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
package io.lettuce.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.*;
import io.netty.channel.Channel;

/**
 * Internal connection state representing the requested {@link ProtocolVersion} and other options for connection initialization
 * and connection state restoration. This class is part of the internal API.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public class ConnectionState implements ConnectionInitializer {

    private final RedisCommandBuilder<String, String> commandBuilder = new RedisCommandBuilder<>(StringCodec.UTF8);

    private ProtocolVersion requested;
    private boolean pingOnConnect;

    private volatile String username;
    private volatile char[] password;
    private volatile int db;
    private volatile boolean readOnly;
    private volatile String clientName;

    public void setRequestedProtocolVersion(ProtocolVersion requested) {
        this.requested = requested;
    }

    public void setPingOnConnect(boolean pingOnConnect) {
        this.pingOnConnect = pingOnConnect;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public void setDb(int db) {
        this.db = db;
    }

    private boolean hasPassword() {
        return this.password != null && this.password.length > 0;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public CompletionStage<Void> initialize(Channel channel) {

        CompletableFuture<?> handshake;

        List<RedisCommand<?, ?, ?>> postHandshake = new ArrayList<>();

        if (this.requested == ProtocolVersion.RESP2) {
            handshake = initiateHandshakeResp2(channel);

            if (this.clientName != null) {
                postHandshake.add(this.commandBuilder.clientSetname(this.clientName));
            }
        } else if (this.requested == ProtocolVersion.RESP3) {
            handshake = initiateHandshakeResp3(channel);
        } else {
            handshake = new CompletableFuture<>();
            handshake.completeExceptionally(
                    new RedisConnectionException("Protocol version" + this.requested + " not supported"));
        }

        if (this.db > 0) {
            postHandshake.add(this.commandBuilder.select(this.db));
        }

        if (this.readOnly) {
            postHandshake.add(this.commandBuilder.readOnly());
        }

        if (!postHandshake.isEmpty()) {

            List<AsyncCommand<?, ?, ?>> commands = new ArrayList<>();

            for (RedisCommand<?, ?, ?> redisCommand : postHandshake) {

                AsyncCommand<?, ?, ?> async = new AsyncCommand<>(redisCommand);
                handshake = handshake.thenCompose(o -> async);
                commands.add(async);
            }

            channel.writeAndFlush(commands);
        }

        return handshake.thenRun(() -> {
        });
    }

    /**
     * Perform a RESP2 Handshake: Issue a {@code PING} or {@code AUTH}.
     *
     * @param channel
     * @return
     */
    private CompletableFuture<?> initiateHandshakeResp2(Channel channel) {

        if (hasPassword()) {
            return dispatch(channel, this.commandBuilder.auth(this.password));
        } else if (this.pingOnConnect) {
            return dispatch(channel, this.commandBuilder.ping());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Perform a RESP3 Handshake: Issue a {@code HELLO}.
     *
     * @param channel
     * @return
     */
    private CompletableFuture<Map<String, Object>> initiateHandshakeResp3(Channel channel) {

        if (hasPassword()) {

            return dispatch(channel, this.commandBuilder.hello(3,
                    LettuceStrings.isNotEmpty(this.username) ? this.username : "default", this.password, this.clientName));
        }

        return dispatch(channel, this.commandBuilder.hello(3, null, null, this.clientName));
    }

    private <T> AsyncCommand<String, String, T> dispatch(Channel channel, Command<String, String, T> command) {

        AsyncCommand<String, String, T> future = new AsyncCommand<>(command);

        channel.writeAndFlush(future).addListener(writeFuture -> {

            if (!writeFuture.isSuccess()) {
                future.completeExceptionally(writeFuture.cause());
            }
        });
        return future;
    }
}
