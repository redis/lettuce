/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.internal;

import io.lettuce.core.api.StatefulConnection;

/**
 * Factory for creating command API instances. Implementations must be annotated with {@link CommandsFor} and discovered via
 * {@link java.util.ServiceLoader}.
 * <p>
 * Implementations are registered in {@code META-INF/services/io.lettuce.core.internal.CommandsBuilderFactory}.
 *
 * @since 7.7
 */
public interface CommandsBuilderFactory {

    /**
     * Create a command API instance for the given connection.
     *
     * @param connection the connection
     * @return the command API instance
     */
    Object create(StatefulConnection<?, ?> connection);

    /**
     * Check if this factory supports the given connection. Default checks if connection is instance of the annotated connection
     * type.
     *
     * @param connection the connection to check
     * @param connectionType the connection type from the annotation
     * @return true if supported
     */
    default boolean supports(StatefulConnection<?, ?> connection, Class<?> connectionType) {
        return connectionType.isInstance(connection);
    }

}
