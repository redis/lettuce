package com.lambdaworks.redis.metrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Latency metrics for commands. This class provides the count, time unit and firstResponse/completion latencies.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 23.09.15 15:10
 */
public class CommandMetrics {
    private final long count;
    private final TimeUnit timeUnit;

    private final CommandLatency firstResponse;
    private final CommandLatency completion;

    public CommandMetrics(long count, TimeUnit timeUnit, CommandLatency firstResponse, CommandLatency completion) {
        this.count = count;
        this.timeUnit = timeUnit;
        this.firstResponse = firstResponse;
        this.completion = completion;
    }

    /**
     * 
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * 
     * @return the time unit for the {@link #getFirstResponse()} and {@link #getCompletion()} latencies.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * 
     * @return latencies between send and the first command response
     */
    public CommandLatency getFirstResponse() {
        return firstResponse;
    }

    /**
     * 
     * @return latencies between send and the command completion
     */
    public CommandLatency getCompletion() {
        return completion;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("[count=").append(count);
        sb.append(", timeUnit=").append(timeUnit);
        sb.append(", firstResponse=").append(firstResponse);
        sb.append(", completion=").append(completion);
        sb.append(']');
        return sb.toString();
    }

    public static class CommandLatency {
        private final long min;
        private final long max;
        private final Map<Double, Long> percentiles;

        public CommandLatency(long min, long max, Map<Double, Long> percentiles) {
            this.min = min;
            this.max = max;
            this.percentiles = percentiles;
        }

        /**
         * 
         * @return the minimum time
         */
        public long getMin() {
            return min;
        }

        /**
         * 
         * @return the maximum time
         */
        public long getMax() {
            return max;
        }

        /**
         * 
         * @return percentile mapping
         */
        public Map<Double, Long> getPercentiles() {
            return percentiles;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append("[min=").append(min);
            sb.append(", max=").append(max);
            sb.append(", percentiles=").append(percentiles);
            sb.append(']');
            return sb.toString();
        }
    }
}
