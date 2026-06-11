/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.function.Function;

/**
 * A factory that builds a command API ({@code T}) from a connection ({@code C}). It is passed to
 * {@link StatefulConnection#commands(CommandsFactory)}, which applies it against the connection and caches ("stamps") the
 * result under {@link #key()}.
 * <p>
 * Factories are flavor-neutral: this interface carries no reference to a specific reactive/sync/async library. The concrete,
 * family-precise factories are exposed as {@code XxxCommands.factory()} on the command interfaces, which keeps this interface
 * and {@link StatefulConnection} free of any such dependency.
 *
 * @param <C> connection type the factory accepts.
 * @param <T> command API type the factory produces.
 * @since 7.7
 */
public interface CommandsFactory<C, T> extends Function<C, T> {

    /**
     * Cache key under which the produced command API is stamped on the connection. Today this is the command-API type; a future
     * multi-codec factory would return a composite key (e.g. type + codec) so several views of the same type can coexist on one
     * connection.
     *
     * @return the cache key.
     */
    Object key();

    /**
     * Create a {@link CommandsFactory} from a cache {@code key} and a {@code builder}.
     *
     * @param key the cache key (see {@link #key()}).
     * @param builder builds the command API from the connection.
     * @param <C> connection type.
     * @param <T> command API type.
     * @return a new factory.
     */
    static <C, T> CommandsFactory<C, T> of(Object key, Function<C, T> builder) {
        return new CommandsFactory<C, T>() {

            @Override
            public Object key() {
                return key;
            }

            @Override
            public T apply(C connection) {
                return builder.apply(connection);
            }

        };
    }

}
