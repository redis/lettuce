package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link MapOutput}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class MapOutputUnitTests {

    @Test
    void shouldAcceptValue() {

        MapOutput<String, String> sut = new MapOutput<>(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(ByteBuffer.wrap("world".getBytes()));

        assertThat(sut.get()).containsEntry("hello", "world");
    }

    @Test
    void shouldAcceptBoolean() {

        MapOutput<String, Object> sut = new MapOutput(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(true);

        assertThat(sut.get()).containsEntry("hello", true);
    }

    @Test
    void shouldAcceptDouble() {

        MapOutput<String, Object> sut = new MapOutput(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(1.2);

        assertThat(sut.get()).containsEntry("hello", 1.2);
    }

    @Test
    void shouldAcceptInteger() {

        MapOutput<String, Object> sut = new MapOutput(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(1L);

        assertThat(sut.get()).containsEntry("hello", 1L);
    }

}
