package io.lettuce.core.commands;

import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterReadOnlyCommands;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.ReadOnlyCommands;
import io.lettuce.test.KeyValueStreamingAdapter;
import io.lettuce.test.LettuceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thach Le
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadOnlyCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected ReadOnlyCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        redis.flushall();
    }

    @Test
    void testReadOnlyCommands() {
        for (ProtocolKeyword readOnlyCommand : ClusterReadOnlyCommands.getReadOnlyCommands()) {
            assertThat(isCommandReadOnly(readOnlyCommand.name())).isTrue();
        }
    }

    private boolean isCommandReadOnly(String commandName) {
        List<Object> commandInfo = redis.commandInfo(commandName);
        if (commandInfo == null || commandInfo.isEmpty()) {
            throw new IllegalArgumentException("Command not found: " + commandName);
        }

        List<Object> flags = (List<Object>) commandInfo.get(2); // Index 2 is the flags list
        return flags.contains("readonly") && !flags.contains("write");
    }
}
