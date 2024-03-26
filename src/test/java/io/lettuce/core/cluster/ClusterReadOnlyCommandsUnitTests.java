package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Tests for {@link ClusterReadOnlyCommands}.
 *
 * @author Mark Paluch
 */
class ClusterReadOnlyCommandsUnitTests {

    @Test
    void testCount() {
        assertThat(ClusterReadOnlyCommands.getReadOnlyCommands()).hasSize(85);
    }

    @Test
    void testResolvableCommandNames() {

        for (ProtocolKeyword readOnlyCommand : ClusterReadOnlyCommands.getReadOnlyCommands()) {
            assertThat(readOnlyCommand.name()).isEqualTo(CommandType.valueOf(readOnlyCommand.name()).name());
        }
    }
}
