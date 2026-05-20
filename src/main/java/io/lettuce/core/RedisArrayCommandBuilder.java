/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.array.*;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Redis Array commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class RedisArrayCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    public RedisArrayCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    public Command<K, V, Long> arset(K key, long index, V value) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(index).addValue(value);
        return createCommand(ARSET, new IntegerOutput<>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> arset(K key, long index, V... values) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(index).addValues(values);
        return createCommand(ARSET, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> armset(K key, Map<Long, V> indexValuePairs) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (Map.Entry<Long, V> entry : indexValuePairs.entrySet()) {
            args.add(entry.getKey()).addValue(entry.getValue());
        }
        return createCommand(ARMSET, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, V> arget(K key, long index) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(index);
        return createCommand(ARGET, new ValueOutput<>(codec), args);
    }

    public Command<K, V, List<V>> armget(K key, long... indices) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (long index : indices) {
            args.add(index);
        }
        return createCommand(ARMGET, new ValueListOutput<>(codec), args);
    }

    public Command<K, V, Long> ardel(K key, long index) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(index);
        return createCommand(ARDEL, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> ardel(K key, long... indices) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (long index : indices) {
            args.add(index);
        }
        return createCommand(ARDEL, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> ardelrange(K key, long start, long end) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end);
        return createCommand(ARDELRANGE, new IntegerOutput<>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> ardelrange(K key, Range<Long>... ranges) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        for (Range<Long> range : ranges) {
            args.add(range.getLower().getValue()).add(range.getUpper().getValue());
        }
        return createCommand(ARDELRANGE, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> arlen(K key) {
        notNullKey(key);
        return createCommand(ARLEN, new IntegerOutput<>(codec), key);
    }

    public Command<K, V, Long> arcount(K key) {
        notNullKey(key);
        return createCommand(ARCOUNT, new IntegerOutput<>(codec), key);
    }

    public Command<K, V, List<V>> argetrange(K key, long start, long end) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end);
        return createCommand(ARGETRANGE, new ValueListOutput<>(codec), args);
    }

    public Command<K, V, List<IndexedValue<V>>> arscan(K key, long start, long end) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end);
        return createCommand(ARSCAN, new IndexedValueListOutput<>(codec), args);
    }

    public Command<K, V, List<IndexedValue<V>>> arscan(K key, long start, long end, long limit) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end).add(CommandKeyword.LIMIT).add(limit);
        return createCommand(ARSCAN, new IndexedValueListOutput<>(codec), args);
    }

    public Command<K, V, List<Long>> argrep(K key, ArGrepArgs grepArgs) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        grepArgs.build(args);
        return createCommand(ARGREP, new IntegerListOutput<>(codec), args);
    }

    public Command<K, V, List<IndexedValue<V>>> argrepWithValues(K key, ArGrepArgs grepArgs) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        grepArgs.build(args);
        args.add(CommandKeyword.WITHVALUES);
        return createCommand(ARGREP, new IndexedValueListOutput<>(codec), args);
    }

    public Command<K, V, V> aropAggregate(K key, long start, long end, ArAggregateType operation) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end).add(operation.name());
        return createCommand(AROP, new ValueOutput<>(codec), args);
    }

    public Command<K, V, Long> aropBitwise(K key, long start, long end, ArBitwiseType operation) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end).add(operation.name());
        return createCommand(AROP, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> aropCount(K key, long start, long end) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end).add(CommandKeyword.USED);
        return createCommand(AROP, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> aropCount(K key, long start, long end, V matchValue) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end).add(CommandKeyword.MATCH)
                .addValue(matchValue);
        return createCommand(AROP, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> arinsert(K key, V value) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);
        return createCommand(ARINSERT, new IntegerOutput<>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> arinsert(K key, V... values) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);
        return createCommand(ARINSERT, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> arring(K key, long size, V value) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(size).addValue(value);
        return createCommand(ARRING, new IntegerOutput<>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> arring(K key, long size, V... values) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(size).addValues(values);
        return createCommand(ARRING, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> arnext(K key) {
        notNullKey(key);
        return createCommand(ARNEXT, new IntegerOutput<>(codec), key);
    }

    public Command<K, V, Long> arseek(K key, long index) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(index);
        return createCommand(ARSEEK, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, List<V>> arlastitems(K key, long count) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count);
        return createCommand(ARLASTITEMS, new ValueListOutput<>(codec), args);
    }

    public Command<K, V, List<V>> arlastitems(K key, long count, boolean rev) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count);
        if (rev) {
            args.add(CommandKeyword.REV);
        }
        return createCommand(ARLASTITEMS, new ValueListOutput<>(codec), args);
    }

    public Command<K, V, ArrayInfo> arinfo(K key) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(ARINFO, new ComplexOutput<>(codec, ArrayInfoParser.INSTANCE), args);
    }

    public Command<K, V, ArrayInfoFull> arinfoFull(K key) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(CommandKeyword.FULL);
        return createCommand(ARINFO, new ComplexOutput<>(codec, ArrayInfoFullParser.INSTANCE), args);
    }

}
