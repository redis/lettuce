/*
 * Copyright 2019 the original author or authors.
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
package io.lettuce.core.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for Sentinel usage.
 *
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class SentinelSslIntegrationTests extends TestSupport {

    private final ClientResources clientResources;

    @Inject
    SentinelSslIntegrationTests(ClientResources clientResources) {
        this.clientResources = clientResources.mutate()
                .socketAddressResolver(MappingSocketAddressResolver.create(DnsResolver.jvmDefault(), hostAndPort -> {

                    return HostAndPort.of(hostAndPort.getHostText(), hostAndPort.getPort() + 443);
                })).build();
    }

    @Test
    void shouldConnectSentinelDirectly() {

        RedisURI redisURI = RedisURI.create("rediss://" + TestSettings.host() + ":26379");
        redisURI.setVerifyPeer(false);

        RedisClient client = RedisClient.create(clientResources);
        StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(redisURI);

        assertThat(connection.sync().getMasterAddrByName("mymaster")).isNotNull();

        connection.close();
        FastShutdown.shutdown(client);
    }

    @Test
    void shouldConnectToMasterUsingSentinel() {

        RedisURI redisURI = RedisURI.create("rediss-sentinel://" + TestSettings.host() + ":26379?sentinelMasterId=mymaster");
        redisURI.setVerifyPeer(false);

        RedisClient client = RedisClient.create(clientResources);
        StatefulRedisConnection<String, String> connection = client.connect(redisURI);

        assertThat(connection.sync().ping()).isNotNull();

        connection.close();
        FastShutdown.shutdown(client);
    }
}
