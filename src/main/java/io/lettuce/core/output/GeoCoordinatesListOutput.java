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

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * A list output that creates a list with {@link GeoCoordinates}'s.
 *
 * @author Mark Paluch
 */
public class GeoCoordinatesListOutput<K, V> extends CommandOutput<K, V, List<GeoCoordinates>>
        implements StreamingOutput<GeoCoordinates> {

    private Double x;

    private boolean initialized;

    private Subscriber<GeoCoordinates> subscriber;

    public GeoCoordinatesListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        Double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));

        if (x == null) {
            x = value;
            return;
        }

        subscriber.onNext(output, new GeoCoordinates(x, value));
        x = null;
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }

        if (count == -1) {
            subscriber.onNext(output, null);
        }
    }

    @Override
    public void setSubscriber(Subscriber<GeoCoordinates> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<GeoCoordinates> getSubscriber() {
        return subscriber;
    }

}
