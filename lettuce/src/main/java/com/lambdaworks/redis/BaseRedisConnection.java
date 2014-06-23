package com.lambdaworks.redis;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * 
 * Basic synchronous executed commands.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:05
 */
public interface BaseRedisConnection<K, V> extends Closeable {

    /**
     * Post a message to a channel.
     * 
     * @param channel the channel type: key
     * @param message the message type: value
     * @return Long integer-reply the number of clients that received the message.
     */
    Long publish(K channel, V message);

    /**
     * Lists the currently *active channels*.
     * 
     * @return List<K> array-reply a list of active channels, optionally matching the specified pattern.
     */
    List<K> pubsubChannels();

    /**
     * Lists the currently *active channels*.
     * 
     * @param channel the key
     * @return List<K> array-reply a list of active channels, optionally matching the specified pattern.
     */
    List<K> pubsubChannels(K channel);

    /**
     * Returns the number of subscribers (not counting clients subscribed to patterns) for the specified channels.
     * 
     * @param channels
     * @return array-reply a list of channels and number of subscribers for every channel.
     */
    Map<K, String> pubsubNumsub(K... channels);

    /**
     * Returns the number of subscriptions to patterns.
     * 
     * @return Long integer-reply the number of patterns all the clients are subscribed to.
     */
    Long pubsubNumpat();

    /**
     * Echo the given string.
     * 
     * @param msg the message type: value
     * @return V bulk-string-reply
     */
    V echo(V msg);

    /**
     * Ping the server.
     * 
     * @return String simple-string-reply
     */
    String ping();

    /**
     * Close the connection.
     * 
     * @return String simple-string-reply always OK.
     */
    String quit();

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
     * @return String simple-string-reply always `OK`.
     */
    String discard();

    /**
     * Execute all commands issued after MULTI.
     * 
     * @return List<Object> array-reply each element being the reply to each of the commands in the atomic transaction.
     * 
     *         When using `WATCH`, `EXEC` can return a
     */
    List<Object> exec();

    /**
     * Mark the start of a transaction block.
     * 
     * @return String simple-string-reply always `OK`.
     */
    String multi();

    /**
     * Watch the given keys to determine execution of the MULTI/EXEC block.
     * 
     * @param keys the key
     * @return String simple-string-reply always `OK`.
     */
    String watch(K... keys);

    /**
     * Forget about all watched keys.
     * 
     * @return String simple-string-reply always `OK`.
     */
    String unwatch();

    /**
     * Wait for replication.
     * 
     * @param replicas
     * @param timeout
     * @return number of replicas
     */
    Long waitForReplication(int replicas, long timeout);

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
