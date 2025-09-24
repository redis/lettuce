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
        assertThat(ClusterReadOnlyCommands.getReadOnlyCommands()).hasSize(92);
    }

    @Test
    void testResolvableCommandNames() {
        // Use enum name() instead of toString() because complex commands (e.g. "FT.SEARCH")
        // have a different wire command string than the enum constant name ("FT_SEARCH").
        for (ProtocolKeyword readOnlyCommand : ClusterReadOnlyCommands.getReadOnlyCommands()) {
            CommandType ct = (CommandType) readOnlyCommand;
            assertThat(CommandType.valueOf(ct.name())).isEqualTo(ct);
        }
    }

}
