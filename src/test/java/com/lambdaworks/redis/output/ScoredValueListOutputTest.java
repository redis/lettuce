package com.lambdaworks.redis.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
public class ScoredValueListOutputTest {

    private ScoredValueListOutput<String, String> sut = new ScoredValueListOutput<>(new Utf8StringCodec());

    @Test
    public void defaultSubscriberIsSet() throws Exception {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test(expected = IllegalStateException.class)
    public void setIntegerShouldFail() throws Exception {
        sut.set(123L);
    }

    @Test
    public void commandOutputCorrectlyDecoded() throws Exception {

        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.multi(-1);

        assertThat(sut.get()).contains(ScoredValue.fromNullable(4.567, "key"));
    }
}