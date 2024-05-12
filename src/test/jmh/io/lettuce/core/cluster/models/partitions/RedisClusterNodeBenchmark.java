package io.lettuce.core.cluster.models.partitions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import io.lettuce.core.cluster.SlotHash;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class RedisClusterNodeBenchmark {

    private static final List<Integer> ALL_SLOTS = IntStream.range(0, SlotHash.SLOT_COUNT).boxed().collect(Collectors.toList());
    private static final List<Integer> LOWER_SLOTS = IntStream.range(0, 8192).boxed().collect(Collectors.toList());

    private static final RedisClusterNode NODE = new RedisClusterNode(null, null, true, null, 0, 0, 0, ALL_SLOTS,
            Collections.emptySet());

    @Benchmark
    public RedisClusterNode createClusterNodeAllSlots() {
        return new RedisClusterNode(null, null, true, null, 0, 0, 0, ALL_SLOTS, Collections.emptySet());
    }

    @Benchmark
    public RedisClusterNode createClusterNodeLowerSlots() {
        return new RedisClusterNode(null, null, true, null, 0, 0, 0, LOWER_SLOTS, Collections.emptySet());
    }

    @Benchmark
    public void querySlotStatusPresent() {
        NODE.hasSlot(1234);
    }

    @Benchmark
    public void querySlotStatusAbsent() {
        NODE.hasSlot(8193);
    }
}
