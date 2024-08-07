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

package io.lettuce.core.output;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The base type of all aggregate data, collected by a {@link io.lettuce.core.output.DynamicAggregateOutput}
 * <p>
 * Commands typically result in simple types, however some of the commands could return complex nested structures. A simple
 * solution to parse such a structure is to have a dynamic data type and leave the user to parse the result to a domain object.
 * If there is some breach of contract then the code consuming the driver could simply stop using the provided parser and start
 * parsing the new dynamic data itself, without having to change the version of the library. This allows a certain degree of
 * stability against change. Consult the members of the {@link io.lettuce.core.api.parsers} package for details on how aggregate
 * Objects could be parsed.
 * <p>
 * An aggregate data object could contain multiple (or no) units of simple data, as per the
 * <a href="https://github.com/redis/redis-specifications/blob/master/protocol/RESP2.md">RESP2</a> and
 * <a href="https://github.com/redis/redis-specifications/blob/master/protocol/RESP3.md">RESP3</a> protocol definitions.
 * <p>
 * For RESP2 the only possible aggregation is an array. RESP2 commands could also return sets (obviously, by simply making sure
 * the elements of the array are unique) or maps (by sending the keys as odd elements and their values as even elements in the
 * right order one after another.
 * <p>
 * For RESP3 all the three aggregate types are supported (and indicated with special characters when the result is returned by
 * the server).
 * <p>
 * Aggregate data types could also be nested by using the {@link DynamicAggregateData#storeObject(Object)} call.
 * <p>
 * Implementations of this class override the {@link DynamicAggregateData#getDynamicSet()},
 * {@link DynamicAggregateData#getDynamicList()} and {@link DynamicAggregateData#getDynamicMap()} methods to return the data
 * received in the server in a implementation of the Collections framework. If a given implementation could not do the
 * conversion in a meaningful way an {@link UnsupportedOperationException} would be thrown.
 *
 * @see io.lettuce.core.output.DynamicAggregateOutput
 * @see ArrayAggregateData
 * @see SetAggregateData
 * @see MapAggregateData
 * @author Tihomir Mateev
 * @since 6.5
 */
public abstract class DynamicAggregateData {

    /**
     * Store a <code>long</code> value in the underlying data structure
     *
     * @param value the value to store
     */
    public void store(long value) {
        storeObject(value);
    }

    /**
     * Store a <code>double</code> value in the underlying data structure
     *
     * @param value the value to store
     */
    public void store(double value) {
        storeObject(value);
    }

    /**
     * Store a <code>boolean</code> value in the underlying data structure
     *
     * @param value the value to store
     */
    public void store(boolean value) {
        storeObject(value);
    }

    /**
     * Store an {@link Object} value in the underlying data structure. This method should be used when nesting one instance of
     * {@link DynamicAggregateData} inside another
     *
     * @param value the value to store
     */
    public abstract void storeObject(Object value);

    public void store(String value) {
        storeObject(value);
    }

    /**
     * Returns the aggregate data in a {@link List} form. If the underlying implementation could not convert the data structure
     * to a {@link List} then an {@link UnsupportedOperationException} would be thrown.
     *
     * @return a {@link List} of {@link Object} values
     */
    public List<Object> getDynamicList() {
        throw new UnsupportedOperationException("The type of data stored in this dynamic object is not a list");
    }

    /**
     * Returns the aggregate data in a {@link Set} form. If the underlying implementation could not convert the data structure
     * to a {@link Set} then an {@link UnsupportedOperationException} would be thrown.
     *
     * @return a {@link Set} of {@link Object} values
     */
    public Set<Object> getDynamicSet() {
        throw new UnsupportedOperationException("The type of data stored in this dynamic object is not a set");
    }

    /**
     * Returns the aggregate data in a {@link Map} form. If the underlying implementation could not convert the data structure
     * to a {@link Map} then an {@link UnsupportedOperationException} would be thrown.
     *
     * @return a {@link Map} of {@link Object} values
     */
    public Map<Object, Object> getDynamicMap() {
        throw new UnsupportedOperationException("The type of data stored in this dynamic object is not a map");
    }

}
