/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.function.Supplier;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Reservoir;

import io.lettuce.core.internal.LettuceAssert;
import io.micrometer.core.instrument.Tags;

/**
 * Configuration options for {@link DropWizardCommandLatencyRecorder}.
 *
 * @author Michael Bell
 * @since 6.1
 */
public class DropWizardOptions {

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_INCLUDE_ADDRESS = false;

    public static final boolean DEFAULT_LOCAL_DISTINCTION = false;

    private static final DropWizardOptions DISABLED = builder().disable().build();

    private final Builder builder;

    private final boolean enabled;

    private final boolean includeAddress;

    private final Supplier<Reservoir> reservoir;

    private final boolean localDistinction;

    protected DropWizardOptions(Builder builder) {

        this.builder = builder;
        this.enabled = builder.enabled;
        this.includeAddress = builder.includeAddress;
        this.reservoir = builder.reservoir;
        this.localDistinction = builder.localDistinction;
    }

    /**
     * Create a new {@link DropWizardOptions} instance using default settings.
     *
     * @return a new instance of {@link DropWizardOptions} instance using default settings
     */
    public static DropWizardOptions create() {
        return builder().build();
    }

    /**
     * Create a {@link DropWizardOptions} instance with disabled event emission.
     *
     * @return a new instance of {@link DropWizardOptions} with disabled event emission
     */
    public static DropWizardOptions disabled() {
        return DISABLED;
    }

    /**
     * Returns a new {@link DropWizardOptions.Builder} to construct {@link DropWizardOptions}.
     *
     * @return a new {@link DropWizardOptions.Builder} to construct {@link DropWizardOptions}.
     */
    public static DropWizardOptions.Builder builder() {
        return new DropWizardOptions.Builder();
    }

    /**
     * Returns a builder to create new {@link DropWizardOptions} whose settings are replicated from the current
     * {@link DropWizardOptions}.
     *
     * @return a a {@link CommandLatencyCollectorOptions.Builder} to create new {@link DropWizardOptions} whose settings are
     *         replicated from the current {@link DropWizardOptions}
     */
    public DropWizardOptions.Builder mutate() {
        return this.builder;
    }

    /**
     * Builder for {@link DropWizardOptions}.
     */
    public static class Builder {

        private boolean enabled = DEFAULT_ENABLED;

        private boolean includeAddress = DEFAULT_INCLUDE_ADDRESS;

        private Supplier<Reservoir> reservoir = ExponentiallyDecayingReservoir::new;

        private boolean localDistinction = DEFAULT_LOCAL_DISTINCTION;

        private Builder() {
        }

        /**
         * Disable the latency collector.
         *
         * @return this {@link Builder}.
         */
        public Builder disable() {
            this.enabled = false;
            return this;
        }

        /**
         * Enable the latency collector.
         *
         * @return this {@link Builder}.
         */
        public Builder enable() {
            this.enabled = true;
            return this;
        }

        /**
         * Disable inclusion of the address in the metric.
         *
         * @return this {@link Builder}.
         */
        public Builder excludeAddress() {
            this.includeAddress = false;
            return this;
        }

        /**
         * Enable the inclusion of the address in the metricr.
         *
         * @return this {@link Builder}.
         */
        public Builder includeAddress() {
            this.includeAddress = true;
            return this;
        }
        /**
         * Set the reservoir for the histogram. Different choices incur different performance footprints
         * The default is {@link ExponentiallyDecayingReservoir}.
         *
         * @param reservoir Reservoir for histogram
         * @return this {@link Builder}.
         */
        public Builder reservoir(Supplier<Reservoir> reservoir) {
            this.reservoir = reservoir;
            return this;
        }

        /**
         * Enables per connection metrics tracking insead of per host/port. If {@code true}, multiple connections to the same
         * host/connection point will be recorded separately which allows to inspect every connection individually. If
         * {@code false}, multiple connections to the same host/connection point will be recorded together. This allows a
         * consolidated view on one particular service. Defaults to {@code false}. See
         * {@link DropWizardOptions#DEFAULT_LOCAL_DISTINCTION}.
         *
         * Warning: Enabling this could potentially cause a label cardinality explosion in the remote metric system and should
         * be used with caution.
         *
         * @param localDistinction {@code true} if latencies are recorded distinct on local level (per connection)
         * @return this {@link Builder}.
         */
        public Builder localDistinction(boolean localDistinction) {
            this.localDistinction = localDistinction;
            return this;
        }

        /**
         * @return a new instance of {@link DropWizardOptions}.
         */
        public DropWizardOptions build() {
            return new DropWizardOptions(this);
        }

    }

    public boolean isEnabled() {
        return enabled;
    }

    public Supplier<Reservoir> reservoir() {
        return reservoir;
    }

    public boolean localDistinction() {
        return localDistinction;
    }

    public boolean isIncludeAddress() {
        return includeAddress;
    }

}
