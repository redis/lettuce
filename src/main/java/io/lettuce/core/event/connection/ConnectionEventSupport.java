/*
 * Copyright 2011-2020 the original author or authors.
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

import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 3.4
 */
abstract class ConnectionEventSupport implements ConnectionEvent {

    private final SocketAddress local;

    private final SocketAddress remote;

    ConnectionEventSupport(SocketAddress local, SocketAddress remote) {
        LettuceAssert.notNull(local, "Local must not be null");
        LettuceAssert.notNull(remote, "Remote must not be null");

        this.local = local;
        this.remote = remote;
    }

    /**
     * Returns the local address.
     *
     * @return the local address
     */
    public SocketAddress localAddress() {
        return local;
    }

    /**
     * Returns the remote address.
     *
     * @return the remote address
     */
    public SocketAddress remoteAddress() {
        return remote;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append(local);
        sb.append(" -> ").append(remote);
        sb.append(']');
        return sb.toString();
    }

}
