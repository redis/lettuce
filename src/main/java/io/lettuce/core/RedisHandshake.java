/*
 * Copyright 2019-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.lettuce.core.MaintenanceEventsOptions.AddressTypeSource;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ConnectionInitializer;
import io.lettuce.core.protocol.ProtocolVersion;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static io.lettuce.core.protocol.CommandKeyword.MAINT_NOTIFICATIONS;
import static io.lettuce.core.protocol.CommandType.CLIENT;

/**
 * Redis RESP2/RESP3 handshake using the configured {@link ProtocolVersion} and other options for connection initialization and
 * connection state restoration. This class is part of the internal API.
 *
 * @author Mark Paluch
 * @author Tugdual Grall
 * @since 6.0
 */
class RedisHandshake implements ConnectionInitializer {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(RedisHandshake.class);

    private final RedisCommandBuilder<String, String> commandBuilder = new RedisCommandBuilder<>(StringCodec.UTF8);

    private final ProtocolVersion requestedProtocolVersion;

    private final boolean pingOnConnect;

    private final ConnectionState connectionState;

    private volatile ProtocolVersion negotiatedProtocolVersion;

    private final MaintenanceEventsOptions.AddressTypeSource addressTypeSource;

    RedisHandshake(ProtocolVersion requestedProtocolVersion, boolean pingOnConnect, ConnectionState connectionState,
            MaintenanceEventsOptions.AddressTypeSource addressTypeSource) {

        this.addressTypeSource = addressTypeSource;
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

        CompletionStage<?> handshake;

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

        return handshake
                // post-handshake commands, whose execution failures would cause the connection to be considered
                // unsuccessfully established
                .thenCompose(ignore -> applyPostHandshake(channel))
                // post-handshake commands, executed in a 'fire and forget' manner, to avoid having to react to different
                // implementations or versions of the server runtime, and whose execution result (whether a success or a
                // failure ) should not alter the outcome of the connection attempt
                .thenCompose(ignore -> applyConnectionMetadataSafely(channel));
    }

