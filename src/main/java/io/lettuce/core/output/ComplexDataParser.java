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
