/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.List;

import io.lettuce.core.probabilistic.TDigestInfoValue;
import io.lettuce.core.probabilistic.TDigestInfoValueParser;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling T-Digest commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
class RedisTDigestCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisTDigestCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, String> tdigestAdd(K key, V value) {
        notNullKey(key);

        return createCommand(TDIGEST_ADD, new StatusOutput<>(codec), key, value);
    }

    @SafeVarargs
    final Command<K, V, String> tdigestAdd(K key, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(TDIGEST_ADD, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Double>> tdigestByRank(K key, long rank) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(rank);

        return createCommand(TDIGEST_BYRANK, new DoubleListOutput<>(codec), args);
    }

    Command<K, V, List<Double>> tdigestByRank(K key, long... ranks) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (long rank : ranks) {
            args.add(rank);
        }

        return createCommand(TDIGEST_BYRANK, new DoubleListOutput<>(codec), args);
    }

    Command<K, V, List<Double>> tdigestByRevRank(K key, long reverseRank) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(reverseRank);

        return createCommand(TDIGEST_BYREVRANK, new DoubleListOutput<>(codec), args);
    }

    Command<K, V, List<Double>> tdigestByRevRank(K key, long... reverseRanks) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (long reverseRank : reverseRanks) {
            args.add(reverseRank);
        }

        return createCommand(TDIGEST_BYREVRANK, new DoubleListOutput<>(codec), args);
    }

    Command<K, V, List<Double>> tdigestCDF(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);

        return createCommand(TDIGEST_CDF, new DoubleListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Double>> tdigestCDF(K key, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(TDIGEST_CDF, new DoubleListOutput<>(codec), args);
    }

    Command<K, V, String> tdigestCreate(K key) {
        notNullKey(key);

        return createCommand(TDIGEST_CREATE, new StatusOutput<>(codec), key);
    }

    Command<K, V, String> tdigestCreate(K key, long compression) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.COMPRESSION).add(compression);

        return createCommand(TDIGEST_CREATE, new StatusOutput<>(codec), args);
    }

    Command<K, V, TDigestInfoValue> tdigestInfo(K key) {
        notNullKey(key);

        return createCommand(TDIGEST_INFO, new EncodedComplexOutput<>(codec, TDigestInfoValueParser.INSTANCE), key);
    }

    Command<K, V, Double> tdigestMax(K key) {
        notNullKey(key);

        return createCommand(TDIGEST_MAX, new DoubleOutput<>(codec), key);
    }

    Command<K, V, String> tdigestMerge(K destination, K sourceKey) {
        notNullKey(destination);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(1).addKey(sourceKey);

        return createCommand(TDIGEST_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tdigestMerge(K destination, K sourceKey, long compression) {
        notNullKey(destination);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(1).addKey(sourceKey)
                .add(CommandKeyword.COMPRESSION).add(compression);

        return createCommand(TDIGEST_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> tdigestMerge(K destination, K sourceKey, long compression, boolean override) {
        notNullKey(destination);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(1).addKey(sourceKey)
                .add(CommandKeyword.COMPRESSION).add(compression);
        if (override) {
            args.add(CommandKeyword.OVERRIDE);
        }

        return createCommand(TDIGEST_MERGE, new StatusOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, String> tdigestMerge(K destination, K... sourceKeys) {
        notNullKey(destination);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(sourceKeys.length).addKeys(sourceKeys);

        return createCommand(TDIGEST_MERGE, new StatusOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, String> tdigestMerge(K destination, long compression, K... sourceKeys) {
        notNullKey(destination);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(sourceKeys.length).addKeys(sourceKeys)
                .add(CommandKeyword.COMPRESSION).add(compression);

        return createCommand(TDIGEST_MERGE, new StatusOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, String> tdigestMerge(K destination, long compression, boolean override, K... sourceKeys) {
        notNullKey(destination);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(sourceKeys.length).addKeys(sourceKeys)
                .add(CommandKeyword.COMPRESSION).add(compression);
        if (override) {
            args.add(CommandKeyword.OVERRIDE);
        }

        return createCommand(TDIGEST_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, Double> tdigestMin(K key) {
        notNullKey(key);

        return createCommand(TDIGEST_MIN, new DoubleOutput<>(codec), key);
    }

    Command<K, V, List<Double>> tdigestQuantile(K key, double quantile) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(quantile);

        return createCommand(TDIGEST_QUANTILE, new DoubleListOutput<>(codec), args);
    }

    Command<K, V, List<Double>> tdigestQuantile(K key, double... quantiles) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (double quantile : quantiles) {
            args.add(quantile);
        }

        return createCommand(TDIGEST_QUANTILE, new DoubleListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> tdigestRank(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);

        return createCommand(TDIGEST_RANK, new IntegerListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Long>> tdigestRank(K key, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(TDIGEST_RANK, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, String> tdigestReset(K key) {
        notNullKey(key);

        return createCommand(TDIGEST_RESET, new StatusOutput<>(codec), key);
    }

    Command<K, V, List<Long>> tdigestRevRank(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);

        return createCommand(TDIGEST_REVRANK, new IntegerListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Long>> tdigestRevRank(K key, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(TDIGEST_REVRANK, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, Double> tdigestTrimmedMean(K key, double lowCutQuantile, double highCutQuantile) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(lowCutQuantile).add(highCutQuantile);

        return createCommand(TDIGEST_TRIMMED_MEAN, new DoubleOutput<>(codec), args);
    }

}