    private CompletionStage<?> tryHandshakeResp3(Channel channel) {

        CompletableFuture<?> handshake = new CompletableFuture<>();
        CompletionStage<Map<String, Object>> hello = initiateHandshakeResp3(channel, connectionState.getCredentialsProvider());

        hello.whenComplete((settings, throwable) -> {

            if (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }

            if (throwable != null) {
                if (isUnknownCommand(throwable) || isNoProto(throwable)) {
                    try {
                        fallbackToResp2(channel, handshake);
                    } catch (Exception e) {
                        e.addSuppressed(throwable);
                        handshake.completeExceptionally(e);
                    }
                } else {
                    handshake.completeExceptionally(throwable);
                }
            } else {
                try {
                    onHelloResponse(settings);
                    handshake.complete(null);
                } catch (RuntimeException e) {
                    handshake.completeExceptionally(e);
                }
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

        return initiateHandshakeResp2(channel, connectionState.getCredentialsProvider()).thenRun(() -> {
            negotiatedProtocolVersion = ProtocolVersion.RESP2;

            connectionState.setHandshakeResponse(
                    new ConnectionState.HandshakeResponse(negotiatedProtocolVersion, null, null, null, null));
        });
    }

    private CompletionStage<Void> initializeResp3(Channel channel) {
        return initiateHandshakeResp3(channel, connectionState.getCredentialsProvider()).thenAccept(this::onHelloResponse);
    }

    private void onHelloResponse(Map<String, Object> response) {

        Long id = (Long) response.get("id");
        String mode = (String) response.get("mode");
        String version = (String) response.get("version");
        String role = (String) response.get("role");

        negotiatedProtocolVersion = ProtocolVersion.RESP3;

        connectionState.setHandshakeResponse(
                new ConnectionState.HandshakeResponse(negotiatedProtocolVersion, id, version, mode, role));
    }

    /**
     * Perform a RESP2 Handshake: Issue a {@code PING} or {@code AUTH}.
     *
     * @param channel
     * @param credentialsProvider
     * @return
     */
    private CompletableFuture<?> initiateHandshakeResp2(Channel channel, RedisCredentialsProvider credentialsProvider) {

        if (credentialsProvider instanceof RedisCredentialsProvider.ImmediateRedisCredentialsProvider) {
            return dispatchAuthOrPing(channel,
                    ((RedisCredentialsProvider.ImmediateRedisCredentialsProvider) credentialsProvider).resolveCredentialsNow());
        }

        CompletableFuture<RedisCredentials> credentialsFuture = credentialsProvider.resolveCredentials().toFuture();

        return credentialsFuture.thenComposeAsync(credentials -> dispatchAuthOrPing(channel, credentials));
    }

    private CompletableFuture<String> dispatchAuthOrPing(Channel channel, RedisCredentials credentials) {

        if (credentials.hasUsername()) {
            return dispatch(channel, this.commandBuilder.auth(credentials.getUsername(), credentials.getPassword()));
        } else if (credentials.hasPassword()) {
            return dispatch(channel, this.commandBuilder.auth(credentials.getPassword()));
        } else if (this.pingOnConnect) {
            return dispatch(channel, this.commandBuilder.ping());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Perform a RESP3 Handshake: Issue a {@code HELLO}.
     *
     * @param channel
     * @param credentialsProvider
     * @return
     */
    private CompletionStage<Map<String, Object>> initiateHandshakeResp3(Channel channel,
            RedisCredentialsProvider credentialsProvider) {

        if (credentialsProvider instanceof RedisCredentialsProvider.ImmediateRedisCredentialsProvider) {
            return dispatchHello(channel,
                    ((RedisCredentialsProvider.ImmediateRedisCredentialsProvider) credentialsProvider).resolveCredentialsNow());
        }

        CompletableFuture<RedisCredentials> credentialsFuture = credentialsProvider.resolveCredentials().toFuture();

        return credentialsFuture.thenComposeAsync(credentials -> dispatchHello(channel, credentials));
    }

    private AsyncCommand<String, String, Map<String, Object>> dispatchHello(Channel channel, RedisCredentials credentials) {

        if (credentials.hasPassword()) {
            return dispatch(channel,
                    this.commandBuilder.hello(3,
                            LettuceStrings.isNotEmpty(credentials.getUsername()) ? credentials.getUsername() : "default",
                            credentials.getPassword(), connectionState.getClientName()));
        }

        return dispatch(channel, this.commandBuilder.hello(3, null, null, connectionState.getClientName()));
    }

    private CompletableFuture<Void> applyPostHandshake(Channel channel) {

        List<AsyncCommand<?, ?, ?>> postHandshake = new ArrayList<>();

        if (connectionState.getDb() > 0) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.select(connectionState.getDb())));
        }

        if (connectionState.isReadOnly()) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.readOnly()));
        }

        if (addressTypeSource != null) {
            CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(MAINT_NOTIFICATIONS).add("on");
            String addressType = addressType(channel, connectionState, addressTypeSource);

            if (addressType != null) {
                args.add("moving-endpoint-type").add(addressType);
            }

            Command<String, String, String> maintNotificationsOn = new Command<>(CLIENT, new StatusOutput<>(StringCodec.UTF8),
                    args);
            postHandshake.add(new AsyncCommand<>(maintNotificationsOn));
        }

        if (postHandshake.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return dispatch(channel, postHandshake);
    }

    private String addressType(Channel channel, ConnectionState state, AddressTypeSource addressTypeSource) {
        MaintenanceEventsOptions.AddressType addressType = addressTypeSource.getAddressType(channel.remoteAddress(),
                state.getConnectionMetadata().isSslEnabled());

        if (addressType == null) {
            return null;
        }

        switch (addressType) {
            case INTERNAL_IP:
                return "internal-ip";
            case INTERNAL_FQDN:
                return "internal-fqdn";
            case EXTERNAL_IP:
                return "external-ip";
            case EXTERNAL_FQDN:
                return "external-fqdn";
            default:
                throw new IllegalArgumentException("Unknown moving endpoint address type:" + addressType);
        }
    }

