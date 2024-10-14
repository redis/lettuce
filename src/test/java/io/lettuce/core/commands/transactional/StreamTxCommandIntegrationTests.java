package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.StreamCommandIntegrationTests;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
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
