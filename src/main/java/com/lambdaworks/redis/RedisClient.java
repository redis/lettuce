// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.google.common.base.Supplier;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.internal.ChannelGroupListener;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.RedisPubSubConnectionImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A scalable thread-safe <a href="http://redis.io/">Redis</a> client. Multiple threads may share one connection provided they
 * avoid blocking and transactional operations such as BLPOP and MULTI/EXEC.
 * 
 * @author Will Glozer
 */
public class RedisClient extends AbstractRedisClient {

    private RedisCodec<?, ?> codec = new Utf8StringCodec();
    private RedisURI redisURI;

    /**
     * Create a new client that connects to the supplied host on the default port.
     * 
     * @param host Server hostname.
     */
    public RedisClient(String host) {
        this(host, 6379);
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

        timer.start();
    }

    /**
     * Open a new synchronous connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseRedisConnection<String, String>> T connect() {
        return (T) connect(codec);
    }

    /**
     * Creates a connection pool for synchronous connections. 5 max idle connections and 20 max active connections. Please keep
     * in mind to free all collections and close the pool once you do not need it anymore.
     * 
     * @param <T>
     * @return The connection pool.
     */
    public <T extends BaseRedisConnection<String, String>> RedisConnectionPool<T> pool() {
        return pool(5, 20);
    }

    /**
     * Creates a connection pool for synchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore.
     * 
     * @param maxIdle max idle connections (or min pool size)
     * @param maxActive max active connections.
     * @param <T>
     * @return The connection pool.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseRedisConnection<String, String>> RedisConnectionPool<T> pool(int maxIdle, int maxActive) {

        return (RedisConnectionPool) pool(codec, maxIdle, maxActive);
    }

    @SuppressWarnings("unchecked")
    private <K, V, T extends BaseRedisConnection<K, V>> RedisConnectionPool<T> pool(final RedisCodec<K, V> codec, int maxIdle,
            int maxActive) {

        long maxWait = unit.convert(timeout, TimeUnit.MILLISECONDS);
        RedisConnectionPool<RedisConnection<K, V>> pool = new RedisConnectionPool<RedisConnection<K, V>>(
                new RedisConnectionProvider<RedisConnection<K, V>>() {
                    @Override
                    public RedisConnection<K, V> createConnection() {
                        return connect(codec, false);
                    }

                    @Override
                    public Class<?> getComponentType() {
                        return RedisConnection.class;
                    }
                }, maxActive, maxIdle, maxWait);

        pool.addListener(new CloseEvents.CloseListener() {
            @Override
            public void resourceClosed(Object resource) {
                closeableResources.remove(resource);
            }
        });

        closeableResources.add(pool);

        return (RedisConnectionPool<T>) pool;
    }

    /**
     * Creates a connection pool for asynchronous connections. 5 max idle connections and 20 max active connections. Please keep
     * in mind to free all collections and close the pool once you do not need it anymore.
     * 
     * @param <T>
     * @return The connection pool.
     */
    public <T extends BaseRedisAsyncConnection<String, String>> RedisConnectionPool<T> asyncPool() {
        return asyncPool(5, 20);
    }

    /**
     * Creates a connection pool for asynchronous connections. Please keep in mind to free all collections and close the pool
     * once you do not need it anymore.
     * 
     * @param maxIdle max idle connections (or min pool size)
     * @param maxActive max active connections.
     * @param <T>
     * @return The connection pool.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseRedisAsyncConnection<String, String>> RedisConnectionPool<T> asyncPool(int maxIdle, int maxActive) {

        return (RedisConnectionPool) asyncPool(codec, maxIdle, maxActive);
    }

    @SuppressWarnings("unchecked")
    private <K, V, T extends BaseRedisAsyncConnection<K, V>> RedisConnectionPool<T> asyncPool(final RedisCodec<K, V> codec,
            int maxIdle, int maxActive) {

        long maxWait = unit.convert(timeout, TimeUnit.MILLISECONDS);
        RedisConnectionPool<RedisAsyncConnection<K, V>> pool = new RedisConnectionPool<RedisAsyncConnection<K, V>>(
                new RedisConnectionProvider<RedisAsyncConnection<K, V>>() {
                    @Override
                    public RedisAsyncConnection<K, V> createConnection() {
                        return connectAsyncImpl(codec, false);
                    }

                    @Override
                    public Class<?> getComponentType() {
                        return RedisAsyncConnection.class;
                    }
                }, maxActive, maxIdle, maxWait);

        pool.addListener(new CloseEvents.CloseListener() {
            @Override
            public void resourceClosed(Object resource) {
                closeableResources.remove(resource);
            }
        });

        closeableResources.add(pool);

        return (RedisConnectionPool<T>) pool;
    }

    /**
     * Open a new asynchronous connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseRedisAsyncConnection<String, String>> T connectAsync() {
        return (T) connectAsync(codec);
    }

    /**
     * Open a new pub/sub connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public RedisPubSubConnectionImpl<String, String> connectPubSub() {
        return connectPubSub((RedisCodec) codec);
    }

    /**
     * Open a new synchronous connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * 
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <K, V, T extends BaseRedisConnection<K, V>> T connect(RedisCodec<K, V> codec) {

        return (T) connect(codec, true);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <K, V> RedisConnection connect(RedisCodec<K, V> codec, boolean withReconnect) {
        FutureSyncInvocationHandler<K, V> h = new FutureSyncInvocationHandler<K, V>(connectAsyncImpl(codec, withReconnect));
        return (RedisConnection<K, V>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { RedisConnection.class }, h);
    }

    /**
     * Open a new asynchronous connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * 
     * @return A new connection.
     */
    @SuppressWarnings("unchecked")
    public <K, V, T extends BaseRedisAsyncConnection<K, V>> T connectAsync(RedisCodec<K, V> codec) {
        return (T) connectAsyncImpl(codec, true);
    }

