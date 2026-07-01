/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.*;
import io.lettuce.core.probabilistic.topk.TopKInfoValue;
import io.lettuce.core.probabilistic.topk.TopKInfoValueParser;
import io.lettuce.core.probabilistic.topk.TopKListValue;
import io.lettuce.core.probabilistic.topk.TopKListValueParser;
import io.lettuce.core.probabilistic.topk.arguments.TopKReserveArgs;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;

import static io.lettuce.core.protocol.CommandKeyword.WITHCOUNT;
import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Top-K commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
class RedisTopKCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisTopKCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, List<String>> topKAdd(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);

        return createCommand(TOPK_ADD, new StringListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<String>> topKAdd(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(TOPK_ADD, new StringListOutput<>(codec), args);
    }

    Command<K, V, List<Value<String>>> topKAddValues(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);

        return createCommand(TOPK_ADD, new StringValueListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Value<String>>> topKAddValues(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(TOPK_ADD, new StringValueListOutput<>(codec), args);
    }

    Command<K, V, List<String>> topKIncrBy(K key, Pair<V, Long> pair) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(pair.getFirst()).add(pair.getSecond());

        return createCommand(TOPK_INCRBY, new StringListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<String>> topKIncrBy(K key, Pair<V, Long>... pairs) {
        notNullKey(key);
        notEmptyValues(pairs);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (Pair<V, Long> pair : pairs) {
            args.addValue(pair.getFirst()).add(pair.getSecond());
        }

        return createCommand(TOPK_INCRBY, new StringListOutput<>(codec), args);
    }

    Command<K, V, List<Value<String>>> topKIncrByValues(K key, Pair<V, Long> pair) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(pair.getFirst()).add(pair.getSecond());

        return createCommand(TOPK_INCRBY, new StringValueListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Value<String>>> topKIncrByValues(K key, Pair<V, Long>... pairs) {
        notNullKey(key);
        notEmptyValues(pairs);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (Pair<V, Long> pair : pairs) {
            args.addValue(pair.getFirst()).add(pair.getSecond());
        }

        return createCommand(TOPK_INCRBY, new StringValueListOutput<>(codec), args);
    }

    Command<K, V, TopKInfoValue> topKInfo(K key) {
        notNullKey(key);

        return createCommand(TOPK_INFO, new EncodedComplexOutput<>(codec, TopKInfoValueParser.INSTANCE), key);
    }

    Command<K, V, List<String>> topKList(K key) {
        notNullKey(key);

        return createCommand(TOPK_LIST, new StringListOutput<>(codec), key);
    }

    Command<K, V, List<TopKListValue>> topKList(K key, boolean withCount) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        if (withCount) {
            args.add(WITHCOUNT);
        }

        return createCommand(TOPK_LIST, new EncodedComplexOutput<>(codec, TopKListValueParser.INSTANCE), args);

    }

    Command<K, V, List<Boolean>> topKQuery(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);

        return createCommand(TOPK_QUERY, new BooleanListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> topKQuery(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(TOPK_QUERY, new BooleanListOutput<>(codec), args);
    }

    Command<K, V, String> topKReserve(K key, long k) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(k);

        return createCommand(TOPK_RESERVE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> topKReserve(K key, long k, TopKReserveArgs args) {
        notNullKey(key);

        CommandArgs<K, V> cmdArgs = new CommandArgs<>(codec).addKey(key).add(k);
        args.build(cmdArgs);

        return createCommand(TOPK_RESERVE, new StatusOutput<>(codec), cmdArgs);
    }

}
