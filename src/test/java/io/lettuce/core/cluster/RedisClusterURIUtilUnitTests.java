package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import reactor.test.StepVerifier;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class RedisClusterURIUtilUnitTests {

    @Test
    void testSimpleUri() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis://host:7479"));

        assertThat(redisURIs).hasSize(1);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.getHost()).isEqualTo("host");
        assertThat(host1.getPort()).isEqualTo(7479);
    }

    @Test
    void testMultipleHosts() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis://host1,host2"));

        assertThat(redisURIs).hasSize(2);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);

        RedisURI host2 = redisURIs.get(1);
        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPort()).isEqualTo(6379);
    }

    @Test
    void testMultipleHostsWithPorts() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis://host1:6379,host2:6380"));

        assertThat(redisURIs).hasSize(2);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);

        RedisURI host2 = redisURIs.get(1);
        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPort()).isEqualTo(6380);
    }

    @Test
    void testSslWithPasswordSingleHost() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis+tls://password@host1"));

        assertThat(redisURIs).hasSize(1);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.isSsl()).isTrue();
        assertThat(host1.isStartTls()).isTrue();
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);
        StepVerifier.create(host1.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();
    }

    @Test
    void testSslWithPasswordMultipleHosts() {

        List<RedisURI> redisURIs = RedisClusterURIUtil.toRedisURIs(URI.create("redis+tls://password@host1:6379,host2:6380"));

        assertThat(redisURIs).hasSize(2);

        RedisURI host1 = redisURIs.get(0);
        assertThat(host1.isSsl()).isTrue();
        assertThat(host1.isStartTls()).isTrue();
        assertThat(host1.getHost()).isEqualTo("host1");
        assertThat(host1.getPort()).isEqualTo(6379);
        StepVerifier.create(host1.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();

        RedisURI host2 = redisURIs.get(1);
        assertThat(host2.isSsl()).isTrue();
        assertThat(host2.isStartTls()).isTrue();
        assertThat(host2.getHost()).isEqualTo("host2");
        assertThat(host2.getPort()).isEqualTo(6380);
        StepVerifier.create(host2.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();
    }

}
