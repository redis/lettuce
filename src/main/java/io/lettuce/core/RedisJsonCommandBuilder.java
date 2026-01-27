/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonType;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;

import java.util.List;
import java.util.function.Supplier;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling JSON commands.
 *
 * @author Tihomir Mateev
 * @author SeugnSu Kim
 * @since 6.5
 */
class RedisJsonCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    private final Supplier<JsonParser> parser;

    RedisJsonCommandBuilder(RedisCodec<K, V> codec, Supplier<JsonParser> theParser) {
        super(codec);
        parser = theParser;
    }

    Command<K, V, List<Long>> jsonArrappend(K key, JsonPath jsonPath, JsonValue... jsonValues) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        for (JsonValue value : jsonValues) {
            args.add(value.asByteBuffer().array());
        }

        return createCommand(JSON_ARRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrappend(K key, JsonPath jsonPath, String... jsonValues) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        for (String value : jsonValues) {
            args.add(value);
        }

        return createCommand(JSON_ARRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrindex(K key, JsonPath jsonPath, JsonValue value, JsonRangeArgs range) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(jsonPath.toString());
        args.add(value.asByteBuffer().array());

        if (range != null) {
            // OPTIONAL as per API
            range.build(args);
        }

        return createCommand(JSON_ARRINDEX, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrindex(K key, JsonPath jsonPath, String value, JsonRangeArgs range) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(jsonPath.toString());
        args.add(value);

        if (range != null) {
            // OPTIONAL as per API
            range.build(args);
        }

        return createCommand(JSON_ARRINDEX, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrinsert(K key, JsonPath jsonPath, int index, JsonValue... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(jsonPath.toString());
        args.add(index);

        for (JsonValue value : values) {
            args.add(value.asByteBuffer().array());
        }

        return createCommand(JSON_ARRINSERT, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrinsert(K key, JsonPath jsonPath, int index, String... values) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(jsonPath.toString());
        args.add(index);

        for (String value : values) {
            args.add(value);
        }

        return createCommand(JSON_ARRINSERT, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrlen(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }
        return createCommand(JSON_ARRLEN, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue>> jsonArrpop(K key, JsonPath jsonPath, int index) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null) {
            if (index != -1) {
                // OPTIONAL as per API
                args.add(jsonPath.toString());
                args.add(index);
            } else if (!jsonPath.isRootPath()) {
                // OPTIONAL as per API
                args.add(jsonPath.toString());
            }
        }

        return createCommand(JSON_ARRPOP, new JsonValueListOutput<>(codec, parser.get()), args);
    }

    Command<K, V, List<String>> jsonArrpopRaw(K key, JsonPath jsonPath, int index) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null) {
            if (index != -1) {
                // OPTIONAL as per API
                args.add(jsonPath.toString());
                args.add(index);
            } else if (!jsonPath.isRootPath()) {
                // OPTIONAL as per API
                args.add(jsonPath.toString());
            }
        }

        return createCommand(JSON_ARRPOP, new StringListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrtrim(K key, JsonPath jsonPath, JsonRangeArgs range) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());

        if (range != null) {
            range.build(args);
        }

        return createCommand(JSON_ARRTRIM, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, Long> jsonClear(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_CLEAR, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue>> jsonGet(K key, JsonGetArgs options, JsonPath... jsonPaths) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (options != null) {
            // OPTIONAL as per API
            options.build(args);
        }

        if (jsonPaths != null) {
            // OPTIONAL as per API
            for (JsonPath jsonPath : jsonPaths) {
                if (jsonPath != null) {
                    args.add(jsonPath.toString());
                }
            }
        }

        return createCommand(JSON_GET, new JsonValueListOutput<>(codec, parser.get()), args);
    }

    Command<K, V, List<String>> jsonGetRaw(K key, JsonGetArgs options, JsonPath... jsonPaths) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (options != null) {
            // OPTIONAL as per API
            options.build(args);
        }

        if (jsonPaths != null) {
            // OPTIONAL as per API
            for (JsonPath jsonPath : jsonPaths) {
                if (jsonPath != null) {
                    args.add(jsonPath.toString());
                }
            }
        }

        return createCommand(JSON_GET, new StringListOutput<>(codec), args);
    }

    Command<K, V, String> jsonMerge(K key, JsonPath jsonPath, JsonValue value) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(value.asByteBuffer().array());

        return createCommand(JSON_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> jsonMerge(K key, JsonPath jsonPath, String value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(value);

        return createCommand(JSON_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue>> jsonMGet(JsonPath jsonPath, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        args.add(jsonPath.toString());

        return createCommand(JSON_MGET, new JsonValueListOutput<>(codec, parser.get()), args);
    }

    Command<K, V, List<String>> jsonMGetRaw(JsonPath jsonPath, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        args.add(jsonPath.toString());

        return createCommand(JSON_MGET, new StringListOutput<>(codec), args);
    }

    Command<K, V, String> jsonMSet(List<JsonMsetArgs<K, V>> arguments) {

        notEmpty(arguments.toArray());

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        for (JsonMsetArgs<K, V> argument : arguments) {
            argument.build(args);
        }

        return createCommand(JSON_MSET, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Number>> jsonNumincrby(K key, JsonPath jsonPath, Number number) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(number.toString());

        return createCommand(JSON_NUMINCRBY, new NumberListOutput<>(codec), args);
    }

    Command<K, V, List<V>> jsonObjkeys(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_OBJKEYS, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonObjlen(K key, JsonPath jsonPath) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_OBJLEN, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, String> jsonSet(K key, JsonPath jsonPath, JsonValue value, JsonSetArgs options) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(jsonPath.toString());

        args.add(value.asByteBuffer().array());

        if (options != null) {
            // OPTIONAL as per API
            options.build(args);
        }

        return createCommand(JSON_SET, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> jsonSet(K key, JsonPath jsonPath, String value, JsonSetArgs options) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(jsonPath.toString());
        args.add(value);

        if (options != null) {
            // OPTIONAL as per API
            options.build(args);
        }

        return createCommand(JSON_SET, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrappend(K key, JsonPath jsonPath, JsonValue value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        args.add(value.asByteBuffer().array());

        return createCommand(JSON_STRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrappend(K key, JsonPath jsonPath, String jsonString) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(jsonString.getBytes());

        return createCommand(JSON_STRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrlen(K key, JsonPath jsonPath) {

        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_STRLEN, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonToggle(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());

        return createCommand(JSON_TOGGLE, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, Long> jsonDel(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }
        return createCommand(JSON_DEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<JsonType>> jsonType(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            // OPTIONAL as per API
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_TYPE, new JsonTypeListOutput<>(codec), args);
    }

}
