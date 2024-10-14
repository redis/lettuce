package io.lettuce.core.sentinel.reactive;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.sentinel.SentinelCommandIntegrationTests;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
public class SentinelReactiveCommandIntegrationTests extends SentinelCommandIntegrationTests {

    @Inject
    public SentinelReactiveCommandIntegrationTests(RedisClient redisClient) {
        super(redisClient);
    }

    @Override
    protected RedisSentinelCommands<String, String> getSyncConnection(
            StatefulRedisSentinelConnection<String, String> connection) {
        return ReactiveSyncInvocationHandler.sync(connection);
    }

}