    private <K, V> RedisAsyncConnectionImpl<K, V> connectAsyncImpl(RedisCodec<K, V> codec, boolean withReconnect) {
        BlockingQueue<Command<K, V, ?>> queue = new LinkedBlockingQueue<Command<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(queue);
        RedisAsyncConnectionImpl<K, V> connection = new RedisAsyncConnectionImpl<K, V>(handler, codec, timeout, unit);

        return connectAsyncImpl(handler, connection, withReconnect);
    }

    private <K, V, T extends RedisAsyncConnectionImpl<K, V>> T connectAsyncImpl(CommandHandler<K, V> handler,
            RedisAsyncConnectionImpl<K, V> connection, boolean withReconnect) {

        connectAsyncImpl(handler, connection, getSocketAddressSupplier(), withReconnect);
        if (redisURI.getPassword() != null) {
            connection.auth(new String(redisURI.getPassword()));
        }

        if (redisURI.getDatabase() != 0) {
            connection.select(redisURI.getDatabase());
        }

        return (T) connection;
    }

    /**
     * Open a new pub/sub connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * 
     * @return A new pub/sub connection.
     */
    public <K, V> RedisPubSubConnectionImpl<K, V> connectPubSub(RedisCodec<K, V> codec) {
        BlockingQueue<Command<K, V, ?>> queue = new LinkedBlockingQueue<Command<K, V, ?>>();

        PubSubCommandHandler<K, V> handler = new PubSubCommandHandler<K, V>(queue, codec);
        RedisPubSubConnectionImpl<K, V> connection = new RedisPubSubConnectionImpl<K, V>(handler, codec, timeout, unit);

        return connectAsyncImpl(handler, connection, true);
    }

    private Supplier<SocketAddress> getSocketAddressSupplier() {
        return new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                try {
                    return getSocketAddress();
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

    private SocketAddress getSocketAddress() throws InterruptedException, TimeoutException, ExecutionException {
        SocketAddress redisAddress;

        if (redisURI.getSentinelMasterId() != null && !redisURI.getSentinels().isEmpty()) {
            logger.debug("Connecting to Redis using Sentinels " + redisURI.getSentinels() + ", MasterId "
                    + redisURI.getSentinelMasterId());
            redisAddress = lookupRedis(redisURI.getSentinelMasterId());

            if (redisAddress == null) {
                throw new RedisException("Cannot provide redisAddress using sentinel for masterId "
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

    /**
     * Creates an asynchronous connection to Sentinel. You must supply a valid RedisURI containing one or more sentinels.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public RedisSentinelAsyncConnection<String, String> connectSentinelAsync() {
        return connectSentinelAsync((RedisCodec) codec);
    }

    /**
     * Creates an asynchronous connection to Sentinel. You must supply a valid RedisURI containing one or more sentinels.
     * 
     * @param <K>
     * @param <V>
     * @return
     */
    public <K, V> RedisSentinelAsyncConnection<K, V> connectSentinelAsync(RedisCodec<K, V> codec) {

        BlockingQueue<Command<K, V, ?>> queue = new LinkedBlockingQueue<Command<K, V, ?>>();

        final CommandHandler<K, V> commandHandler = new CommandHandler<K, V>(queue);
        final RedisSentinelAsyncConnectionImpl<K, V> connection = new RedisSentinelAsyncConnectionImpl<K, V>(commandHandler,
                codec, timeout, unit);

        logger.debug("Trying to get a Sentinel connection for one of: " + redisURI.getSentinels());
        final Bootstrap sentinelBootstrap = new Bootstrap().channel(NioSocketChannel.class).group(group);
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

        connection.addListener(new CloseEvents.CloseListener() {
            @Override
            public void resourceClosed(Object resource) {
                closeableResources.remove(resource);
            }
        });

        return connection;
    }

}
