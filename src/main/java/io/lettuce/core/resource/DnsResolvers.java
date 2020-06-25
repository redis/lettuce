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
package io.lettuce.core.resource;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Predefined DNS resolvers.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public enum DnsResolvers implements DnsResolver {

    /**
     * Java VM default resolver.
     */
    JVM_DEFAULT {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return InetAddress.getAllByName(host);
        }

    },

    /**
     * Non-resolving {@link DnsResolver}. Returns an empty {@link InetAddress} to indicate an unresolved address.
     *
     * @see java.net.InetSocketAddress#createUnresolved(String, int)
     * @since 4.4
     */
    UNRESOLVED {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return new InetAddress[0];
        }

    };

}
