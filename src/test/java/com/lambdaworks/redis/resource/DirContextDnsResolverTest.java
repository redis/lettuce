package com.lambdaworks.redis.resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class DirContextDnsResolverTest {

    DirContextDnsResolver resolver;

    @Before
    public void before() throws Exception {

        System.getProperties().remove(DirContextDnsResolver.PREFER_IPV4_KEY);
        System.getProperties().remove(DirContextDnsResolver.PREFER_IPV6_KEY);
    }

    @After
    public void tearDown() throws Exception {

        if (resolver != null) {
            resolver.close();
        }
    }

    @Test
    public void shouldResolveDefault() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(1);
        assertThat(resolved[0]).isInstanceOf(Inet6Address.class);
        assertThat(resolved[0].getHostName()).isEqualTo("google.com");
        assertThat(resolved[resolved.length - 1]).isInstanceOf(Inet4Address.class);
    }

    @Test
    public void shouldResolvePreferIpv4WithProperties() throws Exception {

        resolver = new DirContextDnsResolver(true, false, new Properties());

        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(1);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
    }

    @Test
    public void shouldResolveWithDnsServer() throws Exception {

        resolver = new DirContextDnsResolver("8.8.8.8");

        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(1);
    }

    @Test
    public void shouldPreferIpv4() throws Exception {

        System.setProperty(DirContextDnsResolver.PREFER_IPV4_KEY, "true");

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
    }

    @Test
    public void shouldPreferIpv4AndNotIpv6() throws Exception {

        System.setProperty(DirContextDnsResolver.PREFER_IPV4_KEY, "true");
        System.setProperty(DirContextDnsResolver.PREFER_IPV6_KEY, "false");

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
    }

    @Test
    public void shouldPreferIpv6AndNotIpv4() throws Exception {

        System.setProperty(DirContextDnsResolver.PREFER_IPV4_KEY, "false");
        System.setProperty(DirContextDnsResolver.PREFER_IPV6_KEY, "true");

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("google.com");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet6Address.class);
    }

    @Test(expected = UnknownHostException.class)
    public void shouldFailWithUnknownHost() throws Exception {

        resolver = new DirContextDnsResolver(Arrays.asList("8.8.8.8"));

        resolver.resolve("unknown-domain-name");
    }

    @Test
    public void shouldResolveCname() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("www.github.io");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(InetAddress.class);
        assertThat(resolved[0].getHostName()).isEqualTo("www.github.io");
    }

    @Test
    public void shouldResolveWithoutSubdomain() throws Exception {

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
    public void shouldWorkWithIpv4Address() throws Exception {

        resolver = new DirContextDnsResolver();
        InetAddress[] resolved = resolver.resolve("127.0.0.1");

        assertThat(resolved.length).isGreaterThan(0);
        assertThat(resolved[0]).isInstanceOf(Inet4Address.class);
        assertThat(resolved[0].getHostAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    public void shouldWorkWithIpv6Addresses() throws Exception {

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