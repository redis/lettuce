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
package io.lettuce.core.event.connection;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * Flight recorder event variant of {@link ConnectionDeactivatedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Connection Deactivated")
@StackTrace(false)
class JfrConnectionDeactivatedEvent extends Event {

    private final String redisUri;

    private final String epId;

    private final String channelId;

    private final String local;

    private final String remote;

    public JfrConnectionDeactivatedEvent(ConnectionEventSupport event) {
        this.redisUri = event.getRedisUri();
        this.epId = event.getChannelId();
        this.channelId = event.getChannelId();
        this.local = event.localAddress().toString();
        this.remote = event.remoteAddress().toString();
    }

}
