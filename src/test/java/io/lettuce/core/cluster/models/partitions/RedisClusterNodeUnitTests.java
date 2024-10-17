package io.lettuce.core.cluster.models.partitions;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.SlotHash;

/**
 * Unit tests for {@link RedisClusterNode}.
 *
 * @author Mark Paluch
 */
class RedisClusterNodeUnitTests {

    @Test
    void shouldCreateNodeWithEmptySlots() {

        BitSet slots = new BitSet();
        RedisClusterNode node = new RedisClusterNode(RedisURI.create("localhost", 6379), "1", true, null, 0, 0, 0, slots,
                Collections.emptySet());

        assertThat(node.getSlots()).isEmpty();
        assertThat(node.getSlots()).isNotNull();
    }

    @Test
    void shouldCreateNodeWithNonEmptySlots() {

        BitSet slots = new BitSet();
        slots.set(1);
        slots.set(2);
        RedisClusterNode node = new RedisClusterNode(RedisURI.create("localhost", 6379), "1", true, null, 0, 0, 0, slots,
                Collections.emptySet());

        assertThat(node.getSlots()).containsExactly(1, 2);
    }

    @Test
    void shouldCopyNodeWithEmptySlots() {

        BitSet slots = new BitSet();
        RedisClusterNode originalNode = new RedisClusterNode(RedisURI.create("localhost", 6379), "1", true, null, 0, 0, 0,
                slots, Collections.emptySet());

        RedisClusterNode copiedNode = new RedisClusterNode(originalNode);

        assertThat(copiedNode.getSlots()).isEmpty();
        assertThat(copiedNode.getSlots()).isNotNull();
    }

    @Test
    void shouldCopyNodeWithNonEmptySlots() {

        BitSet slots = new BitSet();
        slots.set(1);
        slots.set(2);
        RedisClusterNode originalNode = new RedisClusterNode(RedisURI.create("localhost", 6379), "1", true, null, 0, 0, 0,
                slots, Collections.emptySet());

        RedisClusterNode copiedNode = new RedisClusterNode(originalNode);

        assertThat(copiedNode.getSlots()).containsExactly(1, 2);
    }

    @Test
    public void testHasSameSlotsAs() {

        BitSet emptySlots = new BitSet(SlotHash.SLOT_COUNT);
        emptySlots.set(1);
        emptySlots.set(2);

        RedisClusterNode node1 = new RedisClusterNode(RedisURI.create("localhost", 6379), "nodeId1", true, "slaveOf", 0L, 0L,
                0L, emptySlots, new HashSet<>());

        RedisClusterNode node2 = new RedisClusterNode(node1);

        assertThat(node1.hasSameSlotsAs(node2)).isTrue();
    }

    @Test
    public void testHasDifferentSlotsAs() {

        BitSet slots1 = new BitSet(SlotHash.SLOT_COUNT);
        slots1.set(1);

        BitSet slots2 = new BitSet(SlotHash.SLOT_COUNT);
        slots2.set(2);

        RedisClusterNode node1 = new RedisClusterNode(RedisURI.create("localhost", 6379), "nodeId1", true, "slaveOf", 0L, 0L,
                0L, slots1, new HashSet<>());
        RedisClusterNode node2 = new RedisClusterNode(RedisURI.create("localhost", 6379), "nodeId2", true, "slaveOf", 0L, 0L,
                0L, slots2, new HashSet<>());

        assertThat(node1.hasSameSlotsAs(node2)).isFalse();
    }

    @Test
    void shouldCopyNode() {

        RedisClusterNode node = new RedisClusterNode();
        node.setSlots(Arrays.asList(1, 2, 3, SlotHash.SLOT_COUNT - 1));
        node.addAlias(RedisURI.create("foo", 6379));

        RedisClusterNode copy = new RedisClusterNode(node);

        assertThat(copy.getSlots()).containsExactly(1, 2, 3, SlotHash.SLOT_COUNT - 1);
        assertThat(copy.hasSlot(1)).isTrue();
        assertThat(copy.hasSlot(SlotHash.SLOT_COUNT - 1)).isTrue();
        assertThat(copy.getAliases()).contains(RedisURI.create("foo", 6379));
    }

    @Test // considers nodeId only
    void testEquality() {

        RedisClusterNode node = RedisClusterNode.of("1");

        assertThat(node).isEqualTo(RedisClusterNode.of("1"));
        assertThat(node).hasSameHashCodeAs(RedisClusterNode.of("1"));

        node.setUri(RedisURI.create("127.0.0.1", 1));
        assertThat(node).hasSameHashCodeAs(RedisClusterNode.of("1"));
        assertThat(node).isEqualTo(RedisClusterNode.of("1"));

        assertThat(node).doesNotHaveSameHashCodeAs(RedisClusterNode.of("2"));
        assertThat(node).isNotEqualTo(RedisClusterNode.of("2"));
    }

    @Test
    void testToString() {

        RedisClusterNode node = new RedisClusterNode();

        assertThat(node.toString()).contains(RedisClusterNode.class.getSimpleName());
    }

    @Test
    void shouldReturnTrueWhenSlotsAreNull() {

        BitSet emptySlots = null;
        RedisClusterNode node = new RedisClusterNode(RedisURI.create("localhost", 6379), "1", true, null, 0, 0, 0, emptySlots,
                Collections.emptySet());

        assertThat(node.hasNoSlots()).isTrue();
    }

    @Test
    void shouldReturnTrueWhenSlotsAreEmpty() {

        BitSet emptySlots = new BitSet(); // Empty BitSet
        RedisClusterNode node = new RedisClusterNode(RedisURI.create("localhost", 6379), "1", true, null, 0, 0, 0, emptySlots,
                Collections.emptySet());

        assertThat(node.hasNoSlots()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSlotsAreAssigned() {

        BitSet slots = new BitSet();
        slots.set(1); // Assign a slot
        RedisClusterNode node = new RedisClusterNode(RedisURI.create("localhost", 6379), "1", true, null, 0, 0, 0, slots,
                Collections.emptySet());

        assertThat(node.hasNoSlots()).isFalse();
    }

}
