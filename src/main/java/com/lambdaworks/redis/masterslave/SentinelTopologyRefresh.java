package com.lambdaworks.redis.masterslave;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Sentinel Pub/Sub listener-enabled topology refresh.
 *
 * @author Mark Paluch
 * @since 4.2
 */
class SentinelTopologyRefresh implements Closeable {

    private final static InternalLogger LOG = InternalLoggerFactory.getInstance(SentinelTopologyRefresh.class);

    private final List<StatefulRedisPubSubConnection<String, String>> pubSubConnections = new ArrayList<>();
    private final RedisClient redisClient;
    private final String masterId;
    private final List<RedisURI> sentinels;
    private final AtomicReference<Timeout> timeoutRef = new AtomicReference<Timeout>();
    private final Set<String> PROCESSING_CHANNELS = new HashSet<>(Arrays.asList("failover-end", "failover-end-for-timeout"));

    private int timeout = 5;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    private RedisPubSubAdapter<String, String> adapter;

    SentinelTopologyRefresh(RedisClient redisClient, String masterId, List<RedisURI> sentinels) {

        this.redisClient = redisClient;
        this.masterId = masterId;
        this.sentinels = sentinels;
    }

    @Override
    public void close() throws IOException {

        pubSubConnections.forEach(c -> c.removeListener(adapter));
        pubSubConnections.forEach(StatefulConnection::close);
    }

    void bind(Runnable runnable) {

        Utf8StringCodec codec = new Utf8StringCodec();
        AtomicReference<RedisConnectionException> ref = new AtomicReference<>();

        sentinels.forEach(redisURI -> {

            try {
                StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(codec, redisURI);
                pubSubConnections.add(pubSubConnection);
            } catch (RedisConnectionException e) {
                if (ref.get() == null) {
                    ref.set(e);
                } else {
                    ref.get().addSuppressed(e);
                }
            }
        });

        if (sentinels.isEmpty() && ref.get() != null) {
            throw ref.get();
        }

        adapter = new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String pattern, String channel, String message) {

                if (processingAllowed(channel, message)) {

                    LOG.debug("Received topology changed signal from Redis Sentinel, scheduling topology update");

                    Timeout timeout = timeoutRef.get();
                    if (timeout == null) {
                        getEventExecutor().submit(runnable);
                    } else {
                        getEventExecutor().schedule(runnable, timeout.remaining(), TimeUnit.MILLISECONDS);
                    }
                }
            }
        };

        pubSubConnections.forEach(c -> {

            c.addListener(adapter);
            c.async().psubscribe("*");
        });

    }

    private boolean processingAllowed(String channel, String message) {

        if (getEventExecutor().isShuttingDown()) {
            return false;
        }

        if (!messageMatches(channel, message)) {
            return false;
        }

        Timeout existingTimeout = timeoutRef.get();

        if (existingTimeout != null) {
            if (!existingTimeout.isExpired()) {
                return false;
            }
        }

        Timeout timeout = new Timeout(this.timeout, this.timeUnit);
        if (timeoutRef.compareAndSet(existingTimeout, timeout)) {
            return true;
        }

        return false;
    }

    protected EventExecutorGroup getEventExecutor() {
        return redisClient.getResources().eventExecutorGroup();
    }

    private boolean messageMatches(String channel, String message) {

        // trailing spaces after the master name are not bugs
        if (channel.equals("+elected-leader")) {
            if (message.startsWith(String.format("master %s ", masterId))) {
                return true;
            }
        }

        if (channel.equals("+switch-master")) {
            if (message.startsWith(String.format("%s ", masterId))) {
                return true;
            }
        }

        if (channel.equals("fix-slave-config")) {
            if (message.contains(String.format("@ %s ", masterId))) {
                return true;
            }
        }

        if (PROCESSING_CHANNELS.contains(channel)) {
            return true;
        }

        return false;
    }
}
