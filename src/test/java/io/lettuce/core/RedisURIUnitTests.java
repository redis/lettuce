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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.internal.LettuceSets;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link RedisURI}
 *
 * @author Mark Paluch
 * @author Lei Zhang
 * @author Jacob Halsey
 */
@Tag(UNIT_TEST)
class RedisURIUnitTests {

    @Test
    void equalsTest() {

        RedisURI redisURI1 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI2 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI3 = RedisURI.create("redis://auth@localhost:1231/5");

        assertThat(redisURI1).isEqualTo(redisURI2);
        assertThat(redisURI1.hashCode()).isEqualTo(redisURI2.hashCode());
        assertThat(redisURI1).hasToString("redis://****@localhost:1234/5");

        assertThat(redisURI3).isNotEqualTo(redisURI2);
        assertThat(redisURI3.hashCode()).isNotEqualTo(redisURI2.hashCode());
    }

    @Test
    void setUsage() {

        RedisURI redisURI1 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI2 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI3 = RedisURI.create("redis://auth@localhost:1234/6");

        Set<RedisURI> set = LettuceSets.unmodifiableSet(redisURI1, redisURI2, redisURI3);

        assertThat(set).hasSize(2);
    }

    @Test
    void toStringTest() {

        assertThat(RedisURI.create("redis://auth@localhost:1234/5")).hasToString("redis://****@localhost:1234/5");
        assertThat(RedisURI.create("redis://user:auth@localhost:1234/5")).hasToString("redis://user:****@localhost:1234/5");
        assertThat(RedisURI.create("redis://localhost:1234/5")).hasToString("redis://localhost:1234/5");
    }

    @Test
    void mapUsage() {

        RedisURI redisURI1 = RedisURI.create("redis://auth@localhost:1234/5");
        RedisURI redisURI2 = RedisURI.create("redis://auth@localhost:1234/5");

        Map<RedisURI, String> map = new LinkedHashMap<>();
        map.put(redisURI1, "something");

        assertThat(map.get(redisURI2)).isEqualTo("something");
    }

