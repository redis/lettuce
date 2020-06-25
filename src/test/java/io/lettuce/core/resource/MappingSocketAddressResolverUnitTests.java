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
package io.lettuce.core.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.HostAndPort;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MappingSocketAddressResolverUnitTests {

    @Mock
    DnsResolver dnsResolver;

    @BeforeEach
    void before() throws UnknownHostException {
        when(dnsResolver.resolve(anyString())).thenReturn(new InetAddress[0]);
    }

    @Test
    void shouldPassThruHostAndPort() {

        RedisURI localhost = RedisURI.create("localhost", RedisURI.DEFAULT_REDIS_PORT);
        MappingSocketAddressResolver resolver = MappingSocketAddressResolver.create(dnsResolver, Function.identity());

        InetSocketAddress resolve = (InetSocketAddress) resolver.resolve(localhost);

        assertThat(resolve.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(resolve.getHostString()).isEqualTo("localhost");
    }

    @Test
    void shouldMapHostAndPort() {

        RedisURI localhost = RedisURI.create("localhost", RedisURI.DEFAULT_REDIS_PORT);
        MappingSocketAddressResolver resolver = MappingSocketAddressResolver.create(dnsResolver,
                it -> HostAndPort.of(it.getHostText() + "-foo", it.getPort() + 100));

        InetSocketAddress resolve = (InetSocketAddress) resolver.resolve(localhost);

        assertThat(resolve.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT + 100);
        assertThat(resolve.getHostString()).isEqualTo("localhost-foo");
    }

}
