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

/**
 * Configuration interface for command latency collection.
 *
 * @author Mark Paluch
 */
public interface CommandLatencyCollectorOptions {

    /**
     * Create a new {@link CommandLatencyCollectorOptions} instance using default settings.
     *
     * @return a new instance of {@link CommandLatencyCollectorOptions} instance using default settings
     * @since 5.1
     */
    static CommandLatencyCollectorOptions create() {
        return builder().build();
    }

    /**
     * Create a {@link CommandLatencyCollectorOptions} instance with disabled event emission.
     *
     * @return a new instance of {@link CommandLatencyCollectorOptions} with disabled event emission
     * @since 5.1
     */
    static CommandLatencyCollectorOptions disabled() {
        return DefaultCommandLatencyCollectorOptions.disabled();
    }

    /**
     * Returns a new {@link CommandLatencyCollectorOptions.Builder} to construct {@link CommandLatencyCollectorOptions}.
     *
     * @return a new {@link CommandLatencyCollectorOptions.Builder} to construct {@link CommandLatencyCollectorOptions}.
     * @since 5.1
     */
    static CommandLatencyCollectorOptions.Builder builder() {
        return DefaultCommandLatencyCollectorOptions.builder();
    }

    /**
     * Returns a builder to create new {@link CommandLatencyCollectorOptions} whose settings are replicated from the current
     * {@link CommandLatencyCollectorOptions}.
     *
     * @return a a {@link CommandLatencyCollectorOptions.Builder} to create new {@link CommandLatencyCollectorOptions} whose
     *         settings are replicated from the current {@link CommandLatencyCollectorOptions}
     *
     * @since 5.1
     */
    CommandLatencyCollectorOptions.Builder mutate();

    /**
     * Returns the target {@link TimeUnit} for the emitted latencies.
     *
     * @return the target {@link TimeUnit} for the emitted latencies
     */
    TimeUnit targetUnit();

    /**
     * Returns the percentiles which should be exposed in the metric.
     *
     * @return the percentiles which should be exposed in the metric
     */
    double[] targetPercentiles();

    /**
     * Returns whether the latencies should be reset once an event is emitted.
     *
     * @return {@code true} if the latencies should be reset once an event is emitted.
     */
    boolean resetLatenciesAfterEvent();

    /**
     * Returns whether to distinct latencies on local level. If {@code true}, multiple connections to the same
     * host/connection point will be recorded separately which allows to inspect every connection individually. If
     * {@code false}, multiple connections to the same host/connection point will be recorded together. This allows a
     * consolidated view on one particular service.
     *
     * @return {@code true} if latencies are recorded distinct on local level (per connection)
     */
    boolean localDistinction();

    /**
     * Returns whether the latency collector is enabled.
     *
     * @return {@code true} if the latency collector is enabled
     */
    boolean isEnabled();

    /**
     * Builder for {@link CommandLatencyCollectorOptions}.
     *
     * @since 5.1
     */
    interface Builder {

        /**
         * Disable the latency collector.
         *
         * @return this
         */
        Builder disable();

        /**
         * Enable the latency collector.
         *
         * @return this {@link DefaultCommandLatencyCollectorOptions.Builder}.
         */
        Builder enable();

        /**
         * Enables per connection metrics tracking insead of per host/port. If {@code true}, multiple connections to the same
         * host/connection point will be recorded separately which allows to inspect every connection individually. If
         * {@code false}, multiple connections to the same host/connection point will be recorded together. This allows a
         * consolidated view on one particular service. Defaults to {@code false}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_LOCAL_DISTINCTION}.
         *
         * @param localDistinction {@code true} if latencies are recorded distinct on local level (per connection).
         * @return this {@link Builder}.
         */
        Builder localDistinction(boolean localDistinction);

        /**
         * Sets whether the recorded latencies should be reset once the metrics event was emitted. Defaults to {@code true}.
         * See {@link DefaultCommandLatencyCollectorOptions#DEFAULT_RESET_LATENCIES_AFTER_EVENT}.
         *
         * @param resetLatenciesAfterEvent {@code true} if the recorded latencies should be reset once the metrics event was
         *        emitted.
         *
         * @return this {@link Builder}.
         */
        Builder resetLatenciesAfterEvent(boolean resetLatenciesAfterEvent);

        /**
         * Sets the emitted percentiles. Defaults to 50.0, 90.0, 95.0, 99.0, 99.9}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_TARGET_PERCENTILES}.
         *
         * @param targetPercentiles the percentiles which should be emitted, must not be {@code null}.
         *
         * @return this {@link Builder}.
         */
        Builder targetPercentiles(double[] targetPercentiles);

        /**
         * Set the target unit for the latencies. Defaults to {@link TimeUnit#MILLISECONDS}. See
         * {@link DefaultCommandLatencyCollectorOptions#DEFAULT_TARGET_UNIT}.
         *
         * @param targetUnit the target unit, must not be {@code null}.
         * @return this {@link Builder}.
         *
         */
        Builder targetUnit(TimeUnit targetUnit);

        /**
         * @return a new instance of {@link CommandLatencyCollectorOptions}.
         */
        CommandLatencyCollectorOptions build();

    }

}
