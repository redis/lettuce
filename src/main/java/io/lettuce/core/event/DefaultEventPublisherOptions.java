/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.event;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.metrics.CommandLatencyCollectorOptions;

/**
 * The default implementation of {@link CommandLatencyCollectorOptions}.
 *
 * @author Mark Paluch
 */
public class DefaultEventPublisherOptions implements EventPublisherOptions {

    public static final long DEFAULT_EMIT_INTERVAL = 10;
    public static final TimeUnit DEFAULT_EMIT_INTERVAL_UNIT = TimeUnit.MINUTES;

    private static final DefaultEventPublisherOptions DISABLED = new Builder().eventEmitInterval(0, TimeUnit.SECONDS).build();

    private final long eventEmitInterval;
    private final TimeUnit eventEmitIntervalUnit;

    private DefaultEventPublisherOptions(Builder builder) {
        this.eventEmitInterval = builder.eventEmitInterval;
        this.eventEmitIntervalUnit = builder.eventEmitIntervalUnit;
    }

    /**
     * Returns a new {@link DefaultEventPublisherOptions.Builder} to construct {@link DefaultEventPublisherOptions}.
     *
     * @return a new {@link DefaultEventPublisherOptions.Builder} to construct {@link DefaultEventPublisherOptions}.
     */
    public static Builder builder(){
        return new Builder();
    }

    /**
     * Builder for {@link DefaultEventPublisherOptions}.
     */
    public static class Builder {

        private long eventEmitInterval = DEFAULT_EMIT_INTERVAL;
        private TimeUnit eventEmitIntervalUnit = DEFAULT_EMIT_INTERVAL_UNIT;

        private Builder() {
        }

        /**
         * Sets the emit interval and the interval unit. Event emission will be disabled if the {@code eventEmitInterval} is set
         * to 0}. Defaults to 10} {@link TimeUnit#MINUTES}. See {@link DefaultEventPublisherOptions#DEFAULT_EMIT_INTERVAL}
         * {@link DefaultEventPublisherOptions#DEFAULT_EMIT_INTERVAL_UNIT}.
         *
         * @param eventEmitInterval the event interval, must be greater or equal to 0}
         * @param eventEmitIntervalUnit the {@link TimeUnit} for the interval, must not be null
         * @return this
         */
        public Builder eventEmitInterval(long eventEmitInterval, TimeUnit eventEmitIntervalUnit) {
            LettuceAssert.isTrue(eventEmitInterval >= 0, "EventEmitInterval must be greater or equal to 0");
            LettuceAssert.notNull(eventEmitIntervalUnit, "EventEmitIntervalUnit must not be null");

            this.eventEmitInterval = eventEmitInterval;
            this.eventEmitIntervalUnit = eventEmitIntervalUnit;
            return this;
        }

        /**
         *
         * @return a new instance of {@link DefaultEventPublisherOptions}.
         */
        public DefaultEventPublisherOptions build() {
            return new DefaultEventPublisherOptions(this);
        }
    }

    @Override
    public long eventEmitInterval() {
        return eventEmitInterval;
    }

    @Override
    public TimeUnit eventEmitIntervalUnit() {
        return eventEmitIntervalUnit;
    }

    /**
     * Create a new {@link DefaultEventPublisherOptions} using default settings.
     *
     * @return a new instance of a default {@link DefaultEventPublisherOptions} instance
     */
    public static DefaultEventPublisherOptions create() {
        return new Builder().build();
    }

    /**
     * Create a disabled {@link DefaultEventPublisherOptions} using default settings.
     *
     * @return a new instance of a default {@link DefaultEventPublisherOptions} instance with disabled event emission
     */
    public static DefaultEventPublisherOptions disabled() {
        return DISABLED;
    }
}
