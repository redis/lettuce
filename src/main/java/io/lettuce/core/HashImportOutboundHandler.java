/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.HashImportPrepareCommand;
import io.lettuce.core.protocol.RedisCommand;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Outbound pipeline handler that lazily prepares {@code HIMPORT} fieldsets per physical connection.
 * <p>
 * Positioned outbound of {@code CommandHandler}, it inspects every write for a {@link HashImportSetCommand}. The first time a
 * given fieldset is seen on this channel it injects a fire-and-forget {@code HIMPORT PREPARE} ahead of the {@code SET} and
 * records the connection on the fieldset (for cleanup via {@link HashImport#close()}). Subsequent {@code SET}s for the same
 * fieldset pass through untouched. Because {@link #write} runs on the channel event loop, the check-and-inject is atomic and
 * correctly ordered without locking; because a reconnect yields a fresh handler (and {@link #channelInactive} clears the set
 * defensively), a {@code SET} traversing the pipeline after a reconnect re-prepares automatically — including buffered and
 * requeued commands flushed as a batch.
 * <p>
 * The handler is codec-agnostic: the {@link HashImportSetCommand} carries its connection's codec, so a fresh {@code PREPARE} is
 * built per injection (required for reconnect/redirect, where a prebuilt command could not be reused).
 * <p>
 * This class is part of the internal API.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
class HashImportOutboundHandler extends ChannelDuplexHandler {

    private final Set<HashImport<?>> prepared = Collections.newSetFromMap(new WeakHashMap<>());

    // lazily built from the first HIMPORT SET's codec and reused; a channel has one codec for its lifetime, and write() is
    // single-threaded on the event loop, so no synchronization is needed
    private RedisCommandBuilder<?, ?> commandBuilder;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        // Fast path: only HIMPORT commands can be a SET carrying a fieldset, so this enum check filters out every other
        // command (and batch element) before any unwrap or prepare work.
        if (msg instanceof RedisCommand) {
            RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) msg;
            if (command.getType() == CommandType.HIMPORT) {
                maybePrepare(ctx, command);
            }
        } else if (msg instanceof Collection) {
            for (Object element : (Collection<?>) msg) {
                if (element instanceof RedisCommand && ((RedisCommand<?, ?, ?>) element).getType() == CommandType.HIMPORT) {
                    maybePrepare(ctx, (RedisCommand<?, ?, ?>) element);
                }
            }
        }

        super.write(ctx, msg, promise);
    }

    private <K, V> void maybePrepare(ChannelHandlerContext ctx, RedisCommand<?, ?, ?> command) {

        @SuppressWarnings("unchecked")
        HashImportSetCommand<K, V> set = CommandWrapper.unwrap((RedisCommand<K, V, String>) command,
                HashImportSetCommand.class);
        if (set == null) {
            return;
        }

        HashImport<K> fieldset = set.fieldset();
        if (prepared.contains(fieldset)) {
            return;
        }

        RedisCodec<K, V> codec = set.codec();
        if (!fieldset.registerConnection(ctx.channel(), codec)) {
            return;
        }

        ctx.write(new HashImportPrepareCommand<>(new AsyncCommand<>(commandBuilder(codec).himportPrepare(fieldset))));
        prepared.add(fieldset);
    }

    @SuppressWarnings("unchecked")
    private <K, V> RedisCommandBuilder<K, V> commandBuilder(RedisCodec<K, V> codec) {
        if (commandBuilder == null) {
            commandBuilder = new RedisCommandBuilder<>(codec);
        }
        return (RedisCommandBuilder<K, V>) commandBuilder;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        prepared.clear();
        super.channelInactive(ctx);
    }

}
