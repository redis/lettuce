package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkState;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.protocol.CommandEncoder;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Connection builder for connections. This class is part of the internal API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 02.02.15 09:40
 */
public class ConnectionBuilder {

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

    public static ConnectionBuilder connectionBuilder() {
        return new ConnectionBuilder();
    }

    public ConnectionBuilder socketAddressSupplier(Supplier<SocketAddress> socketAddressSupplier) {
        this.socketAddressSupplier = socketAddressSupplier;
        return this;
    }

    public SocketAddress socketAddress() {
        checkState(socketAddressSupplier != null, "socketAddressSupplier must be set");
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

    protected List<ChannelHandler> buildHandlers() {
        checkState(channelGroup != null, "channelGroup must be set");
        checkState(connectionEvents != null, "connectionEvents must be set");
        checkState(connection != null, "connection must be set");
        checkState(clientResources != null, "clientResources must be set");

        List<ChannelHandler> handlers = Lists.newArrayList();
        if (clientOptions.isAutoReconnect()) {
            checkState(bootstrap != null, "bootstrap must be set for autoReconnect=true");
            checkState(timer != null, "timer must be set for autoReconnect=true");
            checkState(socketAddressSupplier != null, "socketAddressSupplier must be set for autoReconnect=true");

            ConnectionWatchdog watchdog = new ConnectionWatchdog(clientOptions, bootstrap, timer, workerPool,
                    socketAddressSupplier);

            watchdog.setListenOnChannelInactive(true);
            handlers.add(watchdog);
        }

        connection.setOptions(clientOptions);

        handlers.add(new ChannelGroupListener(channelGroup));
        handlers.add(new CommandEncoder());
        handlers.add(commandHandler);
        handlers.add(connection);
        handlers.add(new ConnectionEventTrigger(connectionEvents, connection, clientResources.eventBus()));

        return handlers;

    }

    public RedisChannelInitializer build() {
        return new PlainChannelInitializer(clientOptions.isPingBeforeActivateConnection(), buildHandlers(),
                clientResources.eventBus());
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

}
