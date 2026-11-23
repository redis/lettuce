package io.lettuce.test.resource;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 * @author Hari Mani
 */
public class DefaultRedisClient {

    private static final DefaultRedisClient instance = new DefaultRedisClient();

    private final RedisClient redisClient;

    private DefaultRedisClient() {
        RedisURI.Builder builder = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port());
        if (TestSettings.tls()) {
            builder.withSsl(true).withVerifyPeer(false);
        }
        Endpoints.Endpoint endpoint = TestSettings.endpoint();
        if (endpoint != null && endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
            builder.withAuthentication(TestSettings.username(), endpoint.getPassword());
        }
        redisClient = RedisClient.create(builder.build());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FastShutdown.shutdown(redisClient)));
    }

    /**
     * Do not close the client.
     *
     * @return the default redis client for the tests.
     */
    public static RedisClient get() {
        return instance.redisClient;
    }

}
