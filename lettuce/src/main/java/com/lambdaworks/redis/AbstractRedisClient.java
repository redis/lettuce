package com.lambdaworks.redis;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.lambdaworks.redis.internal.ChannelGroupListener;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ConcurrentSet;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:28
 */
public abstract class AbstractRedisClient {
    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisClient.class);

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil
                .getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 4));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }

    protected EventLoopGroup eventLoopGroup;

    protected HashedWheelTimer timer;
    protected ChannelGroup channels;
    protected long timeout;
    protected TimeUnit unit;
    protected ConnectionEvents connectionEvents = new ConnectionEvents();
    protected Set<Closeable> closeableResources = new ConcurrentSet<Closeable>();

    public AbstractRedisClient() {
        timer = new HashedWheelTimer();
        eventLoopGroup = new NioEventLoopGroup(DEFAULT_EVENT_LOOP_THREADS);
        channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        timer.start();
    }

    /**
     * Set the default timeout for {@link com.lambdaworks.redis.RedisConnection connections} created by this client. The timeout
     * applies to connection attempts and non-blocking commands.
     * 
     * @param timeout Default connection timeout.
     * @param unit Unit of time for the timeout.
     */
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    protected <K, V, T extends RedisAsyncConnectionImpl<K, V>> T connectAsyncImpl(final CommandHandler<K, V> handler,
            final T connection, final Supplier<SocketAddress> socketAddressSupplier, final boolean withReconnect) {
        try {

            SocketAddress redisAddress = socketAddressSupplier.get();

            logger.debug("Connecting to Redis, address: " + redisAddress);

            final Bootstrap redisBootstrap = new Bootstrap().channel(NioSocketChannel.class).group(eventLoopGroup);
            redisBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) unit.toMillis(timeout));

            final ConnectionWatchdog watchdog = new ConnectionWatchdog(redisBootstrap, timer, socketAddressSupplier);

            redisBootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {

                    if (withReconnect) {
                        watchdog.setReconnect(true);
                        ch.pipeline().addLast(watchdog);
                    }

                    ch.pipeline().addLast(new ChannelGroupListener(channels),
                            new ConnectionEventTrigger(connectionEvents, connection), handler, connection);
                }
            });

            redisBootstrap.connect(redisAddress).get();

            connection.addListener(new CloseEvents.CloseListener() {
                @Override
                public void resourceClosed(Object resource) {
                    closeableResources.remove(resource);
                }
            });
            closeableResources.add(connection);

            return connection;
        } catch (Exception e) {
            throw new RedisException("Unable to connect", e);
        }
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown.
     */
    public void shutdown() {

        ImmutableList<Closeable> autoCloseables = ImmutableList.copyOf(closeableResources);
        for (Closeable closeableResource : autoCloseables) {
            try {
                closeableResource.close();
            } catch (Exception e) {
                logger.debug("Exception on Close: " + e.getMessage(), e);

            }
        }

        for (Channel c : channels) {
            ChannelPipeline pipeline = c.pipeline();

            CommandHandler<?, ?> commandHandler = pipeline.get(CommandHandler.class);
            if (commandHandler != null && !commandHandler.isClosed()) {
                commandHandler.close();
            }

            PubSubCommandHandler<?, ?> psCommandHandler = pipeline.get(PubSubCommandHandler.class);
            if (psCommandHandler != null && !psCommandHandler.isClosed()) {
                psCommandHandler.close();
            }
        }

        ChannelGroupFuture closeFuture = channels.close();
        Future<?> groupCloseFuture = eventLoopGroup.shutdownGracefully();
        try {
            closeFuture.get();
            groupCloseFuture.get();
        } catch (Exception e) {
            throw new RedisException(e);
        }
        timer.stop();
    }

    protected int getResourceCount() {
        return closeableResources.size();
    }

    protected int getChannelCount() {
        return channels.size();
    }

    /**
     * Add a listener for the RedisConnectionState. The listener is notified every time a connect/disconnect/IO exception
     * happens. The listeners are not bound to a specific connection, so every time a connection event happens on any
     * connection, the listener will be notified. The corresponding netty channel handler (async connection) is passed on the
     * event.
     * 
     * @param listener
     */
    public void addListener(RedisConnectionStateListener listener) {
        connectionEvents.addListener(listener);
    }

    /**
     * Removes a listener.
     * 
     * @param listener
     */
    public void removeListener(RedisConnectionStateListener listener) {
        connectionEvents.removeListener(listener);
    }
}
