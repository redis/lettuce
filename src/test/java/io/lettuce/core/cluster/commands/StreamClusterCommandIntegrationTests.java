package io.lettuce.core.cluster.commands;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.StreamCommandIntegrationTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisStreamCommands} using Redis Cluster.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class StreamClusterCommandIntegrationTests extends StreamCommandIntegrationTests {

    @Inject
    StreamClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Test
    @Override
    public void xreadTransactional() {
        super.xreadTransactional();
    }

    @Disabled("Required node reconfiguration with stream-node-max-entries")
    @Test
    @Override
    public void xaddMinidLimit() {
        super.xaddMinidLimit();
    }

}
