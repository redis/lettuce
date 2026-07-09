/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.List;

import io.lettuce.core.probabilistic.CMSInfoValue;
import io.lettuce.core.probabilistic.CMSInfoValueParser;
import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.probabilistic.MergePair;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Count-Min Sketch commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
class RedisCMSCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisCMSCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, List<Long>> cmsIncrBy(K key, IncrementPair<V> pair) {
        notNullKey(key);
        LettuceAssert.notNull(pair, "IncrementPair " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(pair.getValue()).add(pair.getIncrement());

        return createCommand(CMS_INCRBY, new IntegerListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Long>> cmsIncrBy(K key, IncrementPair<V>... pairs) {
        notNullKey(key);
        LettuceAssert.notEmpty(pairs, "IncrementPair " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (IncrementPair<V> pair : pairs) {
            args.addValue(pair.getValue()).add(pair.getIncrement());
        }

        return createCommand(CMS_INCRBY, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, CMSInfoValue> cmsInfo(K key) {
        notNullKey(key);

        return createCommand(CMS_INFO, new EncodedComplexOutput<>(codec, CMSInfoValueParser.INSTANCE), key);
    }

    Command<K, V, String> cmsInitByDim(K key, long width, long depth) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(width).add(depth);

        return createCommand(CMS_INITBYDIM, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> cmsInitByProb(K key, double error, double probability) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(error).add(probability);

        return createCommand(CMS_INITBYPROB, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> cmsMerge(K destination, K source) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(1).addKey(source);

        return createCommand(CMS_MERGE, new StatusOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, String> cmsMerge(K destination, K... sources) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(sources, "Sources " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(sources.length).addKeys(sources);

        return createCommand(CMS_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> cmsMerge(K destination, K source, long weight) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(1).addKey(source).add(CommandKeyword.WEIGHTS)
                .add(weight);

        return createCommand(CMS_MERGE, new StatusOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, String> cmsMerge(K destination, MergePair<K>... sources) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(sources, "Sources " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(sources.length);
        for (MergePair<K> source : sources) {
            args.addKey(source.getKey());
        }
        args.add(CommandKeyword.WEIGHTS);
        for (MergePair<K> source : sources) {
            args.add(source.getWeight());
        }

        return createCommand(CMS_MERGE, new StatusOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Long>> cmsQuery(K key, V... values) {
        notNullKey(key);
        LettuceAssert.notEmpty(values, "Values " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(CMS_QUERY, new IntegerListOutput<>(codec), args);
    }

}
