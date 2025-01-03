/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
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

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Predicate;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.RedisJsonException;
import io.lettuce.core.protocol.DecodeBufferPolicies;
import io.lettuce.core.protocol.DecodeBufferPolicy;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.ReadOnlyCommands;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import reactor.core.publisher.Mono;

/**
 * Client Options to control the behavior of {@link RedisClient}.
 *
 * @author Mark Paluch
 * @author Gavin Cook
 * @author Jim Brunner
 */
@SuppressWarnings("serial")
public class ClientOptions implements Serializable {

    public static final boolean DEFAULT_AUTO_RECONNECT = true;

    public static final Predicate<RedisCommand<?, ?, ?>> DEFAULT_REPLAY_FILTER = (cmd) -> false;

    public static final int DEFAULT_BUFFER_USAGE_RATIO = 3;

    public static final boolean DEFAULT_CANCEL_CMD_RECONNECT_FAIL = false;

    public static final DisconnectedBehavior DEFAULT_DISCONNECTED_BEHAVIOR = DisconnectedBehavior.DEFAULT;

    public static final ReauthenticateBehavior DEFAULT_REAUTHENTICATE_BEHAVIOUR = ReauthenticateBehavior.DEFAULT;

    public static final boolean DEFAULT_PUBLISH_ON_SCHEDULER = false;

    public static final boolean DEFAULT_PING_BEFORE_ACTIVATE_CONNECTION = true;

    public static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = ProtocolVersion.newestSupported();

    public static final ReadOnlyCommands.ReadOnlyPredicate DEFAULT_READ_ONLY_COMMANDS = ReadOnlyCommands.asPredicate();

    public static final int DEFAULT_REQUEST_QUEUE_SIZE = Integer.MAX_VALUE;

    public static final Charset DEFAULT_SCRIPT_CHARSET = StandardCharsets.UTF_8;

    public static final SocketOptions DEFAULT_SOCKET_OPTIONS = SocketOptions.create();

    public static final Mono<JsonParser> DEFAULT_JSON_PARSER = Mono.defer(() -> Mono.fromCallable(() -> {
        try {
            Iterator<JsonParser> services = ServiceLoader.load(JsonParser.class).iterator();
            return services.hasNext() ? services.next() : null;
        } catch (ServiceConfigurationError e) {
            throw new RedisJsonException("Could not load JsonParser, please consult the guide"
                    + "at https://redis.github.io/lettuce/user-guide/redis-json/", e);
        }
    }));

    public static final SslOptions DEFAULT_SSL_OPTIONS = SslOptions.create();

    public static final boolean DEFAULT_SUSPEND_RECONNECT_PROTO_FAIL = false;

    public static final TimeoutOptions DEFAULT_TIMEOUT_OPTIONS = TimeoutOptions.enabled();

    public static final boolean DEFAULT_USE_HASH_INDEX_QUEUE = true;

    private final boolean autoReconnect;

    private final Predicate<RedisCommand<?, ?, ?>> replayFilter;

    private final boolean cancelCommandsOnReconnectFailure;

    private final DecodeBufferPolicy decodeBufferPolicy;

    private final DisconnectedBehavior disconnectedBehavior;

    private final ReauthenticateBehavior reauthenticateBehavior;

    private final boolean publishOnScheduler;

    private final boolean pingBeforeActivateConnection;

    private final ProtocolVersion protocolVersion;

    private final ReadOnlyCommands.ReadOnlyPredicate readOnlyCommands;

    private final int requestQueueSize;

    private final Charset scriptCharset;

    private final Mono<JsonParser> jsonParser;

    private final SocketOptions socketOptions;

    private final SslOptions sslOptions;

    private final boolean suspendReconnectOnProtocolFailure;

    private final TimeoutOptions timeoutOptions;

    private final boolean useHashIndexedQueue;

