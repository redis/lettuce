/*
 * Copyright 2019-2020 the original author or authors.
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
package io.lettuce.core.masterreplica;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration test for Master/Replica using Redis Sentinel over SSL.
 *
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class UpstreamReplicaSentinelSslIntegrationTests extends TestSupport {

    private final ClientResources clientResources;

    @Inject
    UpstreamReplicaSentinelSslIntegrationTests(ClientResources clientResources) {
        this.clientResources = clientResources.mutate()
                .socketAddressResolver(MappingSocketAddressResolver.create(DnsResolver.jvmDefault(), hostAndPort -> {

                    return HostAndPort.of(hostAndPort.getHostText(), hostAndPort.getPort() + 443);
                })).build();
    }

    @Test
    void testMasterReplicaSentinelBasic() {

        RedisClient client = RedisClient.create(clientResources);
        RedisURI redisURI = RedisURI.create("rediss-sentinel://" + TestSettings.host() + ":26379?sentinelMasterId=mymaster");
        redisURI.setVerifyPeer(false);
        StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(client, StringCodec.UTF8,
                redisURI);

        connection.setReadFrom(ReadFrom.REPLICA);

        connection.sync().set(key, value);
        connection.sync().set(key, value);
        connection.sync().get(key);

        connection.close();

        FastShutdown.shutdown(client);
    }
}
