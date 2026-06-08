/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

/**
 * Marker interface for Redis command API surfaces (sync, async, reactive). Serves as the common type cached on a connection by
 * the {@code XxxCommands.from(connection)} factories (see {@link io.lettuce.core.internal.CommandsCache}).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.7
 */
public interface Commands<K, V> {

}
