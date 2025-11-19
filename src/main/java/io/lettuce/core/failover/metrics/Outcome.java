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

public enum Outcome {
    SUCCESS, FAILURE
}
