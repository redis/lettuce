package com.lambdaworks.redis.protocol;

import io.netty.buffer.ByteBuf;

/**
 * A redis command that holds an output, arguments and a state, whether it is completed or not.
 *
 * Commands can be wrapped. Outer commands have to notify inner commands but inner commands do not communicate with outer
 * commands.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Output type.
 * @since 3.0
 */
public interface RedisCommand<K, V, T> {

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
     * Cancel a command.
     */
    void cancel();

    /**
     * 
     * @return the current command args
     */
    CommandArgs<K, V> getArgs();

    /**
     *
     * @param throwable the exception
     * @return {@code true} if this invocation caused this CompletableFuture to transition to a completed state, else
     *         {@code false}
     */
    boolean completeExceptionally(Throwable throwable);

    /**
     *
     * @return the redis command type like {@literal SADD}, {@literal HMSET}, {@literal QUIT}.
     */
    ProtocolKeyword getType();

    /**
     * Encode the command.
     *
     * @param buf byte buffer to operate on.
     */
    void encode(ByteBuf buf);

    /**
     *
     * @return true if the command is cancelled.
     */
    boolean isCancelled();

    /**
     * Set a new output.
     * 
     * @param output
     */
    void setOutput(CommandOutput<K, V, T> output);
}
