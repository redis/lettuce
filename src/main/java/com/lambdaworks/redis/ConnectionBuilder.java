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
package com.lambdaworks.redis;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.*;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;

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
    private CommandHandler<?, ?> commandHandler;
    private ChannelGroup channelGroup;
    private Timer timer;
    private Bootstrap bootstrap;
    private ClientOptions clientOptions;
    private EventExecutorGroup workerPool;
    private long timeout;
    private TimeUnit timeUnit;
    private ClientResources clientResources;
    private char[] password;
    private ReconnectionListener reconnectionListener = ReconnectionListener.NO_OP;
    private Supplier<AsyncCommand<?, ?, ?>> pingCommandSupplier = PlainChannelInitializer.NO_PING;

    public static ConnectionBuilder connectionBuilder() {
        return new ConnectionBuilder();
    }

    protected List<ChannelHandler> buildHandlers() {

        LettuceAssert.assertState(channelGroup != null, "ChannelGroup must be set");
        LettuceAssert.assertState(connectionEvents != null, "ConnectionEvents must be set");
        LettuceAssert.assertState(connection != null, "Connection must be set");
        LettuceAssert.assertState(clientResources != null, "ClientResources must be set");

        List<ChannelHandler> handlers = new ArrayList<>();

        connection.setOptions(clientOptions);

        handlers.add(new ChannelGroupListener(channelGroup));
        handlers.add(new CommandEncoder());
        handlers.add(commandHandler);
        handlers.add(connection);
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

        LettuceAssert.assertState(bootstrap != null, "Bootstrap must be set for autoReconnect=true");
        LettuceAssert.assertState(timer != null, "Timer must be set for autoReconnect=true");
        LettuceAssert.assertState(socketAddressSupplier != null, "SocketAddressSupplier must be set for autoReconnect=true");

        ConnectionWatchdog watchdog = new ConnectionWatchdog(clientResources.reconnectDelay(), clientOptions, bootstrap, timer,
                workerPool, socketAddressSupplier, reconnectionListener);

        watchdog.setListenOnChannelInactive(true);
        return watchdog;
    }

    public RedisChannelInitializer build() {
        return new PlainChannelInitializer(pingCommandSupplier, buildHandlers(), clientResources, timeout, timeUnit);
    }

    public ConnectionBuilder socketAddressSupplier(Supplier<SocketAddress> socketAddressSupplier) {
        this.socketAddressSupplier = socketAddressSupplier;
        return this;
    }

    public SocketAddress socketAddress() {
        LettuceAssert.assertState(socketAddressSupplier != null, "SocketAddressSupplier must be set");
        return socketAddressSupplier.get();
    }

    public ConnectionBuilder timeout(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        return this;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
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

    public ConnectionBuilder workerPool(EventExecutorGroup workerPool) {
        this.workerPool = workerPool;
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

    public ConnectionBuilder commandHandler(CommandHandler<?, ?> commandHandler) {
        this.commandHandler = commandHandler;
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

    public CommandHandler<?, ?> commandHandler() {
        return commandHandler;
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

    public EventExecutorGroup workerPool() {
        return workerPool;
    }

    Supplier<AsyncCommand<?, ?, ?>> getPingCommandSupplier() {
        return pingCommandSupplier;
    }
}
