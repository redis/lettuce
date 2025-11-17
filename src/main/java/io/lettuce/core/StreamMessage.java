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

    private final Long msSinceLastDelivery;

    private final Long redeliveryCount;

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
        this.msSinceLastDelivery = null;
        this.redeliveryCount = null;
    }

    /**
     * Create a new {@link StreamMessage}.
     *
     * @param stream the stream.
     * @param id the message id.
     * @param msSinceLastDelivery the milliseconds since last delivery when CLAIM was used.
     * @param redeliveryCount the number of prior deliveries when CLAIM was used.
     * @param body map containing the message body.
     */
    public StreamMessage(K stream, String id, Map<K, V> body, long msSinceLastDelivery, long redeliveryCount) {

        this.stream = stream;
        this.id = id;
        this.body = body;
        this.msSinceLastDelivery = msSinceLastDelivery;
        this.redeliveryCount = redeliveryCount;
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
     * @return the milliseconds since the last delivery of this message when CLAIM was used. Default: 0. ul>
     *         <li>{@code null} when not applicable</li>
     *         <li>{@code 0} means not claimed from the pending entries list (PEL)</li>
     *         <li>{@code > 0} means claimed from the PEL</li>
     *         </ul>
     * @since 7.1
     */
    public Long getMsSinceLastDelivery() {
        return msSinceLastDelivery;
    }

    /**
     * /**
     * 
     * @return the number of prior deliveries of this message when CLAIM was used:
     *         <ul>
     *         <li>{@code null} when not applicable</li>
     *         <li>{@code 0} means not claimed from the pending entries list (PEL)</li>
     *         <li>{@code > 0} means claimed from the PEL</li>
     *         </ul>
     * @since 7.1
     */
    public Long getRedeliveryCount() {
        return redeliveryCount;
    }

    public boolean isClaimed() {
        return redeliveryCount != null && redeliveryCount > 0;
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
