/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.BundleOutput;
import io.lettuce.core.output.CommandOutput;
import io.netty.buffer.ByteBuf;

/**
 * A transaction bundle that encapsulates MULTI + commands + EXEC as a single atomic unit.
 * <p>
 * This class implements {@link RedisCommand} and encodes all transaction commands together, ensuring they are written
 * atomically to the network buffer. The bundle handles the multi-response protocol (OK for MULTI, QUEUED for each command,
 * array for EXEC) internally.
 * <p>
 * The bundle is designed to be dispatched as a single command through the Lettuce command dispatch mechanism, preventing
 * command interleaving from other threads.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
public class TransactionBundle<K, V> implements RedisCommand<K, V, TransactionResult>, MultiResponseCommand {

    private static final byte ST_INITIAL = 0;

    private static final byte ST_COMPLETED = 1;

    private static final byte ST_CANCELLED = 2;

    private final RedisCodec<K, V> codec;

    private final List<RedisCommand<K, V, ?>> commands;

    private final K[] watchKeys;

    private final BundleOutput<K, V> output;

    private final CompletableFuture<TransactionResult> future;

    private volatile byte status = ST_INITIAL;

    /**
     * Create a new transaction bundle.
     *
     * @param codec the codec for encoding keys and values.
     * @param commands the list of commands to include in the transaction.
     * @param watchKeys optional keys to WATCH before the transaction (may be {@code null}).
     */
    @SafeVarargs
    public TransactionBundle(RedisCodec<K, V> codec, List<RedisCommand<K, V, ?>> commands, K... watchKeys) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        LettuceAssert.notNull(commands, "Commands must not be null");

        this.codec = codec;
        this.commands = new ArrayList<>(commands);
        this.watchKeys = (watchKeys != null && watchKeys.length > 0) ? watchKeys : null;
        this.output = new BundleOutput<>(codec, this.commands, hasWatch());
        this.future = new CompletableFuture<>();
    }

    /**
     * Guard against pathological transaction sizes. A bundle is buffered and written as one atomic {@code ByteBuf}, so an
     * unbounded command count would produce an unbounded contiguous write. Throws when {@code maxAllowed > 0} and {@code size}
     * exceeds it; a non-positive {@code maxAllowed} disables the guard.
     *
     * @param size the number of commands collected for the transaction.
     * @param maxAllowed the configured maximum (see {@link io.lettuce.core.ClientOptions#getMaxTransactionBundleSize()}).
     * @throws RedisException if the guard is enabled and {@code size} exceeds {@code maxAllowed}.
     */
    public static void checkSize(int size, int maxAllowed) {
        if (maxAllowed > 0 && size > maxAllowed) {
            throw new RedisException("Transaction bundle size " + size + " exceeds the configured maximum of " + maxAllowed
                    + " (ClientOptions.maxTransactionBundleSize)");
        }
    }

    /**
     * Check if this bundle includes WATCH keys.
     *
     * @return {@code true} if WATCH keys are present.
     */
    public boolean hasWatch() {
        return watchKeys != null && watchKeys.length > 0;
    }

    /**
     * Get the number of commands in this transaction (excluding MULTI/EXEC/WATCH).
     *
     * @return the command count.
     */
    public int getCommandCount() {
        return commands.size();
    }

    /**
     * Get the expected number of Redis responses for this bundle.
     * <p>
     * This includes:
     * <ul>
     * <li>1 for WATCH (if present)</li>
     * <li>1 for MULTI</li>
     * <li>N for QUEUED responses (one per command)</li>
     * <li>1 for EXEC response (array)</li>
     * </ul>
     *
     * @return the expected response count.
     */
    public int getExpectedResponseCount() {
        return (hasWatch() ? 1 : 0) + 1 + commands.size() + 1;
    }

    /**
     * Get the future that will be completed when the transaction finishes.
     *
     * @return the completable future.
     */
    public CompletableFuture<TransactionResult> getFuture() {
        return future;
    }

    @Override
    public CommandOutput<K, V, TransactionResult> getOutput() {
        return output;
    }

    @Override
    public void complete() {
        if (status == ST_INITIAL) {
            status = ST_COMPLETED;
            if (output.hasError()) {
                // A transaction-level failure was recorded (e.g. WATCH/MULTI error or EXECABORT). Surface it
                // exceptionally rather than returning a bogus TransactionResult. The queued commands never executed, so
                // fail their per-command futures with the same cause instead of leaving them dangling.
                Throwable cause = ExceptionFactory.createExecutionException(output.getError());
                completePendingCommandsExceptionally(cause);
                future.completeExceptionally(cause);
            } else {
                TransactionResult result = output.get();
                if (result.wasDiscarded()) {
                    // WATCH failure / null EXEC array: no per-element results arrive so BundleOutput never completed the
                    // inner command futures. Cancel them so callers awaiting them do not hang. On a successful EXEC the
                    // inner futures are already completed by BundleOutput and this sweep is a no-op.
                    cancelPendingCommands();
                }
                future.complete(result);
            }
        }
    }

    @Override
    public boolean completeExceptionally(Throwable throwable) {
        if (status == ST_INITIAL) {
            status = ST_COMPLETED;
            output.setError(throwable.getMessage());
            completePendingCommandsExceptionally(throwable);
            future.completeExceptionally(throwable);
            return true;
        }
        return false;
    }

    @Override
    public void cancel() {
        if (status == ST_INITIAL) {
            status = ST_CANCELLED;
            cancelPendingCommands();
            future.cancel(false);
        }
    }

    /**
     * Fail every not-yet-completed inner command future with the given cause. Commands already completed by
     * {@link BundleOutput} (the successful-EXEC path) are skipped.
     *
     * @param cause the failure to propagate to the queued command futures.
     */
    private void completePendingCommandsExceptionally(Throwable cause) {
        for (RedisCommand<K, V, ?> command : commands) {
            if (!command.isDone()) {
                command.completeExceptionally(cause);
            }
        }
    }

    /**
     * Cancel every not-yet-completed inner command future. Used when the transaction was discarded or the bundle itself was
     * cancelled, so the queued commands never executed.
     */
    private void cancelPendingCommands() {
        for (RedisCommand<K, V, ?> command : commands) {
            if (!command.isDone()) {
                command.cancel();
            }
        }
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.touch("TransactionBundle.encode(…)");

        // Bundled transactions are at-most-once: a dispatched MULTI/EXEC is never replayed after a disconnect (see
        // DefaultEndpoint#notifyDrainQueuedCommands), so encode() is only ever called once and no output reset is needed.

        // Encode WATCH if present
        if (hasWatch()) {
            encodeWatch(buf);
        }

        // Encode MULTI
        encodeMulti(buf);

        // Encode all transaction commands
        for (RedisCommand<K, V, ?> cmd : commands) {
            cmd.encode(buf);
        }

        // Encode EXEC
        encodeExec(buf);
    }

    private void encodeWatch(ByteBuf buf) {
        // *N\r\n$5\r\nWATCH\r\n...keys...
        int argCount = 1 + watchKeys.length;
        buf.writeByte('*');
        CommandArgs.IntegerArgument.writeInteger(buf, argCount);
        buf.writeBytes(CommandArgs.CRLF);
        CommandArgs.BytesArgument.writeBytes(buf, CommandType.WATCH.getBytes());

        for (K key : watchKeys) {
            CommandArgs.ByteBufferArgument.writeByteBuffer(buf, codec.encodeKey(key));
        }
    }

    private void encodeMulti(ByteBuf buf) {
        // *1\r\n$5\r\nMULTI\r\n
        buf.writeByte('*');
        CommandArgs.IntegerArgument.writeInteger(buf, 1);
        buf.writeBytes(CommandArgs.CRLF);
        CommandArgs.BytesArgument.writeBytes(buf, CommandType.MULTI.getBytes());
    }

    private void encodeExec(ByteBuf buf) {
        // *1\r\n$4\r\nEXEC\r\n
        buf.writeByte('*');
        CommandArgs.IntegerArgument.writeInteger(buf, 1);
        buf.writeBytes(CommandArgs.CRLF);
        CommandArgs.BytesArgument.writeBytes(buf, CommandType.EXEC.getBytes());
    }

    @Override
    public boolean isCancelled() {
        return status == ST_CANCELLED;
    }

    @Override
    public boolean isDone() {
        return status != ST_INITIAL;
    }

    @Override
    public ProtocolKeyword getType() {
        // Return MULTI as the primary command type for logging/tracing purposes
        return CommandType.MULTI;
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        // Bundle doesn't have a single args object
        return null;
    }

    /**
     * Get the list of commands in this transaction.
     *
     * @return unmodifiable list of commands.
     */
    public List<RedisCommand<K, V, ?>> getCommands() {
        return java.util.Collections.unmodifiableList(commands);
    }

    @Override
    public boolean isResponseComplete() {
        return output.isResponseComplete();
    }

    @Override
    public void setOutput(CommandOutput<K, V, TransactionResult> output) {
        throw new UnsupportedOperationException("Cannot change output of TransactionBundle");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [commands=").append(commands.size());
        sb.append(", watch=").append(hasWatch() ? watchKeys.length : 0);
        sb.append(", status=").append(status == ST_INITIAL ? "INITIAL" : status == ST_COMPLETED ? "COMPLETED" : "CANCELLED");
        sb.append(']');
        return sb.toString();
    }

}
