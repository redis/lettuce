/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
