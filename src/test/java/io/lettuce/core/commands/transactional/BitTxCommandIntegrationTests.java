package io.lettuce.core.commands.transactional;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.BitFieldArgs.unsigned;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.BitCommandIntegrationTests;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class BitTxCommandIntegrationTests extends BitCommandIntegrationTests {

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    BitTxCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, TxSyncInvocationHandler.sync(connection));
        this.connection = connection;
    }

    @Disabled
    @Override
    public void bitrangeintarrayUnknownCommand() {
    }

    /**
     * Scenario BRIA-E04: inside {@code MULTI}/{@code EXEC} the reply is one transaction entry holding the bit offsets.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayInTransaction() {

        RedisCommands<String, String> sync = connection.sync();
        sync.bitfield("bria:f1", BitFieldArgs.Builder.set(unsigned(32), 0, 2172748161L));

        sync.multi();
        sync.bitrangeintarray("bria:f1", 1, 1);
        TransactionResult result = sync.exec();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(1);
        assertThat((Object) result.get(0)).isEqualTo(Arrays.asList(8L, 15L));
    }

}
