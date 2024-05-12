package io.lettuce.core.masterreplica;

import io.lettuce.core.event.Event;

/**
 * Event triggered when Redis Sentinel indicates a topology change trigger.
 *
 * @author Mark Paluch
 * @since 6.1
 */
class SentinelTopologyRefreshEvent implements Event {

    private final String source;

    private final String message;

    private final long delayMs;

    public SentinelTopologyRefreshEvent(String source, String message, long delayMs) {
        this.source = source;
        this.message = message;
        this.delayMs = delayMs;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public long getDelayMs() {
        return delayMs;
    }

}
