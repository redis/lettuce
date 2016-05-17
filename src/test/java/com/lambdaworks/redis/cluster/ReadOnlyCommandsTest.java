package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ReadOnlyCommandsTest {

    @Test
    public void testCount() throws Exception {
        assertThat(ReadOnlyCommands.READ_ONLY_COMMANDS).hasSize(69);
    }

    @Test
    public void testResolvableCommandNames() throws Exception {
        for (ProtocolKeyword readOnlyCommand : ReadOnlyCommands.READ_ONLY_COMMANDS) {
            assertThat(new String(readOnlyCommand.getBytes())).isEqualTo(CommandType.valueOf(new String(readOnlyCommand.getBytes())).name());
        }
    }
}
