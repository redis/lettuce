/*
 * Copyright 2020 the original author or authors.
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

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
}
