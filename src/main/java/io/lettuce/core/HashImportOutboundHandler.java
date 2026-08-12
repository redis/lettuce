/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.Collection;
import java.util.List;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Outbound pipeline adapter that drives a per-connection {@link HashImportContext} from the netty write path. It splits each
 * outbound write into its individual commands (a single command or a batched {@link Collection}) and, on the channel event
 * loop:
 * <ol>
 * <li>feeds each command to the {@link TransactionState} so the wire-order {@code MULTI} state stays current, and asks the
 * context whether a {@code HIMPORT PREPARE} must be injected ahead of the first {@code SET} for a fieldset
 * ({@link HashImportContext#prepareFor}) — writing the returned command itself,</li>
 * <li>after forwarding, if a cleanup {@code DISCARD} is pending ({@link HashImportContext#isDiscardsPending()}) and no
 * transaction is open, drains it ({@link HashImportContext#drainDiscards()}) and writes it so it lands right after the
 * {@code EXEC}/{@code DISCARD} that ended the transaction — and never inside one.</li>
 * </ol>
 * The handler owns transaction tracking (it holds and drives {@link TransactionState}) and performs every write; the context is
 * netty-unaware and only produces the commands to send and holds the channel-scoped state. A fresh handler, context, and
 * transaction state are created per channel init, so re-preparation after a reconnect happens automatically.
 * <p>
 * This class is part of the internal API.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
class HashImportOutboundHandler extends ChannelDuplexHandler {

    private final HashImportContext context;

    private final TransactionState transactionState;

    HashImportOutboundHandler() {
        this.context = new HashImportContext();
        this.transactionState = new TransactionState();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (msg instanceof RedisCommand) {
            observeAndPrepare(ctx, (RedisCommand<?, ?, ?>) msg);
        } else if (msg instanceof Collection) {
            for (Object element : (Collection<?>) msg) {
                if (element instanceof RedisCommand) {
                    observeAndPrepare(ctx, (RedisCommand<?, ?, ?>) element);
                }
            }
        }

        super.write(ctx, msg, promise);

        if (context.isDiscardsPending() && !transactionState.isOpen()) {
            List<RedisCommand<?, ?, ?>> discards = context.drainDiscards();
            if (!discards.isEmpty()) {
                ctx.writeAndFlush(discards);
            }
        }
    }

    private void observeAndPrepare(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command) {

        if (command.isDone()) {
            return;
        }

        transactionState.observe(command);

        if (!transactionState.isOpen() && command.getType() == CommandType.HIMPORT) {
            RedisCommand<?, ?, ?> prepare = context.prepareFor(command);
            if (prepare != null) {
                ctx.write(prepare);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        transactionState.reset();
        context.clear();
        super.channelInactive(ctx);
    }

}
