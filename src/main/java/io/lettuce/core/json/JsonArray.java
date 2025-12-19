/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import java.util.Iterator;
import java.util.List;

/**
 * Representation of a JSON array as per <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-5"> </a>RFC 8259 - The
 * JavaScript Object Notation (JSON) Data Interchange Format, Section 5. Arrays</a>
 * <p>
 *
 * @see JsonValue
 * @author Tihomir Mateev
 * @author Ko Su
 * @since 6.5
 */
public interface JsonArray extends JsonValue {

    /**
     * Add a new {@link JsonValue} to the array. Supports chaining of calls.
     *
     * @param element the value to add
     * @return the updated {@link JsonArray} to allow call chaining
     */
    JsonArray add(JsonValue element);

    /**
     * Add all elements from the provided {@link JsonArray} to this array.
     *
     * @param element the array to add all elements from
     */
    void addAll(JsonArray element);

    /**
     * Get all the {@link JsonValue}s in the array as a {@link List}.
     *
     * @return the {@link List} of {@link JsonValue}s in the array
     */
    List<JsonValue> asList();

    /**
     * Get the {@link JsonValue} at the provided index.
     *
     * @param index the index to get the value for
     * @return the {@link JsonValue} at the provided index or {@code null} if no value is found
     */
    JsonValue get(int index);

    /**
     * Get the first {@link JsonValue} in the array.
     *
     * @return the first {@link JsonValue} in the array or {@code null} if the array is empty
     */
    JsonValue getFirst();

    /**
     * Get an {@link Iterator} allowing access to all the {@link JsonValue}s in the array.
     *
     * @return the last {@link JsonValue} in the array or {@code null} if the array is empty
     */
    Iterator<JsonValue> iterator();

    /**
     * Remove the {@link JsonValue} at the provided index.
     *
     * @param index the index to remove the value for
     * @return the removed {@link JsonValue} or {@code null} if no value is found
     */
    JsonValue remove(int index);

    /**
     * Replace the {@link JsonValue} at the provided index with the provided new {@link JsonValue}.
     *
     * @param index the index to replace the value for
     * @param newElement the new value to replace the old one with
     * @return the oldValue {@link JsonValue} that was replaced
     */
    JsonValue replace(int index, JsonValue newElement);

    /**
     * Replace the {@link JsonValue} at the provided index with the given new {@link JsonValue}.
     *
     * @param index the index to replace the value for
     * @param newElement the new value to replace the old one with
     * @return the updated {@link JsonArray}
     * @since 7.0
     */
    JsonArray swap(int index, JsonValue newElement);

    /**
     * @return the number of elements in this {@link JsonArray}
     */
    int size();

}
