package io.lettuce.core.sentinel;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for Redis Sentinel using ACL authentication.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("ACL")
public class SentinelAclIntegrationTests extends TestSupport {

    private final RedisClient redisClient;

    private final RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), 26381, SentinelTestSettings.MASTER_ID)
            // data node auth
            .withAuthentication("default", TestSettings.password()).build();

    private final RedisURI sentinelWithAcl = RedisURI.Builder
            .sentinel(TestSettings.host(), 26381, SentinelTestSettings.MASTER_ID)
            // data node auth
            .withAuthentication("default", TestSettings.password()).build();

    @Inject
    public SentinelAclIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;

        // sentinel node auth
        for (RedisURI sentinel : redisURI.getSentinels()) {
            sentinel.setPassword(TestSettings.password());
        }

        // sentinel node auth
        for (RedisURI sentinel : sentinelWithAcl.getSentinels()) {
            sentinel.setUsername(TestSettings.aclUsername());
            sentinel.setPassword(TestSettings.aclPassword());
        }
    }

    @BeforeEach
    void setUp() {
        StatefulRedisConnection<String, String> connection = redisClient.connect(redisURI.getSentinels().get(0));
        WithPassword.enableAuthentication(connection.sync());
        connection.close();
    }

    @Test
    void sentinelWithAuthentication() {

        StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel(sentinelWithAcl);

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    void connectThroughSentinel() {

        StatefulRedisConnection<String, String> connection = redisClient.connect(sentinelWithAcl);

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

}
