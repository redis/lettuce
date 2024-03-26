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
