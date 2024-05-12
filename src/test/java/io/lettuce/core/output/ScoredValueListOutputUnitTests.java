package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.StringCodec;

/**
 * @author Mark Paluch
 */
class ScoredValueListOutputUnitTests {

    private ScoredValueListOutput<String, String> sut = new ScoredValueListOutput<>(StringCodec.UTF8);

    @Test
    void defaultSubscriberIsSet() {

        sut.multi(1);
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void setIntegerShouldFail() {
        assertThatThrownBy(() -> sut.set(123L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void commandOutputCorrectlyDecoded() {

        sut.multi(1);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.multi(-1);

        assertThat(sut.get()).contains(ScoredValue.just(4.567, "key"));
    }

}
