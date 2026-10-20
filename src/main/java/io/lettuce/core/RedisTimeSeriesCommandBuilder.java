/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandType.DEBUG;
import static io.lettuce.core.protocol.CommandType.TS_ALTER;
import static io.lettuce.core.protocol.CommandType.TS_CREATE;
import static io.lettuce.core.protocol.CommandType.TS_DEL;
import static io.lettuce.core.protocol.CommandType.TS_INFO;

import io.lettuce.core.Range.Boundary;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.timeseries.TsInfoValue;
import io.lettuce.core.timeseries.TsInfoValueParser;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Redis TimeSeries commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
class RedisTimeSeriesCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisTimeSeriesCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, String> tsCreate(K key) {
        notNullKey(key);

        return createCommand(TS_CREATE, new StatusOutput<>(codec), key);
    }

    Command<K, V, String> tsCreate(K key, TsCreateArgs createArgs) {
        notNullKey(key);
        LettuceAssert.notNull(createArgs, "TsCreateArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        createArgs.build(args);

        return createCommand(TS_CREATE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tsAlter(K key, TsAlterArgs alterArgs) {
        notNullKey(key);
        LettuceAssert.notNull(alterArgs, "TsAlterArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        alterArgs.build(args);

        return createCommand(TS_ALTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> tsDel(K key, Range<Long> range) {
        notNullKey(key);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        addRange(args, range);

        return createCommand(TS_DEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, TsInfoValue<K>> tsInfo(K key) {
        notNullKey(key);

        return createCommand(TS_INFO, new EncodedComplexOutput<>(codec, new TsInfoValueParser<>(codec)), key);
    }

    Command<K, V, TsInfoValue<K>> tsInfo(K key, boolean debug) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        if (debug) {
            args.add(DEBUG);
        }

        return createCommand(TS_INFO, new EncodedComplexOutput<>(codec, new TsInfoValueParser<>(codec)), args);
    }

    /**
     * Add the {@code from} and {@code to} timestamps of {@code range}. Unbounded sides become {@code -} and {@code +}. Because
     * timestamps are integer milliseconds, an exclusive bound becomes its inclusive neighbor.
     */
    private static void addRange(CommandArgs<?, ?> args, Range<Long> range) {

        Boundary<Long> lower = range.getLower();
        Boundary<Long> upper = range.getUpper();

        if (lower.isUnbounded() || lower.getValue() == null) {
            args.add("-");
        } else {
            long from = lower.getValue();
            if (!lower.isIncluding()) {
                LettuceAssert.isTrue(from != Long.MAX_VALUE, "Exclusive lower bound must be less than Long.MAX_VALUE");
                from++;
            }
            args.add(from);
        }

        if (upper.isUnbounded() || upper.getValue() == null) {
            args.add("+");
        } else {
            long to = upper.getValue();
            if (!upper.isIncluding()) {
                LettuceAssert.isTrue(to != Long.MIN_VALUE, "Exclusive upper bound must be greater than Long.MIN_VALUE");
                to--;
            }
            args.add(to);
        }
    }

}
