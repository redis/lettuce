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
 * Implementation of the {@link DelegateJsonObject} that delegates most of its functionality to the Jackson {@link ObjectNode}.
 *
 * @author Tihomir Mateev
 */
class DelegateJsonObject extends DelegateJsonValue implements JsonObject {

    DelegateJsonObject() {
        super(new ObjectNode(JsonNodeFactory.instance));
    }

    DelegateJsonObject(JsonNode node) {
        super(node);
    }

    @Override
    public JsonObject put(String key, JsonValue element) {
        JsonNode newNode = ((DelegateJsonValue) element).getNode();

        ((ObjectNode) node).replace(key, newNode);
        return this;
    }

    @Override
    public JsonValue get(String key) {
        JsonNode value = node.get(key);

        return value == null ? null : wrap(value);
    }

    @Override
    public JsonValue remove(String key) {
        JsonNode value = ((ObjectNode) node).remove(key);

        return wrap(value);
    }

    @Override
    public int size() {
        return node.size();
    }

    @Override
    public JsonObject asJsonObject() {
        return this;
    }

}
