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

    /**
     * Whether this message was reclaimed from the pending entries list (PEL) using XREADGROUP … CLAIM. Default: false.
     *
     * Note: When CLAIM is used, servers may attach delivery metadata to all entries in the reply (including fresh ones). Use
     * this indicator to distinguish actually reclaimed entries (true) from normal entries (false).
     */
    public boolean isClaimed() {
        return false;
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
