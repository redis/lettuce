/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;

import java.util.List;
import java.util.Set;

/**
 * Interface for components that are aware of re-bind events.
 *
 * @author Tihomir Mateev
 * @since 6.7
 * @see ClientOptions#isProactiveRebindEnabled()
 */
public interface RebindAwareComponent {

    /**
     * Called whenever a shard migration is initiated
     */
    void onMigrateStarted();

    /**
     * Called whenever a shard migration is completed
     */
    void onMigrateCompleted();

    /**
     * Called whenever a fail over is initiated
     */
    void onFailoverStarted(String shards);

    /**
     * Called whenever a fail over is completed
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
