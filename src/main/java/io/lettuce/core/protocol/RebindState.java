/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;

/**
 * The current step of the re-bind process
 *
 * @author Tihomir Mateev
 * @since 7.0
 * @see ClientOptions#getMaintenanceEventsOptions()
 */
public enum RebindState {
    /**
     * The re-bind has been initiated by the remote server
     */
    STARTED,

    /**
     * The re-bind has been completed
     */
    COMPLETED
}
