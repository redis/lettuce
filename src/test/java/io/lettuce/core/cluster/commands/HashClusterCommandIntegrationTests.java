package io.lettuce.core.cluster.commands;

import javax.inject.Inject;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.HashCommandIntegrationTests;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisHashCommands} using Redis Cluster.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
public class HashClusterCommandIntegrationTests extends HashCommandIntegrationTests {

    @Inject
    public HashClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

}
