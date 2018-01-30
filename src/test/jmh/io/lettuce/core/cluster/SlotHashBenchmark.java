/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.cluster;

import java.nio.ByteBuffer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class SlotHashBenchmark {

    private static final byte[] data = "this is my buffer".getBytes();
    private static final byte[] tagged = "this is{my buffer}".getBytes();
    private static final ByteBuffer heap = (ByteBuffer) ByteBuffer.allocate(data.length).put(data).flip();
    private static final ByteBuffer direct = (ByteBuffer) ByteBuffer.allocateDirect(data.length).put(data).flip();

    private static final ByteBuffer heapTagged = (ByteBuffer) ByteBuffer.allocate(tagged.length).put(tagged).flip();
    private static final ByteBuffer directTagged = (ByteBuffer) ByteBuffer.allocateDirect(tagged.length).put(tagged).flip();

    @Benchmark
    public void measureSlotHashHeap(Blackhole blackhole) {
        blackhole.consume(SlotHash.getSlot(heap));
    }

    @Benchmark
    public void measureSlotHashDirect(Blackhole blackhole) {
        blackhole.consume(SlotHash.getSlot(direct));
    }

    @Benchmark
    public void measureSlotHashTaggedHeap(Blackhole blackhole) {
        blackhole.consume(SlotHash.getSlot(heapTagged));
    }

    @Benchmark
    public void measureSlotHashTaggedDirect(Blackhole blackhole) {
        blackhole.consume(SlotHash.getSlot(directTagged));
    }
}
