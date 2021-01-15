/*
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link Map} of usernames and rules output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Mikhael Sokolov
 */
public class UserRulesOutput<K, V> extends CommandOutput<K, V, Map<String, Object>> {

    private String username;

    public UserRulesOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (username == null) {
            username = (bytes == null) ? null : decodeAscii(bytes);
            return;
        }

        Object value = (bytes == null) ? null : codec.decodeValue(bytes);
        output.put(username, value);
        username = null;
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        set(bytes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(long integer) {

        if (username == null) {
            username = String.valueOf(integer);
            return;
        }

        V value = (V) Long.valueOf(integer);
        output.put(username, value);
        username = null;
    }

    @Override
    public void set(double number) {

        if (username == null) {
            username = String.valueOf(number);
            return;
        }

        Object value = number;
        output.put(username, value);
        username = null;
    }

    @Override
    public void multi(int count) {
        if (output == null) {
            output = new LinkedHashMap<>(count / 2, 1);
        }
    }
}
