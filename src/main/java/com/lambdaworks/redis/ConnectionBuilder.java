package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.*;

import java.net.SocketAddress;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 02.02.15 09:40
 */
class ConnectionBuilder {

    private Supplier<SocketAddress> socketAddressSupplier;
    private boolean withReconnect;
    private ConnectionEvents connectionEvents;
    private RedisChannelHandler<?, ?> connection;
    private CommandHandler<?, ?> commandHandler;
    private ChannelGroup channelGroup;
    private Timer timer;
    private Bootstrap bootstrap;

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

    public ConnectionBuilder withReconnect(boolean withReconnect) {
        this.withReconnect = withReconnect;
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

    protected List<ChannelHandler> buildHandlers() {
        checkState(channelGroup != null, "channelGroup must be set");
        checkState(connectionEvents != null, "connectionEvents must be set");
        checkState(connection != null, "channelGroup must be set");

        List<ChannelHandler> handlers = Lists.newArrayList();
        if (withReconnect) {
            checkState(bootstrap != null, "bootstrap must be set for withReconnect=true");
            checkState(timer != null, "timer must be set for withReconnect=true");
            checkState(socketAddressSupplier != null, "socketAddressSupplier must be set for withReconnect=true");

            ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, timer, socketAddressSupplier);

            watchdog.setReconnect(true);
            handlers.add(watchdog);
        }

        handlers.add(new ChannelGroupListener(channelGroup));
        handlers.add(new ConnectionEventTrigger(connectionEvents, connection));
        handlers.add(commandHandler);
        handlers.add(connection);

        return handlers;

    }

    public RedisChannelInitializer build() {

        return new PlainChannelInitializer(buildHandlers());
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
}
