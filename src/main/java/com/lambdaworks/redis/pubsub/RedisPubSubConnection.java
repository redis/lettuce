// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import org.jboss.netty.channel.*;

import java.util.*;
import java.util.concurrent.*;

import static com.lambdaworks.redis.protocol.CommandType.*;

/**
 * An asynchronous thread-safe pub/sub connection to a redis server. After one or
 * more channels are subscribed to only pub/sub related commands or {@link #quit}
 * may be called.
 *
 * Incoming messages and results of the {@link #subscribe}/{@link #unsubscribe}
 * calls will be passed to all registered {@link RedisPubSubListener}s.
 *
 * A {@link com.lambdaworks.redis.protocol.ConnectionWatchdog} monitors each
 * connection and reconnects automatically until {@link #close} is called. Channel
 * and pattern subscriptions are renewed after reconnecting.
 *
 * @author Will Glozer
 */
public class RedisPubSubConnection<K, V> extends RedisConnection<K, V> {
    private List<RedisPubSubListener<V>> listeners;
    private Set<String> channels;
    private Set<String> patterns;

    /**
     * Initialize a new connection.
     *
     * @param queue     Command queue.
     * @param codec     Codec used to encode/decode keys and values.
     * @param timeout   Maximum time to wait for a responses.
     * @param unit      Unit of time for the timeout.
     */
    public RedisPubSubConnection(BlockingQueue<Command<?>> queue, RedisCodec<K, V> codec, int timeout, TimeUnit unit) {
        super(queue, codec, timeout, unit);
        listeners = new CopyOnWriteArrayList<RedisPubSubListener<V>>();
        channels  = new HashSet<String>();
        patterns  = new HashSet<String>();
    }

    /**
     * Add a new listener.
     *
     * @param listener Listener.
     */
    public void addListener(RedisPubSubListener<V> listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener Listener.
     */
    public void removeListener(RedisPubSubListener<V> listener) {
        listeners.remove(listener);
    }

    public void psubscribe(String... patterns) {
        dispatch(PSUBSCRIBE, new PubSubOutput<V>(codec), args(patterns));
    }

    public void punsubscribe(String... patterns) {
        dispatch(PUNSUBSCRIBE, new PubSubOutput<V>(codec), args(patterns));
    }

    public void subscribe(String... channels) {
        dispatch(SUBSCRIBE, new PubSubOutput<V>(codec), args(channels));
    }

    public void unsubscribe(String... channels) {
        dispatch(UNSUBSCRIBE, new PubSubOutput<V>(codec), args(channels));
    }

    @Override
    public synchronized void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);

        if (channels.size() > 0) {
            String[] channelArray = new String[channels.size()];
            subscribe(channels.toArray(channelArray));
            channels.clear();
        }

        if (patterns.size() > 0) {
            String[] patternArray = new String[patterns.size()];
            psubscribe(patterns.toArray(patternArray));
            patterns.clear();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        PubSubOutput<V> output = (PubSubOutput<V>) e.getMessage();
        for (RedisPubSubListener<V> listener : listeners) {
            switch (output.type()) {
                case message:
                    listener.message(output.channel(), output.get());
                    break;
                case pmessage:
                    listener.message(output.pattern(), output.channel(), output.get());
                    break;
                case psubscribe:
                    patterns.add(output.pattern());
                    listener.psubscribed(output.pattern(), output.count());
                    break;
                case punsubscribe:
                    patterns.remove(output.pattern());
                    listener.punsubscribed(output.pattern(), output.count());
                    break;
                case subscribe:
                    channels.add(output.channel());
                    listener.subscribed(output.channel(), output.count());
                    break;
                case unsubscribe:
                    channels.remove(output.channel());
                    listener.unsubscribed(output.channel(), output.count());
                    break;
            }
        }
    }

    private CommandArgs<K, V> args(String... strings) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        for (String c : strings) {
            args.add(c);
        }
        return args;
    }
}
