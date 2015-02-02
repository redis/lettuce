package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public abstract class AbstractRedisClient {
    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisClient.class);

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1,
                SystemPropertyUtil.getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 4));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }

    protected EventLoopGroup eventLoopGroup;

    protected HashedWheelTimer timer;
    protected ChannelGroup channels;
    protected long timeout = 60;
    protected TimeUnit unit;
    protected ConnectionEvents connectionEvents = new ConnectionEvents();
    protected Set<Closeable> closeableResources = Sets.newConcurrentHashSet();

    protected AbstractRedisClient() {
        timer = new HashedWheelTimer();
        eventLoopGroup = new NioEventLoopGroup(DEFAULT_EVENT_LOOP_THREADS);
        channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        timer.start();
        unit = TimeUnit.SECONDS;
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

        ConnectionBuilder connectionBuilder = ConnectionBuilder.connectionBuilder();
        connectionBuilder(handler, connection, socketAddressSupplier, withReconnect, connectionBuilder, null);
        return (T) connectAsyncImpl(connectionBuilder);
    }

    /**
     * Populate connection builder with necessary resources.
     * 
     * @param handler
     * @param connection
     * @param socketAddressSupplier
     * @param withReconnect
     * @param connectionBuilder
     */
    protected void connectionBuilder(CommandHandler<?, ?> handler, RedisChannelHandler<?, ?> connection,
            Supplier<SocketAddress> socketAddressSupplier, boolean withReconnect, ConnectionBuilder connectionBuilder,
            RedisURI redisURI) {

        Bootstrap redisBootstrap = new Bootstrap().channel(NioSocketChannel.class).group(eventLoopGroup);

        if (redisURI == null) {
            redisBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) unit.toMillis(timeout));
        } else {
            redisBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) redisURI.getUnit()
                    .toMillis(redisURI.getTimeout()));
        }
        connectionBuilder.bootstrap(redisBootstrap).withReconnect(withReconnect);
        connectionBuilder.channelGroup(channels).connectionEvents(connectionEvents).timer(timer);
        connectionBuilder.commandHandler(handler).socketAddressSupplier(socketAddressSupplier).connection(connection);
    }

    protected <K, V, T extends RedisAsyncConnectionImpl<K, V>> T connectAsyncImpl(ConnectionBuilder connectionBuilder) {

        RedisChannelHandler<?, ?> connection = connectionBuilder.connection();
        try {

            SocketAddress redisAddress = connectionBuilder.socketAddress();

            logger.debug("Connecting to Redis, address: " + redisAddress);

            Bootstrap redisBootstrap = connectionBuilder.bootstrap();
            RedisChannelInitializer initializer = connectionBuilder.build();
            redisBootstrap.handler(initializer);
            ChannelFuture future = redisBootstrap.connect(redisAddress);

            future.await();

            if (!future.isSuccess()) {
                if (future.cause() instanceof Exception) {
                    throw (Exception) future.cause();
                }
                future.get();
            }

            initializer.channelInitialized().get(timeout, unit);
            connection.registerCloseables(closeableResources, connection, connectionBuilder.commandHandler());

            return (T) connection;
        } catch (RedisException e) {
            connection.close();
            throw e;
        } catch (Exception e) {
            connection.close();
            throw new RedisConnectionException("Unable to connect", e);
        } finally {
        }
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown. The shutdown
     * has 2 secs quiet time and a timeout of 15 secs.
     */
    public void shutdown() {
        shutdown(2, 15, TimeUnit.SECONDS);
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown.
     * 
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout the maximum amount of time to wait until the executor is shutdown regardless if a task was submitted
     *        during the quiet period
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}
     */
    public void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {

        while (!closeableResources.isEmpty()) {
            Closeable closeableResource = closeableResources.iterator().next();
            try {
                closeableResource.close();
            } catch (Exception e) {
                logger.debug("Exception on Close: " + e.getMessage(), e);
            }
            closeableResources.remove(closeableResource);
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
        Future<?> groupCloseFuture = eventLoopGroup.shutdownGracefully(quietPeriod, timeout, timeUnit);
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

    protected static <K, V> Object syncHandler(RedisChannelHandler<K, V> connection, Class<?>... interfaceClasses) {
        FutureSyncInvocationHandler<K, V> h = new FutureSyncInvocationHandler<K, V>(connection);
        return Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(), interfaceClasses, h);
    }

    /**
     * Add a listener for the RedisConnectionState. The listener is notified every time a connect/disconnect/IO exception
     * happens. The listeners are not bound to a specific connection, so every time a connection event happens on any
     * connection, the listener will be notified. The corresponding netty channel handler (async connection) is passed on the
     * event.
     * 
     * @param listener must not be {@literal null}
     */
    public void addListener(RedisConnectionStateListener listener) {
        checkArgument(listener != null, "RedisConnectionStateListener must not be null");
        connectionEvents.addListener(listener);
    }

    /**
     * Removes a listener.
     * 
     * @param listener must not be {@literal null

     */
    public void removeListener(RedisConnectionStateListener listener) {

        checkArgument(listener != null, "RedisConnectionStateListener must not be null");
        connectionEvents.removeListener(listener);
    }
}
