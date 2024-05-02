package io.lettuce.core.sentinel.reactive;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.sentinel.SentinelServerCommandIntegrationTests;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class SentinelServerReactiveCommandTest extends SentinelServerCommandIntegrationTests {

    @Inject
    public SentinelServerReactiveCommandTest(RedisClient redisClient) {
        super(redisClient);
    }

    @Override
    protected RedisSentinelCommands<String, String> getSyncConnection(
            StatefulRedisSentinelConnection<String, String> connection) {
        return ReactiveSyncInvocationHandler.sync(connection);
    }

}
