/*
 * Copyright 2018-2022 the original author or authors.
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
import jdk.jfr.Timespan;

/**
 * Flight recorder event variant of {@link ReconnectAttemptEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reconnect attempt")
@StackTrace(false)
public class JfrReconnectAttemptEvent extends Event {

    private final String redisUri;

    private final String epId;

    private final String remote;

    private final int attempt;

    @Timespan
    private final long delay;

    public JfrReconnectAttemptEvent(ReconnectAttemptEvent event) {

        this.epId = event.getEpId();
        this.remote = event.remoteAddress().toString();
        this.redisUri = event.getRedisUri();
        this.attempt = event.getAttempt();
        this.delay = event.getDelay().toNanos();
    }

}
