/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.codec.RedisCodec;

public class JsonParserRegistry {

    private JsonParserRegistry() {
    }

    // TODO make this configurable with ClientOptions to enable other types of parsers
    public static <K, V> JsonParser<V> getJsonParser(RedisCodec<K, V> codec) {
        return new DefaultJsonParser<>(codec);
    }

}
