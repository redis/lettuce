/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;

/**
 * Interface for components that are aware of slot migration maintenance events.
 * <p>
 * Slot migrations are announced by {@literal SMIGRATING} and {@literal SMIGRATED} maintenance push notifications and are
 * specific to Redis Enterprise operating in OSS cluster mode. They are kept apart from {@link MaintenanceAwareComponent}
 * because a component typically reacts to either the connection-level maintenance events or the slot topology events, not both.
 * </p>
 *
 * @since 7.7
 * @see MaintenanceAwareComponent
 * @see ClientOptions#getMaintNotificationsConfig()
 */
public interface MaintenanceAwareClusterComponent {

    /**
     * Called whenever slots start migrating.
     *
     * @param slots the slots that are migrating
     */
    default void onSlotMigrateStarted(String slots) {
    }

    /**
     * Called whenever a slot migration is completed.
     *
     * @param slots the slots that were migrated
     */
    default void onSlotMigrateCompleted(String slots) {
    }

}
