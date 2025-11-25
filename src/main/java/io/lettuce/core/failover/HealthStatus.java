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
 */
package io.lettuce.core.failover;

/**
 * Enumeration representing the health status of a database endpoint.
 * <p>
 * Used by the {@link RedisDatabase} to track and report the current health state of database connections.
 *
 * @author Ali Takavci
 * @since 7.1
 */
public enum HealthStatus {

    /**
     * Health status is unknown. This is the initial state before any health checks and/or any organic load have been performed.
     */
    UNKNOWN,

    /**
     * The endpoint is healthy and operating normally. Health checks are passing and the circuit breaker is in the CLOSED state.
     */
    HEALTHY,

    /**
     * The endpoint is unhealthy and experiencing failures. Possible issues with health checks or the circuit breaker is in the
     * OPEN state.
     */
    UNHEALTHY

}
