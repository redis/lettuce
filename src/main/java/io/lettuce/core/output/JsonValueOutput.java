/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.JsonParser;

import java.nio.ByteBuffer;

/**
 * Single {@link JsonValue} output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
public class JsonValueOutput<K, V> extends CommandOutput<K, V, JsonValue> {

    private final JsonParser parser;

    public JsonValueOutput(RedisCodec<K, V> codec, JsonParser theParser) {
        super(codec, null);
        parser = theParser;
    }

    @Override
    public void set(ByteBuffer bytes) {
        ByteBuffer fetched = null;
        if (bytes != null) {
            fetched = ByteBuffer.allocate(bytes.remaining());
            fetched.put(bytes);
            fetched.flip();
        }

        output = parser.loadJsonValue(fetched);
    }

}
