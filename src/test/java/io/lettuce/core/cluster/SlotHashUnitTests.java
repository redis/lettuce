package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 * @since 3.0
 */
@Tag(UNIT_TEST)
class SlotHashUnitTests {

    private static final byte[] BYTES = "123456789".getBytes();

    private static final byte[] TAGGED = "key{123456789}a".getBytes();

    @Test
    void shouldGetSlotHeap() {

        int result = SlotHash.getSlot(BYTES);
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    void shouldGetTaggedSlotHeap() {

        int result = SlotHash.getSlot(TAGGED);
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    void shouldGetSlotDirect() {

        int result = SlotHash.getSlot((ByteBuffer) ByteBuffer.allocateDirect(BYTES.length).put(BYTES).flip());
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    void testHashWithHash() {

        int result = SlotHash.getSlot((ByteBuffer) ByteBuffer.allocateDirect(TAGGED.length).put(TAGGED).flip());
        assertThat(result).isEqualTo(0x31C3);
    }

}
