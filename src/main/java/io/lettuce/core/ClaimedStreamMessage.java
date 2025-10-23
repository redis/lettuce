package io.lettuce.core;

import java.time.Duration;
import java.util.Map;

/**
 * Stream message returned by XREADGROUP when entries were claimed from the PEL using CLAIM min-idle-time. Contains additional
 * metadata: milliseconds since last delivery and redelivery count.
 */
public class ClaimedStreamMessage<K, V> extends StreamMessage<K, V> {

    private final long msSinceLastDelivery;

    private final long redeliveryCount;

    public ClaimedStreamMessage(K stream, String id, Map<K, V> body, long msSinceLastDelivery, long redeliveryCount) {
        super(stream, id, body);
        this.msSinceLastDelivery = msSinceLastDelivery;
        this.redeliveryCount = redeliveryCount;
    }

    public long getMsSinceLastDelivery() {
        return msSinceLastDelivery;
    }

    public Duration getSinceLastDelivery() {
        return Duration.ofMillis(msSinceLastDelivery);
    }

    public long getRedeliveryCount() {
        return redeliveryCount;
    }

    @Override
    public boolean isClaimed() {
        // "Really claimed" implies it was previously delivered at least once.
        return redeliveryCount >= 1;
    }

}
