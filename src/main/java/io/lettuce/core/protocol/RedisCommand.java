package io.lettuce.core.protocol;

import io.lettuce.core.output.CommandOutput;
import io.netty.buffer.ByteBuf;

/**
 * A redis command that holds an output, arguments and a state, whether it is completed or not.
 *
 * Commands can be wrapped. Outer commands have to notify inner commands but inner commands do not communicate with outer
 * commands.
 *
 * @author Mark Paluch
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
     * Complete this command by attaching the given {@link Throwable exception}.
     *
     * @param throwable the exception
     * @return {@code true} if this invocation caused this CompletableFuture to transition to a completed state, else
     *         {@code false}
     */
    boolean completeExceptionally(Throwable throwable);

    /**
     * Attempts to cancel execution of this command. This attempt will fail if the task has already completed, has already been
     * cancelled, or could not be cancelled for some other reason.
     */
    void cancel();

    /**
     * @return the current command args.
     */
    CommandArgs<K, V> getArgs();

    /**
     *
     * @return the Redis command type like {@code SADD}, {@code HMSET}, {@code QUIT}.
     */
    ProtocolKeyword getType();

    /**
     * Encode the command.
     *
     * @param buf byte buffer to operate on.
     */
    void encode(ByteBuf buf);

    /**
     * Returns {@code true} if this task was cancelled before it completed normally.
     *
     * @return {@code true} if the command was cancelled before it completed normally.
     */
    boolean isCancelled();

    /**
     * Returns {@code true} if this task completed.
     *
     * Completion may be due to normal termination, an exception, or cancellation. In all of these cases, this method will
     * return {@code true}.
     *
     * @return {@code true} if this task completed.
     */
    boolean isDone();

    /**
     * Mark this command as having failed during encoding.
     * This indicates the command was never successfully sent to Redis.
     * 
     * @since 6.2.2-uber-0.5
     */
    void markEncodingError();

    /**
     * Returns {@code true} if this command failed during encoding and was never sent to Redis.
     * Commands with encoding errors should be cleaned up from the response queue without
     * waiting for Redis responses.
     * 
     * @return {@code true} if this command failed during encoding
     * @since 6.2.2-uber-0.5
     */
    boolean hasEncodingError();

    /**
     * Set a new output. Only possible as long as the command is not completed/cancelled.
     *
     * @param output the new command output
     * @throws IllegalStateException if the command is cancelled/completed
     */
    void setOutput(CommandOutput<K, V, T> output);

}
