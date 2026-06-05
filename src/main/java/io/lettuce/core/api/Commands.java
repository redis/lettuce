/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.api;

/**
 * Common supertype for every Redis command API surface. Extended by the flavor-specific umbrellas ({@code ReactiveCommands},
 * and in the future {@code SyncCommands} / {@code AsyncCommands}). Serves as the value type cached on a connection and the
 * upper bound for {@link CommandsFactory}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.7
 */
public interface Commands<K, V> {

}
