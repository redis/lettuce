package io.lettuce.core.models.stream;

import java.util.Map;

import io.lettuce.core.Range;

/**
 * Value object representing the output of the Redis {@literal XPENDING} reporting a summary on pending messages.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class PendingMessages {

    private final long count;

    private final Range<String> messageIds;

    private final Map<String, Long> consumerMessageCount;

    public PendingMessages(long count, Range<String> messageIds, Map<String, Long> consumerMessageCount) {

        this.count = count;
        this.messageIds = messageIds;
        this.consumerMessageCount = consumerMessageCount;
    }

    public long getCount() {
        return count;
    }

    public Range<String> getMessageIds() {
        return messageIds;
    }

    public Map<String, Long> getConsumerMessageCount() {
        return consumerMessageCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PendingMessages))
            return false;

        PendingMessages that = (PendingMessages) o;

        if (count != that.count)
            return false;
        if (messageIds != null ? !messageIds.equals(that.messageIds) : that.messageIds != null)
            return false;
        return consumerMessageCount != null ? consumerMessageCount.equals(that.consumerMessageCount)
                : that.consumerMessageCount == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (count ^ (count >>> 32));
        result = 31 * result + (messageIds != null ? messageIds.hashCode() : 0);
        result = 31 * result + (consumerMessageCount != null ? consumerMessageCount.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [count=").append(count);
        sb.append(", messageIds=").append(messageIds);
        sb.append(", consumerMessageCount=").append(consumerMessageCount);
        sb.append(']');
        return sb.toString();
    }
}
