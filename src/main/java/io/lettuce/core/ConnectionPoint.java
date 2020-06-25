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
package io.lettuce.core;

/**
 * Interface for a connection point described with a host and port or socket.
 *
 * @author Mark Paluch
 */
public interface ConnectionPoint {

    /**
     * Returns the host that should represent the hostname or IPv4/IPv6 literal.
     *
     * @return the hostname/IP address.
     */
    String getHost();

    /**
     * Get the current port number.
     *
     * @return the port number.
     */
    int getPort();

    /**
     * Get the socket path.
     *
     * @return path to a Unix Domain Socket.
     */
    String getSocket();

}
