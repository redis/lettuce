package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;

/**
 * Asynchronous executed commands for Server Control.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:32
 */
public interface RedisServerAsyncConnection<K, V> {
    /**
     * Asynchronously rewrite the append-only file.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always <code>OK</code>.
     */
    RedisFuture<String> bgrewriteaof();

    /**
     * Asynchronously save the dataset to disk.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> bgsave();

    /**
     * Get the current connection name.
     * 
     * @return RedisFuture&lt;K&gt; bulk-string-reply The connection name, or a null bulk reply if no name is set.
     */
    RedisFuture<K> clientGetname();

    /**
     * Set the current connection name.
     * 
     * @param name the name
     * @return RedisFuture&lt;String&gt; simple-string-reply <code>OK</code> if the connection name was successfully set.
     */
    RedisFuture<String> clientSetname(K name);

    /**
     * Kill the connection of a client identified by ip:port.
     * 
     * @param addr the addr in format ip:port
     * @return RedisFuture&lt;String&gt; simple-string-reply <code>OK</code> if the connection exists and has been closed
     */
    RedisFuture<String> clientKill(String addr);

    /**
     * Kill connections of clients which are filtered by {@code killArgs}
     *
     * @param killArgs args for the kill operation
     * @return RedisFuture&lt;Long&gt; integer-reply number of killed connections
     */
    RedisFuture<Long> clientKill(KillArgs killArgs);

    /**
     * Stop processing commands from clients for some time.
     * 
     * @param timeout the timeout
     * @return RedisFuture&lt;String&gt; simple-string-reply The command returns OK or an error if the timeout is invalid.
     */
    RedisFuture<String> clientPause(long timeout);

    /**
     * Get the list of client connections.
     * 
     * @return RedisFuture&lt;String&gt; bulk-string-reply a unique string, formatted as follows: One client connection per line
     *         (separated by LF), each line is composed of a succession of property=value fields separated by a space character.
     */
    RedisFuture<String> clientList();

    /**
     * Returns Array reply of details about all Redis commands.
     * 
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply
     */
    RedisFuture<List<Object>> command();

    /**
     * Get total number of Redis commands.
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply of number of total commands in this Redis server.
     */
    RedisFuture<Long> commandCount();

    /**
     * Get the value of a configuration parameter.
     * 
     * @param parameter the parameter
     * @return RedisFuture&lt;List&lt;String&gt;&gt; bulk-string-reply
     */
    RedisFuture<List<String>> configGet(String parameter);

    /**
     * Reset the stats returned by INFO.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply always <code>OK</code>.
     */
    RedisFuture<String> configResetstat();

    /**
     * Rewrite the configuration file with the in memory configuration.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply <code>OK</code> when the configuration was rewritten properly.
     *         Otherwise an error is returned.
     */
    RedisFuture<String> configRewrite();

    /**
     * Set a configuration parameter to the given value.
     * 
     * @param parameter the parameter
     * @param value the value
     * @return RedisFuture&lt;String&gt; simple-string-reply: <code>OK</code> when the configuration was set properly. Otherwise
     *         an error is returned.
     */
    RedisFuture<String> configSet(String parameter, String value);

    /**
     * Return the number of keys in the selected database.
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply
     */
    RedisFuture<Long> dbsize();

    /**
     * Get debugging information about a key.
     * 
     * @param key the key
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> debugObject(K key);

    /**
     * Make the server crash: Invalid pointer access.
     */
    void debugSegfault();

    /**
     * Make the server crash: Out of memory.
     */
    void debugOom();

    /**
     * Remove all keys from all databases.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> flushall();

    /**
     * Remove all keys from the current database.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> flushdb();

    /**
     * Get information and statistics about the server.
     * 
     * @return RedisFuture&lt;String&gt; bulk-string-reply as a collection of text lines.
     */
    RedisFuture<String> info();

    /**
     * Get information and statistics about the server.
     * 
     * @param section the section type: string
     * @return RedisFuture&lt;String&gt; bulk-string-reply as a collection of text lines.
     */
    RedisFuture<String> info(String section);

    /**
     * Get the UNIX time stamp of the last successful save to disk.
     * 
     * @return RedisFuture&lt;Date&gt; integer-reply an UNIX time stamp.
     */
    RedisFuture<Date> lastsave();

    /**
     * Synchronously save the dataset to disk.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply The commands returns OK on success.
     */
    RedisFuture<String> save();

    /**
     * Synchronously save the dataset to disk and then shut down the server.
     * 
     * @param save
     */
    void shutdown(boolean save);

    /**
     * Make the server a slave of another instance.
     * 
     * @param host the host type: string
     * @param port the port type: string
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> slaveof(String host, int port);

    /**
     * Promote server as master.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> slaveofNoOne();

    /**
     * Read the slow log.
     * 
     * @return List&lt;Object&gt; deeply nested multi bulk replies
     */
    RedisFuture<List<Object>> slowlogGet();

    /**
     * Read the slow log.
     * 
     * @param count the count
     * @return List&lt;Object&gt; deeply nested multi bulk replies
     */
    RedisFuture<List<Object>> slowlogGet(int count);

    /**
     * Obtaining the current length of the slow log.
     * 
     * @return RedisFuture&lt;Long&gt; length of the slow log.
     */
    RedisFuture<Long> slowlogLen();

    /**
     * Resetting the slow log.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply The commands returns OK on success.
     */
    RedisFuture<String> slowlogReset();

    /**
     * Internal command used for replication.
     * 
     * @return RedisFuture&lt;String&gt;
     */
    RedisFuture<String> sync();

    /**
     * Return the current server time.
     * 
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply specifically:
     * 
     *         A multi bulk reply containing two elements:
     * 
     *         unix time in seconds. microseconds.
     */
    RedisFuture<List<V>> time();
}
