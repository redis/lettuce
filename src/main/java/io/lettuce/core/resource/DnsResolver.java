/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.resource;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Users may implement this interface to override the normal DNS lookup offered by the OS.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public interface DnsResolver {

    /**
     * Returns the IP address for the specified host name.
     *
     * @param host the hostname, must not be empty or {@literal null}.
     * @return array of one or more {@link InetAddress adresses}. An empty array indicates that DNS resolution is not supported
     *         by this {@link DnsResolver} and should happen by netty, see
     *         {@link java.net.InetSocketAddress#createUnresolved(String, int)}.
     * @throws UnknownHostException if the given host is not recognized or the associated IP address cannot be used to build an
     *         {@link InetAddress} instance
     */
    InetAddress[] resolve(String host) throws UnknownHostException;
}
