package io.lettuce.core.cluster;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Functional routing Strategy: takes the original command and returns the command to send.
 */
@FunctionalInterface
interface ClusterDistributionStrategy {

    RedisCommand<?, ?, ?> execute(RedisCommand<?, ?, ?> original);

}
