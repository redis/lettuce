/*
 * Copyright 2024, Redis Ltd. and Contributors
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

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import io.lettuce.core.output.IntegerListOutput;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.JsonValueListOutput;
import io.lettuce.core.output.NumberListOutput;
import io.lettuce.core.output.ValueListOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.RedisCommand;

import java.util.List;

import static io.lettuce.core.protocol.CommandType.JSON_ARRAPPEND;
import static io.lettuce.core.protocol.CommandType.JSON_ARRLEN;
import static io.lettuce.core.protocol.CommandType.JSON_ARRPOP;
import static io.lettuce.core.protocol.CommandType.JSON_DEL;
import static io.lettuce.core.protocol.CommandType.JSON_GET;
import static io.lettuce.core.protocol.CommandType.JSON_NUMINCRBY;
import static io.lettuce.core.protocol.CommandType.JSON_TYPE;

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

    Command<K, V, List<Long>> jsonArrappend(K key, JsonPath jsonPath, JsonValue<V>[] jsonValues) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        for (JsonValue<V> value : jsonValues) {
            args.addValue(value.toValue());
        }

        return createCommand(JSON_ARRAPPEND, new IntegerListOutput<>(codec), args);
    }

    RedisCommand<K, V, List<Long>> jsonArrindex(K key, JsonPath jsonPath, JsonValue<V> value, JsonRangeArgs range) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.addValue(value.toValue());

        if (range != null) {
            // OPTIONAL as per API
            range.build(args);
        }

        return createCommand(JSON_ARRLEN, new IntegerListOutput<>(codec), args);
    }

    RedisCommand<K, V, List<Long>> jsonArrinsert(K key, JsonPath jsonPath, int index, JsonValue<V>[] values) {
        return null;
    }

    RedisCommand<K, V, List<Long>> jsonArrlen(K key, JsonPath jsonPath) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }
        return createCommand(JSON_ARRLEN, new IntegerListOutput<>(codec), args);
    }

    RedisCommand<K, V, List<JsonValue<V>>> jsonArrpop(K key, JsonPath jsonPath, int index) {
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

    RedisCommand<K, V, List<Long>> jsonArrtrim(K key, JsonPath jsonPath, JsonRangeArgs range) {
        return null;
    }

    RedisCommand<K, V, Long> jsonClear(K key, JsonPath jsonPath) {
        return null;
    }

    RedisCommand<K, V, List<JsonValue<V>>> jsonGet(K key, JsonGetArgs options, JsonPath[] jsonPaths) {
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

    RedisCommand<K, V, Boolean> jsonMerge(K key, JsonPath jsonPath, JsonValue<V> value) {
        return null;
    }

    RedisCommand<K, V, List<JsonValue<V>>> jsonMGet(JsonPath jsonPath, K[] keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);

        if (jsonPath != null) {
            args.add(jsonPath.toString());
        }

        return createCommand(JSON_GET, new JsonValueListOutput<>(codec), args);
    }

    RedisCommand<K, V, Boolean> jsonMSet(JsonMsetArgs[] arguments) {
        return null;
    }

    RedisCommand<K, V, List<Number>> jsonNumincrby(K key, JsonPath jsonPath, Number number) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (jsonPath != null && !jsonPath.isRootPath()) {
            args.add(jsonPath.toString());
        }

        args.add(number.toString());

        return createCommand(JSON_NUMINCRBY, new NumberListOutput<>(codec), args);
    }

    RedisCommand<K, V, List<List<V>>> jsonObjkeys(K key, JsonPath jsonPath) {
        return null;
    }

    RedisCommand<K, V, List<Long>> jsonObjlen(K key, JsonPath jsonPath) {
        return null;
    }

    RedisCommand<K, V, Boolean> jsonSet(K key, JsonPath jsonPath, JsonValue<V> value, JsonSetArgs options) {
        return null;
    }

    RedisCommand<K, V, List<Long>> jsonStrappend(K key, JsonPath jsonPath, JsonValue<V> value) {
        return null;
    }

    RedisCommand<K, V, List<Long>> jsonStrlen(K key, JsonPath jsonPath) {
        return null;
    }

    RedisCommand<K, V, List<Boolean>> jsonToggle(K key, JsonPath jsonPath) {
        return null;
    }

    RedisCommand<K, V, Long> jsonDel(K key, JsonPath jsonPath) {
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
