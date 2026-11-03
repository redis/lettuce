/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for Redis Array commands using Redis Cluster.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
public class RedisArrayClusterIntegrationTests extends RedisArrayIntegrationTests {

    @Inject
    RedisArrayClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

}
