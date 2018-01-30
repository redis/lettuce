/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.metrics;

import java.util.concurrent.TimeUnit;

/**
 * Configuration interface for command latency collection.
 *
 * @author Mark Paluch
 */
public interface CommandLatencyCollectorOptions {

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
     * @return {@literal true} if the latencies should be reset once an event is emitted.
     */
    boolean resetLatenciesAfterEvent();

    /**
     * Returns whether to distinct latencies on local level. If {@literal true}, multiple connections to the same
     * host/connection point will be recorded separately which allows to inspect every connection individually. If
     * {@literal false}, multiple connections to the same host/connection point will be recorded together. This allows a
     * consolidated view on one particular service.
     *
     * @return {@literal true} if latencies are recorded distinct on local level (per connection)
     */
    boolean localDistinction();

    /**
     * Returns whether the latency collector is enabled.
     *
     * @return {@literal true} if the latency collector is enabled
     */
    boolean isEnabled();
}
