/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

/**
 * JSON types as returned by the JSON.TYPE command
 *
 * @see io.lettuce.core.api.sync.RedisCommands#jsonType
 * @since 6.5
 * @author Tihomir Mateev
 */
public enum JsonType {

    OBJECT, ARRAY, STRING, INTEGER, NUMBER, BOOLEAN, UNKNOWN;

    public static JsonType fromString(String s) {
        switch (s) {
            case "object":
                return OBJECT;
            case "array":
                return ARRAY;
            case "string":
                return STRING;
            case "integer":
                return INTEGER;
            case "number":
                return NUMBER;
            case "boolean":
                return BOOLEAN;
            default:
                return UNKNOWN;
        }
    }

}
