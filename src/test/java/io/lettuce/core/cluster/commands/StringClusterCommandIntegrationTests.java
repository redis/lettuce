package io.lettuce.core.cluster.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.BitRangeIntArrayArgs;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.commands.StringCommandIntegrationTests;
import io.lettuce.test.KeyValueStreamingAdapter;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisStringCommands} using Redis Cluster.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class StringClusterCommandIntegrationTests extends StringCommandIntegrationTests {

    private final RedisClusterCommands<String, String> redis;

    @Inject
    StringClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
        this.redis = connection.sync();
    }

    @Test
    void msetnx() {
        redis.set("one", "1");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(redis.msetnx(map)).isFalse();
        redis.del("one");
        redis.del("two"); // probably set on a different node
        assertThat(redis.msetnx(map)).isTrue();
        assertThat(redis.get("two")).isEqualTo("2");
    }

    @Test
    void mgetStreaming() {
        setupMget();

        KeyValueStreamingAdapter<String, String> streamingAdapter = new KeyValueStreamingAdapter<>();
        Long count = redis.mget(streamingAdapter, "one", "two");

        assertThat(streamingAdapter.getMap()).containsEntry("one", "1").containsEntry("two", "2");

        assertThat(count.intValue()).isEqualTo(2);
    }

    /**
     * Scenario E05: in a cluster the command is routed by its key and returns the set bit offsets, and an empty list with a
     * limit of 0.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarray() {
        String key = "{bria}:f1";
        redis.bitfield(key, BitFieldArgs.Builder.set(BitFieldArgs.unsigned(32), 0, 2172748161L));

        assertThat(redis.bitrangeintarray(key, 0, -1)).containsExactly(0L, 7L, 8L, 15L, 16L, 23L, 24L, 31L);
        assertThat(redis.bitrangeintarray(key, 0, -1, BitRangeIntArrayArgs.Builder.limit(0))).isNotNull().isEmpty();
    }

}
