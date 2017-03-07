/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
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
