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
 * @author Mingi Lee
 */
@Tag(UNIT_TEST)
class ClusterReadOnlyCommandsUnitTests {

    @Test
    void testCount() {
        assertThat(ClusterReadOnlyCommands.getReadOnlyCommands()).hasSize(101);
    }

    @Test
    void testResolvableCommandNames() {

        for (ProtocolKeyword readOnlyCommand : ClusterReadOnlyCommands.getReadOnlyCommands()) {
            // Convert command string to enum name format (e.g., "JSON.GET" -> "JSON_GET")
            String enumName = readOnlyCommand.toString().replace('.', '_');
            enumName = enumName.replace("__", "_");
            assertThat(readOnlyCommand.toString()).isEqualTo(CommandType.valueOf(enumName).toString());
        }
    }

}
