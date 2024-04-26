package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.SetCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class SetTxCommandIntegrationTests extends SetCommandIntegrationTests {

    @Inject
    SetTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
