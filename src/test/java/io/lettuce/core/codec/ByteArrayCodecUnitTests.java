package io.lettuce.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

class ByteArrayCodecUnitTests {

    @Test
    void testDecodeValue_withNonEmptyByteBuffer() {
        final ByteArrayCodec byteArrayCodec = ByteArrayCodec.INSTANCE;
        final byte[] expectedBytes = { 1, 2, 3, 4, 5 };
        final ByteBuffer buffer = ByteBuffer.wrap(expectedBytes);
        final byte[] result = byteArrayCodec.decodeValue(buffer);
        assertThat(result).isEqualTo(expectedBytes);
    }

    @Test
    void testDecodeValue_withEmptyByteBuffer() {
        final ByteArrayCodec byteArrayCodec = ByteArrayCodec.INSTANCE;
        final ByteBuffer buffer = ByteBuffer.allocate(0);
        final byte[] result = byteArrayCodec.decodeValue(buffer);
        assertThat(result).isEmpty();
    }

    @Test
    void testDecodeValue_withNullByteBuffer() {
        final ByteArrayCodec byteArrayCodec = ByteArrayCodec.INSTANCE;
        final byte[] result = byteArrayCodec.decodeValue(null);
        assertThat(result).isEmpty();
    }

}
