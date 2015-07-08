package com.lambdaworks.redis.cluster;

import java.io.Closeable;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;

/**
 * Connection provider for cluster operations.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
interface ClusterConnectionProvider extends Closeable {
    /**
     * Provide a connection for the intent and cluster slot.
     * 
     * @param intent
     * @param slot
     * @return a valid connection which handles the slot.
     */
    <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, int slot);

    /**
     * Provide a connection for the intent and host/port.
     * 
     * @param intent
     * @param host
     * @param port
     * @return a valid connection to the given host.
     */
    <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, String host, int port);

    /**
     * Close the connections and free all resources.
     */
    @Override
    void close();

    /**
     * Reset the writer state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection.
     */
    void reset();

    /**
     * Update partitions.
     * 
     * @param partitions
     */
    void setPartitions(Partitions partitions);

    /**
     * Disable or enable auto-flush behavior. Default is {@literal true}. If autoFlushCommands is disabled, multiple commands
     * can be issued without writing them actually to the transport. Commands are buffered until a {@link #flushCommands()} is
     * issued. After calling {@link #flushCommands()} commands are sent to the transport and executed by Redis.
     *
     * @param autoFlush state of autoFlush.
     */
    void setAutoFlushCommands(boolean autoFlush);

    /**
     * Flush pending commands. This commands forces a flush on the channel and can be used to buffer ("pipeline") commands to
     * achieve batching. No-op if channel is not connected.
     */
    void flushCommands();

    public static enum Intent {
        READ, WRITE;
    }
}
