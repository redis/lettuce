/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core.dynamic;

import io.lettuce.core.dynamic.segment.CommandSegments;

/**
 * Verifies {@link CommandMethod} declarations by checking available Redis commands.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
interface CommandMethodVerifier {

    /**
     * Default instance that does not verify commands.
     */
    CommandMethodVerifier NONE = (commandSegments, commandMethod) -> {
    };

    /**
     * Verify a {@link CommandMethod} with its {@link CommandSegments}. This method verifies that the command exists and that
     * the required number of arguments is declared.
     *
     * @param commandSegments must not be {@code null}.
     * @param commandMethod must not be {@code null}.
     */
    void validate(CommandSegments commandSegments, CommandMethod commandMethod) throws CommandMethodSyntaxException;

}
