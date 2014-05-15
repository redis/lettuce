// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandHandler;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;

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
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * A scalable thread-safe <a href="http://redis.io/">Redis</a> client. Multiple threads may share one connection provided they
 * avoid blocking and transactional operations such as BLPOP and MULTI/EXEC.
 * 
 * @author Will Glozer
 */
public class RedisClient {
    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private HashedWheelTimer timer;
    private ChannelGroup channels;
    private long timeout;
    private TimeUnit unit;
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
        this.redisURI = redisURI;
        InetSocketAddress addr = new InetSocketAddress(redisURI.getHost(), redisURI.getPort());

        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap().channel(NioSocketChannel.class).group(group).remoteAddress(addr);

        setDefaultTimeout(redisURI.getTimeout(), redisURI.getUnit());

        channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        timer = new HashedWheelTimer();
        timer.start();
    }

    /**
     * Set the default timeout for {@link RedisConnection connections} created by this client. The timeout applies to connection
     * attempts and non-blocking commands.
     * 
     * @param timeout Default connection timeout.
     * @param unit Unit of time for the timeout.
     */
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) unit.toMillis(timeout));
    }

    /**
     * Open a new synchronous connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    public RedisConnection<String, String> connect() {
        return connect((RedisCodec) codec);
    }

    public RedisConnectionPool<RedisConnection<String, String>> pool() {
        return pool(5, 20);
    }

    public RedisConnectionPool<RedisConnection<String, String>> pool(int maxIdle, int maxActive) {

        long maxWait = unit.convert(timeout, TimeUnit.MILLISECONDS);
        RedisConnectionPool<RedisConnection<String, String>> pool = new RedisConnectionPool<RedisConnection<String, String>>(
                new RedisConnectionProvider<RedisConnection<String, String>>() {
                    @Override
                    public RedisConnection<String, String> createConnection() {
                        return connect(codec, false);
                    }
                }, maxActive, maxIdle, maxWait);
        return pool;
    }

    public RedisConnectionPool<RedisAsyncConnection<String, String>> asyncPool() {
        return asyncPool(5, 20);
    }

    public RedisConnectionPool<RedisAsyncConnection<String, String>> asyncPool(int maxIdle, int maxActive) {

        long maxWait = unit.convert(timeout, TimeUnit.MILLISECONDS);
        RedisConnectionPool<RedisAsyncConnection<String, String>> pool = new RedisConnectionPool<RedisAsyncConnection<String, String>>(
                new RedisConnectionProvider<RedisAsyncConnection<String, String>>() {
                    @Override
                    public RedisAsyncConnection<String, String> createConnection() {
                        return (RedisAsyncConnection<String, String>) connectAsyncImpl(codec, false);
                    }
                }, maxActive, maxIdle, maxWait);
        return pool;
    }

    /**
     * Open a new asynchronous connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    public RedisAsyncConnection<String, String> connectAsync() {
        return connectAsync((RedisCodec) codec);
    }

    /**
     * Open a new pub/sub connection to the redis server that treats keys and values as UTF-8 strings.
     * 
     * @return A new connection.
     */
    public RedisPubSubConnection<String, String> connectPubSub() {
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
    public <K, V> RedisConnection<K, V> connect(RedisCodec<K, V> codec) {

        RedisConnection c = connect(codec, true);
        return c;
    }

    private <K, V> RedisConnection connect(RedisCodec<K, V> codec, boolean withReconnect) {
        FutureSyncInvocationHandler h = new FutureSyncInvocationHandler(connectAsyncImpl(codec, withReconnect));
        return (RedisConnection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { RedisConnection.class }, h);
    }

    /**
     * Open a new asynchronous connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * 
     * @return A new connection.
     */
    public <K, V> RedisAsyncConnection<K, V> connectAsync(RedisCodec<K, V> codec) {
        return connectAsyncImpl(codec, true);
    }

    private <K, V> RedisAsyncConnectionImpl<K, V> connectAsyncImpl(RedisCodec<K, V> codec, boolean withReconnect) {
        BlockingQueue<Command<K, V, ?>> queue = new LinkedBlockingQueue<Command<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(queue);
        RedisAsyncConnectionImpl<K, V> connection = new RedisAsyncConnectionImpl<K, V>(queue, codec, timeout, unit);

        return connect(handler, connection, withReconnect);
    }

    /**
     * Open a new pub/sub connection to the redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     * 
     * @param codec Use this codec to encode/decode keys and values.
     * 
     * @return A new pub/sub connection.
     */
    public <K, V> RedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {
        BlockingQueue<Command<K, V, ?>> queue = new LinkedBlockingQueue<Command<K, V, ?>>();

        PubSubCommandHandler<K, V> handler = new PubSubCommandHandler<K, V>(queue, codec);
        RedisPubSubConnection<K, V> connection = new RedisPubSubConnection<K, V>(queue, codec, timeout, unit);

        return connect(handler, connection, true);
    }

    private <K, V, T extends RedisAsyncConnectionImpl<K, V>> T connect(final CommandHandler<K, V> handler, final T connection,
            final boolean withReconnect) {
        try {

            bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {

                    if (withReconnect) {
                        ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, channels, timer);
                        ch.pipeline().addLast(watchdog);
                        watchdog.setReconnect(true);
                    }

                    ch.pipeline().addLast(handler, connection);
                }
            });

            bootstrap.connect().sync();

            if (redisURI.getPassword() != null) {
                connection.auth(redisURI.getPassword());
            }

            if (redisURI.getDatabase() != 0) {
                connection.select(redisURI.getDatabase());
            }

            return connection;
        } catch (Throwable e) {
            throw new RedisException("Unable to connect", e);
        }
    }

    /**
     * Shutdown this client and close all open connections. The client should be discarded after calling shutdown.
     */
    public void shutdown() {
        for (Channel c : channels) {
            ChannelPipeline pipeline = c.pipeline();
            RedisAsyncConnectionImpl<?, ?> connection = pipeline.get(RedisAsyncConnectionImpl.class);
            connection.close();
        }
        ChannelGroupFuture future = channels.close();
        future.awaitUninterruptibly();
        group.shutdownGracefully().syncUninterruptibly();
        timer.stop();
    }
}
