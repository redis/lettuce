package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.models.command.CommandDetailParser;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
public class CommandSetIntegrationTests {

    private final RedisCommands<String, String> redis;

    @Inject
    CommandSetIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.redis = connection.sync();
    }

    @Test
    void shouldDiscoverCommands() {

        List<CommandDetail> commandDetails = CommandDetailParser.parse(redis.command());
        CommandSet state = new CommandSet(commandDetails);

        assertThat(state.hasCommand(CommandType.GEOADD)).isTrue();
        assertThat(state.hasCommand(UnknownCommand.FOO)).isFalse();
    }

    enum UnknownCommand implements ProtocolKeyword {

        FOO;

        @Override
        public byte[] getBytes() {
            return name().getBytes();
        }

    }

}
