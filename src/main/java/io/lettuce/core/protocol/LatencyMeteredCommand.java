package io.lettuce.core.protocol;

/**
 * {@link CommandWrapper} implementation to track {@link WithLatency command latency}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class LatencyMeteredCommand<K, V, T> extends CommandWrapper<K, V, T> implements WithLatency {

    private long sentNs = -1;

    private long firstResponseNs = -1;

    private long completedNs = -1;

    public LatencyMeteredCommand(RedisCommand<K, V, T> command) {
        super(command);
    }

    @Override
    public void sent(long timeNs) {
        sentNs = timeNs;
        firstResponseNs = -1;
        completedNs = -1;
    }

    @Override
    public void firstResponse(long timeNs) {
        firstResponseNs = timeNs;
    }

    @Override
    public void completed(long timeNs) {
        completedNs = timeNs;
    }

    @Override
    public long getSent() {
        return sentNs;
    }

    @Override
    public long getFirstResponse() {
        return firstResponseNs;
    }

    @Override
    public long getCompleted() {
        return completedNs;
    }

}
