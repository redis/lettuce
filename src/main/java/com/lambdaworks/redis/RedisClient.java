// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Supplier;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.RedisPubSubConnectionImpl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * A scalable thread-safe <a href="http://redis.io/">Redis</a> client. Multiple threads may share one connection provided they
 * avoid blocking and transactional operations such as BLPOP and MULTI/EXEC.
 * 
 * @author Will Glozer
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisClient extends AbstractRedisClient {

    private final RedisCodec<String, String> codec = new Utf8StringCodec();
    private final RedisURI redisURI;

    /**
     * Creates a uri-less RedisClient. You can connect to different redis servers but you must supply a {@link RedisURI} on
     * connecting. Methods without having a {@link RedisURI} will fail with a {@link java.lang.IllegalStateException}.
     */
    public RedisClient() {
        redisURI = null;
        setDefaultTimeout(60, TimeUnit.MINUTES);
    }

    /**
     * Create a new client that connects to the supplied host on the default port.
     * 
     * @param host Server hostname.
     */
    public RedisClient(String host) {
        this(host, RedisURI.DEFAULT_REDIS_PORT);
    }

    /**
     * Create a new client that connects to the supplied host and port. Connection attempts and non-blocking commands will
     * {@link #setDefaultTimeout timeout} after 60 seconds.
     * 
     * @param host Server hostname.
     * @param port Server port.
     */
    public RedisClient(String host, int port) {
        this(RedisURI.Builder.redis(host, port).build());
    }

    /**
     * Create a new client that connects to the supplied host and port. Connection attempts and non-blocking commands will
     * {@link #setDefaultTimeout timeout} after 60 seconds.
     * 
     * @param redisURI Redis URI.
     */
    public RedisClient(RedisURI redisURI) {
        super();
        this.redisURI = redisURI;
        setDefaultTimeout(redisURI.getTimeout(), redisURI.getUnit());
    }

    /**
     * Creates a connection pool for synchronous connections. 5 max idle connections and 20 max active connections. Please keep
     * in mind to free all collections and close the pool once you do not need it anymore.
     * 
     * @return The connection pool.
     */
    public RedisConnectionPool<RedisConnection<String, String>> pool() {
        return pool(5, 20);
    }

    /**
     * Creates a connection pool for synchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore.
     * 
     * @param maxIdle max idle connections (or min pool size)
     * @param maxActive max active connections.
     * @return The connection pool.
     */
    public RedisConnectionPool<RedisConnection<String, String>> pool(int maxIdle, int maxActive) {

        return pool(codec, maxIdle, maxActive);
    }

    /**
     * Creates a connection pool for synchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore.
     * 
     * @param codec
     * @param maxIdle
     * @param maxActive
     * @param <K> Key type.
     * @param <V> Value type.
     * @return RedisConnectionPool<RedisConnection<K, V>>
     */
    @SuppressWarnings("unchecked")
    public <K, V> RedisConnectionPool<RedisConnection<K, V>> pool(final RedisCodec<K, V> codec, int maxIdle, int maxActive) {

        checkForRedisURI();

        long maxWait = unit.convert(timeout, TimeUnit.MILLISECONDS);
        RedisConnectionPool<RedisConnection<K, V>> pool = new RedisConnectionPool<RedisConnection<K, V>>(
                new RedisConnectionProvider<RedisConnection<K, V>>() {
                    @Override
                    public RedisConnection<K, V> createConnection() {
                        return connect(codec, false, redisURI);
                    }

                    @Override
                    @SuppressWarnings("rawtypes")
                    public Class<? extends RedisConnection<K, V>> getComponentType() {
                        return (Class) RedisConnection.class;
                    }
                }, maxActive, maxIdle, maxWait);

        pool.addListener(new CloseEvents.CloseListener() {
            @Override
            public void resourceClosed(Object resource) {
                closeableResources.remove(resource);
            }
        });

        closeableResources.add(pool);

        return pool;
    }

    private void checkForRedisURI() {
        checkState(this.redisURI != null,
                "RedisURI is not available. Use RedisClient(Host), RedisClient(Host, Port) or RedisClient(RedisURI) to construct your client.");
    }

    /**
     * Creates a connection pool for asynchronous connections. 5 max idle connections and 20 max active connections. Please keep
     * in mind to free all collections and close the pool once you do not need it anymore.
     * 
     * @return The connection pool.
     */
    public RedisConnectionPool<RedisAsyncConnection<String, String>> asyncPool() {
        return asyncPool(5, 20);
    }

    /**
     * Creates a connection pool for asynchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore.
     * 
     * @param maxIdle max idle connections (or min pool size)
     * @param maxActive max active connections.
     * @return The connection pool.
     */
    public RedisConnectionPool<RedisAsyncConnection<String, String>> asyncPool(int maxIdle, int maxActive) {

        return asyncPool(codec, maxIdle, maxActive);
    }

    /**
     * Creates a connection pool for asynchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore.
     * 
     * @param codec
     * @param maxIdle
     * @param maxActive
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection pool.
     */
    public <K, V> RedisConnectionPool<RedisAsyncConnection<K, V>> asyncPool(final RedisCodec<K, V> codec, int maxIdle,
            int maxActive) {

        checkForRedisURI();
        long maxWait = unit.convert(timeout, TimeUnit.MILLISECONDS);
        RedisConnectionPool<RedisAsyncConnection<K, V>> pool = new RedisConnectionPool<RedisAsyncConnection<K, V>>(
                new RedisConnectionProvider<RedisAsyncConnection<K, V>>() {
                    @Override
                    public RedisAsyncConnection<K, V> createConnection() {
                        return connectAsyncImpl(codec, false, redisURI);
                    }

                    @Override
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    public Class<? extends RedisAsyncConnection<K, V>> getComponentType() {
                        return (Class) RedisAsyncConnection.class;
                    }
                }, maxActive, maxIdle, maxWait);

        pool.addListener(new CloseEvents.CloseListener() {
            @Override
            public void resourceClosed(Object resource) {
                closeableResources.remove(resource);
            }
        });

        closeableResources.add(pool);

        return pool;
    }

    /**
     * Open a new synchronous connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RedisConnection<String, String> connect() {
        return (RedisConnection<String, String>) connect((RedisCodec) codec);
    }

    /**
     * Open a new synchronous connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     * 
     * @param codec Use this codec to encode/decode keys and values, must note be {@literal null}
     * 
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <K, V> RedisConnection<K, V> connect(RedisCodec<K, V> codec) {
        checkForRedisURI();
        checkArgument(codec != null, "RedisCodec must not be null");
        return connect(codec, true, this.redisURI);
    }

    /**
     * Open a new synchronous connection to the supplied {@link RedisURI} that treats keys and values as UTF-8 strings.
     *
     * @param redisURI the redis server to connect to, must not be {@literal null}
     * @return A new connection.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RedisConnection<String, String> connect(RedisURI redisURI) {
        checkValidRedisURI(redisURI);
        return (RedisConnection<String, String>) connect((RedisCodec) codec, true, redisURI);
    }

    private void checkValidRedisURI(RedisURI redisURI) {
        checkArgument(redisURI != null && LettuceStrings.isNotEmpty(redisURI.getHost()),
                "A valid RedisURI with a host is needed");
    }

    @SuppressWarnings({ "rawtypes" })
    private <K, V> RedisConnection connect(RedisCodec<K, V> codec, boolean withReconnect, RedisURI redisURI) {
        return (RedisConnection) syncHandler(connectAsyncImpl(codec, withReconnect, redisURI), RedisConnection.class,
                RedisClusterConnection.class);
    }

    /**
     * Open a new asynchronous connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    public RedisAsyncConnection<String, String> connectAsync() {
        return connectAsync(codec);
    }

    /**
     * Open a new asynchronous connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     * 
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * 
     * @return A new connection.
     */
    public <K, V> RedisAsyncConnection<K, V> connectAsync(RedisCodec<K, V> codec) {
        checkForRedisURI();
        checkArgument(codec != null, "RedisCodec must not be null");
        return connectAsyncImpl(codec, true, redisURI);
    }

    /**
     * Open a new asynchronous connection to the supplied {@link RedisURI} that treats keys and values as UTF-8 strings.
     *
     * @param redisURI the redis server to connect to, must not be {@literal null}
     * @return A new connection.
     */
    public RedisAsyncConnection<String, String> connectAsync(RedisURI redisURI) {
        checkValidRedisURI(redisURI);
        return connectAsyncImpl(codec, true, redisURI);
    }

    private <K, V> RedisAsyncConnectionImpl<K, V> connectAsyncImpl(RedisCodec<K, V> codec, boolean withReconnect,
            RedisURI redisURI) {
        BlockingQueue<RedisCommand<K, V, ?>> queue = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(queue);
        RedisAsyncConnectionImpl<K, V> connection = new RedisAsyncConnectionImpl<K, V>(handler, codec, timeout, unit);

        connectAsyncImpl(handler, connection, withReconnect, redisURI);

        return connection;
    }

    private <K, V> void connectAsyncImpl(CommandHandler<K, V> handler, RedisAsyncConnectionImpl<K, V> connection,
            boolean withReconnect, RedisURI redisURI) {

        ConnectionBuilder connectionBuilder;
        if (redisURI.isSsl()) {
            SslConnectionBuilder sslConnectionBuilder = SslConnectionBuilder.sslConnectionBuilder();
            sslConnectionBuilder.ssl(redisURI);
            connectionBuilder = sslConnectionBuilder;
        } else {
            connectionBuilder = ConnectionBuilder.connectionBuilder();
        }

        connectionBuilder(handler, connection, getSocketAddressSupplier(redisURI), withReconnect, connectionBuilder, redisURI);
        connectAsyncImpl(connectionBuilder);

        if (redisURI.getPassword() != null && redisURI.getPassword().length != 0) {
            connection.auth(new String(redisURI.getPassword()));
        }

        if (redisURI.getDatabase() != 0) {
            connection.select(redisURI.getDatabase());
        }
    }

    /**
     * Open a new pub/sub connection to the redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new connection.
     */
    public RedisPubSubConnectionImpl<String, String> connectPubSub() {
        return connectPubSub(codec);
    }

    /**
     * Open a new pub/sub connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     *
     * @return A new pub/sub connection.
     */
    public <K, V> RedisPubSubConnectionImpl<K, V> connectPubSub(RedisCodec<K, V> codec) {

        checkForRedisURI();
        checkArgument(codec != null, "RedisCodec must not be null");
        BlockingQueue<RedisCommand<K, V, ?>> queue = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();

        PubSubCommandHandler<K, V> handler = new PubSubCommandHandler<K, V>(queue, codec);
        RedisPubSubConnectionImpl<K, V> connection = new RedisPubSubConnectionImpl<K, V>(handler, codec, timeout, unit);

        connectAsyncImpl(handler, connection, true, redisURI);

        return connection;
    }

    /**
     * Creates an asynchronous connection to Sentinel. You must supply a valid RedisURI containing one or more sentinels.
     * 
     * @return a new connection.
     */
    public RedisSentinelAsyncConnection<String, String> connectSentinelAsync() {
        return connectSentinelAsync(codec);
    }

    /**
     * Creates an asynchronous connection to Sentinel. You must supply a valid RedisURI containing one or more sentinels.
     * 
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection.
     */
    public <K, V> RedisSentinelAsyncConnection<K, V> connectSentinelAsync(RedisCodec<K, V> codec) {

        checkForRedisURI();
        checkArgument(codec != null, "RedisCodec must not be null");
        return connectSentinelAsyncImpl(codec, redisURI);
    }

    /**
     * Creates an asynchronous connection to Sentinel. You must supply a valid RedisURI containing a redis host or one or more
     * sentinels.
     *
     * @param redisURI the redis server to connect to, must not be {@literal null}
     * @return A new connection.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RedisSentinelAsyncConnection<String, String> connectSentinelAsync(RedisURI redisURI) {
        return (RedisSentinelAsyncConnection<String, String>) connectSentinelAsyncImpl((RedisCodec) codec, redisURI);
    }

    private <K, V> RedisSentinelAsyncConnection<K, V> connectSentinelAsyncImpl(RedisCodec<K, V> codec, RedisURI redisURI) {
        BlockingQueue<RedisCommand<K, V, ?>> queue = new LinkedBlockingQueue<RedisCommand<K, V, ?>>();

        final CommandHandler<K, V> commandHandler = new CommandHandler<K, V>(queue);
        final RedisSentinelAsyncConnectionImpl<K, V> connection = new RedisSentinelAsyncConnectionImpl<K, V>(commandHandler,
                codec, timeout, unit);

        logger.debug("Trying to get a Sentinel connection for one of: " + redisURI.getSentinels());
        final Bootstrap sentinelBootstrap = new Bootstrap().channel(NioSocketChannel.class).group(eventLoopGroup);
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(sentinelBootstrap, timer);
        watchdog.setReconnect(true);

        sentinelBootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {

                ch.pipeline().addLast(watchdog, new ChannelGroupListener(channels), watchdog, commandHandler,
                        new ConnectionEventTrigger(connectionEvents, connection));

            }
        });

        if (redisURI.getSentinels().isEmpty() && LettuceStrings.isNotEmpty(redisURI.getHost())) {
            sentinelBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    (int) redisURI.getUnit().toMillis(redisURI.getTimeout()));
            ChannelFuture connect = sentinelBootstrap.connect(redisURI.getResolvedAddress());
            logger.debug("Connecting to Sentinel, address: " + redisURI.getResolvedAddress());
            try {
                connect.sync();
            } catch (InterruptedException e) {
                throw new RedisException(e.getMessage(), e);
            }
        } else {

            boolean connected = false;
            Exception causingException = null;
            for (RedisURI uri : redisURI.getSentinels()) {

                sentinelBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) uri.getUnit().toMillis(uri.getTimeout()));
                ChannelFuture connect = sentinelBootstrap.connect(uri.getResolvedAddress());
                logger.debug("Connecting to Sentinel, address: " + uri.getResolvedAddress());
                try {
                    connect.sync();
                    connected = true;
                } catch (Exception e) {
                    logger.warn("Cannot connect sentinel at " + uri.getHost() + ":" + uri.getPort() + ": " + e.toString());
                    if (causingException == null) {
                        causingException = e;
                    }
                    if (e instanceof ConnectException) {
                        continue;
                    }
                }
            }
            if (!connected) {
                throw new RedisException("Cannot connect to a sentinel: " + redisURI.getSentinels(), causingException);
            }
        }

        connection.registerCloseables(closeableResources, connection);

        return connection;
    }

    private Supplier<SocketAddress> getSocketAddressSupplier(final RedisURI redisURI) {
        return new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                try {
                    return getSocketAddress(redisURI);
                } catch (InterruptedException e) {
                    throw new RedisException(e);
                } catch (TimeoutException e) {
                    throw new RedisException(e);
                } catch (ExecutionException e) {
                    throw new RedisException(e);
                }
            }
        };
    }

    protected SocketAddress getSocketAddress(RedisURI redisURI) throws InterruptedException, TimeoutException,
            ExecutionException {
        SocketAddress redisAddress;

        if (redisURI.getSentinelMasterId() != null && !redisURI.getSentinels().isEmpty()) {
            logger.debug("Connecting to Redis using Sentinels " + redisURI.getSentinels() + ", MasterId "
                    + redisURI.getSentinelMasterId());
            redisAddress = lookupRedis(redisURI.getSentinelMasterId());

            if (redisAddress == null) {
                throw new RedisConnectionException("Cannot provide redisAddress using sentinel for masterId "
                        + redisURI.getSentinelMasterId());
            }

        } else {
            redisAddress = redisURI.getResolvedAddress();
        }
        return redisAddress;
    }

    private SocketAddress lookupRedis(String sentinelMasterId) throws InterruptedException, TimeoutException,
            ExecutionException {
        RedisSentinelAsyncConnection<String, String> connection = connectSentinelAsync();
        try {
            return connection.getMasterAddrByName(sentinelMasterId).get(timeout, unit);
        } finally {
            connection.close();
        }
    }

}
