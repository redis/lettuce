/*
 * Copyright 2011-2019 the original author or authors.
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

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;
import java.util.List;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * A list output that creates a list with either double/long or {@link GeoCoordinates}'s.
 *
 * @author Mark Paluch
 */
public class GeoWithinListOutput<K, V> extends CommandOutput<K, V, List<GeoWithin<V>>> implements StreamingOutput<GeoWithin<V>> {

    private V member;
    private Double distance;
    private Long geohash;
    private GeoCoordinates coordinates;

    private Double x;

    private boolean withDistance;
    private boolean withHash;
    private boolean withCoordinates;
    private Subscriber<GeoWithin<V>> subscriber;

    public GeoWithinListOutput(RedisCodec<K, V> codec, boolean withDistance, boolean withHash, boolean withCoordinates) {
        super(codec, OutputFactory.newList(16));
        this.withDistance = withDistance;
        this.withHash = withHash;
        this.withCoordinates = withCoordinates;
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {

        if (member == null) {
            member = (V) (Long) integer;
            return;
        }

        if (withHash) {
            geohash = integer;
        }
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (member == null) {
            member = codec.decodeValue(bytes);
            return;
        }

        double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));
        set(value);
    }

    @Override
    public void set(double number) {

        if (withDistance) {
            if (distance == null) {
                distance = number;
                return;
            }
        }

        if (withCoordinates) {
            if (x == null) {
                x = number;
                return;
            }

            coordinates = new GeoCoordinates(x, number);
        }
    }

    @Override
    public void complete(int depth) {

        if (depth == 1) {
            subscriber.onNext(output, new GeoWithin<V>(member, distance, geohash, coordinates));

            member = null;
            distance = null;
            geohash = null;
            coordinates = null;
            x = null;
        }
    }

    @Override
    public void setSubscriber(Subscriber<GeoWithin<V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<GeoWithin<V>> getSubscriber() {
        return subscriber;
    }
}
