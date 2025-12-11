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
package io.lettuce.core.failover.health;

import io.lettuce.core.failover.RedisDatabase;

/**
 * Enumeration representing the health status of a database endpoint as determined by the health check service.
 * <p>
 * This status reflects the results from active health checks performed by the health check service. Health checks periodically
 * probes endpoints to verify their availability and responsiveness, updating this status accordingly.
 * <p>
 * Used by the {@link RedisDatabase} to track and report health check results from the health check API.
 *
 * @author Ali Takavci
 * @since 7.1
 */
public enum HealthStatus {

    /**
     * Health status is unknown. This is the initial state before any health checks have been performed by the health check
     * service.
     */
    UNKNOWN,

    /**
     * The endpoint is healthy and operating normally according to health check probes. Health checks are passing and the
     * endpoint is responding as expected.
     */
    HEALTHY,

    /**
     * The endpoint is unhealthy according to health check probes. Health checks are failing, indicating the endpoint is not
     * responding or not meeting health criteria.
     */
    UNHEALTHY

}
