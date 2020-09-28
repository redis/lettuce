/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * Set a new output. Only possible as long as the command is not completed/cancelled.
     *
     * @param output the new command output
     * @throws IllegalStateException if the command is cancelled/completed
     */
    void setOutput(CommandOutput<K, V, T> output);

}
