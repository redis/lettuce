/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.TransactionResult;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Output handler for {@link io.lettuce.core.protocol.TransactionBundle}.
 * <p>
 * This output handles the multi-phase response of a bundled transaction:
 * <ol>
 * <li>WATCH response: +OK (if WATCH keys present)</li>
 * <li>MULTI response: +OK</li>
 * <li>QUEUED responses: +QUEUED for each command</li>
 * <li>EXEC response: array of results (or null if WATCH failed)</li>
 * </ol>
 * <p>
 * The output routes EXEC array elements to the appropriate command outputs and collects results into a
 * {@link TransactionResult}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
public class BundleOutput<K, V> extends CommandOutput<K, V, TransactionResult> {

    /**
     * Response processing phases.
     */
    enum Phase {

        WATCH, MULTI, QUEUED, EXEC

    }

    private final List<RedisCommand<K, V, ?>> commands;

    private final boolean hasWatch;

    private final List<Object> responses;

    private Phase currentPhase;

    private int queuedCount;

    private int execIndex;

    private Boolean discarded;

    private Integer execArraySize;

    private int responseCount;

    private final int expectedResponseCount;

    /**
     * Create a new BundleOutput.
     *
     * @param codec the codec.
     * @param commands the list of commands in the transaction.
     * @param hasWatch whether WATCH is included.
     */
    public BundleOutput(RedisCodec<K, V> codec, List<RedisCommand<K, V, ?>> commands, boolean hasWatch) {
        super(codec, null);
        this.commands = commands;
        this.hasWatch = hasWatch;
        this.responses = new ArrayList<>(commands.size());
        this.currentPhase = hasWatch ? Phase.WATCH : Phase.MULTI;
        this.queuedCount = 0;
        this.execIndex = 0;
        this.discarded = null;
        this.responseCount = 0;
        // Expected: WATCH (if present) + MULTI + N QUEUED + EXEC
        this.expectedResponseCount = (hasWatch ? 1 : 0) + 1 + commands.size() + 1;
    }

    @Override
    public void set(ByteBuffer bytes) {
        switch (currentPhase) {
            case WATCH:
            case MULTI:
                // Expect +OK, phase advancement happens in complete(0)
                break;
            case QUEUED:
                // QUEUED responses are filtered by RSM, so set() is not called for them
                // Phase advancement happens in complete(0)
                break;
            case EXEC:
                // Route to current command's output
                if (execIndex < commands.size()) {
                    RedisCommand<K, V, ?> cmd = commands.get(execIndex);
                    if (cmd.getOutput() != null) {
                        cmd.getOutput().set(bytes);
                    }
                }
                break;
        }
    }

    @Override
    public void set(long integer) {
        if (currentPhase == Phase.EXEC && execIndex < commands.size()) {
            RedisCommand<K, V, ?> cmd = commands.get(execIndex);
            if (cmd.getOutput() != null) {
                cmd.getOutput().set(integer);
            }
        }
    }

    @Override
    public void set(double number) {
        if (currentPhase == Phase.EXEC && execIndex < commands.size()) {
            RedisCommand<K, V, ?> cmd = commands.get(execIndex);
            if (cmd.getOutput() != null) {
                cmd.getOutput().set(number);
            }
        }
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        if (currentPhase == Phase.EXEC && execIndex < commands.size()) {
            RedisCommand<K, V, ?> cmd = commands.get(execIndex);
            if (cmd.getOutput() != null) {
                cmd.getOutput().setSingle(bytes);
            }
        }
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        if (currentPhase == Phase.EXEC && execIndex < commands.size()) {
            RedisCommand<K, V, ?> cmd = commands.get(execIndex);
            if (cmd.getOutput() != null) {
                cmd.getOutput().setBigNumber(bytes);
            }
        }
    }

    @Override
    public void set(boolean value) {
        if (currentPhase == Phase.EXEC && execIndex < commands.size()) {
            RedisCommand<K, V, ?> cmd = commands.get(execIndex);
            if (cmd.getOutput() != null) {
                cmd.getOutput().set(value);
            }
        }
    }

