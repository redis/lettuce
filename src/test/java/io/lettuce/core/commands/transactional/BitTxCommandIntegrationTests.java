package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.BitCommandIntegrationTests;

/**
 * @author Mark Paluch
 */
class BitTxCommandIntegrationTests extends BitCommandIntegrationTests {

    @Inject
    BitTxCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, TxSyncInvocationHandler.sync(connection));
    }

}
