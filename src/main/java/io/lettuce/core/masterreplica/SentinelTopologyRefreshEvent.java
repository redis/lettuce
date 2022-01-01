/*
 * Copyright 2021-2022 the original author or authors.
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
package io.lettuce.core.masterreplica;

import io.lettuce.core.event.Event;

/**
 * Event triggered when Redis Sentinel indicates a topology change trigger.
 *
 * @author Mark Paluch
 * @since 6.1
 */
class SentinelTopologyRefreshEvent implements Event {

    private final String source;

    private final String message;

    private final long delayMs;

    public SentinelTopologyRefreshEvent(String source, String message, long delayMs) {
        this.source = source;
        this.message = message;
        this.delayMs = delayMs;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public long getDelayMs() {
        return delayMs;
    }

}
