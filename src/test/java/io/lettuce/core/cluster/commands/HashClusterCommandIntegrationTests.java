package io.lettuce.core.cluster.commands;

import javax.inject.Inject;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.HashCommandIntegrationTests;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisHashCommands} using Redis Cluster.
 *
 * @author Mark Paluch
 */
public class HashClusterCommandIntegrationTests extends HashCommandIntegrationTests {

    @Inject
    public HashClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

}
