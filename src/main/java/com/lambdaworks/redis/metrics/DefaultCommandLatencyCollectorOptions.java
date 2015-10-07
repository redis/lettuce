package com.lambdaworks.redis.metrics;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.TimeUnit;

/**
 * The default implementation of {@link CommandLatencyCollectorOptions}.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class DefaultCommandLatencyCollectorOptions implements CommandLatencyCollectorOptions {

    public static final TimeUnit DEFAULT_TARGET_UNIT = TimeUnit.MICROSECONDS;
    public static final double[] DEFAULT_TARGET_PERCENTILES = new double[] { 50.0, 90.0, 95.0, 99.0, 99.9 };
    public static final boolean DEFAULT_RESET_LATENCIES_AFTER_EVENT = true;
    public static final boolean DEFAULT_LOCAL_DISTINCTION = false;
    public static final boolean DEFAULT_ENABLED = true;

    private static final DefaultCommandLatencyCollectorOptions DISABLED = new Builder().disable().build();

    private final TimeUnit targetUnit;
    private final double[] targetPercentiles;
    private final boolean resetLatenciesAfterEvent;
    private final boolean localDistinction;
    private final boolean enabled;

    protected DefaultCommandLatencyCollectorOptions(Builder builder) {
        this.targetUnit = builder.targetUnit;
        this.targetPercentiles = builder.targetPercentiles;
        this.resetLatenciesAfterEvent = builder.resetLatenciesAfterEvent;
        this.localDistinction = builder.localDistinction;
        this.enabled = builder.enabled;
    }

    /**
     * Builder for {@link DefaultCommandLatencyCollectorOptions}.
     */
    public static class Builder {

        private TimeUnit targetUnit = DEFAULT_TARGET_UNIT;
        private double[] targetPercentiles = DEFAULT_TARGET_PERCENTILES;
        private boolean resetLatenciesAfterEvent = DEFAULT_RESET_LATENCIES_AFTER_EVENT;
        private boolean localDistinction = DEFAULT_LOCAL_DISTINCTION;
        private boolean enabled = DEFAULT_ENABLED;

        public Builder() {
        }

        /**
         * Disable the latency collector.
         * 
         * @return this
         */
        public Builder disable() {
            this.enabled = false;
            return this;
        }

        /**
         * Set the target unit for the latencies. Defaults to {@link TimeUnit#MILLISECONDS}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_TARGET_UNIT}.
         * 
         * @param targetUnit the target unit, must not be {@literal null}
         * @return this
         *
         */
        public Builder targetUnit(TimeUnit targetUnit) {
            checkArgument(targetUnit != null, "targetUnit must not be null");
            this.targetUnit = targetUnit;
            return this;
        }

        /**
         * Sets the emitted percentiles. Defaults to 50.0, 90.0, 95.0, 99.0, 99.9} . See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_TARGET_PERCENTILES}.
         *
         * @param targetPercentiles the percentiles which should be emitted, must not be {@literal null}
         * 
         * @return this
         */
        public Builder targetPercentiles(double[] targetPercentiles) {
            checkArgument(targetPercentiles != null, "targetPercentiles must not be null");
            this.targetPercentiles = targetPercentiles;
            return this;
        }

        /**
         * Sets whether the recorded latencies should be reset once the metrics event was emitted. Defaults to {@literal true}.
         * See {@link DefaultCommandLatencyCollectorOptions#DEFAULT_RESET_LATENCIES_AFTER_EVENT}.
         *
         * @param resetLatenciesAfterEvent {@literal true} if the recorded latencies should be reset once the metrics event was
         *        emitted
         * 
         * @return this
         */
        public Builder resetLatenciesAfterEvent(boolean resetLatenciesAfterEvent) {
            this.resetLatenciesAfterEvent = resetLatenciesAfterEvent;
            return this;
        }

        /**
         * Sets whether whether to distinct latencies on local level. If {@literal true}, multiple connections to the same
         * host/connection point will be recorded separately which allows to inspect every connection individually. If
         * {@literal false}, multiple connections to the same host/connection point will be recorded together. This allows a
         * consolidated view on one particular service. Defaults to {@literal false}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_LOCAL_DISTINCTION}.
         *
         * @param localDistinction {@literal true} if latencies are recorded distinct on local level (per connection)
         * @return this
         */
        public Builder localDistinction(boolean localDistinction) {
            this.localDistinction = localDistinction;
            return this;
        }

        /**
         *
         * @return a new instance of {@link DefaultCommandLatencyCollectorOptions}.
         */
        public DefaultCommandLatencyCollectorOptions build() {
            return new DefaultCommandLatencyCollectorOptions(this);
        }
    }

    @Override
    public TimeUnit targetUnit() {
        return targetUnit;
    }

    @Override
    public double[] targetPercentiles() {
        double[] result = new double[targetPercentiles.length];
        System.arraycopy(targetPercentiles, 0, result, 0, targetPercentiles.length);
        return result;
    }

    @Override
    public boolean resetLatenciesAfterEvent() {
        return resetLatenciesAfterEvent;
    }

    @Override
    public boolean localDistinction() {
        return localDistinction;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Create a new {@link DefaultCommandLatencyCollectorOptions} instance using default settings.
     * 
     * @return a new instance of {@link DefaultCommandLatencyCollectorOptions} instance using default settings
     */
    public static DefaultCommandLatencyCollectorOptions create() {
        return new Builder().build();
    }

    /**
     * Create a {@link DefaultCommandLatencyCollectorOptions} instance with disabled event emission.
     * 
     * @return a new instance of {@link DefaultCommandLatencyCollectorOptions} with disabled event emission
     */
    public static DefaultCommandLatencyCollectorOptions disabled() {
        return DISABLED;
    }
}
