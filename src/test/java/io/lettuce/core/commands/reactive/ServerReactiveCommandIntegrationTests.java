package io.lettuce.core.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.commands.ServerCommandIntegrationTests;
import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.models.command.CommandDetailParser;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * Integration tests for {@link io.lettuce.core.api.reactive.RedisServerReactiveCommands}.
 *
 * @author Mark Paluch
 */
class ServerReactiveCommandIntegrationTests extends ServerCommandIntegrationTests {

    private RedisReactiveCommands<String, String> reactive;

    @Inject
    ServerReactiveCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
    }

    /**
     * Luckily these commands do not destroy anything in contrast to sync/async.
     */
    @Test
    void shutdown() {
        reactive.shutdown(true);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    void debugOom() {
        reactive.debugOom();
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    void debugSegfault() {
        reactive.debugSegfault();
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    void debugRestart() {
        reactive.debugRestart(1L);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    void migrate() {
        reactive.migrate("host", 1234, "key", 1, 10);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    @Override
    public void commandInfo() {

        List<Object> result = reactive.commandInfo(CommandType.GETRANGE, CommandType.SET).collectList().block();

        assertThat(result).hasSize(2);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);
    }

}
