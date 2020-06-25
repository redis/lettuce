/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
class GeoWithinListOutputUnitTests {

    private GeoWithinListOutput<String, String> sut = new GeoWithinListOutput<>(StringCodec.UTF8, false, false, false);

    @Test
    void defaultSubscriberIsSet() {

        sut.multi(1);
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void commandOutputKeyOnlyDecoded() {

        sut.multi(1);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", null, null, null));
    }

    @Test
    void commandOutputKeyAndDistanceDecoded() {

        sut = new GeoWithinListOutput<>(StringCodec.UTF8, true, false, false);

        sut.multi(1);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", 4.567, null, null));
    }

    @Test
    void commandOutputKeyAndHashDecoded() {

        sut = new GeoWithinListOutput<>(StringCodec.UTF8, false, true, false);

        sut.multi(1);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(4567);
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", null, 4567L, null));
    }

    @Test
    void commandOutputLongKeyAndHashDecoded() {

        GeoWithinListOutput<Long, Long> sut = new GeoWithinListOutput<>((RedisCodec) StringCodec.UTF8, false, true, false);

        sut.multi(1);
        sut.set(1234);
        sut.set(4567);
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>(1234L, null, 4567L, null));
    }

    @Test
    void commandOutputKeyAndCoordinatesDecoded() {

        sut = new GeoWithinListOutput<>(StringCodec.UTF8, false, false, true);

        sut.multi(1);
        sut.set(ByteBuffer.wrap("key".getBytes()));
        sut.set(ByteBuffer.wrap("1.234".getBytes()));
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
        sut.complete(1);

        assertThat(sut.get()).contains(new GeoWithin<>("key", null, null, new GeoCoordinates(1.234, 4.567)));
    }

}