    @Test
    void simpleUriTest() {
        RedisURI redisURI = RedisURI.create("redis://localhost:6379");
        assertThat(redisURI).hasToString("redis://localhost");
        assertThat(redisURI).hasToString("redis://localhost");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnMalformedUri() {
        assertThatThrownBy(() -> RedisURI.create("localhost")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sslUriTest() {
        RedisURI redisURI = RedisURI.create("redis+ssl://localhost:6379");
        assertThat(redisURI).hasToString("rediss://localhost:6379");

        redisURI = RedisURI.create("redis+ssl://localhost:6379?verifyPeer=ca");
        assertThat(redisURI.getVerifyMode()).isEqualTo(SslVerifyMode.CA);
        assertThat(redisURI).hasToString("rediss://localhost:6379?verifyPeer=CA");
    }

    @Test
    void tlsUriTest() {
        RedisURI redisURI = RedisURI.create("redis+tls://localhost:6379");
        assertThat(redisURI).hasToString("redis+tls://localhost:6379");
    }

    @Test
    void multipleClusterNodesTest() {
        RedisURI redisURI = RedisURI.create("redis+ssl://password@host1:6379,host2:6380");
        assertThat(redisURI).hasToString("rediss://********@host1:6379,host2:6380");
    }

    @Test
    void sentinelEqualsTest() {

        RedisURI redisURI1 = RedisURI.create("redis-sentinel://auth@h1:222,h2,h3:1234/5?sentinelMasterId=masterId");
        RedisURI redisURI2 = RedisURI.create("redis-sentinel://auth@h1:222,h2,h3:1234/5#masterId");
        RedisURI redisURI3 = RedisURI.create("redis-sentinel://auth@h1,h2,h3:1234/5#OtherMasterId");

        assertThat(redisURI1).isEqualTo(redisURI2);
        assertThat(redisURI1.hashCode()).isEqualTo(redisURI2.hashCode());
        assertThat(redisURI1.toString()).contains("h1");

        assertThat(redisURI3).isNotEqualTo(redisURI2);
        assertThat(redisURI3.hashCode()).isNotEqualTo(redisURI2.hashCode());
    }

    @Test
    void sentinelUriTest() {

        RedisURI redisURI = RedisURI.create("redis-sentinel://auth@h1:222,h2,h3:1234/5?sentinelMasterId=masterId");
        assertThat(redisURI.getSentinelMasterId()).isEqualTo("masterId");
        assertThat(redisURI.getSentinelPrimaryId()).isEqualTo("masterId");
        assertThat(redisURI.getSentinels().get(0).getPort()).isEqualTo(222);
        assertThat(redisURI.getSentinels().get(1).getPort()).isEqualTo(RedisURI.DEFAULT_SENTINEL_PORT);
        assertThat(redisURI.getSentinels().get(2).getPort()).isEqualTo(1234);
        assertThat(redisURI.getDatabase()).isEqualTo(5);

        assertThat(redisURI).hasToString("redis-sentinel://****@h1:222,h2,h3:1234/5?sentinelPrimaryId=masterId");
    }

    @Test
    void sentinelSecureUriTest() {

        RedisURI redisURI = RedisURI.create("rediss-sentinel://auth@h1:222,h2,h3:1234/5?sentinelMasterId=masterId");
        assertThat(redisURI.isSsl()).isTrue();

        assertThat(redisURI).hasToString("rediss-sentinel://****@h1:222,h2,h3:1234/5?sentinelPrimaryId=masterId");
    }

    @Test
    void socketEqualsTest() {

        RedisURI redisURI1 = RedisURI.create("redis-socket:///var/tmp/socket");
        RedisURI redisURI2 = RedisURI.create("redis-socket:///var/tmp/socket");
        RedisURI redisURI3 = RedisURI.create("redis-socket:///var/tmp/other-socket?database=2");

        assertThat(redisURI1).isEqualTo(redisURI2);
        assertThat(redisURI1.hashCode()).isEqualTo(redisURI2.hashCode());
        assertThat(redisURI1.toString()).contains("/var/tmp/socket");

        assertThat(redisURI3).isNotEqualTo(redisURI2);
        assertThat(redisURI3.hashCode()).isNotEqualTo(redisURI2.hashCode());
        assertThat(redisURI3).hasToString("redis-socket:///var/tmp/other-socket?database=2");
    }

    @Test
    void socketUriTest() {

        RedisURI redisURI = RedisURI.create("redis-socket:///var/tmp/other-socket?db=2");

        assertThat(redisURI.getDatabase()).isEqualTo(2);
        assertThat(redisURI.getSocket()).isEqualTo("/var/tmp/other-socket");
        assertThat(redisURI).hasToString("redis-socket:///var/tmp/other-socket?database=2");
    }

    @Test
    void socketAltUriTest() {

        RedisURI redisURI = RedisURI.create("redis+socket:///var/tmp/other-socket?db=2");

        assertThat(redisURI.getDatabase()).isEqualTo(2);
        assertThat(redisURI.getSocket()).isEqualTo("/var/tmp/other-socket");
        assertThat(redisURI).hasToString("redis-socket:///var/tmp/other-socket?database=2");
    }

    @Test
    void escapeCharacterParsingTest() throws UnsupportedEncodingException {

        String password = "abc@#d";
        String translatedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.name());

        // redis sentinel
        String uri = "redis-sentinel://" + translatedPassword + "@h1:1234,h2:1234,h3:1234/0?sentinelMasterId=masterId";
        RedisURI redisURI = RedisURI.create(uri);
        assertThat(redisURI.getSentinels().get(0).getHost()).isEqualTo("h1");
        StepVerifier.create(redisURI.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo(password.toCharArray());
        }).verifyComplete();

        // redis standalone
        uri = "redis://" + translatedPassword + "@h1:1234/0";
        redisURI = RedisURI.create(uri);
        assertThat(redisURI.getHost()).isEqualTo("h1");
        StepVerifier.create(redisURI.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo(password.toCharArray());
        }).verifyComplete();
    }

