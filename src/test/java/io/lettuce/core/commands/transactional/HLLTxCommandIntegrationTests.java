package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HLLCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class HLLTxCommandIntegrationTests extends HLLCommandIntegrationTests {

    @Inject
    HLLTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
