package io.lettuce.test.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class DefaultRedisClusterClient {

    private static final DefaultRedisClusterClient instance = new DefaultRedisClusterClient();

    private RedisClusterClient redisClient;

    private DefaultRedisClusterClient() {
        redisClient = RedisClusterClient.create(seedUris());
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                FastShutdown.shutdown(redisClient);
            }

        });
    }

    /**
     * Seed URIs of the Redis Cluster test database, resolved from the {@literal cluster} endpoint of the endpoint configuration
     * (see {@link TestSettings#clusterEndpoint()}).
     *
     * @return the seed URIs to bootstrap a {@link RedisClusterClient} for the tests.
     */
    public static List<RedisURI> seedUris() {

        Endpoints.Endpoint endpoint = TestSettings.clusterEndpoint();

        if (endpoint == null || endpoint.getEndpoints() == null || endpoint.getEndpoints().isEmpty()) {
            return Collections.singletonList(
                    RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).withClientName("my-client").build());
        }

        List<RedisURI> seeds = new ArrayList<>();
        for (String uri : endpoint.getEndpoints()) {
            RedisURI redisURI = RedisURI.create(uri);
            redisURI.setClientName("my-client");
            if (endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
                String username = endpoint.getUsername() != null && !endpoint.getUsername().isEmpty() ? endpoint.getUsername()
                        : null;
                redisURI.setAuthentication(username, endpoint.getPassword());
            }
            if (endpoint.isTls()) {
                redisURI.setSsl(true);
                redisURI.setVerifyPeer(false);
            }
            seeds.add(redisURI);
        }
        return seeds;
    }

    /**
     * Do not close the client.
     *
     * @return the default redis client for the tests.
     */
    public static RedisClusterClient get() {
        instance.redisClient.setOptions(ClusterClientOptions.create());
        return instance.redisClient;
    }

}
