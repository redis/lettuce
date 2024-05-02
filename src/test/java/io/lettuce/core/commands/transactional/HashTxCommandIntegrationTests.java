package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HashCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class HashTxCommandIntegrationTests extends HashCommandIntegrationTests {

    @Inject
    HashTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
