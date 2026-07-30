/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * A {@code HIMPORT SET} command that carries the {@link HashImport} fieldset it imports into.
 * <p>
 * The attached fieldset lets the outbound {@code HashImportOutboundHandler} lazily inject the required {@code HIMPORT PREPARE}
 * ahead of this {@code SET} the first time the fieldset is seen on a physical connection, and record that connection on the
 * fieldset for best-effort cleanup on {@link HashImport#close()}. It is discovered on the write path via
 * {@link io.lettuce.core.protocol.CommandWrapper#unwrap(io.lettuce.core.protocol.RedisCommand, Class)}.
 * <p>
 * This class is part of the internal API.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.7
 */
class HashImportSetCommand<K, V> extends Command<K, V, String> {

    private final HashImport<K> fieldset;

    private final RedisCodec<K, V> codec;

    HashImportSetCommand(ProtocolKeyword type, CommandOutput<K, V, String> output, CommandArgs<K, V> args,
            HashImport<K> fieldset, RedisCodec<K, V> codec) {
        super(type, output, args);
        this.fieldset = fieldset;
        this.codec = codec;
    }

    /**
     * @return the fieldset this {@code SET} imports into.
     */
    HashImport<K> fieldset() {
        return fieldset;
    }

    /**
     * @return the codec of the connection, used to build the injected {@code PREPARE} and to encode the eventual
     *         {@code DISCARD}.
     */
    RedisCodec<K, V> codec() {
        return codec;
    }

}
