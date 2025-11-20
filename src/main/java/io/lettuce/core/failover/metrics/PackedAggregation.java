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
 * Copyright 2024 Florentin Simion and Rares Vlasceanu
 * Licensed under the Apache License, Version 2.0
 * https://github.com/resilience4j/resilience4j
 *
 * Modifications:
 * - Ported to be compatible with Java 8: Replaced VarHandle with AtomicReference
 * - Stripped down unused metrics: Removed duration and slow call tracking
 */
package io.lettuce.core.failover.metrics;

import java.util.Arrays;

/**
 * A measurement implementation used in sliding windows to track the total number of calls and failed calls in the window,
 * along with the number of calls and failed calls of the current entry/bucket.
 *
 * <p>
 * This implementation has the advantage of being cache friendly, benefiting from cache locality when counting/discarding the
 * tracked metrics.
 *
 * <p>
 * Besides this, metrics can also be quickly cloned, which is important for the lock-free algorithms which are operating with
 * immutable objects.
 */
class PackedAggregation implements CumulativeMeasurement {

    private int[] counts = new int[4];

    private static final int TOTAL_FAILED_CALLS_INDEX = 0;

    private static final int TOTAL_CALLS_INDEX = 1;

    private static final int FAILED_CALLS_INDEX = 2;

    private static final int CALLS_INDEX = 3;

    public PackedAggregation() {
    }

    public PackedAggregation(int[] counts) {
        this.counts = counts;
    }

    PackedAggregation copy() {
        return new PackedAggregation(counts.clone());
    }

    void discard(PackedAggregation discarded) {

        counts[TOTAL_FAILED_CALLS_INDEX] -= discarded.counts[FAILED_CALLS_INDEX];
        counts[TOTAL_CALLS_INDEX] -= discarded.counts[CALLS_INDEX];

        counts[FAILED_CALLS_INDEX] = 0;
        counts[CALLS_INDEX] = 0;
    }

    @Override
    public void record(Outcome outcome) {

        counts[TOTAL_CALLS_INDEX]++;
        counts[CALLS_INDEX]++;

        if (outcome == Outcome.FAILURE) {
            counts[TOTAL_FAILED_CALLS_INDEX]++;
            counts[FAILED_CALLS_INDEX]++;
        }
    }

    @Override
    public int getNumberOfFailedCalls() {
        return counts[TOTAL_FAILED_CALLS_INDEX];
    }

    @Override
    public int getNumberOfCalls() {
        return counts[TOTAL_CALLS_INDEX];
    }

    @Override
    public String toString() {
        return "PackedAggregation{" + "counts=" + Arrays.toString(counts) + '}';
    }

}
