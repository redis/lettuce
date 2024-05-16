package io.lettuce.core.commands.reactive;

import io.lettuce.core.KeyValue;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HashCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import javax.inject.Inject;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * x
 * 
 * @author Mark Paluch
 */
class HashReactiveCommandIntegrationTests extends HashCommandIntegrationTests {

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    HashReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.connection = connection;
    }

    @Test
    public void hgetall() {

        connection.sync().hset(key, "zero", "0");
        connection.sync().hset(key, "one", "1");
        connection.sync().hset(key, "two", "2");

        connection.reactive().hgetall(key).collect(Collectors.toMap(KeyValue::getKey, Value::getValue)).as(StepVerifier::create)
                .assertNext(actual -> {

                    assertThat(actual).containsEntry("zero", "0").containsEntry("one", "1").containsEntry("two", "2");
                }).verifyComplete();
    }

    @Test
    @Disabled("API differences")
    public void hgetallStreaming() {

    }

    @Test
    @Disabled("API differences")
    public void hexpiretime() {

    }

}
