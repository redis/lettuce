package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.SortedSetCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class SortedSetTxCommandIntegrationTests extends SortedSetCommandIntegrationTests {

    @Inject
    SortedSetTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }
}
