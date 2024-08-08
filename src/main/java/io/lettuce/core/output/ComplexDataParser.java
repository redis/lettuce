/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

/**
 * Any usage of the {@link ComplexOutput} comes hand in hand with a respective {@link ComplexDataParser} that is able to parse
 * the data extracted from the server to a meaningful Java object.
 * 
 * @param <T> the type of the parsed object
 * @author Tihomir Mateev
 * @see ComplexData
 * @see ComplexOutput
 * @since 6.5
 */
public interface ComplexDataParser<T> {

    /**
     * Parse the data extracted from the server to a specific domain object.
     *
     * @param data the data to parse
     * @return the parsed object
     * @since 6.5
     */
    T parse(ComplexData data);

}
