package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.KeyCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
class KeyReactiveCommandIntegrationTests extends KeyCommandIntegrationTests {

    @Inject
    KeyReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
