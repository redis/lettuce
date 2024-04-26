package io.lettuce.core.sentinel.reactive;

import javax.inject.Inject;

import io.lettuce.RedisBug;
import io.lettuce.core.RedisClient;
import io.lettuce.core.sentinel.SentinelCommandIntegrationTests;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class SentinelReactiveCommandTest extends SentinelCommandIntegrationTests {

    @Inject
    public SentinelReactiveCommandTest(RedisClient redisClient) {
        super(redisClient);
    }

    @Override
    protected RedisSentinelCommands<String, String> getSyncConnection(
            StatefulRedisSentinelConnection<String, String> connection) {
        return ReactiveSyncInvocationHandler.sync(connection);
    }

}
