/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.output.IntegerListOutput;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.KeyListOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.timeseries.TsAggregationType;
import io.lettuce.core.timeseries.TsInfoValue;
import io.lettuce.core.timeseries.TsInfoValueParser;
import io.lettuce.core.timeseries.TsMGetValue;
import io.lettuce.core.timeseries.TsMGetValueParser;
import io.lettuce.core.timeseries.TsSample;
import io.lettuce.core.timeseries.TsSampleParser;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsGetArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.lettuce.core.timeseries.arguments.TsMGetArgs;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling RedisTimeSeries commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.7
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

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        createArgs.build(args);

        return createCommand(TS_CREATE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tsAlter(K key, TsAlterArgs alterArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        alterArgs.build(args);

        return createCommand(TS_ALTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tsCreateRule(K sourceKey, K destKey, TsAggregationType aggregationType, long bucketDuration) {
        notNullKey(sourceKey);
        notNullKey(destKey);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(sourceKey).addKey(destKey).add(CommandKeyword.AGGREGATION)
                .add(aggregationType).add(bucketDuration);

        return createCommand(TS_CREATERULE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tsCreateRule(K sourceKey, K destKey, TsAggregationType aggregationType, long bucketDuration,
            long alignTimestamp) {
        notNullKey(sourceKey);
        notNullKey(destKey);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(sourceKey).addKey(destKey).add(CommandKeyword.AGGREGATION)
                .add(aggregationType).add(bucketDuration).add(alignTimestamp);

        return createCommand(TS_CREATERULE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tsDeleteRule(K sourceKey, K destKey) {
        notNullKey(sourceKey);
        notNullKey(destKey);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(sourceKey).addKey(destKey);

        return createCommand(TS_DELETERULE, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> tsDel(K key, long fromTimestamp, long toTimestamp) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(fromTimestamp).add(toTimestamp);

        return createCommand(TS_DEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsAdd(K key, long timestamp, double value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp).add(value);

        return createCommand(TS_ADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsAdd(K key, long timestamp, double value, TsAddArgs addArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp).add(value);
        addArgs.build(args);

        return createCommand(TS_ADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsAdd(K key, double value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add("*").add(value);

        return createCommand(TS_ADD, new IntegerOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Long>> tsMAdd(Map.Entry<K, TsSample>... entries) {
        notEmpty(entries);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        for (Map.Entry<K, TsSample> entry : entries) {
            addMAddEntry(args, entry);
        }

        return createCommand(TS_MADD, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> tsMAdd(Map.Entry<K, TsSample> entry) {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addMAddEntry(args, entry);

        return createCommand(TS_MADD, new IntegerListOutput<>(codec), args);
    }

    private void addMAddEntry(CommandArgs<K, V> args, Map.Entry<K, TsSample> entry) {
        TsSample sample = entry.getValue();
        if (sample.getValues().size() != 1) {
            throw new IllegalArgumentException("TS.MADD does not support samples with more than one value");
        }
        args.addKey(entry.getKey()).add(sample.getTimestamp()).add(sample.getValue());
    }

    Command<K, V, Long> tsIncrBy(K key, double value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(value);

        return createCommand(TS_INCRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsIncrBy(K key, double value, TsIncrByArgs incrByArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(value);
        incrByArgs.build(args);

        return createCommand(TS_INCRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsDecrBy(K key, double value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(value);

        return createCommand(TS_DECRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> tsDecrBy(K key, double value, TsIncrByArgs decrByArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(value);
        decrByArgs.build(args);

        return createCommand(TS_DECRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, TsSample> tsGet(K key) {
        notNullKey(key);

        return createCommand(TS_GET, new EncodedComplexOutput<>(codec, TsSampleParser.INSTANCE), key);
    }

    Command<K, V, TsSample> tsGet(K key, TsGetArgs getArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        getArgs.build(args);

        return createCommand(TS_GET, new EncodedComplexOutput<>(codec, TsSampleParser.INSTANCE), args);
    }

    Command<K, V, TsInfoValue<K>> tsInfo(K key) {
        notNullKey(key);

        return createCommand(TS_INFO, new EncodedComplexOutput<>(codec, new TsInfoValueParser<>(codec)), key);
    }

    Command<K, V, TsInfoValue<K>> tsInfoDebug(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add("DEBUG");

        return createCommand(TS_INFO, new EncodedComplexOutput<>(codec, new TsInfoValueParser<>(codec)), args);
    }

    @SafeVarargs
    final Command<K, V, List<TsMGetValue<K>>> tsMGet(V... filters) {
        notEmptyValues(filters);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandKeyword.FILTER).addValues(filters);

        return createCommand(TS_MGET, new EncodedComplexOutput<>(codec, new TsMGetValueParser<>(codec)), args);
    }

    Command<K, V, List<TsMGetValue<K>>> tsMGet(V filter) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandKeyword.FILTER).addValue(filter);

        return createCommand(TS_MGET, new EncodedComplexOutput<>(codec, new TsMGetValueParser<>(codec)), args);
    }

    @SafeVarargs
    final Command<K, V, List<TsMGetValue<K>>> tsMGet(TsMGetArgs mGetArgs, V... filters) {
        notEmptyValues(filters);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        mGetArgs.build(args);
        args.add(CommandKeyword.FILTER).addValues(filters);

        return createCommand(TS_MGET, new EncodedComplexOutput<>(codec, new TsMGetValueParser<>(codec)), args);
    }

    Command<K, V, List<TsMGetValue<K>>> tsMGet(TsMGetArgs mGetArgs, V filter) {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        mGetArgs.build(args);
        args.add(CommandKeyword.FILTER).addValue(filter);

        return createCommand(TS_MGET, new EncodedComplexOutput<>(codec, new TsMGetValueParser<>(codec)), args);
    }

    @SafeVarargs
    final Command<K, V, List<K>> tsQueryIndex(V... filters) {
        notEmptyValues(filters);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addValues(filters);

        return createCommand(TS_QUERYINDEX, new KeyListOutput<>(codec), args);
    }

    Command<K, V, List<K>> tsQueryIndex(V filter) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).addValue(filter);

        return createCommand(TS_QUERYINDEX, new KeyListOutput<>(codec), args);
    }

}
