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
 * Describes a path to a certain {@link JsonElement} inside a JSON document.<p/>
 * <p>
 * The Redis server implements its own JSONPath implementation, based on existing technologies.
 * The generic rules to build a path string are:
 * <ul>
 *     <li><code>$</code> - The root (outermost JSON element), starts the path.</li>
 *     <li><code>. or []</code> - Selects a child element.</li>
 *     <li><code>..</code> - Recursively descends through the JSON document.</li>
 *     <li><code>*</code> - Wildcard, returns all elements.</li>
 *     <li><code>[]</code> - Subscript operator, accesses an array element.</li>
 *     <li><code>[,]]</code> - Union, selects multiple elements.</li>
 *     <li><code>[start:end:step]</code> - Array slice where start, end, and step are indexes.</li>
 *     <li><code>?()</code> - Filters a JSON object or array. Supports comparison operators (==, !=, <, <=, >, >=, =~), logical operators (&&, ||), and parenthesis ((, )).</li>
 *     <li><code>()</code> - Script expression.</li>
 *     <li><code>@</code> - The current element, used in filter or script expressions.</li>
 * </ul>
 * <p>
 * For example, given the following JSON document:
 * <pre>
 * {
 *     "inventory": {
 *         "mountain_bikes": [
 *             {
 *                 "id": "bike:1",
 *                 "model": "Phoebe",
 *                 "description": "This is a mid-travel trail slayer that is a fantastic daily...",
 *                 "price": 1920,
 *                 "specs": {"material": "carbon", "weight": 13.1},
 *                 "colors": ["black", "silver"],
 *             },
 *             ...
 *         }
 *     }
 *}
 * </pre>
 * <p>
 * To get a list of all the {@code mountain_bikes} inside the {@code inventory} you would write something like:
 * <p>
 * {@code JSON.GET store '$.inventory["mountain_bikes"]' }
 *
 * @author Tihomir Mateev
 * @since 6.5
 * @see <a href="https://redis.io/docs/latest/develop/data-types/json/path/#jsonpath-support">JSON Path in Redis docs</a>
 */
public class JsonPath {

    /**
     * The root path {@code $} as defined by the second version of the RedisJSON implementation.
     * <p>
     * @since 6.5
     */
    public static final JsonPath ROOT_PATH = new JsonPath("$");

    /**
     * The legacy root path {@code .} as defined by the first version of the RedisJSON implementation.
     * @deprecated since 6.5, use {@link #ROOT_PATH} instead.
     */
    public static final JsonPath ROOT_PATH_LEGACY = new JsonPath(".");

    private final String path;

    /**
     * Create a new {@link JsonPath} given a path string.
     *
     * @param pathString the path string, must not be {@literal null} or empty.
     */
    public JsonPath(final String pathString) {

        if (pathString == null) {
            throw new IllegalArgumentException("Path cannot be null.");
        }

        if (pathString.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty.");
        }

        this.path = pathString;
    }

    @Override
    public String toString() {
        return path;
    }

    /**
     * Create a new {@link JsonPath} given a path string.
     *
     * @param path the path string, must not be {@literal null} or empty.
     * @return the {@link JsonPath}.
     */
    public static JsonPath of(final String path) {
        return new JsonPath(path);
    }

    @Override
    public boolean equals(Object obj) {
        return this.path.equals(obj);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    public boolean isRootPath() {
        return ROOT_PATH.toString().equals(path) || ROOT_PATH_LEGACY.toString().equals(path);
    }

}
