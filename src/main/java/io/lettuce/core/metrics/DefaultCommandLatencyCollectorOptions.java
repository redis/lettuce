/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.metrics;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.LettuceAssert;

/**
 * The default implementation of {@link CommandLatencyCollectorOptions}.
 *
 * @author Mark Paluch
 */
public class DefaultCommandLatencyCollectorOptions implements CommandLatencyCollectorOptions {

    public static final TimeUnit DEFAULT_TARGET_UNIT = TimeUnit.MICROSECONDS;

    public static final double[] DEFAULT_TARGET_PERCENTILES = new double[] { 50.0, 90.0, 95.0, 99.0, 99.9 };

    public static final boolean DEFAULT_RESET_LATENCIES_AFTER_EVENT = true;

    public static final boolean DEFAULT_LOCAL_DISTINCTION = false;

    public static final boolean DEFAULT_ENABLED = true;

    private static final DefaultCommandLatencyCollectorOptions DISABLED = builder().disable().build();

    private final TimeUnit targetUnit;

    private final double[] targetPercentiles;

    private final boolean resetLatenciesAfterEvent;

    private final boolean localDistinction;

    private final boolean enabled;

    private final Builder builder;

    protected DefaultCommandLatencyCollectorOptions(Builder builder) {
        this.targetUnit = builder.targetUnit;
        this.targetPercentiles = builder.targetPercentiles;
        this.resetLatenciesAfterEvent = builder.resetLatenciesAfterEvent;
        this.localDistinction = builder.localDistinction;
        this.enabled = builder.enabled;
        this.builder = builder;
    }

    /**
     * Create a new {@link DefaultCommandLatencyCollectorOptions} instance using default settings.
     *
     * @return a new instance of {@link DefaultCommandLatencyCollectorOptions} instance using default settings.
     */
    public static DefaultCommandLatencyCollectorOptions create() {
        return builder().build();
    }

    /**
     * Create a {@link DefaultCommandLatencyCollectorOptions} instance with disabled event emission.
     *
     * @return a new instance of {@link DefaultCommandLatencyCollectorOptions} with disabled event emission.
     */
    public static DefaultCommandLatencyCollectorOptions disabled() {
        return DISABLED;
    }

    /**
     * Returns a new {@link DefaultCommandLatencyCollectorOptions.Builder} to construct
     * {@link DefaultCommandLatencyCollectorOptions}.
     *
     * @return a new {@link DefaultCommandLatencyCollectorOptions.Builder} to construct
     *         {@link DefaultCommandLatencyCollectorOptions}.
     */
    public static DefaultCommandLatencyCollectorOptions.Builder builder() {
        return new DefaultCommandLatencyCollectorOptions.Builder();
    }

    /**
     * Returns a builder to create new {@link DefaultCommandLatencyCollectorOptions} whose settings are replicated from the
     * current {@link DefaultCommandLatencyCollectorOptions}.
     *
     * @return a a {@link CommandLatencyCollectorOptions.Builder} to create new {@link DefaultCommandLatencyCollectorOptions}
     *         whose settings are replicated from the current {@link DefaultCommandLatencyCollectorOptions}.
     * @since 5.1
     */
    @Override
    public DefaultCommandLatencyCollectorOptions.Builder mutate() {
        return this.builder;
    }

    /**
     * Builder for {@link DefaultCommandLatencyCollectorOptions}.
     */
    public static class Builder implements CommandLatencyCollectorOptions.Builder {

        private TimeUnit targetUnit = DEFAULT_TARGET_UNIT;

        private double[] targetPercentiles = DEFAULT_TARGET_PERCENTILES;

        private boolean resetLatenciesAfterEvent = DEFAULT_RESET_LATENCIES_AFTER_EVENT;

        private boolean localDistinction = DEFAULT_LOCAL_DISTINCTION;

        private boolean enabled = DEFAULT_ENABLED;

        private Builder() {
        }

        /**
         * Disable the latency collector.
         *
         * @return this {@link Builder}.
         */
        @Override
        public Builder disable() {
            this.enabled = false;
            return this;
        }

        /**
         * Enable the latency collector.
         *
         * @return this {@link Builder}.
         * @since 5.1
         */
        @Override
        public Builder enable() {
            this.enabled = true;
            return this;
        }

        /**
         * Set the target unit for the latencies. Defaults to {@link TimeUnit#MILLISECONDS}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_TARGET_UNIT}.
         *
         * @param targetUnit the target unit, must not be {@code null}
         * @return this {@link Builder}.
         */
        @Override
        public Builder targetUnit(TimeUnit targetUnit) {
            LettuceAssert.notNull(targetUnit, "TargetUnit must not be null");
            this.targetUnit = targetUnit;
            return this;
        }

        /**
         * Sets the emitted percentiles. Defaults to 50.0, 90.0, 95.0, 99.0, 99.9} . See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_TARGET_PERCENTILES}.
         *
         * @param targetPercentiles the percentiles which should be emitted, must not be {@code null}
         * @return this {@link Builder}.
         */
        @Override
        public Builder targetPercentiles(double[] targetPercentiles) {
            LettuceAssert.notNull(targetPercentiles, "TargetPercentiles must not be null");
            this.targetPercentiles = targetPercentiles;
            return this;
        }

        /**
         * Sets whether the recorded latencies should be reset once the metrics event was emitted. Defaults to {@code true}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_RESET_LATENCIES_AFTER_EVENT}.
         *
         * @param resetLatenciesAfterEvent {@code true} if the recorded latencies should be reset once the metrics event was
         *        emitted
         * @return this {@link Builder}.
         */
        @Override
        public Builder resetLatenciesAfterEvent(boolean resetLatenciesAfterEvent) {
            this.resetLatenciesAfterEvent = resetLatenciesAfterEvent;
            return this;
        }

        /**
         * Enables per connection metrics tracking insead of per host/port. If {@code true}, multiple connections to the same
         * host/connection point will be recorded separately which allows to inspect every connection individually. If
         * {@code false}, multiple connections to the same host/connection point will be recorded together. This allows a
         * consolidated view on one particular service. Defaults to {@code false}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_LOCAL_DISTINCTION}.
         *
         * @param localDistinction {@code true} if latencies are recorded distinct on local level (per connection)
         * @return this {@link Builder}.
         */
        @Override
        public Builder localDistinction(boolean localDistinction) {
            this.localDistinction = localDistinction;
            return this;
        }

        /**
         * @return a new instance of {@link DefaultCommandLatencyCollectorOptions}.
         */
        @Override
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

}
