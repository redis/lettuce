/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.output.IntegerListOutput;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.JsonValueListOutput;
import io.lettuce.core.output.KeyListOutput;
import io.lettuce.core.output.NumberListOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.ValueListOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;

import java.util.List;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling JSON commands.
 *
 * @author Tihomir Mateev
 * @since 6.5
 */
class RedisJsonCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisJsonCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, List<Long>> jsonArrappend(K key, JsonPath jsonPath, JsonValue<K, V>... jsonValues) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        for (JsonValue<K, V> value : jsonValues) {
            args.add(value.asByteBuffer().array());
        }

        return createCommand(JSON_ARRAPPEND, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrindex(K key, JsonPath jsonPath, JsonValue<K, V> value, JsonRangeArgs range) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(value.asByteBuffer().array());

        if (range != null) {
            // OPTIONAL as per API
            range.build(args);
        }

        return createCommand(JSON_ARRINDEX, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrinsert(K key, JsonPath jsonPath, int index, JsonValue<K, V>... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(index);

        for (JsonValue<K, V> value : values) {
            args.add(value.asByteBuffer().array());
        }

        return createCommand(JSON_ARRINSERT, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrlen(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }
        return createCommand(JSON_ARRLEN, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue<K, V>>> jsonArrpop(K key, JsonPath jsonPath, int index) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null) {
            args.add(jsonPath.toString());

            if (index != -1) {
                args.add(index);
            }
        }

        return createCommand(JSON_ARRPOP, new JsonValueListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrtrim(K key, JsonPath jsonPath, JsonRangeArgs range) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        if (range != null) {
            range.build(args);
        }

        return createCommand(JSON_ARRTRIM, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, Long> jsonClear(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_CLEAR, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue<K, V>>> jsonGet(K key, JsonGetArgs options, JsonPath... jsonPaths) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (options != null) {
            options.build(args);
        }

        if (jsonPaths != null) {
            for (JsonPath jsonPath : jsonPaths) {
                if (jsonPath != null) {
                    args.add(jsonPath.toString());
                }
            }
        }

        return createCommand(JSON_GET, new JsonValueListOutput<>(codec), args);
    }

    Command<K, V, Boolean> jsonMerge(K key, JsonPath jsonPath, JsonValue<K, V> value) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(value.asByteBuffer().array());

        return createCommand(JSON_MERGE, new BooleanOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue<K, V>>> jsonMGet(JsonPath jsonPath, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);

        if (jsonPath != null) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_MGET, new JsonValueListOutput<>(codec), args);
    }

    Command<K, V, String> jsonMSet(JsonMsetArgs... arguments) {

        notEmpty(arguments);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        for (JsonMsetArgs argument : arguments) {
            argument.build(args);
        }

        return createCommand(JSON_MSET, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Number>> jsonNumincrby(K key, JsonPath jsonPath, Number number) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(number.toString());

        return createCommand(JSON_NUMINCRBY, new NumberListOutput<>(codec), args);
    }

    Command<K, V, List<K>> jsonObjkeys(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_OBJKEYS, new KeyListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonObjlen(K key, JsonPath jsonPath) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_OBJLEN, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, String> jsonSet(K key, JsonPath jsonPath, JsonValue<K, V> value, JsonSetArgs options) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(value.asByteBuffer().array());

        if (options != null) {
            options.build(args);
        }

        return createCommand(JSON_SET, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrappend(K key, JsonPath jsonPath, JsonValue<K, V> value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(value.asByteBuffer().array());

        return createCommand(JSON_STRAPPEND, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrlen(K key, JsonPath jsonPath) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_STRLEN, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonToggle(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_TOGGLE, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, Long> jsonDel(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }
        return createCommand(JSON_DEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<V>> jsonType(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_TYPE, new ValueListOutput<>(codec), args);
    }

}
