/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonType;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link List} of {@link JsonType} output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.5
 */
public class JsonTypeListOutput<K, V> extends CommandOutput<K, V, List<JsonType>> {

    private boolean initialized;

    public JsonTypeListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (!initialized) {
            multi(1);
        }

        output.add(JsonType.fromString(decodeString(bytes)));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

}
