/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster.routing;

import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.protocol.RedisCommand;

/**
 * SPI to classify keyless commands (no first encoded key) for routing purposes.
 *
 * Implementations can attach routing intent metadata that downstream components may use to decide whether to send a command to
 * a single node or broadcast.
 *
 * Default behavior must not alter runtime routing unless explicitly opted in by users.
 */
public interface KeylessRoutingPolicy {

    enum Strategy {
        SINGLE_NODE, BROADCAST, REJECT
    }

    final class Decision {

        public final Strategy strategy;

        public final Iterable<RedisClusterNode> candidates; // may be null to signal no-op/default routing

        private Decision(Strategy strategy, Iterable<RedisClusterNode> candidates) {
            this.strategy = strategy;
            this.candidates = candidates;
        }

        public static Decision singleNode(Iterable<RedisClusterNode> candidates) {
            return new Decision(Strategy.SINGLE_NODE, candidates);
        }

        public static Decision broadcast(Iterable<RedisClusterNode> candidates) {
            return new Decision(Strategy.BROADCAST, candidates);
        }

        public static Decision reject() {
            return new Decision(Strategy.REJECT, null);
        }

    }

    <K, V, T> Decision classify(RedisCommand<K, V, T> command, Partitions topology);

}
