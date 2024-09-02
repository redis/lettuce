/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
