package com.lambdaworks.redis.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
public class GeoCoordinatesValueListOutputTest {

	private GeoCoordinatesValueListOutput<?, ?> sut = new GeoCoordinatesValueListOutput<>(new Utf8StringCodec());

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

		sut.set(ByteBuffer.wrap("1.234".getBytes()));
		sut.set(ByteBuffer.wrap("4.567".getBytes()));
		sut.multi(-1);

		assertThat(sut.get()).contains(Value.just(new GeoCoordinates(1.234, 4.567)));
	}
}