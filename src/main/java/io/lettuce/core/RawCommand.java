/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Represents a raw Redis command that can be added to a {@link TransactionBuilder}.
 * <p>
 * This interface allows users to add custom or unsupported commands to a transaction by providing the command type, arguments,
 * and expected output type.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
public interface RawCommand<K, V> {

    /**
     * Get the command type.
     *
     * @return the protocol keyword representing the command.
     */
    ProtocolKeyword getType();

    /**
     * Get the command arguments.
     *
     * @return the command arguments, may be {@code null}.
     */
    CommandArgs<K, V> getArgs();

    /**
     * Get the command output handler.
     *
     * @return the command output, may be {@code null} for fire-and-forget commands.
     */
    CommandOutput<K, V, ?> getOutput();

    /**
     * Create a new raw command.
     *
     * @param type the command type.
     * @param output the command output.
     * @param args the command arguments.
     * @param <K> key type.
     * @param <V> value type.
     * @return a new raw command instance.
     */
    static <K, V> RawCommand<K, V> of(ProtocolKeyword type, CommandOutput<K, V, ?> output, CommandArgs<K, V> args) {
        return new DefaultRawCommand<>(type, output, args);
    }

    /**
     * Create a new raw command without arguments.
     *
     * @param type the command type.
     * @param output the command output.
     * @param <K> key type.
     * @param <V> value type.
     * @return a new raw command instance.
     */
    static <K, V> RawCommand<K, V> of(ProtocolKeyword type, CommandOutput<K, V, ?> output) {
        return new DefaultRawCommand<>(type, output, null);
    }

    /**
     * Convert this raw command to a {@link RedisCommand}.
     *
     * @param codec the codec (unused, but may be needed for future extensions).
     * @return the Redis command.
     */
    @SuppressWarnings("unchecked")
    default RedisCommand<K, V, ?> toCommand(RedisCodec<K, V> codec) {
        return new Command<>(getType(), (CommandOutput<K, V, Object>) getOutput(), getArgs());
    }

}

/**
 * Default implementation of {@link RawCommand}.
 */
class DefaultRawCommand<K, V> implements RawCommand<K, V> {

    private final ProtocolKeyword type;

    private final CommandOutput<K, V, ?> output;

    private final CommandArgs<K, V> args;

    DefaultRawCommand(ProtocolKeyword type, CommandOutput<K, V, ?> output, CommandArgs<K, V> args) {
        this.type = type;
        this.output = output;
        this.args = args;
    }

    @Override
    public ProtocolKeyword getType() {
        return type;
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return args;
    }

    @Override
    public CommandOutput<K, V, ?> getOutput() {
        return output;
    }

}
