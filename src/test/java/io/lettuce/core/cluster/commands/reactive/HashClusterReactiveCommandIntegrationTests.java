package io.lettuce.core.cluster.commands.reactive;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.HashCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class HashClusterReactiveCommandIntegrationTests extends HashCommandIntegrationTests {

    @Inject
    HashClusterReactiveCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

    @Test
    @Disabled("API differences")
    public void hgetall() {

    }

    @Test
    @Disabled("API differences")
    public void hgetallStreaming() {

    }

}
