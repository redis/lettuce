package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.StreamCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class StreamTxCommandIntegrationTests extends StreamCommandIntegrationTests {

    @Inject
    StreamTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

    @Test
    @Disabled("TX over TX doesn't work")
    public void xreadTransactional() {
    }

}
