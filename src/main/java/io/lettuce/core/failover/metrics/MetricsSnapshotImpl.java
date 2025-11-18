package io.lettuce.core.failover.metrics;

/**
 * Immutable snapshot of metrics at a point in time. Represents the state of success and failure counts within a specific time
 * window.
 *
 * <p>
 * This class is thread-safe and immutable. It captures a consistent view of metrics at the moment of creation and does not
 * change afterward.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class MetricsSnapshotImpl implements MetricsSnapshot {

    /**
     * Number of successful command executions in the time window.
     */
    private final long successCount;

    /**
     * Number of failed command executions in the time window.
     */
    private final long failureCount;

    /**
     * Create a new metrics snapshot with the given counts.
     *
     * @param successCount the number of successful commands
     * @param failureCount the number of failed commands
     */
    public MetricsSnapshotImpl(long successCount, long failureCount) {
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    /**
     * Get the number of successful command executions in the time window.
     *
     * @return the success count
     */
    @Override
    public long getSuccessCount() {
        return successCount;
    }

    /**
     * Get the number of failed command executions in the time window.
     *
     * @return the failure count
     */
    @Override
    public long getFailureCount() {
        return failureCount;
    }

    /**
     * Get the total number of commands (success + failure) in the time window.
     *
     * @return the total count
     */
    @Override
    public long getTotalCount() {
        return successCount + failureCount;
    }

    /**
     * Get the failure rate as a percentage (0-100).
     *
     * @return the failure rate, or 0 if no commands have been executed
     */
    @Override
    public double getFailureRate() {
        long total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (failureCount * 100.0) / total;
    }

    /**
     * Get the success rate as a percentage (0-100).
     *
     * @return the success rate, or 0 if no commands have been executed
     */
    @Override
    public double getSuccessRate() {
        return 100.0 - getFailureRate();
    }

    @Override
    public String toString() {
        return "MetricsSnapshot{" + "success=" + successCount + ", failure=" + failureCount + ", total=" + getTotalCount()
                + ", failureRate=" + String.format("%.2f", getFailureRate()) + "%" + '}';
    }

}
