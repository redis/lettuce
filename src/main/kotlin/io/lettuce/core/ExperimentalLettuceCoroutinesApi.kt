/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
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
package io.lettuce.core

import kotlin.RequiresOptIn.Level.WARNING
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*

/**
 * Previously marked Lettuce Coroutine APIs that required explicit opt-in.
 *
 * Lettuce Coroutine APIs are no longer experimental, so this marker is retained only to keep existing source code
 * that still references `@OptIn(ExperimentalLettuceCoroutinesApi::class)` compatible.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@Deprecated("Lettuce Coroutine APIs are no longer experimental. Remove this opt-in marker from your code.")
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
