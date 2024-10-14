package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.codec.StringCodec;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class GeoCoordinatesListOutputUnitTests {

    private GeoCoordinatesListOutput<?, ?> sut = new GeoCoordinatesListOutput<>(StringCodec.UTF8);

    @Test
    void setIntegerShouldFail() {
        assertThatThrownBy(() -> sut.set(123L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void commandOutputCorrectlyDecoded() {

        sut.multi(2);
        sut.set(ByteBuffer.wrap("1.234".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.multi(-1);

        assertThat(sut.get()).contains(new GeoCoordinates(1.234, 4.567));
    }

}
