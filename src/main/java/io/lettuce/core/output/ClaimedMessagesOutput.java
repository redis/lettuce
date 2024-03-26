/*
 * Copyright 2021-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.models.stream.ClaimedMessages;

/**
 * Decodes {@link ClaimedMessages}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author dengliming
 * @since 6.1
 */
public class ClaimedMessagesOutput<K, V> extends CommandOutput<K, V, ClaimedMessages<K, V>> {

    private final boolean justId;

    private final K stream;

    private String startId;

    private String id;

    private boolean hasId;

    private K key;

    private boolean hasKey;

    private Map<K, V> body;

    private boolean bodyReceived;

    private final List<StreamMessage<K, V>> messages;

    public ClaimedMessagesOutput(RedisCodec<K, V> codec, K stream, boolean justId) {
        super(codec, null);
        this.stream = stream;
        this.messages = new ArrayList<>();
        this.justId = justId;
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (startId == null) {
            startId = decodeAscii(bytes);
            return;
        }

        if (id == null) {
            id = decodeAscii(bytes);
            return;
        }

        if (justId) {
            return;
        }

        if (!hasKey) {
            bodyReceived = true;
            hasKey = true;

            if (bytes == null) {
                return;
            }

            key = codec.decodeKey(bytes);
            return;
        }

        if (body == null) {
            body = new LinkedHashMap<>();
        }

        body.put(key, bytes == null ? null : codec.decodeValue(bytes));
        key = null;
        hasKey = false;
    }

    @Override
    public void complete(int depth) {

        if (depth == 3 && bodyReceived) {
            messages.add(new StreamMessage<>(stream, id, body == null ? Collections.emptyMap() : body));
            bodyReceived = false;
            key = null;
            hasKey = false;
            body = null;
            id = null;
            hasId = false;
        }

        if (depth == 2 && justId) {
            messages.add(new StreamMessage<>(stream, id, null));
            key = null;
            hasKey = false;
            body = null;
            id = null;
            hasId = false;
        }

        if (depth == 0) {
            output = new ClaimedMessages<>(startId, Collections.unmodifiableList(messages));
        }
    }

}
