/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.util.List;

import io.lettuce.core.bf.BfInfoValue;
import io.lettuce.core.bf.BfInfoValueParser;
import io.lettuce.core.bf.BfScanDumpValue;
import io.lettuce.core.bf.BfScanDumpValueParser;
import io.lettuce.core.bf.arguments.BfInsertArgs;
import io.lettuce.core.bf.arguments.BfReserveArgs;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Bloom Filter commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
class RedisBloomFilterCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisBloomFilterCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, Boolean> bfAdd(K key, V value) {
        notNullKey(key);

        return createCommand(BF_ADD, new BooleanOutput<>(codec), key, value);
    }

    Command<K, V, Long> bfCard(K key) {
        notNullKey(key);

        return createCommand(BF_CARD, new IntegerOutput<>(codec), key);
    }

    Command<K, V, Boolean> bfExists(K key, V value) {
        notNullKey(key);

        return createCommand(BF_EXISTS, new BooleanOutput<>(codec), key, value);
    }

    Command<K, V, BfInfoValue> bfInfo(K key) {
        notNullKey(key);

        return createCommand(BF_INFO, new EncodedComplexOutput<>(codec, BfInfoValueParser.INSTANCE), key);
    }

    Command<K, V, List<Boolean>> bfInsert(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.ITEMS).add(value);

        return createCommand(BF_INSERT, new BfBooleanListOutput<>(codec), args);
    }

    Command<K, V, List<Boolean>> bfInsert(K key, BfInsertArgs insertArgs, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        insertArgs.build(args);
        args.add(CommandKeyword.ITEMS).addValue(value);

        return createCommand(BF_INSERT, new BfBooleanListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> bfInsert(K key, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.ITEMS).addValues(values);

        return createCommand(BF_INSERT, new BfBooleanListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> bfInsert(K key, BfInsertArgs insertArgs, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        insertArgs.build(args);
        args.add(CommandKeyword.ITEMS).addValues(values);

        return createCommand(BF_INSERT, new BfBooleanListOutput<>(codec), args);
    }

    Command<K, V, String> bfLoadChunk(K key, long iterator, byte[] data) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(iterator).add(data);

        return createCommand(BF_LOADCHUNK, new StatusOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> bfMAdd(K key, V... values) {
        notNullKey(key);

        return createCommand(BF_MADD, new BooleanListOutput<>(codec), key, values);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> bfMExists(K key, V... values) {
        notNullKey(key);

        return createCommand(BF_MEXISTS, new BooleanListOutput<>(codec), key, values);
    }

    Command<K, V, String> bfReserve(K key, double errorRate, long capacity) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(errorRate).add(capacity);

        return createCommand(BF_RESERVE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> bfReserve(K key, double errorRate, long capacity, BfReserveArgs reserveArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(errorRate).add(capacity);
        reserveArgs.build(args);

        return createCommand(BF_RESERVE, new StatusOutput<>(codec), args);
    }

    Command<K, V, BfScanDumpValue> bfScanDump(K key, long iterator) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(iterator);

        return createCommand(BF_SCANDUMP, new EncodedComplexOutput<>(codec, BfScanDumpValueParser.INSTANCE), args);
    }

}
