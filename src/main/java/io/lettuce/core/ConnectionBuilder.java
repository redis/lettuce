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
package io.lettuce.core;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;

/**
 * Connection builder for connections. This class is part of the internal API.
 *
 * @author Mark Paluch
 */
public class ConnectionBuilder {

    private static final RedisCommandBuilder<String, String> INITIALIZING_CMD_BUILDER = new RedisCommandBuilder<>(
            new Utf8StringCodec());
    private static final Supplier<AsyncCommand<?, ?, ?>> PING_COMMAND_SUPPLIER = () -> new AsyncCommand<>(
            INITIALIZING_CMD_BUILDER.ping());

    private Supplier<SocketAddress> socketAddressSupplier;
    private ConnectionEvents connectionEvents;
    private RedisChannelHandler<?, ?> connection;
    private DefaultEndpoint endpoint;
    private Supplier<CommandHandler> commandHandlerSupplier;
    private ChannelGroup channelGroup;
    private Timer timer;
    private Bootstrap bootstrap;
    private ClientOptions clientOptions;
    private Duration timeout;
    private ClientResources clientResources;
    private char[] password;
    private ReconnectionListener reconnectionListener = ReconnectionListener.NO_OP;
    private ConnectionWatchdog connectionWatchdog;
    private Supplier<AsyncCommand<?, ?, ?>> pingCommandSupplier = PlainChannelInitializer.NO_PING;

    public static ConnectionBuilder connectionBuilder() {
        return new ConnectionBuilder();
    }

    protected List<ChannelHandler> buildHandlers() {

        LettuceAssert.assertState(channelGroup != null, "ChannelGroup must be set");
        LettuceAssert.assertState(connectionEvents != null, "ConnectionEvents must be set");
        LettuceAssert.assertState(connection != null, "Connection must be set");
        LettuceAssert.assertState(clientResources != null, "ClientResources must be set");
        LettuceAssert.assertState(endpoint != null, "Endpoint must be set");

        List<ChannelHandler> handlers = new ArrayList<>();

        connection.setOptions(clientOptions);

        handlers.add(new ChannelGroupListener(channelGroup));
        handlers.add(new CommandEncoder());
        handlers.add(commandHandlerSupplier.get());

        if (clientOptions.isAutoReconnect()) {
            handlers.add(createConnectionWatchdog());
        }

        handlers.add(new ConnectionEventTrigger(connectionEvents, connection, clientResources.eventBus()));

        if (clientOptions.isAutoReconnect()) {
            handlers.add(createConnectionWatchdog());
        }

        return handlers;
    }

    public void enablePingBeforeConnect() {
        pingCommandSupplier = PING_COMMAND_SUPPLIER;
    }

    public void enableAuthPingBeforeConnect() {
        pingCommandSupplier = () -> new AsyncCommand<>(INITIALIZING_CMD_BUILDER.auth(new String(password)));
    }

    protected ConnectionWatchdog createConnectionWatchdog() {

        if (connectionWatchdog != null) {
            return connectionWatchdog;
        }

        LettuceAssert.assertState(bootstrap != null, "Bootstrap must be set for autoReconnect=true");
        LettuceAssert.assertState(timer != null, "Timer must be set for autoReconnect=true");
        LettuceAssert.assertState(socketAddressSupplier != null, "SocketAddressSupplier must be set for autoReconnect=true");

        ConnectionWatchdog watchdog = new ConnectionWatchdog(clientResources.reconnectDelay(), clientOptions, bootstrap, timer,
                clientResources.eventExecutorGroup(), socketAddressSupplier, reconnectionListener, connection);

        endpoint.registerConnectionWatchdog(watchdog);

        connectionWatchdog = watchdog;
        return watchdog;
    }

    public RedisChannelInitializer build() {
        return new PlainChannelInitializer(pingCommandSupplier, this::buildHandlers, clientResources, timeout);
    }

    public ConnectionBuilder socketAddressSupplier(Supplier<SocketAddress> socketAddressSupplier) {
        this.socketAddressSupplier = socketAddressSupplier;
        return this;
    }

    public SocketAddress socketAddress() {
        LettuceAssert.assertState(socketAddressSupplier != null, "SocketAddressSupplier must be set");
        return socketAddressSupplier.get();
    }

    public ConnectionBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public ConnectionBuilder reconnectionListener(ReconnectionListener reconnectionListener) {

        LettuceAssert.notNull(reconnectionListener, "ReconnectionListener must not be null");
        this.reconnectionListener = reconnectionListener;
        return this;
    }

    public ConnectionBuilder clientOptions(ClientOptions clientOptions) {
        this.clientOptions = clientOptions;
        return this;
    }

    public ConnectionBuilder connectionEvents(ConnectionEvents connectionEvents) {
        this.connectionEvents = connectionEvents;
        return this;
    }

    public ConnectionBuilder connection(RedisChannelHandler<?, ?> connection) {
        this.connection = connection;
        return this;
    }

    public ConnectionBuilder channelGroup(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
        return this;
    }

    public ConnectionBuilder commandHandler(Supplier<CommandHandler> supplier) {
        this.commandHandlerSupplier = supplier;
        return this;
    }

    public ConnectionBuilder timer(Timer timer) {
        this.timer = timer;
        return this;
    }

    public ConnectionBuilder bootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public ConnectionBuilder endpoint(DefaultEndpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ConnectionBuilder clientResources(ClientResources clientResources) {
        this.clientResources = clientResources;
        return this;
    }

    public ConnectionBuilder password(char[] password) {
        this.password = password;
        return this;
    }

    public RedisChannelHandler<?, ?> connection() {
        return connection;
    }

    public Bootstrap bootstrap() {
        return bootstrap;
    }

    public ClientOptions clientOptions() {
        return clientOptions;
    }

    public ClientResources clientResources() {
        return clientResources;
    }

    public char[] password() {
        return password;
    }

    public DefaultEndpoint endpoint() {
        return endpoint;
    }

    Supplier<AsyncCommand<?, ?, ?>> getPingCommandSupplier() {
        return pingCommandSupplier;
    }
}
