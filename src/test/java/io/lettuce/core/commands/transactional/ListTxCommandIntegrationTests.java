package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.ListCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class ListTxCommandIntegrationTests extends ListCommandIntegrationTests {

    @Inject
    ListTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }
}
