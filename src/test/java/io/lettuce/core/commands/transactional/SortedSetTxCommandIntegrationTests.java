package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.SortedSetCommandIntegrationTests;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class SortedSetTxCommandIntegrationTests extends SortedSetCommandIntegrationTests {

    @Inject
    SortedSetTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
