/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.core;

import java.util.Map;
import java.util.Objects;

/**
 * A stream message and its id.
 *
 * @author Mark Paluch
 * @since 5.1
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

    /**
     * @return the message body. Can be {@code null} for commands that do not return the message body.
     */
    public Map<K, V> getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof StreamMessage))
            return false;
        StreamMessage<?, ?> that = (StreamMessage<?, ?>) o;
        return Objects.equals(stream, that.stream) && Objects.equals(id, that.id) && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stream, id, body);
    }

    @Override
    public String toString() {
        return String.format("StreamMessage[%s:%s]%s", stream, id, body);
    }

}
