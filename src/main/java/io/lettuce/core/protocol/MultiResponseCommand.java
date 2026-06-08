/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

/**
 * Marker interface for commands that expect multiple Redis responses.
 * <p>
 * When a command implements this interface, the {@link CommandHandler} will keep the command on the stack until
 * {@link #isResponseComplete()} returns {@code true}. This is used for commands like {@link TransactionBundle} that send
 * multiple Redis commands (WATCH, MULTI, commands, EXEC) and expect multiple responses.
 *
 * @author Tihomir Mateev
 * @since 7.6
 */
public interface MultiResponseCommand {

    /**
     * Check if all expected responses have been received.
     *
     * @return {@code true} if all responses have been received and the command can be completed.
     */
    boolean isResponseComplete();

}
