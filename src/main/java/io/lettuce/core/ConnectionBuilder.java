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
package io.lettuce.core;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;

/**
 * Connection builder for connections. This class is part of the internal API.
 *
 * @author Mark Paluch
 */
public class ConnectionBuilder {

    private Mono<SocketAddress> socketAddressSupplier;

    private ConnectionEvents connectionEvents;

    private RedisChannelHandler<?, ?> connection;

    private Endpoint endpoint;

    private Supplier<CommandHandler> commandHandlerSupplier;

    private ChannelGroup channelGroup;

    private Bootstrap bootstrap;

    private ClientOptions clientOptions;

    private Duration timeout;

    private ClientResources clientResources;

    private ConnectionInitializer connectionInitializer;

    private ReconnectionListener reconnectionListener = ReconnectionListener.NO_OP;

    private ConnectionWatchdog connectionWatchdog;

    public static ConnectionBuilder connectionBuilder() {
        return new ConnectionBuilder();
    }

    /**
     * Apply settings from {@link RedisURI}
     *
     * @param redisURI
     */
    public void apply(RedisURI redisURI) {
        timeout(redisURI.getTimeout());
    }

    protected List<ChannelHandler> buildHandlers() {

        LettuceAssert.assertState(channelGroup != null, "ChannelGroup must be set");
        LettuceAssert.assertState(connectionEvents != null, "ConnectionEvents must be set");
        LettuceAssert.assertState(connection != null, "Connection must be set");
        LettuceAssert.assertState(clientResources != null, "ClientResources must be set");
        LettuceAssert.assertState(endpoint != null, "Endpoint must be set");
        LettuceAssert.assertState(connectionInitializer != null, "ConnectionInitializer must be set");

        List<ChannelHandler> handlers = new ArrayList<>();

        connection.setOptions(clientOptions);

        handlers.add(new ChannelGroupListener(channelGroup, clientResources.eventBus()));
        handlers.add(new CommandEncoder());
        handlers.add(getHandshakeHandler());
        handlers.add(commandHandlerSupplier.get());

        handlers.add(new ConnectionEventTrigger(connectionEvents, connection, clientResources.eventBus()));

        if (clientOptions.isAutoReconnect()) {
            handlers.add(createConnectionWatchdog());
        }

        return handlers;
    }

    protected ChannelHandler getHandshakeHandler() {
        return new RedisHandshakeHandler(connectionInitializer, clientResources, timeout);
    }

    protected ConnectionWatchdog createConnectionWatchdog() {

        if (connectionWatchdog != null) {
            return connectionWatchdog;
        }

        LettuceAssert.assertState(bootstrap != null, "Bootstrap must be set for autoReconnect=true");
        LettuceAssert.assertState(socketAddressSupplier != null, "SocketAddressSupplier must be set for autoReconnect=true");

        ConnectionWatchdog watchdog = new ConnectionWatchdog(clientResources.reconnectDelay(), clientOptions, bootstrap,
                clientResources.timer(), clientResources.eventExecutorGroup(), socketAddressSupplier, reconnectionListener,
                connection, clientResources.eventBus());

        endpoint.registerConnectionWatchdog(watchdog);

        connectionWatchdog = watchdog;
        return watchdog;
    }

    public ChannelInitializer<Channel> build(SocketAddress socketAddress) {
        return new PlainChannelInitializer(this::buildHandlers, clientResources);
    }

    public ConnectionBuilder socketAddressSupplier(Mono<SocketAddress> socketAddressSupplier) {
        this.socketAddressSupplier = socketAddressSupplier;
        return this;
    }

    public Mono<SocketAddress> socketAddress() {
        LettuceAssert.assertState(socketAddressSupplier != null, "SocketAddressSupplier must be set");
        return socketAddressSupplier;
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

    public ConnectionBuilder bootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public ConnectionBuilder endpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ConnectionBuilder clientResources(ClientResources clientResources) {
        this.clientResources = clientResources;
        return this;
    }

    public ConnectionBuilder connectionInitializer(ConnectionInitializer connectionInitializer) {
        this.connectionInitializer = connectionInitializer;
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

    public Endpoint endpoint() {
        return endpoint;
    }

    static class PlainChannelInitializer extends ChannelInitializer<Channel> {

        private final Supplier<List<ChannelHandler>> handlers;

        private final ClientResources clientResources;

        PlainChannelInitializer(Supplier<List<ChannelHandler>> handlers, ClientResources clientResources) {
            this.handlers = handlers;
            this.clientResources = clientResources;
        }

        @Override
        protected void initChannel(Channel channel) {
            doInitialize(channel);
        }

        private void doInitialize(Channel channel) {

            for (ChannelHandler handler : handlers.get()) {
                channel.pipeline().addLast(handler);
            }

            clientResources.nettyCustomizer().afterChannelInitialized(channel);
        }

    }

}
