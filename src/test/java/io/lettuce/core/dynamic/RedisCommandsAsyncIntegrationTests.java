package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class RedisCommandsAsyncIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    RedisCommandsAsyncIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.redis = connection.sync();
    }

    @Test
    void async() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        Future<String> set = api.set(key, value);
        assertThat(set).isInstanceOf(CompletableFuture.class);
    }

    static interface MultipleExecutionModels extends Commands {
        Future<String> set(String key, String value);
    }
}
