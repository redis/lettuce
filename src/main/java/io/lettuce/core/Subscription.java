/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import java.io.Closeable;

/**
 * Handle to a callback subscription on a Lettuce streaming SPI, such as
 * {@link io.lettuce.core.event.EventBus#subscribe(java.util.function.Consumer)}. Closing the subscription stops delivery of
 * further values to the registered callback.
 * <p>
 * This is not related to {@code org.reactivestreams.Subscription} or to Redis Pub/Sub channel subscriptions.
 *
 * @author Aleksandar Todorov
 * @since 8.0
 */
public interface Subscription extends Closeable {

    /**
     * Stop delivering to the registered callback. Idempotent; calling it more than once has no further effect and never throws.
     */
    @Override
    void close();

}
