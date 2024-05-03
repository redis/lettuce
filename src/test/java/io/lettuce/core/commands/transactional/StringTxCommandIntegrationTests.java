package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.StringCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class StringTxCommandIntegrationTests extends StringCommandIntegrationTests {

    @Inject
    StringTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
