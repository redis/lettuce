/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import java.nio.ByteBuffer;

public interface JsonParser<V> {

    JsonValue<V> createJsonValue(ByteBuffer bytes);

    JsonValue<V> createJsonValue(V value);

    JsonObject<V> createEmptyJsonObject();

    JsonArray<V> createEmptyJsonArray();

    JsonValue<V> parse(ByteBuffer byteBuffer);

}
