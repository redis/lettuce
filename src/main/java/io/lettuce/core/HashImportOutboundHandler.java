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
 * Outbound pipeline adapter that injects the {@code HIMPORT PREPARE} declaring a fieldset ahead of the first {@code SET} that
 * uses it on this connection, and writes the cleanup {@code DISCARD}s a closed fieldset leaves behind.
 * <p>
 * Owns the wire-order {@link TransactionState} and performs every write for its {@link HashImportContext}. A cleanup
 * {@code DISCARD} is held back while a transaction is open, so it never lands inside one.
 * <p>
 * Created fresh per channel init, so a reconnected connection re-declares its fieldsets.
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