    @Test
    void timeoutParsingTest() {

        checkUriTimeout("redis://auth@localhost:1234/5?timeout=5000", 5000, TimeUnit.MILLISECONDS);
        checkUriTimeout("redis://auth@localhost:1234/5?timeout=5000ms", 5000, TimeUnit.MILLISECONDS);
        checkUriTimeout("redis://auth@localhost:1234/5?timeout=5s", 5, TimeUnit.SECONDS);
        checkUriTimeout("redis://auth@localhost:1234/5?timeout=100us", 100, TimeUnit.MICROSECONDS);
        checkUriTimeout("redis://auth@localhost:1234/5?TIMEOUT=1000000NS", 1000000, TimeUnit.NANOSECONDS);
        checkUriTimeout("redis://auth@localhost:1234/5?timeout=60m", 60, TimeUnit.MINUTES);
        checkUriTimeout("redis://auth@localhost:1234/5?timeout=24h", 24, TimeUnit.HOURS);
        checkUriTimeout("redis://auth@localhost:1234/5?timeout=1d", 1, TimeUnit.DAYS);

        checkUriTimeout("redis://auth@localhost:1234/5?timeout=-1", 0, TimeUnit.MILLISECONDS);

        RedisURI defaultUri = new RedisURI();
        checkUriTimeout("redis://auth@localhost:1234/5?timeout=junk", defaultUri.getTimeout().getSeconds(), TimeUnit.SECONDS);

        assertThat(RedisURI.create("redis://auth@localhost:1234/5?timeout=5000ms"))
                .hasToString("redis://****@localhost:1234/5?timeout=5s");
        assertThat(RedisURI.create("redis://auth@localhost:1234/5?timeout=123ms"))
                .hasToString("redis://****@localhost:1234/5?timeout=123000000ns");
    }

    @Test
    void queryStringDecodingTest() {
        String timeout = "%74%69%6D%65%6F%75%74";
        String eq = "%3d";
        String s = "%73";
        checkUriTimeout("redis://auth@localhost:1234/5?" + timeout + eq + "5" + s, 5, TimeUnit.SECONDS);
    }

