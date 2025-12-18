/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link RedisURI.Builder}.
 *
 * @author Mark Paluch
 * @author Guy Korland
 */
@Tag(UNIT_TEST)
class RedisURIBuilderUnitTests {

    @Test
    void sentinel() {
        RedisURI result = RedisURI.Builder.sentinel("localhost").withTimeout(Duration.ofHours(2)).build();
        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getTimeout()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void sentinelWithHostShouldFail() {
        assertThatThrownBy(() -> RedisURI.Builder.sentinel("localhost").withHost("localhost"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sentinelWithPort() {
        RedisURI result = RedisURI.Builder.sentinel("localhost", 1).withTimeout(Duration.ofHours(2)).build();
        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getTimeout()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void shouldFailIfBuilderIsEmpty() {
        assertThatThrownBy(() -> RedisURI.builder().build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void redisWithHostAndPort() {
        RedisURI result = RedisURI.builder().withHost("localhost").withPort(1234).build();

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(1234);
    }

    @Test
    void redisWithPort() {
        RedisURI result = RedisURI.Builder.redis("localhost").withPort(1234).build();

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(1234);
    }

    @Test
    void redisWithClientName() {
        RedisURI result = RedisURI.Builder.redis("localhost").withClientName("hello").build();

        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getClientName()).isEqualTo("hello");
    }

    @Test
    void redisWithLibrary() {
        RedisURI result = RedisURI.Builder.redis("localhost").withLibraryName("name").withLibraryVersion("1.foo").build();

        assertThat(result.getLibraryName()).isEqualTo("name");
        assertThat(result.getLibraryVersion()).isEqualTo("1.foo");
    }

    @Test
    void redisWithDriverInfo() {
        DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0").build();
        RedisURI result = RedisURI.Builder.redis("localhost").withDriverInfo(driverInfo).build();

        assertThat(result.getLibraryName()).isEqualTo("Lettuce(spring-data-redis_v3.2.0)");
    }

    @Test
    void redisWithDriverInfoMultipleDrivers() {
        DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0")
                .addUpstreamDriver("spring-boot", "3.3.0").build();
        RedisURI result = RedisURI.Builder.redis("localhost").withDriverInfo(driverInfo).build();

        // Most recently added driver should appear first (prepend behavior)
        assertThat(result.getLibraryName()).isEqualTo("Lettuce(spring-boot_v3.3.0;spring-data-redis_v3.2.0)");
    }

    @Test
    void redisWithDriverInfoCustomName() {
        DriverInfo driverInfo = DriverInfo.builder().name("my-custom-lib").addUpstreamDriver("spring-data-redis", "3.2.0")
                .build();
        RedisURI result = RedisURI.Builder.redis("localhost").withDriverInfo(driverInfo).build();

        assertThat(result.getLibraryName()).isEqualTo("my-custom-lib(spring-data-redis_v3.2.0)");
    }

    @Test
    void redisWithDriverInfoNullShouldFail() {
        assertThatThrownBy(() -> RedisURI.Builder.redis("localhost").withDriverInfo(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redisWithLibraryNameThenDriverInfo() {
        // Last call wins: withDriverInfo overwrites previous withLibraryName
        DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0").build();
        RedisURI result = RedisURI.Builder.redis("localhost").withLibraryName("my-lib").withDriverInfo(driverInfo).build();

        assertThat(result.getLibraryName()).isEqualTo("Lettuce(spring-data-redis_v3.2.0)");
    }

    @Test
    void redisWithDriverInfoThenLibraryName() {
        // Last call wins: withLibraryName overwrites previous withDriverInfo
        DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0").build();
        RedisURI result = RedisURI.Builder.redis("localhost").withDriverInfo(driverInfo).withLibraryName("my-lib").build();

        assertThat(result.getLibraryName()).isEqualTo("my-lib");
    }

    @Test
    void redisHostAndPortWithInvalidPort() {
        assertThatThrownBy(() -> RedisURI.Builder.redis("localhost", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redisWithInvalidPort() {
        assertThatThrownBy(() -> RedisURI.Builder.redis("localhost").withPort(65536))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redisFromUrl() {
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS + "://password@localhost/21");

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        StepVerifier.create(result.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();
        assertThat(result.getDatabase()).isEqualTo(21);
        assertThat(result.isSsl()).isFalse();
    }

    @Test
    void redisFromUrlNoPassword() {
        RedisURI redisURI = RedisURI.create("redis://localhost:1234/5");
        StepVerifier.create(redisURI.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isNull();
        }).verifyComplete();

        redisURI = RedisURI.create("redis://h:@localhost.com:14589");
        StepVerifier.create(redisURI.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isNull();
        }).verifyComplete();
    }

    @Test
    void redisFromUrlPassword() {
        RedisURI redisURI = RedisURI.create("redis://h:password@localhost.com:14589");

        StepVerifier.create(redisURI.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isEqualTo("h");
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();
    }

    @Test
    void redisWithSSL() {
        RedisURI result = RedisURI.Builder.redis("localhost").withSsl(true).withStartTls(true).build();

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.isSsl()).isTrue();
        assertThat(result.isStartTls()).isTrue();
    }

    @Test
    void redisWithSSLFromUri() {
        RedisURI template = RedisURI.Builder.redis("localhost").withSsl(true).withVerifyPeer(SslVerifyMode.CA)
                .withStartTls(true).build();
        RedisURI result = RedisURI.builder().withHost("localhost").withSsl(template).build();

        assertThat(result.isSsl()).isTrue();
        assertThat(result.getVerifyMode()).isEqualTo(SslVerifyMode.CA);
        assertThat(result.isStartTls()).isTrue();
    }

    @Test
    void redisSslFromUrl() {
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SECURE + "://:password@localhost/1");

        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isEqualTo("localhost");
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        StepVerifier.create(result.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();

        assertThat(result.isSsl()).isTrue();
    }

    @Test
    void redisSentinelFromUrl() {
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SENTINEL + "://password@localhost/1#master");

        assertThat(result.getSentinels()).hasSize(1);
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(result.getSentinelMasterId()).isEqualTo("master");
        assertThat(result.toString()).contains("master");
        StepVerifier.create(result.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();

        result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SENTINEL + "://password@host1:1,host2:3423,host3/1#master");

        assertThat(result.getSentinels()).hasSize(3);
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(result.getSentinelMasterId()).isEqualTo("master");
        StepVerifier.create(result.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();

        RedisURI sentinel1 = result.getSentinels().get(0);
        assertThat(sentinel1.getPort()).isEqualTo(1);
        assertThat(sentinel1.getHost()).isEqualTo("host1");

        RedisURI sentinel2 = result.getSentinels().get(1);
        assertThat(sentinel2.getPort()).isEqualTo(3423);
        assertThat(sentinel2.getHost()).isEqualTo("host2");

        RedisURI sentinel3 = result.getSentinels().get(2);
        assertThat(sentinel3.getPort()).isEqualTo(RedisURI.DEFAULT_SENTINEL_PORT);
        assertThat(sentinel3.getHost()).isEqualTo("host3");
    }

    @Test
    void withAuthenticatedSentinel() {

        RedisURI result = RedisURI.Builder.sentinel("host", 1234, "master", "foo").build();

        RedisURI sentinel = result.getSentinels().get(0);
        StepVerifier.create(sentinel.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("foo".toCharArray());
        }).verifyComplete();
    }

    @Test
    void withTlsSentinel() {

        RedisURI result = RedisURI.Builder.sentinel("host", 1234, "master", "foo").withSsl(true).withStartTls(true)
                .withVerifyPeer(false).build();

        RedisURI sentinel = result.getSentinels().get(0);
        assertThat(sentinel.isSsl()).isTrue();
        assertThat(sentinel.isStartTls()).isTrue();
        assertThat(sentinel.isVerifyPeer()).isFalse();

        StepVerifier.create(sentinel.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("foo".toCharArray());
        }).verifyComplete();
    }

    @Test
    void withAuthenticatedSentinelUri() {

        RedisURI sentinel = new RedisURI("host", 1234, Duration.ZERO);
        sentinel.setAuthentication("bar".toCharArray());
        RedisURI result = RedisURI.Builder.sentinel("host", 1234, "master").withSentinel(sentinel).build();

        StepVerifier.create(result.getSentinels().get(0).getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertThat(credentials.getUsername()).isNull();
                    assertThat(credentials.getPassword()).isNull();
                }).verifyComplete();

        StepVerifier.create(result.getSentinels().get(1).getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertThat(credentials.getUsername()).isNull();
                    assertThat(credentials.getPassword()).isEqualTo("bar".toCharArray());
                }).verifyComplete();
    }

    @Test
    void withAuthenticatedSentinelWithSentinel() {

        RedisURI result = RedisURI.Builder.sentinel("host", 1234, "master", "foo").withSentinel("bar").build();

        StepVerifier.create(result.getSentinels().get(0).getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertThat(credentials.getUsername()).isNull();
                    assertThat(credentials.getPassword()).isEqualTo("foo".toCharArray());
                }).verifyComplete();

        StepVerifier.create(result.getSentinels().get(1).getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertThat(credentials.getUsername()).isNull();
                    assertThat(credentials.getPassword()).isNull();
                }).verifyComplete();

        result = RedisURI.Builder.sentinel("host", 1234, "master").withPassword("foo".toCharArray())
                .withSentinel("bar", 1234, "baz").build();

        StepVerifier.create(result.getSentinels().get(0).getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertThat(credentials.getUsername()).isNull();
                    assertThat(credentials.getPassword()).isNull();
                }).verifyComplete();

        StepVerifier.create(result.getSentinels().get(1).getCredentialsProvider().resolveCredentials())
                .assertNext(credentials -> {
                    assertThat(credentials.getUsername()).isNull();
                    assertThat(credentials.getPassword()).isEqualTo("baz".toCharArray());
                }).verifyComplete();
    }

    @Test
    void redisSentinelWithInvalidPort() {
        assertThatThrownBy(() -> RedisURI.Builder.sentinel("a", 65536)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redisSentinelWithMasterIdAndInvalidPort() {
        assertThatThrownBy(() -> RedisURI.Builder.sentinel("a", 65536, "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redisSentinelWithNullMasterId() {
        assertThatThrownBy(() -> RedisURI.Builder.sentinel("a", 1, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidScheme() {
        assertThatThrownBy(() -> RedisURI.create("http://www.web.de")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void redisSocket() throws IOException {
        File file = new File("work/socket-6479").getCanonicalFile();
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());

        assertThat(result.getSocket()).isEqualTo(file.getCanonicalPath());
        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(result.isSsl()).isFalse();

        StepVerifier.create(result.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isNull();
        }).verifyComplete();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void redisSocketWithPassword() throws IOException {
        File file = new File("work/socket-6479").getCanonicalFile();
        RedisURI result = RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://password@" + file.getCanonicalPath());

        assertThat(result.getSocket()).isEqualTo(file.getCanonicalPath());
        assertThat(result.getSentinels()).isEmpty();
        assertThat(result.getHost()).isNull();
        assertThat(result.getPort()).isEqualTo(RedisURI.DEFAULT_REDIS_PORT);
        assertThat(result.isSsl()).isFalse();

        StepVerifier.create(result.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("password".toCharArray());
        }).verifyComplete();
    }

    @Test
    void shouldApplySslSettings() {

        RedisURI source = new RedisURI();
        source.setSsl(true);
        source.setVerifyPeer(false);
        source.setStartTls(true);

        RedisURI target = RedisURI.builder().withHost("localhost").withSsl(source).build();

        assertThat(target.isSsl()).isTrue();
        assertThat(target.isVerifyPeer()).isFalse();
        assertThat(target.isStartTls()).isTrue();
    }

    @Test
    void shouldInitializeBuilder() {

        RedisURI source = new RedisURI();
        source.setHost("localhost");
        source.setPort(1234);
        source.setTimeout(Duration.ofSeconds(2));
        source.setClientName("foo");
        source.setLibraryName("lib");
        source.setLibraryVersion("libver");
        source.setAuthentication("foo", "bar");
        source.setDatabase(4);
        source.setSsl(true);
        source.setVerifyPeer(false);
        source.setStartTls(true);

        RedisURI target = RedisURI.builder(source).build();
        target.setAuthentication("baz");

        assertThat(target.getHost()).isEqualTo(source.getHost());
        assertThat(target.getPort()).isEqualTo(source.getPort());
        assertThat(target.getTimeout()).isEqualTo(source.getTimeout());
        assertThat(target.getClientName()).isEqualTo(source.getClientName());
        assertThat(target.getLibraryName()).isEqualTo(source.getLibraryName());
        assertThat(target.getLibraryVersion()).isEqualTo(source.getLibraryVersion());
        assertThat(target.getSocket()).isEqualTo(source.getSocket());
        assertThat(target.getDatabase()).isEqualTo(source.getDatabase());
        assertThat(target.isStartTls()).isEqualTo(source.isStartTls());
        assertThat(target.isSsl()).isEqualTo(source.isSsl());
        assertThat(target.isVerifyPeer()).isEqualTo(source.isVerifyPeer());

        StepVerifier.create(target.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("baz".toCharArray());
        }).verifyComplete();
    }

    @Test
    void shouldInitializeBuilderUsingSocket() {

        RedisURI source = new RedisURI();
        source.setSocket("localhost");

        RedisURI target = RedisURI.builder(source).build();

        assertThat(target.getSocket()).isEqualTo(source.getSocket());
    }

}
