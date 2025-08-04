/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;

/**
 * Interface for components that are aware of maintenance events.
 *
 * @author Tihomir Mateev
 * @since 7.0
 * @see ClientOptions#getMaintenanceEventsOptions()
 */
public interface MaintenanceAwareComponent {

    /**
     * Called whenever a shard migration is initiated
     */
    void onMigrateStarted(String shards);

    /**
     * Called whenever a shard migration is completed
     */
    void onMigrateCompleted(String shards);

    /**
     * Called whenever a failover is initiated
     */
    void onFailoverStarted(String shards);

    /**
     * Called whenever a failover is completed
     */
    void onFailoverCompleted(String shards);

    /**
     * Called whenever a re-bind has been initiated by the remote server
     */
    void onRebindStarted();

    /**
     * Called whenever the re-bind has been completed
     */
    void onRebindCompleted();

}
