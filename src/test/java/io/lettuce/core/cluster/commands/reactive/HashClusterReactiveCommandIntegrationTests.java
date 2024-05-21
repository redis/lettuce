package io.lettuce.core.cluster.commands.reactive;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.HashCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

/**
 * @author Mark Paluch
 */
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
