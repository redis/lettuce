package io.lettuce.core.commands.transactional;

import javax.inject.Inject;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.ZMoveMemberArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.SortedSetCommandIntegrationTests;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class SortedSetTxCommandIntegrationTests extends SortedSetCommandIntegrationTests {

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    SortedSetTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
        this.connection = connection;
    }

    /**
     * An empty member list reaches the server inside a transaction. Without a condition the server rejects the command when it
     * is queued, so the transaction is discarded; with NX the number-of-members error is the command's result. Nothing moves.
     */
    @Test
    @EnabledOnCommand("ZMOVEMEMBER")
    @Override
    public void zmovememberEmptyMemberListIsRejectedByServer() {
        RedisCommands<String, String> sync = connection.sync();
        fixtureF1();

        assertThatThrownBy(() -> zmovememberInTransaction(sync, null)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("EXECABORT");
        assertThatThrownBy(() -> zmovememberInTransaction(sync, ZMoveMemberArgs.Builder.nx()))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("ERR Number of members must be a positive integer");
        assertThat(sync.zcard(SRC)).isEqualTo(3);
        assertThat(sync.exists(DST)).isEqualTo(0);
    }

    private void zmovememberInTransaction(RedisCommands<String, String> sync, ZMoveMemberArgs args) throws Exception {
        sync.multi();
        try {
            if (args == null) {
                sync.zmovemember(SRC, DST, new String[0]);
            } else {
                sync.zmovemember(SRC, DST, args, new String[0]);
            }
            Object result = sync.exec().get(0);
            if (result instanceof Exception) {
                throw (Exception) result;
            }
        } finally {
            if (connection.isMulti()) {
                sync.discard();
            }
        }
    }

}
