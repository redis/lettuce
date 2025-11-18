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

    private final Long millisElapsedFromDelivery;

    private final Long deliveredCount;

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
        this.millisElapsedFromDelivery = null;
        this.deliveredCount = null;
    }

    /**
     * Create a new {@link StreamMessage}.
     *
     * @param stream the stream.
     * @param id the message id.
     * @param millisElapsedFromDelivery the milliseconds since last delivery when CLAIM was used.
     * @param deliveredCount the number of prior deliveries when CLAIM was used.
     * @param body map containing the message body.
     * @since 7.1
     */
    public StreamMessage(K stream, String id, Map<K, V> body, long millisElapsedFromDelivery, long deliveredCount) {

        this.stream = stream;
        this.id = id;
        this.body = body;
        this.millisElapsedFromDelivery = millisElapsedFromDelivery;
        this.deliveredCount = deliveredCount;
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
     * @return the milliseconds since the last delivery of this message when CLAIM was used.
     *         <ul>
     *         <li>{@code null} when not applicable</li>
     *         <li>{@code 0} means not claimed from the pending entries list (PEL)</li>
     *         <li>{@code > 0} means claimed from the PEL</li>
     *         </ul>
     * @since 7.1
     */
    public Long getMillisElapsedFromDelivery() {
        return millisElapsedFromDelivery;
    }

    /**
     * @return the number of prior deliveries of this message when CLAIM was used:
     *         <ul>
     *         <li>{@code null} when not applicable</li>
     *         <li>{@code 0} means not claimed from the pending entries list (PEL)</li>
     *         <li>{@code > 0} means claimed from the PEL</li>
     *         </ul>
     * @since 7.1
     */
    public Long getDeliveredCount() {
        return deliveredCount;
    }

    public boolean isClaimed() {
        return deliveredCount != null && deliveredCount > 0;
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
