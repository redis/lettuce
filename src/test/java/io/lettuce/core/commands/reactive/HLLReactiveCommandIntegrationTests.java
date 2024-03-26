package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HLLCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
class HLLReactiveCommandIntegrationTests extends HLLCommandIntegrationTests {

    @Inject
    HLLReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }
}
