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
