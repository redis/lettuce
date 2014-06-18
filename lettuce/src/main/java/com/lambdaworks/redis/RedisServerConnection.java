package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;

/**
 * Synchronous executed commands for Server Control.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:32
 */
public interface RedisServerConnection<K, V> {
    /**
     * Asynchronously rewrite the append-only file.
     * 
     * @return String simple-string-reply always `OK`.
     */
    String bgrewriteaof();

    /**
     * Asynchronously save the dataset to disk.
     * 
     * @return String simple-string-reply
     */
    String bgsave();

    K clientGetname();

    String clientSetname(K name);

    String clientKill(String addr);

    String clientPause(long timeout);

    String clientList();

    List<String> configGet(String parameter);

    String configResetstat();

    String configRewrite();

    String configSet(String parameter, String value);

    /**
     * Return the number of keys in the selected database.
     * 
     * @return Long integer-reply
     */
    Long dbsize();

    String debugObject(K key);

    void debugSegfault();

    /**
     * Remove all keys from all databases.
     * 
     * @return String simple-string-reply
     */
    String flushall();

    /**
     * Remove all keys from the current database.
     * 
     * @return String simple-string-reply
     */
    String flushdb();

    /**
     * Get information and statistics about the server.
     * 
     * @return String bulk-string-reply as a collection of text lines.
     */
    String info();

    /**
     * Get information and statistics about the server.
     * 
     * @param section the section type: string
     * @return String bulk-string-reply as a collection of text lines.
     */
    String info(String section);

    /**
     * Get the UNIX time stamp of the last successful save to disk.
     * 
     * @return Date integer-reply an UNIX time stamp.
     */
    Date lastsave();

    /**
     * Synchronously save the dataset to disk.
     * 
     * @return String simple-string-reply The commands returns OK on success.
     */
    String save();

    /**
     * Synchronously save the dataset to disk and then shut down the server.
     */
    void shutdown(boolean save);

    /**
     * Make the server a slave of another instance, or promote it as master.
     * 
     * @param host the host type: string
     * @param port the port type: string
     * @return String simple-string-reply
     */
    String slaveof(String host, int port);

    String slaveofNoOne();

    List<Object> slowlogGet();

    List<Object> slowlogGet(int count);

    Long slowlogLen();

    String slowlogReset();

    /**
     * Internal command used for replication.
     */
    String sync();

    /**
     * Return the current server time.
     * 
     * @return List<V> array-reply specifically:
     * 
     *         A multi bulk reply containing two elements:
     * 
     *         unix time in seconds. microseconds.
     */
    List<V> time();
}
