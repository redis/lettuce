package io.lettuce.core.dynamic;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.TestSupport;
import io.lettuce.core.Value;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class RedisCommandsClusterIntegrationTests extends TestSupport {

    private final StatefulRedisClusterConnection<String, String> connection;

    @Inject
    RedisCommandsClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        this.connection = connection;
        this.connection.sync().flushall();
    }

    @Test
    void future() throws ExecutionException, InterruptedException {

        RedisCommandFactory factory = new RedisCommandFactory(connection);

        SynchronousCommands api = factory.getCommands(SynchronousCommands.class);

        api.setSync(key, value, Timeout.create(Duration.ofSeconds(10)));

        assertThat(api.get("key").get()).isEqualTo("value");
        assertThat(api.getAsBytes("key")).isEqualTo("value".getBytes());
    }

    @Test
    void shouldRouteBinaryKey() {

        connection.sync().set(key, value);

        RedisCommandFactory factory = new RedisCommandFactory(connection);

        SynchronousCommands api = factory.getCommands(SynchronousCommands.class);

        assertThat(api.get(key.getBytes())).isEqualTo(value.getBytes());
    }

    @Test
    void mgetAsValues() {

        connection.sync().set(key, value);

        RedisCommandFactory factory = new RedisCommandFactory(connection);

        SynchronousCommands api = factory.getCommands(SynchronousCommands.class);

        List<Value<String>> values = api.mgetAsValues(key);
        assertThat(values).hasSize(1);
        assertThat(values.get(0)).isEqualTo(Value.just(value));
    }

    interface SynchronousCommands extends Commands {

        byte[] get(byte[] key);

        RedisFuture<String> get(String key);

        @Command("GET")
        byte[] getAsBytes(String key);

        @Command("SET")
        String setSync(String key, String value, Timeout timeout);

        @Command("MGET")
        List<Value<String>> mgetAsValues(String... keys);

    }

}
