package com.lambdaworks.redis;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * 
 * Basic asynchronous executed commands.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 17.05.14 21:05
 */
public interface BaseRedisAsyncConnection<K, V> extends Closeable {

    /**
     * Post a message to a channel.
     * 
     * @param channel the channel type: key
     * @param message the message type: value
     * @return RedisFuture<Long> integer-reply the number of clients that received the message.
     */
    RedisFuture<Long> publish(K channel, V message);

    /**
     * Lists the currently *active channels*.
     * 
     * @return RedisFuture<List<K>> array-reply a list of active channels, optionally matching the specified pattern.
     */
    RedisFuture<List<K>> pubsubChannels();

    /**
     * Lists the currently *active channels*.
     * 
     * @param channel the key
     * @return RedisFuture<List<K>> array-reply a list of active channels, optionally matching the specified pattern.
     */
    RedisFuture<List<K>> pubsubChannels(K channel);

    /**
     * Returns the number of subscribers (not counting clients subscribed to patterns) for the specified channels.
     * 
     * @param channels
     * @return array-reply a list of channels and number of subscribers for every channel.
     */
    RedisFuture<Map<K, String>> pubsubNumsub(K... channels);

    /**
     * Returns the number of subscriptions to patterns.
     * 
     * @return RedisFuture<Long> integer-reply the number of patterns all the clients are subscribed to.
     */
    RedisFuture<Long> pubsubNumpat();

    /**
     * Echo the given string.
     * 
     * @param msg the message type: value
     * @return RedisFuture<V> bulk-string-reply
     */
    RedisFuture<V> echo(V msg);

    /**
     * Ping the server.
     * 
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> ping();

    /**
     * Close the connection.
     * 
     * @return RedisFuture<String> simple-string-reply always OK.
     */
    RedisFuture<String> quit();

    /**
     * Create a SHA1 digest from a Lua script.
     * 
     * @param script
     * @return the SHA1 value
     */
    String digest(V script);

    /**
     * Discard all commands issued after MULTI.
     * 
     * @return RedisFuture<String> simple-string-reply always `OK`.
     */
    RedisFuture<String> discard();

    /**
     * Execute all commands issued after MULTI.
     * 
     * @return RedisFuture<List<Object>> array-reply each element being the reply to each of the commands in the atomic
     *         transaction.
     * 
     *         When using `WATCH`, `EXEC` can return a
     */
    RedisFuture<List<Object>> exec();

    /**
     * Mark the start of a transaction block.
     * 
     * @return RedisFuture<String> simple-string-reply always `OK`.
     */
    RedisFuture<String> multi();

    /**
     * Watch the given keys to determine execution of the MULTI/EXEC block.
     * 
     * @param keys the key
     * @return RedisFuture<String> simple-string-reply always `OK`.
     */
    RedisFuture<String> watch(K... keys);

    /**
     * Forget about all watched keys.
     * 
     * @return RedisFuture<String> simple-string-reply always `OK`.
     */
    RedisFuture<String> unwatch();

    /**
     * Wait for replication.
     * 
     * @param replicas
     * @param timeout
     * @return number of replicas
     */
    RedisFuture<Long> waitForReplication(int replicas, long timeout);

    /**
     * Close the connection. The connection will become not usable anymore as soon as this method was called.
     */
    @Override
    void close();

    /**
     * 
     * @return true if the connection is open (connected and not closed).
     */
    boolean isOpen();

}
