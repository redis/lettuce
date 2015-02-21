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
 * @since 3.0
 */
public interface BaseRedisAsyncConnection<K, V> extends Closeable {

    /**
     * Post a message to a channel.
     * 
     * @param channel the channel type: key
     * @param message the message type: value
     * @return RedisFuture&lt;Long&gt; integer-reply the number of clients that received the message.
     */
    RedisFuture<Long> publish(K channel, V message);

    /**
     * Lists the currently *active channels*.
     * 
     * @return RedisFuture&lt;List&lt;K&gt;&gt; array-reply a list of active channels, optionally matching the specified
     *         pattern.
     */
    RedisFuture<List<K>> pubsubChannels();

    /**
     * Lists the currently *active channels*.
     * 
     * @param channel the key
     * @return RedisFuture&lt;List&lt;K&gt;&gt; array-reply a list of active channels, optionally matching the specified
     *         pattern.
     */
    RedisFuture<List<K>> pubsubChannels(K channel);

    /**
     * Returns the number of subscribers (not counting clients subscribed to patterns) for the specified channels.
     *
     * @param channels
     * @return array-reply a list of channels and number of subscribers for every channel.
     */
    RedisFuture<Map<K, Long>> pubsubNumsub(K... channels);

    /**
     * Returns the number of subscriptions to patterns.
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply the number of patterns all the clients are subscribed to.
     */
    RedisFuture<Long> pubsubNumpat();

    /**
     * Echo the given string.
     * 
     * @param msg the message type: value
     * @return RedisFuture&lt;V&gt; bulk-string-reply
     */
    RedisFuture<V> echo(V msg);

    /**
     * Return the role of the instance in the context of replication.
     *
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply where the first element is one of master, slave, sentinel and
     *         the additional elements are role-specific.
     */
    RedisFuture<List<Object>> role();

    /**
     * Ping the server.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> ping();

    RedisFuture<String> clusterNodes();

    RedisFuture<String> clusterInfo();

    RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count);

    /**
     * Get array of Cluster slot to node mappings.
     *
     * @return List&lt;Object&gt; array-reply nested list of slot ranges with IP/Port mappings.
     */
    RedisFuture<List<Object>> clusterSlots();

    RedisFuture<String> readOnly();

    /**
     * Close the connection.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always OK.
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
     * @return RedisFuture&lt;String&gt; simple-string-reply always <code>OK</code>.
     */
    RedisFuture<String> discard();

    /**
     * Execute all commands issued after MULTI.
     * 
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply each element being the reply to each of the commands in the
     *         atomic transaction.
     * 
     *         When using <code>WATCH</code>, <code>EXEC</code> can return a
     */
    RedisFuture<List<Object>> exec();

    /**
     * Mark the start of a transaction block.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always <code>OK</code>.
     */
    RedisFuture<String> multi();

    /**
     * Watch the given keys to determine execution of the MULTI/EXEC block.
     * 
     * @param keys the key
     * @return RedisFuture&lt;String&gt; simple-string-reply always <code>OK</code>.
     */
    RedisFuture<String> watch(K... keys);

    /**
     * Forget about all watched keys.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always <code>OK</code>.
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
