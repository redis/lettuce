/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis;

import java.util.Map;

/**
 * A stream message and its id.
 *
 * @author Mark Paluch
 * @since 4.5
 */
public class StreamMessage<K, V> {

    private final K stream;
    private final String id;
    private final Map<K, V> body;

    /**
     * Create a new {@link StreamMessage}.
     *
     * @param stream the stream.
     * @param id the message id.
     * @param body map containing the message body.
     */
    public StreamMessage(K stream, String id, Map<K, V> body) {

        this.stream = stream;
        this.id = id;
        this.body = body;
    }

    public K getStream() {
        return stream;
    }

    public String getId() {
        return id;
    }

    public Map<K, V> getBody() {
        return body;
    }

    @Override
    public String toString() {
        return String.format("StreamMessage[%s:%s]%s", stream, id, body);
    }
}
