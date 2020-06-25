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

/**
 * Generic metrics collector interface. A metrics collector collects metrics and emits metric events.
 *
 * @author Mark Paluch
 * @param <T> data type of the metrics.
 * @since 3.4
 */
public interface MetricCollector<T> {

    /**
     * Shut down the metrics collector.
     */
    void shutdown();

    /**
     * Returns the collected/aggregated metrics.
     *
     * @return the the collected/aggregated metrics.
     */
    T retrieveMetrics();

    /**
     * Returns {@code true} if the metric collector is enabled.
     *
     * @return {@code true} if the metric collector is enabled.
     */
    boolean isEnabled();

}