    @Test
    void timeoutParsingWithJunkParamTest() {
        RedisURI redisURI1 = RedisURI.create("redis-sentinel://auth@localhost:1234/5?timeout=5s;junkparam=#master-instance");
        assertThat(redisURI1.getTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(redisURI1.getSentinelMasterId()).isEqualTo("master-instance");
    }

    private RedisURI checkUriTimeout(String uri, long expectedTimeout, TimeUnit expectedUnit) {
        RedisURI redisURI = RedisURI.create(uri);
        assertThat(expectedUnit.convert(redisURI.getTimeout().toNanos(), TimeUnit.NANOSECONDS)).isEqualTo(expectedTimeout);
        return redisURI;
    }

    @Test
    void databaseParsingTest() {
        RedisURI redisURI = RedisURI.create("redis://auth@localhost:1234/?database=21");
        assertThat(redisURI.getDatabase()).isEqualTo(21);

        assertThat(redisURI).hasToString("redis://****@localhost:1234/21");
    }

    @Test
    void clientNameParsingTest() {
        RedisURI redisURI = RedisURI.create("redis://auth@localhost:1234/?clientName=hello");
        assertThat(redisURI.getClientName()).isEqualTo("hello");

        assertThat(redisURI).hasToString("redis://****@localhost:1234?clientName=hello");
    }

    @Test
    void libraryParsingTest() {
        RedisURI redisURI = RedisURI.create("redis://auth@localhost:1234/?libraryName=lib&libraryVersion=1.0");
        assertThat(redisURI.getLibraryName()).isEqualTo("lib");
        assertThat(redisURI.getLibraryVersion()).isEqualTo("1.0");

        assertThat(redisURI).hasToString("redis://****@localhost:1234?libraryName=lib&libraryVersion=1.0");
    }

    @Test
    void parsingWithInvalidValuesTest() {
        RedisURI redisURI = RedisURI
                .create("redis://@host:1234/?database=AAA&database=&timeout=&timeout=XYZ&sentinelMasterId=");
        assertThat(redisURI.getDatabase()).isEqualTo(0);
        assertThat(redisURI.getSentinelMasterId()).isNull();

        assertThat(redisURI).hasToString("redis://host:1234");
    }

    @Test
    void shouldApplySslSettings() {

        RedisURI source = new RedisURI();
        source.setStartTls(true);

        RedisURI target = new RedisURI();

        target.applySsl(source);

        assertThat(target.isSsl()).isFalse();
        assertThat(target.isVerifyPeer()).isTrue();
        assertThat(target.isStartTls()).isTrue();

        source.setVerifyPeer(false);
        source.setStartTls(true);

        target.applySsl(source);

        assertThat(target.isSsl()).isFalse();
        assertThat(target.isVerifyPeer()).isFalse();
        assertThat(target.isStartTls()).isTrue();

        source.setSsl(true);
        source.setVerifyPeer(false);
        source.setStartTls(true);

        target.applySsl(source);

        assertThat(target.isSsl()).isTrue();
        assertThat(target.isVerifyPeer()).isFalse();
        assertThat(target.isStartTls()).isTrue();

        source.setVerifyPeer(SslVerifyMode.CA);
        target.applySsl(source);
        assertThat(target.getVerifyMode()).isEqualTo(SslVerifyMode.CA);
    }

    @Test
    void shouldApplyAuthentication() {

        RedisURI source = new RedisURI();
        source.setAuthentication("foo", "bar".toCharArray());

        RedisURI target = new RedisURI();
        target.applyAuthentication(source);

        StepVerifier.create(target.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isEqualTo("foo");
            assertThat(credentials.getPassword()).isEqualTo("bar".toCharArray());
        }).verifyComplete();

        source.setCredentialsProvider(new StaticCredentialsProvider(null, "bar".toCharArray()));
        target.applyAuthentication(source);

        StepVerifier.create(target.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isNull();
            assertThat(credentials.getPassword()).isEqualTo("bar".toCharArray());
        }).verifyComplete();

        RedisCredentialsProvider provider = () -> Mono
                .just(RedisCredentials.just("suppliedUsername", "suppliedPassword".toCharArray()));

        RedisURI sourceCp = new RedisURI();
        sourceCp.setCredentialsProvider(provider);

        RedisURI targetCp = new RedisURI();
        targetCp.applyAuthentication(sourceCp);

