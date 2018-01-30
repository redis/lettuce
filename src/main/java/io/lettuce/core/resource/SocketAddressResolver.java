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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import io.lettuce.core.RedisURI;

/**
 * Resolves a {@link io.lettuce.core.RedisURI} to a {@link java.net.SocketAddress}.
 *
 * @author Mark Paluch
 */
public class SocketAddressResolver {

    /**
     * Resolves a {@link io.lettuce.core.RedisURI} to a {@link java.net.SocketAddress}.
     *
     * @param redisURI must not be {@literal null}
     * @param dnsResolver must not be {@literal null}
     * @return the resolved {@link SocketAddress}
     */
    public static SocketAddress resolve(RedisURI redisURI, DnsResolver dnsResolver) {

        if (redisURI.getSocket() != null) {
            return redisURI.getResolvedAddress();
        }

        try {
            InetAddress[] inetAddress = dnsResolver.resolve(redisURI.getHost());

            if (inetAddress.length == 0) {
                return InetSocketAddress.createUnresolved(redisURI.getHost(), redisURI.getPort());
            }

            return new InetSocketAddress(inetAddress[0], redisURI.getPort());
        } catch (UnknownHostException e) {
            return redisURI.getResolvedAddress();
        }
    }
}
