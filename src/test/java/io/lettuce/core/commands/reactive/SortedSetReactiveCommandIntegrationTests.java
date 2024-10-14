package io.lettuce.core.commands.reactive;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.commands.SortedSetCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for Sorted Sets via {@link io.lettuce.core.api.reactive.RedisReactiveCommands}.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class SortedSetReactiveCommandIntegrationTests extends SortedSetCommandIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    SortedSetReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
        this.connection = connection;
    }

    @Test
    @EnabledOnCommand("ZMSCORE")
    public void zmscore() {

        connection.sync().zadd("zset1", 1.0, "a", 2.0, "b");

        reactive.zmscore("zset1", "a", "c", "b").as(StepVerifier::create).assertNext(actual -> {
            assertThat(actual).isEqualTo(list(1.0, null, 2.0));
        }).verifyComplete();
    }

}
