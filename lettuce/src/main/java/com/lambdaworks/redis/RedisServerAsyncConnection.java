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
     * @return RedisFuture<String> simple-string-reply always `OK`.
     */
    RedisFuture<String> bgrewriteaof();

    /**
     * Asynchronously save the dataset to disk.
     * 
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> bgsave();

    RedisFuture<K> clientGetname();

    RedisFuture<String> clientSetname(K name);

    RedisFuture<String> clientKill(String addr);

    RedisFuture<String> clientPause(long timeout);

    RedisFuture<String> clientList();

    RedisFuture<List<String>> configGet(String parameter);

    RedisFuture<String> configResetstat();

    RedisFuture<String> configRewrite();

    RedisFuture<String> configSet(String parameter, String value);

    /**
     * Return the number of keys in the selected database.
     * 
     * @return RedisFuture<Long> integer-reply
     */
    RedisFuture<Long> dbsize();

    RedisFuture<String> debugObject(K key);

    void debugSegfault();

    /**
     * Remove all keys from all databases.
     * 
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> flushall();

    /**
     * Remove all keys from the current database.
     * 
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> flushdb();

    /**
     * Get information and statistics about the server.
     * 
     * @return RedisFuture<String> bulk-string-reply as a collection of text lines.
     */
    RedisFuture<String> info();

    /**
     * Get information and statistics about the server.
     * 
     * @param section the section type: string
     * @return RedisFuture<String> bulk-string-reply as a collection of text lines.
     */
    RedisFuture<String> info(String section);

    /**
     * Get the UNIX time stamp of the last successful save to disk.
     * 
     * @return RedisFuture<Date> integer-reply an UNIX time stamp.
     */
    RedisFuture<Date> lastsave();

    /**
     * Synchronously save the dataset to disk.
     * 
     * @return RedisFuture<String> simple-string-reply The commands returns OK on success.
     */
    RedisFuture<String> save();

    /**
     * Synchronously save the dataset to disk and then shut down the server.
     */
    void shutdown(boolean save);

    /**
     * Make the server a slave of another instance, or promote it as master.
     * 
     * @param host the host type: string
     * @param port the port type: string
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> slaveof(String host, int port);

    RedisFuture<String> slaveofNoOne();

    RedisFuture<List<Object>> slowlogGet();

    RedisFuture<List<Object>> slowlogGet(int count);

    RedisFuture<Long> slowlogLen();

    RedisFuture<String> slowlogReset();

    /**
     * Internal command used for replication.
     */
    RedisFuture<String> sync();

    /**
     * Return the current server time.
     * 
     * @return RedisFuture<List<V>> array-reply specifically:
     * 
     *         A multi bulk reply containing two elements:
     * 
     *         unix time in seconds. microseconds.
     */
    RedisFuture<List<V>> time();
}
