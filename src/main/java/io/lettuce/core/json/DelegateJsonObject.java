/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.lettuce.core.codec.RedisCodec;

/**
 * Implementation of the {@link DelegateJsonObject} that delegates most of it's dunctionality to the Jackson {@link ObjectNode}.
 *
 * @param <V> Value type.
 * @author Tihomir Mateev
 */
class DelegateJsonObject<V> extends DelegateJsonValue<V> implements JsonObject<V> {

    DelegateJsonObject(RedisCodec<?, V> codec) {
        super(new ObjectNode(JsonNodeFactory.instance), codec);
    }

    DelegateJsonObject(JsonNode node, RedisCodec<?, V> codec) {
        super(node, codec);
    }

    @Override
    public JsonObject<V> put(V key, JsonValue<V> element) {
        String keyString = getStringValue(key);
        JsonNode newNode = ((DelegateJsonValue<V>) element).getNode();

        ((ObjectNode) node).replace(keyString, newNode);
        return this;
    }

    @Override
    public JsonValue<V> get(V key) {
        String keyString = getStringValue(key);
        JsonNode value = node.get(keyString);

        return new DelegateJsonValue<>(value, codec);
    }

    @Override
    public JsonValue<V> remove(V key) {
        String keyString = getStringValue(key);
        JsonNode value = ((ObjectNode) node).remove(keyString);

        return new DelegateJsonValue<>(value, codec);
    }

    @Override
    public int size() {
        return node.size();
    }

    @Override
    public boolean isJsonObject() {
        return true;
    }

    @Override
    public JsonObject<V> asJsonObject() {
        return this;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public String asString() {
        throw new UnsupportedOperationException("The JSON value is not a string");
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public Number asNumber() {
        throw new UnsupportedOperationException("The JSON value is not a number");
    }

    @Override
    public boolean isNull() {
        return false;
    }

}
