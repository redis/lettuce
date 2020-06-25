/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.dynamic.parameter;

import java.util.Iterator;

import io.lettuce.core.dynamic.domain.Timeout;

/**
 * Accessor interface to method parameters during the actual invocation.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface MethodParametersAccessor {

    /**
     * @return number of parameters.
     */
    int getParameterCount();

    /**
     * Returns the bindable value with the given index. Bindable means, that {@link Timeout} values are skipped without noticed
     * in the index. For a method signature taking {@link String}, {@link Timeout} , {@link String},
     * {@code #getBindableParameter(1)} would return the second {@link String} value.
     *
     * @param index parameter index.
     * @return the bindable value.
     */
    Object getBindableValue(int index);

    /**
     *
     * @param index parameter index.
     * @return {@code true} if the parameter at {@code index} is a key.
     */
    boolean isKey(int index);

    /**
     *
     * @param index parameter index.
     * @return {@code true} if the parameter at {@code index} is a value.
     */
    boolean isValue(int index);

    /**
     * Returns an iterator over all <em>bindable</em> parameters. This means parameters assignable to {@link Timeout} will not
     * be included in this {@link Iterator}.
     *
     * @return
     */
    Iterator<Object> iterator();

    /**
     * Resolve a parameter name to its index.
     *
     * @param name the name.
     * @return
     */
    int resolveParameterIndex(String name);

    /**
     * Return {@code true} if the parameter at {@code index} is a bindable {@code null} value that requires a {@code null} value
     * instead of being skipped.
     *
     * @param index parameter index.
     * @return {@code true} if the parameter at {@code index} is a bindable {@code null} value that requires a {@code null}
     *         value instead of being skipped.
     */
    boolean isBindableNullValue(int index);

}
