package io.lettuce.core.models.stream;

import java.time.Duration;

/**
 * Value object representing a pending message reported through XPENDING with range/limit.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class PendingMessage {

    private final String id;

    private final String consumer;

    private final long msSinceLastDelivery;

    private final long redeliveryCount;

    public PendingMessage(String id, String consumer, long msSinceLastDelivery, long redeliveryCount) {

        this.id = id;
        this.consumer = consumer;
        this.msSinceLastDelivery = msSinceLastDelivery;
        this.redeliveryCount = redeliveryCount;
    }

    public String getId() {
        return id;
    }

    public String getConsumer() {
        return consumer;
    }

    public long getMsSinceLastDelivery() {
        return msSinceLastDelivery;
    }

    public Duration getSinceLastDelivery() {
        return Duration.ofMillis(getMsSinceLastDelivery());
    }

    public long getRedeliveryCount() {
        return redeliveryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PendingMessage))
            return false;

        PendingMessage that = (PendingMessage) o;

        if (msSinceLastDelivery != that.msSinceLastDelivery)
            return false;
        if (redeliveryCount != that.redeliveryCount)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null)
            return false;
        return consumer != null ? consumer.equals(that.consumer) : that.consumer == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (consumer != null ? consumer.hashCode() : 0);
        result = 31 * result + (int) (msSinceLastDelivery ^ (msSinceLastDelivery >>> 32));
        result = 31 * result + (int) (redeliveryCount ^ (redeliveryCount >>> 32));
        return result;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [id='").append(id).append('\'');
        sb.append(", consumer='").append(consumer).append('\'');
        sb.append(", msSinceLastDelivery=").append(msSinceLastDelivery);
        sb.append(", redeliveryCount=").append(redeliveryCount);
        sb.append(']');
        return sb.toString();
    }

}
