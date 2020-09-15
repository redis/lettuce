/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core

import kotlin.RequiresOptIn.Level.WARNING
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation marks the Lettuce Coroutine API that is considered experimental and is not subject to the
 * general compatibility guarantees given for the library.
 * The behavior and design of such API may be changed in any further release.
 *
 * Beware using the annotated API especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalLettuceCoroutinesApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalLettuceCoroutinesApi::class)`,
 * or by using the compiler argument `-Xopt-in=io.lettuce.core.ExperimentalLettuceCoroutinesApi`.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@RequiresOptIn(level = WARNING)
@Retention(BINARY)
@Target(
        CLASS,
        ANNOTATION_CLASS,
        PROPERTY,
        FIELD,
        LOCAL_VARIABLE,
        VALUE_PARAMETER,
        CONSTRUCTOR,
        FUNCTION,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        TYPEALIAS
)
annotation class ExperimentalLettuceCoroutinesApi
