package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Unit tests for {@link RoundRobin}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class RoundRobinUnitTests {

    @Test
    void shouldDetermineSimpleConsistency() {

        RedisClusterNode node1 = new RedisClusterNode(RedisURI.create("127.0.0.1", 1), "1", true, "", 0, 0, 0,
                new ArrayList<>(), new HashSet<>());
        RedisClusterNode node2 = new RedisClusterNode(RedisURI.create("127.0.0.0", 1), "2", true, "", 0, 0, 0,
                new ArrayList<>(), new HashSet<>());

        RedisClusterNode newNode1 = new RedisClusterNode(RedisURI.create("127.0.0.0", 1), "1", true, "", 0, 0, 0,
                new ArrayList<>(), new HashSet<>());

        RoundRobin<RedisClusterNode> roundRobin = new RoundRobin<>();
        roundRobin.rebuild(Arrays.asList(node1, node2));

        assertThat(roundRobin.isConsistent(Arrays.asList(node1, node2))).isTrue();

        // RedisClusterNode compares by Id only.
        assertThat(roundRobin.isConsistent(Arrays.asList(newNode1, node2))).isTrue();
        assertThat(roundRobin.isConsistent(Arrays.asList(RedisClusterNode.of("1"), node2))).isTrue();
    }

    @Test
    void shouldDetermineConsistencyWithEqualityCheck() {

        RedisClusterNode node1 = new RedisClusterNode(RedisURI.create("127.0.0.1", 1), "1", true, "", 0, 0, 0,
                new ArrayList<>(), new HashSet<>());
        RedisClusterNode node2 = new RedisClusterNode(RedisURI.create("127.0.0.0", 1), "2", true, "", 0, 0, 0,
                new ArrayList<>(), new HashSet<>());
        RedisClusterNode newNode1 = new RedisClusterNode(RedisURI.create("127.0.0.0", 1), "1", true, "", 0, 0, 0,
                new ArrayList<>(), new HashSet<>());

        RoundRobin<RedisClusterNode> roundRobin = new RoundRobin<>((l, r) -> l.getUri().equals(r.getUri()));
        roundRobin.rebuild(Arrays.asList(node1, node2));

        assertThat(roundRobin.isConsistent(Arrays.asList(node1, node2))).isTrue();

        // RedisClusterNode compares by Id only.
        assertThat(roundRobin.isConsistent(Arrays.asList(newNode1, node2))).isFalse();
        assertThat(roundRobin.isConsistent(Collections.singletonList(newNode1))).isFalse();
        assertThat(roundRobin.isConsistent(Collections.singletonList(node2))).isFalse();
        assertThat(roundRobin.isConsistent(Arrays.asList(RedisClusterNode.of("1"), node2))).isFalse();
    }

    @Test
    void shouldDetermineConsistencyWithEqualityCheckOppositeCheck() {

        RedisClusterNode node1 = RedisClusterNode.of("1");
        RedisClusterNode node2 = RedisClusterNode.of("2");
        RedisClusterNode newNode1 = new RedisClusterNode(RedisURI.create("127.0.0.0", 1), "1", true, "", 0, 0, 0,
                new ArrayList<>(), new HashSet<>());

        RoundRobin<RedisClusterNode> roundRobin = new RoundRobin<>(
                (l, r) -> l.getUri() == r.getUri() || (l.getUri() != null && l.getUri().equals(r.getUri())));
        roundRobin.rebuild(Arrays.asList(node1, node2));

        assertThat(roundRobin.isConsistent(Arrays.asList(node1, node2))).isTrue();

        // RedisClusterNode compares by Id only.
        assertThat(roundRobin.isConsistent(Arrays.asList(newNode1, node2))).isFalse();
        assertThat(roundRobin.isConsistent(Collections.singletonList(newNode1))).isFalse();
        assertThat(roundRobin.isConsistent(Collections.singletonList(node2))).isFalse();
    }

}
