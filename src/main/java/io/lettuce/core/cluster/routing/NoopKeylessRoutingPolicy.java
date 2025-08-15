/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster.routing;

import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.protocol.RedisCommand;

import java.util.Collections;

/**
 * Default no-op policy that preserves existing behavior (single default connection).
 */
public class NoopKeylessRoutingPolicy implements KeylessRoutingPolicy {

    @Override
    public <K, V, T> Decision classify(RedisCommand<K, V, T> command, Partitions topology) {
        // Return null to indicate no-op/default routing behavior.
        return null;
    }

}
