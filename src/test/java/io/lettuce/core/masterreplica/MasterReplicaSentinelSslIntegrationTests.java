package io.lettuce.core.masterreplica;

import javax.inject.Inject;

import io.lettuce.core.*;
import io.lettuce.core.resource.DnsResolvers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.test.settings.TlsSettings.createAndSaveTestTruststore;

/**
 * Integration test for Master/Replica using Redis Sentinel over SSL.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class MasterReplicaSentinelSslIntegrationTests extends TestSupport {

    private final ClientResources clientResources;

    private static File truststoreFile;

    private static Map<Integer, Integer> portMap = new HashMap<>();
    static {
        portMap.put(26379, 26822);
        portMap.put(6482, 8443);
        portMap.put(6483, 8444);
    }

    @Inject
    MasterReplicaSentinelSslIntegrationTests(ClientResources clientResources) {

        this.clientResources = clientResources.mutate()
                .socketAddressResolver(MappingSocketAddressResolver.create(DnsResolvers.UNRESOLVED, hostAndPort -> {
                    int port = hostAndPort.getPort();
                    if (portMap.containsKey(port)) {
                        return HostAndPort.of(hostAndPort.getHostText(), portMap.get(port));
                    }

                    return hostAndPort;
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
