/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Interface for composite command argument objects. Implementing classes of {@link CompositeArgument} consolidate multiple
 * arguments for a particular Redis command in to one type and reduce the amount of individual arguments passed in a method
 * signature.
 * <p>
 * Command builder call {@link #build(CommandArgs)} during command construction to contribute command arguments for command
 * invocation. A composite argument is usually stateless as it can be reused multiple times by different commands.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see CommandArgs
 * @see SetArgs
 * @see ZStoreArgs
 * @see GeoArgs
 */
public interface CompositeArgument {

    /**
     * Build command arguments and contribute arguments to {@link CommandArgs}.
     * <p>
     * Implementing classes are required to implement this method. Depending on the command nature and configured arguments,
     * this method may contribute arguments but is not required to add arguments if none are specified.
     *
     * @param args the command arguments, must not be {@literal null}.
     * @param <K> Key type.
     * @param <V> Value type.
     */
    <K, V> void build(CommandArgs<K, V> args);
}
