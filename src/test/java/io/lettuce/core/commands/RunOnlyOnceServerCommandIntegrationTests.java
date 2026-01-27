package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.test.settings.TestSettings.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.MigrateArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.CanConnect;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Will Glozer
 * @author Mark Paluch
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RunOnlyOnceServerCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    RunOnlyOnceServerCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.redis = connection.sync();
    }

    /**
     * Executed in order: 1 this test causes a stop of the redis. This means, you cannot repeat the test without restarting your
     * redis.
     */
    @Test
    @Disabled
    @Order(1)
    void debugSegfault() {
        assumeTrue(CanConnect.to(host(), port(1)));
        final RedisURI redisURI = RedisURI.Builder.redis(host(), port(1)).build();
        try (StatefulRedisConnection<String, String> connection = client.connect(redisURI)) {
            final RedisAsyncCommands<String, String> commands = connection.async();
            commands.debugSegfault();

            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
            assertThat(connection.isOpen()).isFalse();
        }
    }

    /**
     * Executed in order: 2
     */
    @Test
    @Order(2)
    void migrate() {

        assumeTrue(CanConnect.to(host(), port(7)));

        redis.set(key, value);

        String result = redis.migrate("localhost", TestSettings.port(7), key, 0, 10);
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Executed in order: 3
     */
    @Test
    @Order(3)
    void migrateCopyReplace() {

        assumeTrue(CanConnect.to(host(), port(7)));

        redis.set(key, value);
        redis.set("key2", value);
        redis.set("key3", value);

        String result = redis.migrate("localhost", TestSettings.port(7), 0, 10, MigrateArgs.Builder.keys(key).copy().replace());
        assertThat(result).isEqualTo("OK");

        result = redis.migrate("localhost", TestSettings.port(7), 0, 10,
                MigrateArgs.Builder.keys(Arrays.asList("key1", "key2")).replace());
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Executed in order: 4 this test causes a stop of the redis. This means, you cannot repeat the test without restarting your
     * redis.
     */
    @Test
    @Order(4)
    void shutdown() {
        assumeTrue(CanConnect.to(host(), port(7)));
        final RedisURI redisURI = RedisURI.Builder.redis(host(), port(2)).build();
        try (StatefulRedisConnection<String, String> cnxn = client.connect(redisURI)) {
            final RedisAsyncCommands<String, String> commands = cnxn.async();
            commands.shutdown(true);
            commands.shutdown(false);
            Wait.untilTrue(() -> !cnxn.isOpen()).waitOrTimeout();

            assertThat(cnxn.isOpen()).isFalse();
        }
    }

}
