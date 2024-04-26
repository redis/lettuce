package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * @author Mark Paluch
 */
class BooleanListOutputUnitTests {

    private BooleanListOutput<?, ?> sut = new BooleanListOutput<>(StringCodec.UTF8);

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void commandOutputCorrectlyDecoded() {

        sut.multi(3);
        sut.set(1L);
        sut.set(0L);
        sut.set(2L);

        assertThat(sut.get()).contains(true, false, false);
    }

    @Test
    void setByteNotImplemented() {
        assertThatThrownBy(() -> sut.set(ByteBuffer.wrap("4.567".getBytes())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
