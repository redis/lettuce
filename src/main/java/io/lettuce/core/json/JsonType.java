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
 * @author Yordan Tsintsov
 */
public enum JsonType {

    OBJECT, ARRAY, STRING, INTEGER, NUMBER, BOOLEAN, UNKNOWN;

    /**
     * Resolve a {@link JsonType} from the type name returned by the {@code JSON.TYPE} command.
     *
     * @param s the type name as returned by the server, may be {@code null}.
     * @return the matching {@link JsonType}, {@link #UNKNOWN} if the name is not recognized, or {@code null} if {@code s} is
     *         {@code null}.
     */
    public static JsonType fromString(String s) {
        if (s == null) {
            return null;
        }
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
