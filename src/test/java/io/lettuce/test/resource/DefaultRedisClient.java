package io.lettuce.test.resource;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.test.settings.RedisEnterpriseSettings;
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
        // Against a managed Redis Enterprise database the default connection needs credentials and,
        // optionally, TLS - the local defaults are passwordless plaintext.
        if (RedisEnterpriseSettings.isEnabled()) {
            builder = builder.withAuthentication(RedisEnterpriseSettings.username(), RedisEnterpriseSettings.password());
            if (RedisEnterpriseSettings.tls()) {
                builder = builder.withSsl(true).withVerifyPeer(false);
            }
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
