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

import java.net.SocketAddress;
import java.time.Duration;

/**
 * Event fired on reconnect attempts.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class ReconnectAttemptEvent extends ConnectionEventSupport {

    private final int attempt;

    private final Duration delay;

    public ReconnectAttemptEvent(String redisUri, String epId, SocketAddress local, SocketAddress remote, int attempt,
            Duration delay) {
        super(redisUri, epId, null, local, remote);
        this.attempt = attempt;
        this.delay = delay;
    }

    public ReconnectAttemptEvent(SocketAddress local, SocketAddress remote, int attempt) {
        super(local, remote);
        this.attempt = attempt;
        this.delay = Duration.ZERO;
    }

    /**
     * Returns the reconnect attempt counter for the connection. Zero-based counter, {@code 0} represents the first attempt. The
     * counter is reset upon successful reconnect.
     *
     * @return the reconnect attempt counter for the connection. Zero-based counter, {@code 0} represents the first attempt.
     */
    public int getAttempt() {
        return attempt;
    }

    public Duration getDelay() {
        return delay;
    }

}