        StepVerifier.create(targetCp.getCredentialsProvider().resolveCredentials()).assertNext(credentials -> {
            assertThat(credentials.getUsername()).isEqualTo("suppliedUsername");
            assertThat(credentials.getPassword()).isEqualTo("suppliedPassword".toCharArray());
        }).verifyComplete();
        assertThat(sourceCp.getCredentialsProvider()).isEqualTo(targetCp.getCredentialsProvider());
    }

    @Test
    void setDriverInfoSingleDriver() {
        RedisURI redisURI = RedisURI.create("redis://localhost");
        DriverInfo driverInfo = DriverInfo.builder().name("lettuce").addUpstreamDriver("spring-data-redis", "3.2.0").build();
        redisURI.setDriverInfo(driverInfo);

        assertThat(redisURI.getLibraryName()).isEqualTo("lettuce(spring-data-redis_v3.2.0)");
    }

    @Test
    void setDriverInfoMultipleDrivers() {
        RedisURI redisURI = RedisURI.create("redis://localhost");
        DriverInfo driverInfo = DriverInfo.builder().name("lettuce").addUpstreamDriver("spring-data-redis", "3.2.0")
                .addUpstreamDriver("spring-boot", "3.3.0").build();
        redisURI.setDriverInfo(driverInfo);

        // Most recently added driver should appear first (prepend behavior)
        assertThat(redisURI.getLibraryName()).isEqualTo("lettuce(spring-boot_v3.3.0;spring-data-redis_v3.2.0)");
    }

    @Test
    void driverInfoChaining() {
        RedisURI redisURI = RedisURI.create("redis://localhost");

        // Spring Data Redis adds itself
        DriverInfo springDataInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0").build();
        redisURI.setDriverInfo(springDataInfo);

        // Spring Session chains onto it
        DriverInfo existing = redisURI.getDriverInfo();
        DriverInfo withSession = DriverInfo.builder(existing).addUpstreamDriver("spring-session", "3.3.0").build();
        redisURI.setDriverInfo(withSession);

        assertThat(redisURI.getLibraryName()).isEqualTo("Lettuce(spring-session_v3.3.0;spring-data-redis_v3.2.0)");
    }

    @Test
    void driverInfoBuilderInvalidNameShouldFail() {
        // Name starting with a number
        assertThatThrownBy(() -> DriverInfo.builder().addUpstreamDriver("123driver", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
        // Name with @ symbol
        assertThatThrownBy(() -> DriverInfo.builder().addUpstreamDriver("driver@name", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
        // Name with dots (not allowed in Maven artifactId)
        assertThatThrownBy(() -> DriverInfo.builder().addUpstreamDriver("com.example.driver", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
        // Name starting with hyphen
        assertThatThrownBy(() -> DriverInfo.builder().addUpstreamDriver("-spring-data", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
        // Name with uppercase letters
        assertThatThrownBy(() -> DriverInfo.builder().addUpstreamDriver("Spring-Data-Redis", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
        // Name with spaces
        assertThatThrownBy(() -> DriverInfo.builder().addUpstreamDriver("spring data", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void driverInfoBuilderValidFormats() {
        // Valid Maven artifactId formats (lowercase letters, digits, hyphens)
        DriverInfo.builder().addUpstreamDriver("spring-data-redis", "1.0.0").addUpstreamDriver("lettuce-core", "2.0.0")
                .addUpstreamDriver("commons-math", "3.0.0").addUpstreamDriver("guava", "4.0.0")
                .addUpstreamDriver("jedis", "5.0.0").build();

        // Valid semantic versions with pre-release and build metadata
        DriverInfo.builder().addUpstreamDriver("example-lib", "1.0.0-alpha").addUpstreamDriver("example-lib", "1.0.0-beta.1")
                .addUpstreamDriver("example-lib", "1.0.0+build.123").addUpstreamDriver("example-lib", "1.0.0-rc.1+build.456")
                .build();
    }

    @Test
    void defaultLibraryName() {
        // Requirement 4: Default behavior - library name defaults to "Lettuce"
        RedisURI redisURI = RedisURI.create("redis://localhost");
        assertThat(redisURI.getLibraryName()).isEqualTo("Lettuce");
    }

    @Test
    void getLibraryNameWithoutUpstreamDrivers() {
        RedisURI redisURI = RedisURI.create("redis://localhost");
        redisURI.setLibraryName("lettuce");

        // Without upstream drivers, should return just the library name
        assertThat(redisURI.getLibraryName()).isEqualTo("lettuce");
    }

    @Test
    void setLibraryNameOverridesDriverInfo() {
        RedisURI redisURI = RedisURI.create("redis://localhost");

        // Set driver info first
        DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0").build();
        redisURI.setDriverInfo(driverInfo);

        // Then override with setLibraryName
        redisURI.setLibraryName("my-custom-lib");

        // setLibraryName should completely replace (no drivers)
        assertThat(redisURI.getLibraryName()).isEqualTo("my-custom-lib");
    }

}
