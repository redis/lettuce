/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
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
 * @author Yordan Tsintsov
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
        notNullPath(jsonPath);
        LettuceAssert.notEmpty(jsonValues, "JSON values must not be empty");
        LettuceAssert.noNullElements(jsonValues, "JSON values must not contain null elements");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        for (JsonValue value : jsonValues) {
            args.add(value.asByteBuffer().array());
        }

        return createCommand(JSON_ARRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrappend(K key, JsonPath jsonPath, String... jsonValues) {
        notNullKey(key);
        notNullPath(jsonPath);
        LettuceAssert.notEmpty(jsonValues, "JSON values must not be empty");
        LettuceAssert.noNullElements(jsonValues, "JSON values must not contain null elements");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        for (String value : jsonValues) {
            args.add(value);
        }

        return createCommand(JSON_ARRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrindex(K key, JsonPath jsonPath, JsonValue value, JsonRangeArgs range) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJson(value);
        LettuceAssert.notNull(range, "Range must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(value.asByteBuffer().array());
        range.build(args);

        return createCommand(JSON_ARRINDEX, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrindex(K key, JsonPath jsonPath, String value, JsonRangeArgs range) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJsonString(value);
        LettuceAssert.notNull(range, "Range must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(value);
        range.build(args);

        return createCommand(JSON_ARRINDEX, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrinsert(K key, JsonPath jsonPath, int index, JsonValue... values) {
        notNullKey(key);
        notNullPath(jsonPath);
        LettuceAssert.notEmpty(values, "JSON values must not be empty");
        LettuceAssert.noNullElements(values, "JSON values must not contain null elements");

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
        notNullPath(jsonPath);
        LettuceAssert.notEmpty(values, "JSON values must not be empty");
        LettuceAssert.noNullElements(values, "JSON values must not contain null elements");

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
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());

        return createCommand(JSON_ARRLEN, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue>> jsonArrpop(K key, JsonPath jsonPath, int index) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());
        if (index != -1) {
            args.add(index);
        }

        return createCommand(JSON_ARRPOP, new JsonValueListOutput<>(codec, parser.get()), args);
    }

    Command<K, V, List<String>> jsonArrpopRaw(K key, JsonPath jsonPath, int index) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());
        if (index != -1) {
            args.add(index);
        }

        return createCommand(JSON_ARRPOP, new StringListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonArrtrim(K key, JsonPath jsonPath, int start, int stop) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(start);
        args.add(stop);

        return createCommand(JSON_ARRTRIM, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    @Deprecated
    Command<K, V, List<Long>> jsonArrtrim(K key, JsonPath jsonPath, JsonRangeArgs range) {
        notNullKey(key);
        notNullPath(jsonPath);
        LettuceAssert.notNull(range, "Range must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        range.build(args);

        return createCommand(JSON_ARRTRIM, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, Long> jsonClear(K key, JsonPath jsonPath) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());

        return createCommand(JSON_CLEAR, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue>> jsonGet(K key, JsonGetArgs options, JsonPath... jsonPaths) {
        notNullKey(key);
        LettuceAssert.notNull(options, "Options must not be null");
        LettuceAssert.noNullElements(jsonPaths, "JSON paths must not contain null elements");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        options.build(args);
        for (JsonPath jsonPath : jsonPaths) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_GET, new JsonValueListOutput<>(codec, parser.get()), args);
    }

    Command<K, V, List<String>> jsonGetRaw(K key, JsonGetArgs options, JsonPath... jsonPaths) {
        notNullKey(key);
        LettuceAssert.notNull(options, "Options must not be null");
        LettuceAssert.noNullElements(jsonPaths, "JSON paths must not contain null elements");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        options.build(args);
        for (JsonPath jsonPath : jsonPaths) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_GET, new StringListOutput<>(codec), args);
    }

    Command<K, V, String> jsonMerge(K key, JsonPath jsonPath, JsonValue value) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJson(value);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(value.asByteBuffer().array());

        return createCommand(JSON_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> jsonMerge(K key, JsonPath jsonPath, String value) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJsonString(value);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(value);

        return createCommand(JSON_MERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<JsonValue>> jsonMGet(JsonPath jsonPath, K... keys) {
        notNullPath(jsonPath);
        notEmpty(keys);
        LettuceAssert.noNullElements(keys, "Keys must not contain null elements");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        args.add(jsonPath.toString());

        return createCommand(JSON_MGET, new JsonValueListOutput<>(codec, parser.get()), args);
    }

    Command<K, V, List<String>> jsonMGetRaw(JsonPath jsonPath, K... keys) {
        notNullPath(jsonPath);
        notEmpty(keys);
        LettuceAssert.noNullElements(keys, "Keys must not contain null elements");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        args.add(jsonPath.toString());

        return createCommand(JSON_MGET, new StringListOutput<>(codec), args);
    }

    Command<K, V, String> jsonMSet(List<JsonMsetArgs<K, V>> arguments) {
        notEmpty(arguments.toArray());
        LettuceAssert.noNullElements(arguments, "Arguments must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        for (JsonMsetArgs<K, V> argument : arguments) {
            argument.build(args);
        }

        return createCommand(JSON_MSET, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Number>> jsonNumincrby(K key, JsonPath jsonPath, Number number) {
        notNullKey(key);
        notNullPath(jsonPath);
        LettuceAssert.notNull(number, "Number must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(number.toString());

        return createCommand(JSON_NUMINCRBY, new NumberListOutput<>(codec), args);
    }

    Command<K, V, List<V>> jsonObjkeys(K key, JsonPath jsonPath) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());

        return createCommand(JSON_OBJKEYS, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonObjlen(K key, JsonPath jsonPath) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());

        return createCommand(JSON_OBJLEN, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, String> jsonSet(K key, JsonPath jsonPath, JsonValue value, JsonSetArgs options) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJson(value);
        LettuceAssert.notNull(options, "Options must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());
        args.add(value.asByteBuffer().array());
        options.build(args);

        return createCommand(JSON_SET, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> jsonSet(K key, JsonPath jsonPath, String value, JsonSetArgs options) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJsonString(value);
        LettuceAssert.notNull(options, "Options must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(jsonPath.toString());
        args.add(value);
        options.build(args);

        return createCommand(JSON_SET, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrappend(K key, JsonPath jsonPath, JsonValue value) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJson(value);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());
        args.add(value.asByteBuffer().array());

        return createCommand(JSON_STRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrappend(K key, JsonPath jsonPath, String jsonString) {
        notNullKey(key);
        notNullPath(jsonPath);
        notNullJsonString(jsonString);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());
        args.add(jsonString.getBytes());

        return createCommand(JSON_STRAPPEND, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonStrlen(K key, JsonPath jsonPath) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());

        return createCommand(JSON_STRLEN, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Long>> jsonToggle(K key, JsonPath jsonPath) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(jsonPath.toString());

        return createCommand(JSON_TOGGLE, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, Long> jsonDel(K key, JsonPath jsonPath) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());

        return createCommand(JSON_DEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<JsonType>> jsonType(K key, JsonPath jsonPath) {
        notNullKey(key);
        notNullPath(jsonPath);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(jsonPath.toString());

        return createCommand(JSON_TYPE, new JsonTypeListOutput<>(codec), args);
    }

    private static void notNullPath(JsonPath jsonPath) {
        LettuceAssert.notNull(jsonPath, "JSON path " + MUST_NOT_BE_NULL);
    }

    private static void notNullJson(JsonValue jsonValue) {
        LettuceAssert.notNull(jsonValue, "JSON value" + MUST_NOT_BE_NULL);
    }

    private static void notNullJsonString(String json) {
        LettuceAssert.notNull(json, "JSON string " + MUST_NOT_BE_NULL);
    }

}
