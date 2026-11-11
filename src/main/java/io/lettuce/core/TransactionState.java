/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Tracks whether a {@code MULTI} transaction is currently open on a connection, by observing the commands written to it in wire
 * order: {@code MULTI} opens a transaction and {@code EXEC}/{@code DISCARD} closes it. A nested {@code MULTI} is a no-op (Redis
 * keeps the single open transaction), so the state is an idempotent flag rather than a counter.
 * <p>
 * This is a plain state machine with no synchronization: callers must drive it from a single thread (the channel event loop).
 * <p>
 * This class is part of the internal API.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
class TransactionState {

    private boolean open;

    /**
     * Observe an outbound command and update the transaction state.
     *
     * @param command the command being written.
     * @return {@code true} if this command just ended an open transaction (i.e. an {@code EXEC}/{@code DISCARD} closing an open
     *         {@code MULTI}); {@code false} otherwise.
     */
    boolean observe(RedisCommand<?, ?, ?> command) {

        if (command.getType() == CommandType.MULTI) {
            open = true;
        } else if (command.getType() == CommandType.EXEC || command.getType() == CommandType.DISCARD) {
            if (open) {
                open = false;
                return true;
            }
        }
        return false;
    }

    boolean isOpen() {
        return open;
    }

    void reset() {
        open = false;
    }

}
