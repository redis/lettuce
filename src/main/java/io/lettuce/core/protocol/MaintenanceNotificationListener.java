/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.protocol.MaintenanceNotification.Moving;

/**
 * Listener for maintenance notifications received on a connection.
 * <p>
 * Callbacks are invoked on the connection's event loop and must not block. This interface is part of the internal API and may
 * change without further notice.
 *
 * @since 8.0
 * @see MaintenanceNotification
 * @see MaintenanceAwareConnectionWatchdog
 */
public interface MaintenanceNotificationListener {

    /**
     * Called for each maintenance notification received on the connection.
     *
     * @param notification the notification
     */
    void onMaintenanceNotification(MaintenanceNotification notification);

    /**
     * Called when the connection starts re-binding to another endpoint.
     *
     * @param moving the notification that triggered the re-bind
     */
    default void onRebindStarted(Moving moving) {
    }

    /**
     * Called when the connection has closed its channel to complete a re-bind.
     *
     * @param moving the notification that triggered the re-bind
     */
    default void onRebindCompleted(Moving moving) {
    }

}
