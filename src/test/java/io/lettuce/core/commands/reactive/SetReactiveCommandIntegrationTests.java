package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.SetCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class SetReactiveCommandIntegrationTests extends SetCommandIntegrationTests {

    @Inject
    SetReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