    private CompletionStage<Void> applyConnectionMetadataSafely(Channel channel) {
        return applyConnectionMetadata(channel).handle((result, error) -> {
            if (error != null) {
                LOG.debug("Error applying connection metadata", error);
            }
            return null;
        });
    }

    private CompletableFuture<Void> applyConnectionMetadata(Channel channel) {

        List<AsyncCommand<?, ?, ?>> postHandshake = new ArrayList<>();

        ConnectionMetadata metadata = connectionState.getConnectionMetadata();
        ProtocolVersion negotiatedProtocolVersion = getNegotiatedProtocolVersion();

        if (metadata.getClientName() != null && negotiatedProtocolVersion == ProtocolVersion.RESP2) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.clientSetname(connectionState.getClientName())));
        }

        if (LettuceStrings.isNotEmpty(metadata.getLibraryName())) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.clientSetinfo("lib-name", metadata.getLibraryName())));
        }

        if (LettuceStrings.isNotEmpty(metadata.getLibraryVersion())) {
            postHandshake.add(new AsyncCommand<>(this.commandBuilder.clientSetinfo("lib-ver", metadata.getLibraryVersion())));
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

    private static boolean isUnknownCommand(Throwable error) {
        return error instanceof RedisException && LettuceStrings.isNotEmpty(error.getMessage())
                && ((error.getMessage().startsWith("ERR") && error.getMessage().contains("unknown")));
    }

    private static boolean isNoProto(Throwable error) {
        return error instanceof RedisException && LettuceStrings.isNotEmpty(error.getMessage())
                && error.getMessage().startsWith("NOPROTO");
    }

    /**
     * Value object to represent a Redis version.
     */
    static class RedisVersion {

        private static final Pattern DECIMALS = Pattern.compile("(\\d+)");

        private final static RedisVersion UNKNOWN = new RedisVersion("0.0.0");

        private final static RedisVersion UNSTABLE = new RedisVersion("255.255.255");

        private final int major;

        private final int minor;

        private final int bugfix;

        private RedisVersion(String version) {

            int major = 0;
            int minor = 0;
            int bugfix = 0;

            LettuceAssert.notNull(version, "Version must not be null");

            Matcher matcher = DECIMALS.matcher(version);

            if (matcher.find()) {
                major = Integer.parseInt(matcher.group(1));

                if (matcher.find()) {
                    minor = Integer.parseInt(matcher.group(1));
                }

                if (matcher.find()) {
                    bugfix = Integer.parseInt(matcher.group(1));
                }
            }

            this.major = major;
            this.minor = minor;
            this.bugfix = bugfix;
        }

        /**
         * Construct a new {@link RedisVersion} from a version string containing major, minor and bugfix version such as
         * {@code 7.2.0}.
         *
         * @param version
         * @return
         */
        public static RedisVersion of(String version) {
            return new RedisVersion(version);
        }

        public boolean isGreaterThan(RedisVersion version) {
            return this.compareTo(version) > 0;
        }

        public boolean isGreaterThanOrEqualTo(RedisVersion version) {
            return this.compareTo(version) >= 0;
        }

        public boolean is(RedisVersion version) {
            return this.equals(version);
        }

        public boolean isLessThan(RedisVersion version) {
            return this.compareTo(version) < 0;
        }

        public boolean isLessThanOrEqualTo(RedisVersion version) {
            return this.compareTo(version) <= 0;
        }

        public int compareTo(RedisVersion that) {
            if (this.major != that.major) {
                return this.major - that.major;
            } else if (this.minor != that.minor) {
                return this.minor - that.minor;
            } else {
                return this.bugfix - that.bugfix;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RedisVersion that = (RedisVersion) o;
            return major == that.major && minor == that.minor && bugfix == that.bugfix;
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, bugfix);
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + bugfix;
        }

    }

}
