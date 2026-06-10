/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.function.Function;

/**
 * A factory that builds a command API ({@code T}) from a connection ({@code C}). It is passed to
 * {@link StatefulConnection#commands(CommandsFactory)}, which applies it against the connection and caches ("stamps") the
 * result keyed by {@link #type()}.
 * <p>
 * Factories are flavor-neutral: this interface carries no reference to a specific reactive/sync/async library. The concrete,
 * family-precise factories live in dedicated providers (e.g. {@code CommandsFactoryProvider}), which keeps this interface and
 * {@link StatefulConnection} free of any such dependency.
 *
 * @param <C> connection type the factory accepts.
 * @param <T> command API type the factory produces.
 * @since 7.7
 */
public interface CommandsFactory<C, T> extends Function<C, T> {

    /**
     * @return the command API type, used as the cache key on the connection.
     */
    Class<T> type();

}
