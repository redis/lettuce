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

import java.io.PrintWriter;
import java.io.StringWriter;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Flight recorder event variant of {@link ReconnectFailedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reconnect attempt failed")
public class JfrReconnectFailedEvent extends Event {

    private final String redisUri;

    private final String epId;

    private final String remote;

    private final int attempt;

    private final String cause;

    public JfrReconnectFailedEvent(ReconnectFailedEvent event) {

        this.redisUri = event.getRedisUri();
        this.epId = event.getEpId();
        this.remote = event.remoteAddress().toString();

        StringWriter writer = new StringWriter();
        event.getCause().printStackTrace(new PrintWriter(writer));
        this.cause = writer.getBuffer().toString();
        this.attempt = event.getAttempt();
    }

}