    @Override
    public void setError(ByteBuffer error) {
        switch (currentPhase) {
            case WATCH:
            case MULTI:
                // Error during WATCH or MULTI - transaction failed to start
                super.setError(error);
                break;
            case QUEUED:
                // Error during QUEUED (syntax error in command)
                // The command is still queued but will fail during EXEC
                queuedCount++;
                if (queuedCount >= commands.size()) {
                    currentPhase = Phase.EXEC;
                }
                break;
            case EXEC:
                // Error for a specific command within EXEC results
                if (execIndex < commands.size()) {
                    RedisCommand<K, V, ?> cmd = commands.get(execIndex);
                    if (cmd.getOutput() != null) {
                        cmd.getOutput().setError(error);
                    }
                }
                break;
        }
    }

    @Override
    public void multi(int count) {
        switch (currentPhase) {
            case WATCH:
            case MULTI:
                // Shouldn't happen - WATCH/MULTI return simple strings
                advancePhase();
                break;
            case QUEUED:
                // Shouldn't happen during QUEUED phase
                break;
            case EXEC:
                if (execArraySize == null) {
                    // First multi() call - this is the EXEC array size
                    execArraySize = count;
                    if (count == -1) {
                        // Null array = WATCH failed, transaction discarded
                        discarded = true;
                    } else {
                        discarded = false;
                    }
                } else {
                    // Nested array within a command result
                    if (execIndex < commands.size()) {
                        RedisCommand<K, V, ?> cmd = commands.get(execIndex);
                        if (cmd.getOutput() != null) {
                            cmd.getOutput().multi(count);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void complete(int depth) {
        if (depth == 0) {
            // A complete Redis response has been parsed at depth 0
            responseCount++;

            // Handle phase transitions based on the phase we're completing
            switch (currentPhase) {
                case WATCH:
                    // WATCH response complete, move to MULTI
                    currentPhase = Phase.MULTI;
                    break;
                case MULTI:
                    // MULTI response complete, move to QUEUED (or EXEC if no commands)
                    if (commands.isEmpty()) {
                        currentPhase = Phase.EXEC;
                    } else {
                        currentPhase = Phase.QUEUED;
                    }
                    break;
                case QUEUED:
                    // QUEUED response complete for one command
                    queuedCount++;
                    if (queuedCount >= commands.size()) {
                        currentPhase = Phase.EXEC;
                    }
                    break;
                case EXEC:
                    // EXEC response fully complete - nothing to do
                    break;
            }
        }

        // Handle EXEC array element completions
        if (currentPhase == Phase.EXEC && execArraySize != null) {
            if (depth == 1) {
                // Completed one element in the EXEC array
                if (execIndex < commands.size()) {
                    RedisCommand<K, V, ?> cmd = commands.get(execIndex);
                    CommandOutput<K, V, ?> cmdOutput = cmd.getOutput();
                    if (cmdOutput != null) {
                        cmdOutput.complete(depth - 1);
                        responses.add(!cmdOutput.hasError() ? cmdOutput.get()
                                : ExceptionFactory.createExecutionException(cmdOutput.getError()));
                    } else {
                        responses.add(null);
                    }
                    cmd.complete();
                }
                execIndex++;
            } else if (depth > 1 && execIndex < commands.size()) {
                // Nested completion within a command result
                RedisCommand<K, V, ?> cmd = commands.get(execIndex);
                if (cmd.getOutput() != null) {
                    cmd.getOutput().complete(depth - 1);
                }
            }
        }
    }

    @Override
    public TransactionResult get() {
        boolean wasDiscarded = discarded != null && discarded;
        return new DefaultTransactionResult(wasDiscarded, responses);
    }

    private void advancePhase() {
        switch (currentPhase) {
            case WATCH:
                currentPhase = Phase.MULTI;
                break;
            case MULTI:
                if (commands.isEmpty()) {
                    currentPhase = Phase.EXEC;
                } else {
                    currentPhase = Phase.QUEUED;
                }
                break;
            case QUEUED:
                currentPhase = Phase.EXEC;
                break;
            case EXEC:
                // Already at final phase
                break;
        }
    }

    /**
     * Get the current processing phase.
     *
     * @return the current phase.
     */
    Phase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Check if the transaction was discarded (WATCH failure).
     *
     * @return {@code true} if discarded, {@code false} if not, {@code null} if unknown.
     */
    Boolean isDiscarded() {
        return discarded;
    }

    /**
     * Check if all expected responses have been received.
     * <p>
     * This method is used by {@link io.lettuce.core.protocol.MultiResponseCommand#isResponseComplete()} to determine if the
     * command can be completed and removed from the stack.
     *
     * @return {@code true} if all responses have been received.
     */
    public boolean isResponseComplete() {
        return responseCount >= expectedResponseCount;
    }

}
