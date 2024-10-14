package io.lettuce.core.masterreplica;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
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

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration test for Master/Replica using Redis Sentinel over SSL.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class MasterReplicaSentinelSslIntegrationTests extends TestSupport {

    private final ClientResources clientResources;

    @Inject
    MasterReplicaSentinelSslIntegrationTests(ClientResources clientResources) {
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
