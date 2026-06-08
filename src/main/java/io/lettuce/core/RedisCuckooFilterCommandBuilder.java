/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.List;

import io.lettuce.core.cf.CfInfoValue;
import io.lettuce.core.cf.CfInfoValueParser;
import io.lettuce.core.cf.CfScanDumpValue;
import io.lettuce.core.cf.CfScanDumpValueParser;
import io.lettuce.core.cf.arguments.CfInsertArgs;
import io.lettuce.core.cf.arguments.CfReserveArgs;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Cuckoo Filter commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.7
 */
class RedisCuckooFilterCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisCuckooFilterCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, String> cfReserve(K key, long capacity) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(capacity);

        return createCommand(CF_RESERVE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> cfReserve(K key, long capacity, CfReserveArgs reserveArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(capacity);
        reserveArgs.build(args);

        return createCommand(CF_RESERVE, new StatusOutput<>(codec), args);
    }

    Command<K, V, Boolean> cfAdd(K key, V value) {
        notNullKey(key);

        return createCommand(CF_ADD, new BooleanOutput<>(codec), key, value);
    }

    Command<K, V, Boolean> cfAddNx(K key, V value) {
        notNullKey(key);

        return createCommand(CF_ADDNX, new BooleanOutput<>(codec), key, value);
    }

    Command<K, V, List<Boolean>> cfInsert(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.ITEMS).addValue(value);

        return createCommand(CF_INSERT, new BooleanListOutput<>(codec), args);
    }

    Command<K, V, List<Boolean>> cfInsert(K key, CfInsertArgs insertArgs, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        insertArgs.build(args);
        args.add(CommandKeyword.ITEMS).addValue(value);

        return createCommand(CF_INSERT, new BooleanListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> cfInsert(K key, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.ITEMS).addValues(values);

        return createCommand(CF_INSERT, new BooleanListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> cfInsert(K key, CfInsertArgs insertArgs, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        insertArgs.build(args);
        args.add(CommandKeyword.ITEMS).addValues(values);

        return createCommand(CF_INSERT, new BooleanListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> cfInsertNx(K key, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.ITEMS).addValue(value);

        return createCommand(CF_INSERTNX, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> cfInsertNx(K key, CfInsertArgs insertArgs, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        insertArgs.build(args);
        args.add(CommandKeyword.ITEMS).addValue(value);

        return createCommand(CF_INSERTNX, new IntegerListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Long>> cfInsertNx(K key, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.ITEMS).addValues(values);

        return createCommand(CF_INSERTNX, new IntegerListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, List<Long>> cfInsertNx(K key, CfInsertArgs insertArgs, V... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        insertArgs.build(args);
        args.add(CommandKeyword.ITEMS).addValues(values);

        return createCommand(CF_INSERTNX, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, Boolean> cfExists(K key, V value) {
        notNullKey(key);

        return createCommand(CF_EXISTS, new BooleanOutput<>(codec), key, value);
    }

    @SafeVarargs
    final Command<K, V, List<Boolean>> cfMExists(K key, V... values) {
        notNullKey(key);

        return createCommand(CF_MEXISTS, new BooleanListOutput<>(codec), key, values);
    }

    Command<K, V, Boolean> cfDel(K key, V value) {
        notNullKey(key);

        return createCommand(CF_DEL, new BooleanOutput<>(codec), key, value);
    }

    Command<K, V, Long> cfCount(K key, V value) {
        notNullKey(key);

        return createCommand(CF_COUNT, new IntegerOutput<>(codec), key, value);
    }

    Command<K, V, CfScanDumpValue> cfScanDump(K key, long iterator) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(iterator);

        return createCommand(CF_SCANDUMP, new EncodedComplexOutput<>(codec, CfScanDumpValueParser.INSTANCE), args);
    }

    Command<K, V, String> cfLoadChunk(K key, long iterator, byte[] data) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(iterator).add(data);

        return createCommand(CF_LOADCHUNK, new StatusOutput<>(codec), args);
    }

    Command<K, V, CfInfoValue> cfInfo(K key) {
        notNullKey(key);

        return createCommand(CF_INFO, new EncodedComplexOutput<>(codec, CfInfoValueParser.INSTANCE), key);
    }

}
