package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.KeyCommandIntegrationTests;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
public class KeyTxCommandIntegrationTests extends KeyCommandIntegrationTests {

    @Inject
    KeyTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

    @Disabled
    @Override
    public void move() {
    }

}
