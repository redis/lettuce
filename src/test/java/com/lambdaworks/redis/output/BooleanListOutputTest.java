package com.lambdaworks.redis.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
public class BooleanListOutputTest {

    private BooleanListOutput<?, ?> sut = new BooleanListOutput<>(new Utf8StringCodec());

    @Test
    public void defaultSubscriberIsSet() throws Exception {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    public void commandOutputCorrectlyDecoded() throws Exception {

		sut.set(1L);
        sut.set(0L);
        sut.set(2L);

        assertThat(sut.get()).contains(true, false, false);
    }

    @Test(expected = IllegalStateException.class)
    public void setByteNotImplemented() throws Exception {
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
    }
}