/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.protocol;

import io.lettuce.core.annotations.Experimental;

/**
 * Activation command marking the internally-injected {@code HIMPORT PREPARE} so it bypasses bounded request-queue limits, the
 * same way {@code AUTH} and {@code SELECT} are allowed on top of user commands. The {@code PREPARE} is injected transparently
 * ahead of the first {@code HIMPORT SET} for a fieldset on a physical connection, so it must not consume the caller's
 * configured request-queue capacity.
 * <p>
 * This class is part of the internal API.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Command output type.
 * @author Aleksandar Todorov
 * @since 7.7
 */
@Experimental
public class HashImportPrepareCommand<K, V, T> extends ActivationCommand<K, V, T> {

    public HashImportPrepareCommand(RedisCommand<K, V, T> command) {
        super(command);
    }

}
