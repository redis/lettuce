/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.lettuce.core.annotations.Experimental;

/**
 * Thread-safe registry of the {@link HashImport} fieldsets declared on a connection. It is the client-side source of truth used
 * to (re)establish the per-connection server session state of the {@code HIMPORT} command family across reconnects, new cluster
 * nodes, and pooled connections.
 * <p>
 * In standalone, master-replica, sentinel, and multi-database deployments each connection owns its own registry. In cluster
 * deployments a single registry instance is shared by object identity across the cluster connection and every node connection,
 * so that a node reading its own registry sees the whole cluster's declared fieldsets.
 * <p>
 * This class is part of the internal API.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 * @see HashImport
 * @see ConnectionState#getHashImportRegistry()
 */
@Experimental
public class HashImportRegistry {

    private final Set<HashImport<?>> fieldSets = ConcurrentHashMap.newKeySet();

    /**
     * Record a declared fieldset.
     *
     * @param fieldset the fieldset to register, must not be {@code null}.
     * @since 7.7
     */
    public void add(HashImport<?> fieldset) {
        fieldSets.add(fieldset);
    }

    /**
     * Remove a declared fieldset.
     *
     * @param fieldset the fieldset to remove, must not be {@code null}.
     * @return {@code true} if the fieldset was registered and has been removed.
     * @since 7.7
     */
    public boolean remove(HashImport<?> fieldset) {
        return fieldSets.remove(fieldset);
    }

    /**
     * Remove all declared fieldsets.
     *
     * @since 7.7
     */
    public void clear() {
        fieldSets.clear();
    }

    /**
     * @return the currently declared fieldsets. The returned view reflects concurrent modifications.
     * @since 7.7
     */
    public Collection<HashImport<?>> fieldSets() {
        return fieldSets;
    }

}
