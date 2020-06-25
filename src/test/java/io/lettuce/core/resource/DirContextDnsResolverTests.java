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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DirContextDnsResolver}.
 *
 * @author Mark Paluch
 */
@Disabled("Tests require an internet connection")
class DirContextDnsResolverTests {

    private DirContextDnsResolver resolver;

    @BeforeEach
    void before() {

        System.getProperties().remove(DirContextDnsResolver.PREFER_IPV4_KEY);
        System.getProperties().remove(DirContextDnsResolver.PREFER_IPV6_KEY);
    }

    @AfterEach
    void tearDown() throws Exception {

        if (resolver != null) {
            resolver.close();
        }
    }

    @Test
    @Disabled("Requires guarding against IPv6 absence")
    void shouldResolveDefault() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThanOrEqualTo(2);
        assertThat(resolved[0]).isInstanceOf(Inet6Address.class);
        assertThat(resolved[0].getHostName()).isEqualTo("google.com");
        assertThat(resolved[resolved.length - 1]).isInstanceOf(Inet4Address.class);
    }

    @Test
    void shouldResolvePreferIpv4WithProperties() throws Exception {

        resolver = new DirContextDnsResolver(true, false, new Properties());

        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThanOrEqualTo(1);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
    }

    @Test
    @Disabled("Requires guarding against IPv6 absence")
    void shouldResolveWithDnsServer() throws Exception {

        resolver = new DirContextDnsResolver(Arrays.asList("[2001:4860:4860::8888]", "8.8.8.8"));

        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(1);
    }

    @Test
    void shouldPreferIpv4() throws Exception {

        System.setProperty(DirContextDnsResolver.PREFER_IPV4_KEY, "true");

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
    }

    @Test
    void shouldPreferIpv4AndNotIpv6() throws Exception {

        System.setProperty(DirContextDnsResolver.PREFER_IPV4_KEY, "true");
        System.setProperty(DirContextDnsResolver.PREFER_IPV6_KEY, "false");

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
    }

    @Test
    @Disabled("Requires guarding against IPv6 absence")
    void shouldPreferIpv6AndNotIpv4() throws Exception {

        System.setProperty(DirContextDnsResolver.PREFER_IPV4_KEY, "false");
        System.setProperty(DirContextDnsResolver.PREFER_IPV6_KEY, "true");

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet6Address.class);
    }

    @Test
    void shouldFailWithUnknownHost() {

        resolver = new DirContextDnsResolver("8.8.8.8");

        assertThatThrownBy(() -> resolver.resolve("unknown-domain-name")).isInstanceOf(UnknownHostException.class);
    }

    @Test
    void shouldResolveCname() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("www.github.io");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(InetAddress.class);
        assertThat(resolved[0].getHostName()).isEqualTo("www.github.io");
    }

    @Test
    void shouldResolveWithoutSubdomain() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("paluch.biz");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(InetAddress.class);
        assertThat(resolved[0].getHostName()).isEqualTo("paluch.biz");

        resolved = resolver.resolve("gmail.com");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(InetAddress.class);
        assertThat(resolved[0].getHostName()).isEqualTo("gmail.com");
    }

    @Test
    void shouldWorkWithIpv4Address() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("127.0.0.1");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
        assertThat(resolved[0].getHostAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldWorkWithIpv6Addresses() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("::1");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet6Address.class);
        assertThat(resolved[0].getHostAddress()).isEqualTo("0:0:0:0:0:0:0:1");

        resolved = resolver.resolve("2a00:1450:4001:816::200e");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet6Address.class);
        assertThat(resolved[0].getHostAddress()).isEqualTo("2a00:1450:4001:816:0:0:0:200e");
    }

}
