/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

/**
 * Representation of a JSON object as per <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-4"> </a>RFC 8259 - The
 * JavaScript Object Notation (JSON) Data Interchange Format, Section 4. Objects</a>
 * <p>
 *
 * @param <V> Value type based on the {@link io.lettuce.core.codec.RedisCodec} used.
 * @see JsonValue
 * @author Tihomir Mateev
 * @since 6.5
 */
public interface JsonObject<V> extends JsonValue<V> {

    /**
     * Add (if there is no value with the same key already) or replace (if there is) a new {@link JsonValue} to the object under
     * the provided key. Supports chaining of calls.
     *
     * @param key the key of the {@link JsonValue} to add or replace
     * @param element the value to add or replace
     * @return the updated {@link JsonObject} to allow call chaining
     */
    JsonObject<V> put(V key, JsonValue<V> element);

    /**
     * Get the {@link JsonValue} under the provided key.
     *
     * @param key the key to get the value for
     * @return the {@link JsonValue} under the provided key or {@code null} if no value is found
     */
    JsonValue<V> get(V key);

    /**
     * Remove the {@link JsonValue} under the provided key.
     *
     * @param key the key to remove the value for
     * @return the removed {@link JsonValue} or {@code null} if no value is found
     */
    JsonValue<V> remove(V key);

    /**
     * @return the number of key-value pairs in this {@link JsonObject}
     */
    int size();

}