    protected ClientOptions(Builder builder) {
        this.autoReconnect = builder.autoReconnect;
        this.replayFilter = builder.replayFilter;
        this.cancelCommandsOnReconnectFailure = builder.cancelCommandsOnReconnectFailure;
        this.decodeBufferPolicy = builder.decodeBufferPolicy;
        this.disconnectedBehavior = builder.disconnectedBehavior;
        this.reauthenticateBehavior = builder.reauthenticateBehavior;
        this.publishOnScheduler = builder.publishOnScheduler;
        this.pingBeforeActivateConnection = builder.pingBeforeActivateConnection;
        this.protocolVersion = builder.protocolVersion;
        this.readOnlyCommands = builder.readOnlyCommands;
        this.requestQueueSize = builder.requestQueueSize;
        this.scriptCharset = builder.scriptCharset;
        this.jsonParser = builder.jsonParser;
        this.socketOptions = builder.socketOptions;
        this.sslOptions = builder.sslOptions;
        this.suspendReconnectOnProtocolFailure = builder.suspendReconnectOnProtocolFailure;
        this.timeoutOptions = builder.timeoutOptions;
        this.useHashIndexedQueue = builder.useHashIndexedQueue;
    }

    protected ClientOptions(ClientOptions original) {
        this.autoReconnect = original.isAutoReconnect();
        this.replayFilter = original.getReplayFilter();
        this.cancelCommandsOnReconnectFailure = original.isCancelCommandsOnReconnectFailure();
        this.decodeBufferPolicy = original.getDecodeBufferPolicy();
        this.disconnectedBehavior = original.getDisconnectedBehavior();
        this.reauthenticateBehavior = original.getReauthenticateBehaviour();
        this.publishOnScheduler = original.isPublishOnScheduler();
        this.pingBeforeActivateConnection = original.isPingBeforeActivateConnection();
        this.protocolVersion = original.getConfiguredProtocolVersion();
        this.readOnlyCommands = original.getReadOnlyCommands();
        this.requestQueueSize = original.getRequestQueueSize();
        this.scriptCharset = original.getScriptCharset();
        this.jsonParser = original.getJsonParser();
        this.socketOptions = original.getSocketOptions();
        this.sslOptions = original.getSslOptions();
        this.suspendReconnectOnProtocolFailure = original.isSuspendReconnectOnProtocolFailure();
        this.timeoutOptions = original.getTimeoutOptions();
        this.useHashIndexedQueue = original.isUseHashIndexedQueue();
    }

    /**
     * Create a copy of {@literal options}
     *
     * @param options the original
     * @return A new instance of {@link ClientOptions} containing the values of {@literal options}
     */
    public static ClientOptions copyOf(ClientOptions options) {
        return new ClientOptions(options);
    }

    /**
     * Returns a new {@link ClientOptions.Builder} to construct {@link ClientOptions}.
     *
     * @return a new {@link ClientOptions.Builder} to construct {@link ClientOptions}.
     */
    public static ClientOptions.Builder builder() {
        return new ClientOptions.Builder();
    }

