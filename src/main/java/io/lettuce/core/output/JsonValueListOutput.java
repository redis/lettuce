/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.JsonParser;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link List} of string output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.5
 */
public class JsonValueListOutput<K, V> extends CommandOutput<K, V, List<JsonValue>> {

    private boolean initialized;

    private final JsonParser parser;

    public JsonValueListOutput(RedisCodec<K, V> codec, JsonParser theParser) {
        super(codec, Collections.emptyList());
        parser = theParser;
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (!initialized) {
            multi(1);
        }

        ByteBuffer fetched = null;
        if (bytes != null) {
            fetched = ByteBuffer.allocate(bytes.remaining());
            fetched.put(bytes);
            fetched.flip();
        }

        output.add(parser.loadJsonValue(fetched));
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

}
