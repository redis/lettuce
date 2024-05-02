package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.SortCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class SortTxCommandIntegrationTests extends SortCommandIntegrationTests {

    @Inject
    SortTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