    /**
     * Create a new instance of {@link ClientOptions} with default settings.
     *
     * @return a new instance of {@link ClientOptions} with default settings
     */
    public static ClientOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link ClientOptions}.
     */
    public static class Builder {

        private boolean autoReconnect = DEFAULT_AUTO_RECONNECT;

        private Predicate<RedisCommand<?, ?, ?>> replayFilter = DEFAULT_REPLAY_FILTER;

        private boolean cancelCommandsOnReconnectFailure = DEFAULT_CANCEL_CMD_RECONNECT_FAIL;

        private DecodeBufferPolicy decodeBufferPolicy = DecodeBufferPolicies.ratio(DEFAULT_BUFFER_USAGE_RATIO);

        private DisconnectedBehavior disconnectedBehavior = DEFAULT_DISCONNECTED_BEHAVIOR;

        private boolean pingBeforeActivateConnection = DEFAULT_PING_BEFORE_ACTIVATE_CONNECTION;

        private ProtocolVersion protocolVersion;

        private boolean publishOnScheduler = DEFAULT_PUBLISH_ON_SCHEDULER;

        private ReadOnlyCommands.ReadOnlyPredicate readOnlyCommands = DEFAULT_READ_ONLY_COMMANDS;

        private int requestQueueSize = DEFAULT_REQUEST_QUEUE_SIZE;

        private Charset scriptCharset = DEFAULT_SCRIPT_CHARSET;

        private Mono<JsonParser> jsonParser = DEFAULT_JSON_PARSER;

        private SocketOptions socketOptions = DEFAULT_SOCKET_OPTIONS;

        private SslOptions sslOptions = DEFAULT_SSL_OPTIONS;

        private boolean suspendReconnectOnProtocolFailure = DEFAULT_SUSPEND_RECONNECT_PROTO_FAIL;

        private TimeoutOptions timeoutOptions = DEFAULT_TIMEOUT_OPTIONS;

        private ReauthenticateBehavior reauthenticateBehavior = DEFAULT_REAUTHENTICATE_BEHAVIOUR;

        private boolean useHashIndexedQueue = DEFAULT_USE_HASH_INDEX_QUEUE;

        protected Builder() {
        }

        /**
         * Enables or disables auto reconnection on connection loss. Defaults to {@code true}. See
         * {@link #DEFAULT_AUTO_RECONNECT}.
         *
         * @param autoReconnect true/false
         * @return {@code this}
         */
        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        /**
         * When {@link #autoReconnect(boolean)} is set to true, this {@link Predicate} is used to filter commands to replay when
         * the connection is reestablished after a disconnect. Returning <code>false</code> means the command will not be
         * filtered out and will be replayed. Defaults to replaying all queued commands.
         *
         * @param replayFilter a {@link Predicate} to filter commands to replay. Must not be {@code null}.
         * @see #DEFAULT_REPLAY_FILTER
         * @return {@code this}
         * @since 6.6
         */
        public Builder replayFilter(Predicate<RedisCommand<?, ?, ?>> replayFilter) {
            this.replayFilter = replayFilter;
            return this;
        }

        /**
         * Allows cancelling queued commands in case a reconnect fails.Defaults to {@code false}. See
         * {@link #DEFAULT_CANCEL_CMD_RECONNECT_FAIL}. <b>This flag is deprecated and should not be used as it can lead to race
         * conditions and protocol offsets. The reason is that it internally calls reset() which causes a protocol offset.</b>
         * See {@link StatefulConnection#reset}
         *
         * @param cancelCommandsOnReconnectFailure true/false
         * @return {@code this}
         * @deprecated since 6.2, to be removed with 7.0. This feature is unsafe and may cause protocol offsets if true (i.e.
         *             Redis commands are completed with previous command values).
         */
        @Deprecated
        public Builder cancelCommandsOnReconnectFailure(boolean cancelCommandsOnReconnectFailure) {
            this.cancelCommandsOnReconnectFailure = cancelCommandsOnReconnectFailure;
            return this;
        }

        /**
         * Buffer usage ratio for {@link io.lettuce.core.protocol.CommandHandler}. This ratio controls how often bytes are
         * discarded during decoding. In particular, when buffer usage reaches {@code bufferUsageRatio / bufferUsageRatio + 1}.
         * E.g. setting {@code bufferUsageRatio} to {@literal 3}, will discard read bytes once the buffer usage reaches 75
         * percent. See {@link #DEFAULT_BUFFER_USAGE_RATIO}.
         *
         * @param bufferUsageRatio the buffer usage ratio. Must be between {@code 0} and {@code 2^31-1}, typically a value
         *        between 1 and 10 representing 50% to 90%.
         * @return {@code this}
         * @since 5.2
         * @deprecated since 6.0 in favor of {@link DecodeBufferPolicy}.
         */
        @Deprecated
        public Builder bufferUsageRatio(int bufferUsageRatio) {
            this.decodeBufferPolicy = DecodeBufferPolicies.ratio(bufferUsageRatio);
            return this;
        }

        /**
         * Set the policy to discard read bytes from the decoding aggregation buffer to reclaim memory.
         *
         * @param policy the policy to use in {@link io.lettuce.core.protocol.CommandHandler}
         * @return {@code this}
         * @see DecodeBufferPolicies
         * @since 6.0
         */
        public Builder decodeBufferPolicy(DecodeBufferPolicy policy) {

            LettuceAssert.notNull(policy, "DecodeBufferPolicy must not be null");
            this.decodeBufferPolicy = policy;
            return this;
        }

        /**
         * Sets the behavior for command invocation when connections are in a disconnected state. Defaults to {@code true}. See
         * {@link #DEFAULT_DISCONNECTED_BEHAVIOR}.
         *
         * @param disconnectedBehavior must not be {@code null}.
         * @return {@code this}
         */
        public Builder disconnectedBehavior(DisconnectedBehavior disconnectedBehavior) {

            LettuceAssert.notNull(disconnectedBehavior, "DisconnectedBehavior must not be null");
            this.disconnectedBehavior = disconnectedBehavior;
            return this;
        }

        /**
         * Configure the {@link ReauthenticateBehavior} of the Lettuce driver. Defaults to
         * {@link ReauthenticateBehavior#DEFAULT}.
         *
         * @param reauthenticateBehavior the {@link ReauthenticateBehavior} to use. Must not be {@code null}.
         * @return {@code this}
         */
        public Builder reauthenticateBehavior(ReauthenticateBehavior reauthenticateBehavior) {

            LettuceAssert.notNull(reauthenticateBehavior, "ReuthenticatBehavior must not be null");
            this.reauthenticateBehavior = reauthenticateBehavior;
            return this;
        }

        /**
         * Perform a lightweight {@literal PING} connection handshake when establishing a Redis connection. If {@code true}
         * (default is {@code true}, {@link #DEFAULT_PING_BEFORE_ACTIVATE_CONNECTION}), every connection and reconnect will
         * issue a {@literal PING} command and await its response before the connection is activated and enabled for use. If the
         * check fails, the connect/reconnect is treated as a failure. This option has no effect unless forced to use the RESP 2
         * protocol version. RESP 3/protocol discovery performs a {@code HELLO} handshake.
         * <p>
         *
         * The {@literal PING} handshake validates whether the other end of the connected socket is a service that behaves like
         * a Redis server.
         *
         * @param pingBeforeActivateConnection true/false
         * @return {@code this}
         */
        public Builder pingBeforeActivateConnection(boolean pingBeforeActivateConnection) {
            this.pingBeforeActivateConnection = pingBeforeActivateConnection;
            return this;
        }

        /**
         * Sets the {@link ProtocolVersion} to use. Defaults to {@literal RESP3}. See {@link #DEFAULT_PROTOCOL_VERSION}.
         *
         * @param protocolVersion version to use.
         * @return {@code this}
         * @see ProtocolVersion#newestSupported()
         * @since 6.0
         */
        public Builder protocolVersion(ProtocolVersion protocolVersion) {

            this.protocolVersion = protocolVersion;
            return this;
        }

        /**
         * Use a dedicated {@link reactor.core.scheduler.Scheduler} to emit reactive data signals. Enabling this option can be
         * useful for reactive sequences that require a significant amount of processing with a single/a few Redis connections.
         * <p>
         * A single Redis connection operates on a single thread. Operations that require a significant amount of processing can
         * lead to a single-threaded-like behavior for all consumers of the Redis connection. When enabled, data signals will be
         * emitted using a different thread served by {@link ClientResources#eventExecutorGroup()}. Defaults to {@code false} ,
         * see {@link #DEFAULT_PUBLISH_ON_SCHEDULER}.
         *
         * @param publishOnScheduler true/false
         * @return {@code this}
         * @see org.reactivestreams.Subscriber#onNext(Object)
         * @see ClientResources#eventExecutorGroup()
         * @since 5.2
         */
        public Builder publishOnScheduler(boolean publishOnScheduler) {
            this.publishOnScheduler = publishOnScheduler;
            return this;
        }

        /**
         * Identifies commands (e.g. module commands) as read-only. Defaults {@link #DEFAULT_READ_ONLY_COMMANDS}, see
         * {@link ReadOnlyCommands}.
         *
         * @param readOnlyCommands must not be {@code null}.
         * @return {@code this}
         * @see 6.2.4
         */
        public Builder readOnlyCommands(ReadOnlyCommands.ReadOnlyPredicate readOnlyCommands) {

            LettuceAssert.notNull(readOnlyCommands, "readOnlyCommands must not be null");
            this.readOnlyCommands = readOnlyCommands;
            return this;
        }

        /**
         * Set the per-connection request queue size. The command invocation will lead to a {@link RedisException} if the queue
         * size is exceeded. Setting the {@code requestQueueSize} to a lower value will lead earlier to exceptions during
         * overload or while the connection is in a disconnected state. A higher value means hitting the boundary will take
         * longer to occur, but more requests will potentially be queued up and more heap space is used. Defaults to
         * {@link Integer#MAX_VALUE}. See {@link #DEFAULT_REQUEST_QUEUE_SIZE}.
         *
         * @param requestQueueSize the queue size.
         * @return {@code this}
         */
        public Builder requestQueueSize(int requestQueueSize) {
            this.requestQueueSize = requestQueueSize;
            return this;
        }

        /**
         * Sets the Lua script {@link Charset} to use to encode {@link String scripts} to {@code byte[]}. Defaults to
         * {@link StandardCharsets#UTF_8}. See {@link #DEFAULT_SCRIPT_CHARSET}.
         *
         * @param scriptCharset must not be {@code null}.
         * @return {@code this}
         * @since 6.0
         */
        public Builder scriptCharset(Charset scriptCharset) {

            LettuceAssert.notNull(scriptCharset, "ScriptCharset must not be null");
            this.scriptCharset = scriptCharset;
            return this;
        }

        /**
         * Set a custom implementation for the {@link JsonParser} to use.
         *
         * @param parser a {@link Mono} that emits the {@link JsonParser} to use.
         * @return {@code this}
         * @see JsonParser
         * @since 6.5
         */
        public Builder jsonParser(Mono<JsonParser> parser) {

            LettuceAssert.notNull(parser, "JsonParser must not be null");
            this.jsonParser = parser;
            return this;
        }

        /**
         * Sets the low-level {@link SocketOptions} for the connections kept to Redis servers. See
         * {@link #DEFAULT_SOCKET_OPTIONS}.
         *
         * @param socketOptions must not be {@code null}.
         * @return {@code this}
         */
        public Builder socketOptions(SocketOptions socketOptions) {

            LettuceAssert.notNull(socketOptions, "SocketOptions must not be null");
            this.socketOptions = socketOptions;
            return this;
        }

        /**
         * Sets the {@link SslOptions} for SSL connections kept to Redis servers. See {@link #DEFAULT_SSL_OPTIONS}.
         *
         * @param sslOptions must not be {@code null}.
         * @return {@code this}
         */
        public Builder sslOptions(SslOptions sslOptions) {

            LettuceAssert.notNull(sslOptions, "SslOptions must not be null");
            this.sslOptions = sslOptions;
            return this;
        }

        /**
         * Suspends reconnect when reconnects run into protocol failures (SSL verification, PING before connect fails). Defaults
         * to {@code false}. See {@link #DEFAULT_SUSPEND_RECONNECT_PROTO_FAIL}.
         *
         * @param suspendReconnectOnProtocolFailure true/false
         * @return {@code this}
         */
        public Builder suspendReconnectOnProtocolFailure(boolean suspendReconnectOnProtocolFailure) {
            this.suspendReconnectOnProtocolFailure = suspendReconnectOnProtocolFailure;
            return this;
        }

        /**
         * Sets the {@link TimeoutOptions} to expire and cancel commands. See {@link #DEFAULT_TIMEOUT_OPTIONS}.
         *
         * @param timeoutOptions must not be {@code null}.
         * @return {@code this}
         * @since 5.1
         */
        public Builder timeoutOptions(TimeoutOptions timeoutOptions) {

            LettuceAssert.notNull(timeoutOptions, "TimeoutOptions must not be null");
            this.timeoutOptions = timeoutOptions;
            return this;
        }

        /**
         * Use hash indexed queue, which provides O(1) remove(Object) thus won't cause blocking issues.
         *
         * @param useHashIndexedQueue true/false
         * @return {@code this}
         * @see io.lettuce.core.protocol.CommandHandler.AddToStack
         * @since 6.6
         */
        @SuppressWarnings("JavadocReference")
        public Builder useHashIndexQueue(boolean useHashIndexedQueue) {
            this.useHashIndexedQueue = useHashIndexedQueue;
            return this;
        }

        /**
         * Create a new instance of {@link ClientOptions}.
         *
         * @return new instance of {@link ClientOptions}
         */
        public ClientOptions build() {
            return new ClientOptions(this);
        }

    }

    /**
     * Returns a builder to create new {@link ClientOptions} whose settings are replicated from the current
     * {@link ClientOptions}.
     *
     * @return a {@link ClientOptions.Builder} to create new {@link ClientOptions} whose settings are replicated from the
     *         current {@link ClientOptions}.
     * @since 5.1
     */
    public ClientOptions.Builder mutate() {
        Builder builder = new Builder();

        builder.autoReconnect(isAutoReconnect()).cancelCommandsOnReconnectFailure(isCancelCommandsOnReconnectFailure())
                .replayFilter(getReplayFilter()).decodeBufferPolicy(getDecodeBufferPolicy())
                .disconnectedBehavior(getDisconnectedBehavior()).reauthenticateBehavior(getReauthenticateBehaviour())
                .readOnlyCommands(getReadOnlyCommands()).publishOnScheduler(isPublishOnScheduler())
                .pingBeforeActivateConnection(isPingBeforeActivateConnection()).protocolVersion(getConfiguredProtocolVersion())
                .requestQueueSize(getRequestQueueSize()).scriptCharset(getScriptCharset()).jsonParser(getJsonParser())
                .socketOptions(getSocketOptions()).sslOptions(getSslOptions())
                .suspendReconnectOnProtocolFailure(isSuspendReconnectOnProtocolFailure()).timeoutOptions(getTimeoutOptions());

        return builder;
    }

    /**
     * Controls auto-reconnect behavior on connections. If auto-reconnect is {@code true} (default), it is enabled. As soon as a
     * connection gets closed/reset without the intention to close it, the client will try to reconnect and re-issue any queued
     * commands.
     *
     * This flag has also the effect that disconnected connections will refuse commands and cancel these with an exception.
     *
     * @return {@code true} if auto-reconnect is enabled.
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /**
     * Controls which {@link RedisCommand} will be replayed after a re-connect. The {@link Predicate} returns <code>true</code>
     * if command should be filtered out and not replayed. Defaults to {@link #DEFAULT_REPLAY_FILTER}.
     * 
     * @return the currently set {@link Predicate} used to filter out commands to replay
     */
    public Predicate<RedisCommand<?, ?, ?>> getReplayFilter() {
        return replayFilter;
    }

    /**
     * If this flag is {@code true} any queued commands will be canceled when a reconnect fails within the activation sequence.
     * Default is {@code false}.
     *
     * @return {@code true} if commands should be cancelled on reconnect failures.
     * @deprecated since 6.2, to be removed with 7.0. See {@link Builder#cancelCommandsOnReconnectFailure(boolean)}.
     */
    @Deprecated
    public boolean isCancelCommandsOnReconnectFailure() {
        return cancelCommandsOnReconnectFailure;
    }

    /**
     * Returns the {@link DecodeBufferPolicy} used to reclaim memory.
     *
     * @return the {@link DecodeBufferPolicy}.
     * @since 6.0
     */
    public DecodeBufferPolicy getDecodeBufferPolicy() {
        return decodeBufferPolicy;
    }

    /**
     * Buffer usage ratio for {@link io.lettuce.core.protocol.CommandHandler}. This ratio controls how often bytes are discarded
     * during decoding. In particular, when buffer usage reaches {@code bufferUsageRatio / bufferUsageRatio + 1}. E.g. setting
     * {@code bufferUsageRatio} to {@literal 3}, will discard read bytes once the buffer usage reaches 75 percent.
     *
     * @return zero.
     * @since 5.2
     * @deprecated since 6.0 in favor of {@link DecodeBufferPolicy}.
     */
    @Deprecated
    public int getBufferUsageRatio() {
        return 0;
    }

    /**
     * Behavior for command invocation when connections are in a disconnected state. Defaults to
     * {@link DisconnectedBehavior#DEFAULT true}. See {@link #DEFAULT_DISCONNECTED_BEHAVIOR}.
     *
     * @return the behavior for command invocation when connections are in a disconnected state
     */
    public DisconnectedBehavior getDisconnectedBehavior() {
        return disconnectedBehavior;
    }

    /**
     * Behavior for re-authentication when the {@link RedisCredentialsProvider} emits new credentials. Defaults to
     * {@link ReauthenticateBehavior#DEFAULT}.
     *
     * @return the currently set {@link ReauthenticateBehavior}.
     */
    public ReauthenticateBehavior getReauthenticateBehaviour() {
        return reauthenticateBehavior;
    }

    /**
     * Predicate to identify commands as read-only. Defaults to {@link #DEFAULT_READ_ONLY_COMMANDS}.
     *
     * @return the predicate to identify read-only commands.
     */
    public ReadOnlyCommands.ReadOnlyPredicate getReadOnlyCommands() {
        return readOnlyCommands;
    }

    /**
     * Request queue size for a connection. This value applies per connection. The command invocation will throw a
     * {@link RedisException} if the queue size is exceeded and a new command is requested. Defaults to
     * {@link Integer#MAX_VALUE}.
     *
     * @return the request queue size.
     */
    public int getRequestQueueSize() {
        return requestQueueSize;
    }

    /**
     * Perform a lightweight {@literal PING} connection handshake when establishing a Redis connection. If {@code true} (default
     * is {@code true}), every connection and reconnect will issue a {@literal PING} command and await its response before the
     * connection is activated and enabled for use. If the check fails, the connect/reconnect is treated as a failure. This
     * option has no effect unless forced to use the RESP 2 protocol version. RESP 3/protocol discovery performs a {@code HELLO}
     * handshake.
     * <p>
     * The {@literal PING} handshake validates whether the other end of the connected socket is a service that behaves like a
     * Redis server.
     *
     * @return {@code true} if {@literal PING} handshake is enabled.
     */
    public boolean isPingBeforeActivateConnection() {
        return pingBeforeActivateConnection;
    }

    /**
     * Returns the {@link ProtocolVersion} to use.
     *
     * @return the {@link ProtocolVersion} to use.
     */
    public ProtocolVersion getProtocolVersion() {

        ProtocolVersion protocolVersion = getConfiguredProtocolVersion();
        return protocolVersion == null ? DEFAULT_PROTOCOL_VERSION : protocolVersion;
    }

    /**
     * Returns the configured {@link ProtocolVersion}. May return {@code null} if unconfigured.
     *
     * @return the {@link ProtocolVersion} to use. May be {@code null}.
     * @since 6.0
     */
    public ProtocolVersion getConfiguredProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Use a dedicated {@link reactor.core.scheduler.Scheduler} to emit reactive data signals. Enabling this option can be
     * useful for reactive sequences that require a significant amount of processing with a single/a few Redis connections.
     * <p>
     * A single Redis connection operates on a single thread. Operations that require a significant amount of processing can
     * lead to a single-threaded-like behavior for all consumers of the Redis connection. When enabled, data signals will be
     * emitted using a different thread served by {@link ClientResources#eventExecutorGroup()}. Defaults to {@code false} , see
     * {@link #DEFAULT_PUBLISH_ON_SCHEDULER}.
     *
     * @return {@code true} to use a dedicated {@link reactor.core.scheduler.Scheduler}
     * @since 5.2
     */
    public boolean isPublishOnScheduler() {
        return publishOnScheduler;
    }

    /**
     * If this flag is {@code true}, the reconnect will be suspended on protocol errors. Protocol errors are errors while SSL
     * negotiation or when PING before connect fails.
     *
     * @return {@code true} if reconnect will be suspended on protocol errors.
     */
    public boolean isSuspendReconnectOnProtocolFailure() {
        return suspendReconnectOnProtocolFailure;
    }

    /**
     * Returns the Lua script {@link Charset}.
     *
     * @return the script {@link Charset}.
     * @since 6.0
     */
    public Charset getScriptCharset() {
        return scriptCharset;
    }

    /**
     * Returns the currently set up {@link JsonParser}.
     * 
     * @return the implementation of the {@link JsonParser} to use.
     * @since 6.5
     */
    public Mono<JsonParser> getJsonParser() {
        return jsonParser;
    }

    /**
     * Returns the {@link SocketOptions}.
     *
     * @return the {@link SocketOptions}.
     */
    public SocketOptions getSocketOptions() {
        return socketOptions;
    }

    /**
     * Returns the {@link SslOptions}.
     *
     * @return the {@link SslOptions}.
     */
    public SslOptions getSslOptions() {
        return sslOptions;
    }

    /**
     * Returns the {@link TimeoutOptions}.
     *
     * @return the {@link TimeoutOptions}.
     * @since 5.1
     */
    public TimeoutOptions getTimeoutOptions() {
        return timeoutOptions;
    }

    /**
     * Defines the re-authentication behavior of the Redis client.
     * <p/>
     * Certain implementations of the {@link RedisCredentialsProvider} could emit new credentials at runtime. This setting
     * controls how the driver reacts to these newly emitted credentials.
     */
    public enum ReauthenticateBehavior {

        /**
         * This is the default behavior. The client will fetch current credentials from the underlying
         * {@link RedisCredentialsProvider} only when the driver needs to, e.g. when the connection is first established or when
         * it is re-established after a disconnect.
         * <p/>
         * <p>
         * No re-authentication is performed when new credentials are emitted by a {@link RedisCredentialsProvider} that
         * supports streaming. The client does not subscribe to or react to any updates in the credential stream provided by
         * {@link RedisCredentialsProvider#credentials()}.
         * </p>
         */
        DEFAULT,

        /**
         * Automatically triggers re-authentication whenever new credentials are emitted by a {@link RedisCredentialsProvider}
         * that supports streaming, as indicated by {@link RedisCredentialsProvider#supportsStreaming()}.
         *
         * <p>
         * When this behavior is enabled, the client subscribes to the credential stream provided by
         * {@link RedisCredentialsProvider#credentials()} and issues an {@code AUTH} command to the Redis server each time new
         * credentials are received. This behavior supports dynamic credential scenarios, such as token-based authentication, or
         * credential rotation where credentials are refreshed periodically to maintain access.
         * </p>
         *
         * <p>
         * Note: {@code AUTH} commands issued as part of this behavior may interleave with user-submitted commands, as the
         * client performs re-authentication independently of user command flow.
         * </p>
         */
        ON_NEW_CREDENTIALS
    }

    /**
     * Whether we should use hash indexed queue, which provides O(1) remove(Object)
     *
     * @return if hash indexed queue should be used
     */
    public boolean isUseHashIndexedQueue() {
        return useHashIndexedQueue;
    }

    /**
     * Behavior of connections in disconnected state.
     */
    public enum DisconnectedBehavior {

        /**
         * Accept commands when auto-reconnect is enabled, reject commands when auto-reconnect is disabled.
         */
        DEFAULT,

        /**
         * Accept commands in disconnected state.
         */
        ACCEPT_COMMANDS,

        /**
         * Reject commands in disconnected state.
         */
        REJECT_COMMANDS,
    }

}
