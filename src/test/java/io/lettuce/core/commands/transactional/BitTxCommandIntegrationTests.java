package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.BitCommandIntegrationTests;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class BitTxCommandIntegrationTests extends BitCommandIntegrationTests {

    @Inject
    BitTxCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, TxSyncInvocationHandler.sync(connection));
    }

}
