/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
 *
 * ---
 *
 * Ported from Resilience4j's LockFreeSlidingTimeWindowMetrics
 * Copyright 2019 Robert Winkler and Bohdan Storozhuk
 * Licensed under the Apache License, Version 2.0
 * https://github.com/resilience4j/resilience4j
 *
 * Modifications:
 * - Ported to be compatible with Java 8: Replaced VarHandle with AtomicReference
 * - Stripped down unused metrics: Removed duration and slow call tracking
 */
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
 * @since 7.2
 */
class MetricsSnapshotImpl implements MetricsSnapshot {

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

    @Override
    public long getSuccessCount() {
        return successCount;
    }

    @Override
    public long getFailureCount() {
        return failureCount;
    }

    @Override
    public long getTotalCount() {
        return successCount + failureCount;
    }

    @Override
    public double getFailureRate() {
        long total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (failureCount * 100.0) / total;
    }

    @Override
    public double getSuccessRate() {
        return 100.0 - getFailureRate();
    }

    @Override
    public String toString() {
        return "MetricsSnapshot{" + "success=" + successCount + ", failure=" + failureCount + ", total=" + getTotalCount()
                + ", failureRate=" + String.format("%.2f", getFailureRate()) + "%" + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetricsSnapshotImpl that = (MetricsSnapshotImpl) o;

        if (successCount != that.successCount) {
            return false;
        }
        return failureCount == that.failureCount;
    }

}
