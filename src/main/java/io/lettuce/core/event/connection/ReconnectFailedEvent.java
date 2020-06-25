/*
 * Copyright 2018-2020 the original author or authors.
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

/**
 * Event fired on failed reconnect caused either by I/O issues or during connection initialization.
 *
 * @author Mark Paluch
 * @since 5.2
 */
public class ReconnectFailedEvent extends ConnectionEventSupport {

    private final Throwable cause;

    private final int attempt;

    public ReconnectFailedEvent(SocketAddress local, SocketAddress remote, Throwable cause, int attempt) {
        super(local, remote);
        this.cause = cause;
        this.attempt = attempt;
    }

    /**
     * Returns the {@link Throwable} that describes the reconnect cause.
     *
     * @return the {@link Throwable} that describes the reconnect cause.
     */
    public Throwable getCause() {
        return cause;
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

}
