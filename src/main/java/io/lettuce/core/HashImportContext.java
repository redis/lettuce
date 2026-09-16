/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.RedisCommand;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Connection-scoped {@code HIMPORT} state: the fieldsets already declared on this connection, and the cleanup {@code DISCARD}s
 * waiting to be written.
 * <p>
 * Never writes to the channel. {@link HashImportOutboundHandler} drives this from the outbound path and performs every write.
 * <p>
 * Confined to the channel event loop, except {@link #discard} and {@link #isDiscardsPending()}, which are callable from any
 * thread.
 * <p>
 * This class is part of the internal API.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
class HashImportContext {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HashImportContext.class);

    private final Set<HashImport<?>> prepared = Collections.newSetFromMap(new WeakHashMap<>());

    private final Queue<RedisCommand<?, ?, ?>> pendingDiscards = new ConcurrentLinkedQueue<>();

    private volatile boolean discardsPending;

    private volatile RedisCommandBuilder<?, ?> commandBuilder;

    /**
     * Remove and return the pending cleanup {@code DISCARD}s as a single batch for the caller to write. Runs on the event loop;
     * the caller ({@link HashImportOutboundHandler}) is responsible for only invoking this when a {@code DISCARD} is pending
     * ({@link #isDiscardsPending()}) and no transaction is open, so a cleanup {@code DISCARD} never lands inside a transaction.
     *
     * @return the drained cleanup {@code DISCARD} commands, possibly empty.
     */
    List<RedisCommand<?, ?, ?>> drainDiscards() {

        discardsPending = false;
        List<RedisCommand<?, ?, ?>> batch = new ArrayList<>();
        for (RedisCommand<?, ?, ?> command; (command = pendingDiscards.poll()) != null;) {
            batch.add(command);
        }
        return batch;
    }

    /**
     * Inspect an outbound command <em>before</em> it is forwarded and decide whether a {@code HIMPORT PREPARE} must be injected
     * ahead of it. If it is the first {@code HIMPORT SET} for a fieldset on this connection, the {@code PREPARE} is built and
     * returned (and the fieldset recorded for cleanup) for the caller to write. A {@code SET} whose fieldset can no longer be
     * prepared is failed with an {@link IllegalStateException}. Everything else returns {@code null}. Event loop only.
     *
     * @param command the outbound command about to be forwarded.
     * @return the {@code HIMPORT PREPARE} to inject ahead of {@code command}, or {@code null} if none is needed.
     */
    RedisCommand<?, ?, ?> prepareFor(RedisCommand<?, ?, ?> command) {

        if (command.getType() != CommandType.HIMPORT) {
            return null;
        }

        HashImportSetCommand<?, ?> set = unwrapSet(command);
        if (set == null) {
            return null; // a HIMPORT PREPARE/DISCARD we injected ourselves, not a user SET
        }

        HashImport<?> fieldset = set.fieldset();
        if (prepared.contains(fieldset)) {
            // Steady state: nothing to do. A cleaned-up fieldset cannot reach here — the SET holds a HashImport reservation
            // until it completes, so cleanup cannot have run, and a cancelled SET is skipped as done before this point.
            return null;
        }

        if (!fieldset.registerConnection(this)) {
            command.completeExceptionally(new IllegalStateException("HashImport has been discarded and must not be reused"));
            return null;
        }

        return prepare(set.codec(), fieldset);
    }

    boolean isDiscardsPending() {
        return discardsPending;
    }

    /**
     * Enqueue a best-effort {@code HIMPORT DISCARD} for {@code fieldset}. Callable from any thread: the command is built here
     * on the caller's thread (encoding on the caller is how every Lettuce command is built) and queued; it is handed back by
     * {@link #drainDiscards()} and written by the handler on the next write to this connection. If nothing was ever prepared on
     * this connection ({@code commandBuilder} still {@code null}), there is nothing to discard.
     */
    void discard(HashImport<?> fieldset) {
        if (commandBuilder == null) {
            return;
        }
        pendingDiscards.add(new AsyncCommand<>(himportDiscard(fieldset)));
        discardsPending = true;
    }

    void clear() {
        this.prepared.clear();
        this.pendingDiscards.clear();
        this.discardsPending = false;
    }

    private RedisCommand<?, ?, ?> prepare(RedisCodec<?, ?> codec, HashImport<?> fieldset) {
        if (commandBuilder == null) {
            commandBuilder = new RedisCommandBuilder<>(codec);
        }
        prepared.add(fieldset);
        AsyncCommand<?, ?, String> command = new AsyncCommand<>(himportPrepare(fieldset));
        command.onComplete((status, error) -> {
            if (error != null) {
                prepared.remove(fieldset);
                logger.warn("HIMPORT PREPARE for fieldset {} failed; it will be re-prepared on the next himportSet", fieldset,
                        error);
            }
        });
        return command;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static HashImportSetCommand<?, ?> unwrapSet(RedisCommand<?, ?, ?> command) {
        return CommandWrapper.unwrap((RedisCommand) command, HashImportSetCommand.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private RedisCommand<?, ?, String> himportPrepare(HashImport<?> fieldset) {
        return ((RedisCommandBuilder) commandBuilder).himportPrepare(fieldset);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private RedisCommand<?, ?, Boolean> himportDiscard(HashImport<?> fieldset) {
        return ((RedisCommandBuilder) commandBuilder).himportDiscard(fieldset);
    }

}
