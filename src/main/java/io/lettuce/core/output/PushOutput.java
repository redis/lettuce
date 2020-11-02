/*
 * Copyright 2020 the original author or authors.
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

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Output for push notifications. The response output is always {@code List&lt;Object&gt;} as push notifications may contain
 * arbitrary values. The first response element which denotes the push message type is available through {@link #type()}.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public class PushOutput<K, V> extends NestedMultiOutput<K, V> implements PushMessage {

    private String type;

    public PushOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (type == null) {
            bytes.mark();
            type = StringCodec.ASCII.decodeKey(bytes);
            bytes.reset();
        }

        super.set(bytes);
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        if (type == null) {
            set(bytes);
        }
        super.setSingle(bytes);
    }

    public String type() {
        return type;
    }

    @Override
    public String getType() {
        return type();
    }

    @Override
    public List<Object> getContent() {

        List<Object> copy = new ArrayList<>();

        for (Object o : get()) {
            if (o instanceof ByteBuffer) {
                copy.add(((ByteBuffer) o).asReadOnlyBuffer());
            } else {
                copy.add(o);
            }
        }

        return Collections.unmodifiableList(copy);
    }

    @Override
    public List<Object> getContent(Function<ByteBuffer, Object> decodeFunction) {

        LettuceAssert.notNull(decodeFunction, "Decode function must not be null");

        List<Object> copy = new ArrayList<>();

        for (Object o : get()) {
            copy.add(decode(o, decodeFunction));
        }

        return Collections.unmodifiableList(copy);
    }

    private Object decode(Object toDecode, Function<ByteBuffer, Object> decodeFunction) {

        if (toDecode instanceof List) {

            List<Object> copy = new ArrayList<>(((List<?>) toDecode).size());

            for (Object o : (List<?>) toDecode) {
                copy.add(decode(o, decodeFunction));
            }

            return copy;
        }

        if (toDecode instanceof Set) {

            Set<Object> copy = new LinkedHashSet<>(((Set<?>) toDecode).size());

            for (Object o : (Set<?>) toDecode) {
                copy.add(decode(o, decodeFunction));
            }

            return copy;

        }

        if (toDecode instanceof Map) {

            Map<Object, Object> copy = new LinkedHashMap<>(((Map<?, ?>) toDecode).size());

            ((Map<?, ?>) toDecode).forEach((k, v) -> {
                copy.put(decode(k, decodeFunction), decode(v, decodeFunction));
            });

            return copy;
        }

        if (toDecode instanceof ByteBuffer) {

            ByteBuffer buffer = (ByteBuffer) toDecode;
            try {
                buffer.mark();
                return decodeFunction.apply(buffer);
            } finally {
                buffer.reset();
            }
        }

        return toDecode;
    }

}
