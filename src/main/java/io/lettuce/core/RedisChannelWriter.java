package io.lettuce.core;

import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.api.AsyncCloseable;
import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Writer for a channel. Writers push commands on to the communication channel and maintain a state for the commands.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisChannelWriter extends Closeable, AsyncCloseable {

    /**
     * Write a command on the channel. The command may be changed/wrapped during write and the written instance is returned
     * after the call.
     *
     * @param command the Redis command.
     * @param <T> result type
     * @return the written Redis command.
     */
    <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command);

    /**
     * Write multiple commands on the channel. The commands may be changed/wrapped during write and the written instance is
     * returned after the call.
     *
     * @param commands the Redis commands.
     * @param <K> key type
     * @param <V> value type
     * @return the written redis command
     */
    <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands);

    @Override
    void close();

    /**
     * Asynchronously close the {@link RedisChannelWriter}.
     *
     * @return future for result synchronization.
     * @since 5.1
     */
    @Override
    CompletableFuture<Void> closeAsync();

    /**
     * Reset the command state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection (e.g. errors during external SSL tunneling). Calling this
     * method will reset the protocol state, therefore it is considered unsafe.
     *
     * @deprecated since 5.2. This method is unsafe and can cause protocol offsets (i.e. Redis commands are completed with
     *             previous command values).
     */
    @Deprecated
    void reset();

    /**
     * Set the corresponding connection facade in order to notify it about channel active/inactive state.
     *
     * @param connection the connection facade (external connection object)
     */
    void setConnectionFacade(ConnectionFacade connection);

    /**
     * Disable or enable auto-flush behavior. Default is {@code true}. If autoFlushCommands is disabled, multiple commands can
     * be issued without writing them actually to the transport. Commands are buffered until a {@link #flushCommands()} is
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

    /**
     * @return the {@link ClientResources}.
     * @since 5.1
     */
    ClientResources getClientResources();

}
