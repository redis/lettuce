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
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.Range;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.stream.PendingMessages;

/**
 * Decodes {@link PendingMessages}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
public class PendingMessagesOutput<K, V> extends CommandOutput<K, V, PendingMessages> {

    private Long count;

    private String messageIdsFrom;

    private String messageIdsTo;

    private String consumer;

    private final Map<String, Long> consumerMessageCount = new LinkedHashMap<>();

    public PendingMessagesOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (messageIdsFrom == null) {
            messageIdsFrom = decodeAscii(bytes);
            return;
        }

        if (messageIdsTo == null) {
            messageIdsTo = decodeAscii(bytes);
            return;
        }

        if (consumer == null) {
            consumer = StringCodec.UTF8.decodeKey(bytes);
            return;
        }

        set(Long.parseLong(decodeAscii(bytes)));
    }

    @Override
    public void set(long integer) {

        if (count == null) {
            count = integer;
            return;
        }

        if (consumer != null) {
            consumerMessageCount.put(consumer, integer);
            consumer = null;
        }
    }

    @Override
    public void complete(int depth) {

        if (depth == 0) {

            Range<String> range = messageIdsFrom != null && messageIdsTo != null ? Range.create(messageIdsFrom, messageIdsTo)
                    : Range.unbounded();
            output = new PendingMessages(count == null ? 0 : count, range, consumerMessageCount);
        }
    }

}
