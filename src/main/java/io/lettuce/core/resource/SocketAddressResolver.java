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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Resolves a {@link io.lettuce.core.RedisURI} to a {@link java.net.SocketAddress}.
 *
 * @author Mark Paluch
 * @see MappingSocketAddressResolver
 */
public class SocketAddressResolver {

    private final DnsResolver dnsResolver;

    /**
     * Create a new {@link SocketAddressResolver} given {@link DnsResolver}.
     *
     * @param dnsResolver must not be {@code null}.
     * @since 5.1
     */
    protected SocketAddressResolver(DnsResolver dnsResolver) {

        LettuceAssert.notNull(dnsResolver, "DnsResolver must not be null");

        this.dnsResolver = dnsResolver;
    }

    /**
     * Create a new {@link SocketAddressResolver} given {@link DnsResolver}.
     *
     * @param dnsResolver must not be {@code null}.
     * @return the {@link SocketAddressResolver}.
     * @since 5.1
     */
    public static SocketAddressResolver create(DnsResolver dnsResolver) {
        return new SocketAddressResolver(dnsResolver);
    }

    /**
     * Resolve a {@link RedisURI} to a {@link SocketAddress}.
     *
     * @param redisURI must not be {@code null}.
     * @return the resolved {@link SocketAddress}.
     * @since 5.1
     */
    public SocketAddress resolve(RedisURI redisURI) {

        LettuceAssert.notNull(redisURI, "RedisURI must not be null");

        return resolve(redisURI, dnsResolver);
    }

    /**
     * Resolves a {@link io.lettuce.core.RedisURI} to a {@link java.net.SocketAddress}.
     *
     * @param redisURI must not be {@code null}.
     * @param dnsResolver must not be {@code null}.
     * @return the resolved {@link SocketAddress}.
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
