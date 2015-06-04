package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.RedisFuture;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Output type.
 * @since 3.0
 */
public interface RedisCommand<K, V, T> extends RedisFuture<T> {

    /**
     * The command output. Can be null.
     * 
     * @return the command output.
     */
    CommandOutput<K, V, T> getOutput();

    /**
     * Complete a command.
     */
    void complete();

    /**
     * 
     * @return the current command args
     */
    CommandArgs<K, V> getArgs();

    /**
     * Encode the command.
     * 
     * @param buf byte buffer to operate on.
     */
    void encode(ByteBuf buf);

    /**
     * If not already completed, causes invocations of {@link #get()} and related methods to throw the given exception.
     *
     * @param throwable the exception
     * @return {@code true} if this invocation caused this CompletableFuture to transition to a completed state, else
     *         {@code false}
     */
    boolean completeExceptionally(Throwable throwable);
}
