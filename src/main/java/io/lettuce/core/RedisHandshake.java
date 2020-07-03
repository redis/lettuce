/*
 * Copyright 2019-2020 the original author or authors.
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
package io.lettuce.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.ConnectionInitializer;
import io.lettuce.core.protocol.ProtocolVersion;
import io.netty.channel.Channel;

/**
 * Redis RESP2/RESP3 handshake using the configured {@link ProtocolVersion} and other options for connection initialization and
 * connection state restoration. This class is part of the internal API.
 *
 * @author Mark Paluch
 * @author Tugdual Grall
 * @since 6.0
 */
class RedisHandshake implements ConnectionInitializer {

    private final RedisCommandBuilder<String, String> commandBuilder = new RedisCommandBuilder<>(StringCodec.UTF8);

    private final ProtocolVersion requestedProtocolVersion;

    private final boolean pingOnConnect;

    private final ConnectionState connectionState;

    private volatile ProtocolVersion negotiatedProtocolVersion;

    RedisHandshake(ProtocolVersion requestedProtocolVersion, boolean pingOnConnect, ConnectionState connectionState) {

        this.requestedProtocolVersion = requestedProtocolVersion;
        this.pingOnConnect = pingOnConnect;
        this.connectionState = connectionState;
    }

    /**
     * @return the requested {@link ProtocolVersion}. May be {@code null} if not configured.
     */
    public ProtocolVersion getRequestedProtocolVersion() {
        return requestedProtocolVersion;
    }

    /**
     * @return the negotiated {@link ProtocolVersion} once the handshake is done.
     */
    public ProtocolVersion getNegotiatedProtocolVersion() {
        return negotiatedProtocolVersion;
    }

    @Override
    public CompletionStage<Void> initialize(Channel channel) {

        CompletableFuture<?> handshake;

        if (this.requestedProtocolVersion == ProtocolVersion.RESP2) {
            handshake = initializeResp2(channel);
            negotiatedProtocolVersion = ProtocolVersion.RESP2;
        } else if (this.requestedProtocolVersion == ProtocolVersion.RESP3) {
            handshake = initializeResp3(channel);
        } else if (this.requestedProtocolVersion == null) {
            handshake = tryHandshakeResp3(channel);
        } else {
            handshake = Futures.failed(
                    new RedisConnectionException("Protocol version" + this.requestedProtocolVersion + " not supported"));
        }

        return handshake.thenCompose(ignore -> applyPostHandshake(channel, getNegotiatedProtocolVersion()));
    }

    private CompletableFuture<?> tryHandshakeResp3(Channel channel) {

        CompletableFuture<?> handshake = new CompletableFuture<>();
        AsyncCommand<String, String, Map<String, Object>> hello = initiateHandshakeResp3(channel);

        hello.whenComplete((settings, throwable) -> {

            if (throwable != null) {
                if (isUnknownCommand(hello.getError())) {
                    fallbackToResp2(channel, handshake);
                } else {
                    handshake.completeExceptionally(throwable);
                }
            } else {
                handshake.complete(null);
            }
        });

        return handshake;
    }

    private void fallbackToResp2(Channel channel, CompletableFuture<?> handshake) {

        initializeResp2(channel).whenComplete((o, nested) -> {

            if (nested != null) {
                handshake.completeExceptionally(nested);
            } else {
                handshake.complete(null);
            }
        });
    }

    private CompletableFuture<?> initializeResp2(Channel channel) {
        return initiateHandshakeResp2(channel).thenRun(() -> {
            negotiatedProtocolVersion = ProtocolVersion.RESP2;

            connectionState.setHandshakeResponse(
                    new ConnectionState.HandshakeResponse(negotiatedProtocolVersion, null, null, null, null));
        });
    }

    private CompletableFuture<Void> initializeResp3(Channel channel) {
        return initiateHandshakeResp3(channel).thenAccept(response -> {

            Long id = (Long) response.get("id");
            String mode = (String) response.get("mode");
            String version = (String) response.get("version");
            String role = (String) response.get("role");

            negotiatedProtocolVersion = ProtocolVersion.RESP3;

            connectionState.setHandshakeResponse(
                    new ConnectionState.HandshakeResponse(negotiatedProtocolVersion, id, version, mode, role));
        });
    }

    /**
     * Perform a RESP2 Handshake: Issue a {@code PING} or {@code AUTH}.
     *
     * @param channel
     * @return
     */
    private CompletableFuture<?> initiateHandshakeResp2(Channel channel) {

        if (connectionState.hasUsername()) {
            return dispatch(channel, this.commandBuilder.auth(connectionState.getUsername(), connectionState.getPassword()));
        } else if (connectionState.hasPassword()) {
            return dispatch(channel, this.commandBuilder.auth(connectionState.getPassword()));
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
    private AsyncCommand<String, String, Map<String, Object>> initiateHandshakeResp3(Channel channel) {

        if (connectionState.hasPassword()) {

            return dispatch(channel, this.commandBuilder.hello(3,
                    LettuceStrings.isNotEmpty(connectionState.getUsername()) ? connectionState.getUsername() : "default",
                    connectionState.getPassword(), connectionState.getClientName()));
        }

        return dispatch(channel, this.commandBuilder.hello(3, null, null, connectionState.getClientName()));
    }

    private CompletableFuture<Void> applyPostHandshake(Channel channel, ProtocolVersion negotiatedProtocolVersion) {

        List<AsyncCommand<?, ?, ?>> postHandshake = new ArrayList<>();

        if (connectionState.getClientName() != null && negotiatedProtocolVersion == ProtocolVersion.RESP2) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.clientSetname(connectionState.getClientName())));
        }

        if (connectionState.getDb() > 0) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.select(connectionState.getDb())));
        }

        if (connectionState.isReadOnly()) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.readOnly()));
        }

        if (postHandshake.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return dispatch(channel, postHandshake);
    }

    private CompletableFuture<Void> dispatch(Channel channel, List<AsyncCommand<?, ?, ?>> commands) {

        CompletionStage<Void> writeFuture = Futures.toCompletionStage(channel.writeAndFlush(commands));
        return CompletableFuture.allOf(Futures.allOf(commands), writeFuture.toCompletableFuture());
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

    private static boolean isUnknownCommand(String error) {
        return LettuceStrings.isNotEmpty(error) && error.startsWith("ERR unknown command");
    }

}
