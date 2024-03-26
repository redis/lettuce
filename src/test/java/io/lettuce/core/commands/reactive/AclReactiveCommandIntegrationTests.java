package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.AclCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * Integration tests though the reactive facade for {@link AclCommandIntegrationTests}.
 *
 * @author Mark Paluch
 */
class AclReactiveCommandIntegrationTests extends AclCommandIntegrationTests {

    @Inject
    AclReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
