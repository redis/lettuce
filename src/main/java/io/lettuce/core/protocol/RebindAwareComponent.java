/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;

/**
 * Interface for components that are aware of re-bind events.
 *
 * @author Tihomir Mateev
 * @since 6.7
 * @see ClientOptions#isProactiveRebindEnabled()
 */
public interface RebindAwareComponent {

    /**
     * Called whenever a re-bind has been initiated by the remote server
     */
    void onRebindStarted();

    /**
     * Called whenever the re-bind has been completed
     */
    void onRebindCompleted();

}
