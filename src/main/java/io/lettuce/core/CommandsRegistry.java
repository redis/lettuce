/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.internal.CommandsBuilderFactory;
import io.lettuce.core.internal.CommandsFor;

/**
 * Registry for command API implementations. Discovers classes annotated with {@link CommandsFor} via {@link ServiceLoader} at
 * startup and assigns each a slot index. Command APIs are eagerly initialized when a connection is created via
 * {@link #initialiseSlots(StatefulConnection)}.
 *
 * @since 7.7
 */
final class CommandsRegistry {

    private static final Map<Class<?>, Integer> SLOT_INDEX = new HashMap<>();

    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        for (CommandsBuilderFactory factory : ServiceLoader.load(CommandsBuilderFactory.class)) {
            CommandsFor annotation = factory.getClass().getAnnotation(CommandsFor.class);
            if (annotation == null) {
                throw new IllegalStateException("@CommandsFor annotation missing on " + factory.getClass().getName());
            }
            SLOT_INDEX.put(annotation.api(), ENTRIES.size());
            ENTRIES.add(new Entry(annotation, factory));
        }
    }

    private CommandsRegistry() {
    }

    /**
     * Initialize all command slots for the given connection. Called once during connection creation.
     *
     * @param connection the connection
     * @return array of command API instances for this connection
     */
    static Object[] initialiseSlots(StatefulConnection<?, ?> connection) {
        Object[] slots = new Object[ENTRIES.size()];
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            if (entry.supports(connection)) {
                slots[i] = entry.create(connection);
            }
        }
        return slots;
    }

    /**
     * Get a command API instance for the given type.
     *
     * @param slots the command slots array from the connection
     * @param type the command API type
     * @param <T> Command API type
     * @return the command API instance
     * @throws IllegalArgumentException if no provider is registered for the given type
     */
    @SuppressWarnings("unchecked")
    static <T> T get(Object[] slots, Class<T> type) {
        Integer slot = SLOT_INDEX.get(type);
        if (slot == null) {
            throw new IllegalArgumentException("No provider registered for: " + type.getName());
        }
        return (T) slots[slot];
    }

    /**
     * Internal entry holding annotation metadata and factory.
     */
    private static class Entry {

        private final Class<?> connectionType;

        private final CommandsBuilderFactory factory;

        Entry(CommandsFor annotation, CommandsBuilderFactory factory) {
            this.connectionType = annotation.connection();
            this.factory = factory;
        }

        boolean supports(StatefulConnection<?, ?> connection) {
            return factory.supports(connection, connectionType);
        }

        Object create(StatefulConnection<?, ?> connection) {
            return factory.create(connection);
        }

    }

}
