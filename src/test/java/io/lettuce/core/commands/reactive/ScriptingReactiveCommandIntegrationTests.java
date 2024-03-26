package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.ScriptingCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
class ScriptingReactiveCommandIntegrationTests extends ScriptingCommandIntegrationTests {

    @Inject
    ScriptingReactiveCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, ReactiveSyncInvocationHandler.sync(connection));
    }
}
