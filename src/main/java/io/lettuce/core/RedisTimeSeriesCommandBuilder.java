/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandType.DEBUG;
import static io.lettuce.core.protocol.CommandType.TS_ADD;
import static io.lettuce.core.protocol.CommandType.TS_ALTER;
import static io.lettuce.core.protocol.CommandType.TS_CREATE;
import static io.lettuce.core.protocol.CommandType.TS_CREATERULE;
import static io.lettuce.core.protocol.CommandType.TS_DECRBY;
import static io.lettuce.core.protocol.CommandType.TS_DEL;
import static io.lettuce.core.protocol.CommandType.TS_DELETERULE;
import static io.lettuce.core.protocol.CommandType.TS_GET;
import static io.lettuce.core.protocol.CommandType.TS_INCRBY;
import static io.lettuce.core.protocol.CommandType.TS_INFO;
import static io.lettuce.core.protocol.CommandType.TS_MADD;
import static io.lettuce.core.protocol.CommandType.TS_RANGE;
import static io.lettuce.core.protocol.CommandType.TS_REVRANGE;

import java.util.List;

import io.lettuce.core.Range.Boundary;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.output.ErrorTolerantIntegerListOutput;
import io.lettuce.core.output.ErrorTolerantIntegerValueListOutput;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.TsSampleListOutput;
import io.lettuce.core.output.TsSampleOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsInfoValue;
import io.lettuce.core.timeseries.TsInfoValueParser;
import io.lettuce.core.timeseries.TsMAddValue;
import io.lettuce.core.timeseries.TsSample;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.lettuce.core.timeseries.arguments.TsRangeArgs;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Redis TimeSeries commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
class RedisTimeSeriesCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    // timestamp sentinel: the server clock sets the timestamp
    private static final String SERVER_CLOCK = "*";

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

    Command<K, V, String> tsCreateRule(K sourceKey, K destKey, TsAggregation aggregation, long bucketDuration) {
        return createCommand(TS_CREATERULE, new StatusOutput<>(codec),
                createRuleArgs(sourceKey, destKey, aggregation, bucketDuration));
    }

    Command<K, V, String> tsCreateRule(K sourceKey, K destKey, TsAggregation aggregation, long bucketDuration,
            long alignTimestamp) {

        LettuceAssert.isTrue(alignTimestamp >= 0, "Align timestamp must not be negative");

        CommandArgs<K, V> args = createRuleArgs(sourceKey, destKey, aggregation, bucketDuration).add(alignTimestamp);

        return createCommand(TS_CREATERULE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tsDeleteRule(K sourceKey, K destKey) {
        LettuceAssert.notNull(sourceKey, "Source key " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destKey, "Destination key " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(sourceKey).addKey(destKey);

        return createCommand(TS_DELETERULE, new StatusOutput<>(codec), args);
    }

    private CommandArgs<K, V> createRuleArgs(K sourceKey, K destKey, TsAggregation aggregation, long bucketDuration) {

        LettuceAssert.notNull(sourceKey, "Source key " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destKey, "Destination key " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(aggregation, "Aggregation " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(bucketDuration > 0, "Bucket duration must be positive");

        return new CommandArgs<>(codec).addKey(sourceKey).addKey(destKey).add(CommandKeyword.AGGREGATION).add(aggregation)
                .add(bucketDuration);
    }

    Command<K, V, Long> tsAdd(K key, double value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(SERVER_CLOCK).add(value);

        return createCommand(TS_ADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsAdd(K key, double value, TsAddArgs addArgs) {
        notNullKey(key);
        LettuceAssert.notNull(addArgs, "TsAddArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(SERVER_CLOCK).add(value);
        addArgs.build(args);

        return createCommand(TS_ADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsAdd(K key, long timestamp, double value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp).add(value);

        return createCommand(TS_ADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsAdd(K key, long timestamp, double value, TsAddArgs addArgs) {
        notNullKey(key);
        LettuceAssert.notNull(addArgs, "TsAddArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp).add(value);
        addArgs.build(args);

        return createCommand(TS_ADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<Long>> tsMAdd(List<TsMAddValue<K>> samples) {
        return createCommand(TS_MADD, new ErrorTolerantIntegerListOutput<>(codec), mAddArgs(samples));
    }

    Command<K, V, List<Value<Long>>> tsMAddValues(List<TsMAddValue<K>> samples) {
        return createCommand(TS_MADD, new ErrorTolerantIntegerValueListOutput<>(codec), mAddArgs(samples));
    }

    Command<K, V, Long> tsIncrBy(K key, double addend) {
        notNullKey(key);

        return createCommand(TS_INCRBY, new IntegerOutput<>(codec), new CommandArgs<>(codec).addKey(key).add(addend));
    }

    Command<K, V, Long> tsIncrBy(K key, double addend, TsIncrByArgs incrByArgs) {
        notNullKey(key);
        LettuceAssert.notNull(incrByArgs, "TsIncrByArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(addend);
        incrByArgs.build(args);

        return createCommand(TS_INCRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsDecrBy(K key, double subtrahend) {
        notNullKey(key);

        return createCommand(TS_DECRBY, new IntegerOutput<>(codec), new CommandArgs<>(codec).addKey(key).add(subtrahend));
    }

    Command<K, V, Long> tsDecrBy(K key, double subtrahend, TsIncrByArgs incrByArgs) {
        notNullKey(key);
        LettuceAssert.notNull(incrByArgs, "TsIncrByArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(subtrahend);
        incrByArgs.build(args);

        return createCommand(TS_DECRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, TsSample> tsGet(K key) {
        notNullKey(key);

        return createCommand(TS_GET, new TsSampleOutput<>(codec), key);
    }

    Command<K, V, TsSample> tsGet(K key, boolean latest) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        if (latest) {
            args.add(CommandKeyword.LATEST);
        }

        return createCommand(TS_GET, new TsSampleOutput<>(codec), args);
    }

    Command<K, V, List<TsSample>> tsRange(K key, Range<Long> range) {
        return createCommand(TS_RANGE, new TsSampleListOutput<>(codec), rangeArgs(key, range, null));
    }

    Command<K, V, List<TsSample>> tsRange(K key, Range<Long> range, TsRangeArgs rangeArgs) {
        LettuceAssert.notNull(rangeArgs, "TsRangeArgs " + MUST_NOT_BE_NULL);

        return createCommand(TS_RANGE, new TsSampleListOutput<>(codec), rangeArgs(key, range, rangeArgs));
    }

    Command<K, V, List<TsSample>> tsRevRange(K key, Range<Long> range) {
        return createCommand(TS_REVRANGE, new TsSampleListOutput<>(codec), rangeArgs(key, range, null));
    }

    Command<K, V, List<TsSample>> tsRevRange(K key, Range<Long> range, TsRangeArgs rangeArgs) {
        LettuceAssert.notNull(rangeArgs, "TsRangeArgs " + MUST_NOT_BE_NULL);

        return createCommand(TS_REVRANGE, new TsSampleListOutput<>(codec), rangeArgs(key, range, rangeArgs));
    }

    private CommandArgs<K, V> rangeArgs(K key, Range<Long> range, TsRangeArgs rangeArgs) {
        notNullKey(key);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        addRange(args, range);
        if (rangeArgs != null) {
            rangeArgs.build(args);
        }

        return args;
    }

    private CommandArgs<K, V> mAddArgs(List<TsMAddValue<K>> samples) {

        LettuceAssert.notNull(samples, "Samples " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!samples.isEmpty(), "Samples " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        for (TsMAddValue<K> sample : samples) {
            LettuceAssert.notNull(sample, "Samples " + MUST_NOT_CONTAIN_NULL_ELEMENTS);
            args.addKey(sample.getKey());
            if (sample.getTimestamp() == null) {
                args.add(SERVER_CLOCK);
            } else {
                args.add(sample.getTimestamp().longValue());
            }
            args.add(sample.getValue());
        }

        return args;
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
