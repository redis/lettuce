package com.lambdaworks.redis.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.GeoWithin;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoWithinListOutputTest {

    private GeoWithinListOutput<String, String> sut = new GeoWithinListOutput<>(new Utf8StringCodec(), false, false, false);

    @Test
    public void defaultSubscriberIsSet() throws Exception {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    public void commandOutputKeyOnlyDecoded() throws Exception {

        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", null, null, null));
    }

    @Test
    public void commandOutputKeyAndDistanceDecoded() throws Exception {

        sut = new GeoWithinListOutput<>(new Utf8StringCodec(), true, false, false);

        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", 4.567, null, null));
    }

    @Test
    public void commandOutputKeyAndHashDecoded() throws Exception {

        sut = new GeoWithinListOutput<>(new Utf8StringCodec(), false, true, false);

        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(4567);
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", null, 4567L, null));
    }

    @Test
    public void commandOutputLongKeyAndHashDecoded() throws Exception {

        GeoWithinListOutput<Long, Long> sut = new GeoWithinListOutput<>((RedisCodec) new Utf8StringCodec(), false, true, false);

        sut.set(1234);
        sut.set(4567);
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>(1234L, null, 4567L, null));
    }

    @Test
    public void commandOutputKeyAndCoordinatesDecoded() throws Exception {

        sut = new GeoWithinListOutput<>(new Utf8StringCodec(), false, false, true);

        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("1.234".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", null, null, new GeoCoordinates(1.234, 4.567)));
    }
}