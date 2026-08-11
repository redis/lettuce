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
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.RedisCommand;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Per-physical-connection component shared between {@link HashImportOutboundHandler} and the {@link HashImport} templates
 * prepared on that connection. It owns the connection-scoped {@code HIMPORT} state: which fieldsets have already been prepared
 * (so {@code PREPARE} is injected once), the lazily-built {@link #commandBuilder}, and the queue of cleanup {@code DISCARD}s
 * awaiting a quiet moment. Transaction tracking is <em>not</em> owned here — the {@link HashImportOutboundHandler} owns the
 * wire-order {@link TransactionState} and uses it to decide when to drive this component's flush.
 * <p>
 * The {@link HashImportOutboundHandler} drives it from the outbound write path on the channel event loop, passing the current
 * {@link ChannelHandlerContext}: {@link #prepareFor} before each write is forwarded, and {@link #flushDeferredDiscards} after a
 * write once no transaction is open and a cleanup {@code DISCARD} is pending ({@link #isDiscardsPending()}). Both touch the
 * event-loop-confined state ({@link #prepared}, {@link #commandBuilder}).
 * <p>
 * {@link HashImport#close()} calls {@link #discard} from an arbitrary thread; it builds the {@code DISCARD} command there (as
 * every Lettuce command is encoded on its caller's thread) and enqueues it onto a concurrent queue without writing. The queued
 * commands are flushed on the event loop by the next write to this connection — as one batch, immediately if no transaction is
 * open, otherwise once {@code EXEC}/{@code DISCARD} ends it. A cleanup {@code DISCARD} therefore never lands inside a
 * transaction, and cleanup is best-effort: if the connection never sees another write, the server-side state is released when
 * the connection closes.
 * <p>
 * This class is part of the internal API.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
class HashImportContext {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HashImportContext.class);

    private final Set<HashImport<?>> prepared = Collections.newSetFromMap(new WeakHashMap<>());

    private final BlockingQueue<RedisCommand<?, ?, ?>> pendingDiscards = new LinkedBlockingQueue<>();

    private volatile boolean discardsPending;

    private volatile RedisCommandBuilder<?, ?> commandBuilder;

    /**
     * Drain the pending cleanup {@code DISCARD}s and write them as a single batch. Runs on the event loop; the caller
     * ({@link HashImportOutboundHandler}) is responsible for only invoking this when a {@code DISCARD} is pending
     * ({@link #isDiscardsPending()}) and no transaction is open, so a cleanup {@code DISCARD} never lands inside a transaction.
     */
    void flushDeferredDiscards(ChannelHandlerContext ctx) {

        discardsPending = false;
        List<RedisCommand<?, ?, ?>> batch = new ArrayList<>();
        pendingDiscards.drainTo(batch);
        if (!batch.isEmpty()) {
            ctx.writeAndFlush(batch);
        }
    }

    /**
     * Inspect an outbound command <em>before</em> it is forwarded. If it is the first {@code HIMPORT SET} for a fieldset on
     * this connection, inject a {@code PREPARE} ahead of it and record the fieldset for cleanup; a {@code SET} for an
     * already-closed fieldset is failed client-side with an {@link IllegalStateException}. Everything else passes through.
     * Event loop only.
     */
    void prepareFor(RedisCommand<?, ?, ?> command, ChannelHandlerContext ctx) {

        if (command.getType() != CommandType.HIMPORT) {
            return;
        }

        HashImportSetCommand<?, ?> set = unwrapSet(command);
        if (set == null) {
            return; // a HIMPORT PREPARE/DISCARD we injected ourselves, not a user SET
        }

        HashImport<?> fieldset = set.fieldset();
        if (prepared.contains(fieldset)) {
            if (fieldset.isDiscarded()) {
                command.completeExceptionally(
                        new IllegalStateException("HashImport has been discarded and must not be reused"));
            }
            return;
        }

        if (!fieldset.registerConnection(this)) {
            command.completeExceptionally(new IllegalStateException("HashImport has been discarded and must not be reused"));
            return;
        }

        prepare(set.codec(), fieldset, ctx);
    }

    boolean isDiscardsPending() {
        return discardsPending;
    }

    /**
     * Enqueue a best-effort {@code HIMPORT DISCARD} for {@code fieldset}. Callable from any thread: the command is built here
     * on the caller's thread (encoding on the caller is how every Lettuce command is built) and queued; the write happens on
     * the event loop via {@link #flushDeferredDiscards} on the next write to this connection. If nothing was ever prepared on
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

    private void prepare(RedisCodec<?, ?> codec, HashImport<?> fieldset, ChannelHandlerContext ctx) {
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
        ctx.write(command);
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
