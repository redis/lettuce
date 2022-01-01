/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Event for a created connection object. Creating a connection is the first step in establishing a Redis connection.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class ConnectionCreatedEvent implements Event {

    private final String redisUri;

    private final String epId;

    public ConnectionCreatedEvent(String redisUri, String epId) {
        this.redisUri = redisUri;
        this.epId = epId;
    }

    public String getRedisUri() {
        return redisUri;
    }

    public String getEpId() {
        return epId;
    }

}
