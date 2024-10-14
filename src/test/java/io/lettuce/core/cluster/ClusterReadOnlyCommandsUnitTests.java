package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Tests for {@link ClusterReadOnlyCommands}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class ClusterReadOnlyCommandsUnitTests {

    @Test
    void testCount() {
        assertThat(ClusterReadOnlyCommands.getReadOnlyCommands()).hasSize(84);
    }

    @Test
    void testResolvableCommandNames() {

        for (ProtocolKeyword readOnlyCommand : ClusterReadOnlyCommands.getReadOnlyCommands()) {
            assertThat(readOnlyCommand.toString()).isEqualTo(CommandType.valueOf(readOnlyCommand.toString()).name());
        }
    }

}
