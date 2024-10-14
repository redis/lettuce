package io.lettuce.core.commands.reactive;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.commands.TransactionCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import reactor.test.StepVerifier;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
public class TransactionReactiveCommandIntegrationTests extends TransactionCommandIntegrationTests {

    private final RedisClient client;

    private final RedisReactiveCommands<String, String> commands;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    public TransactionReactiveCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, ReactiveSyncInvocationHandler.sync(connection));
        this.client = client;
        this.commands = connection.reactive();
        this.connection = connection;
    }

    @Test
    void discard() {

        StepVerifier.create(commands.multi()).expectNext("OK").verifyComplete();

        commands.set(key, value).toFuture();

        StepVerifier.create(commands.discard()).expectNext("OK").verifyComplete();
        StepVerifier.create(commands.get(key)).verifyComplete();
    }

    @Test
    void watchRollback() {

        StatefulRedisConnection<String, String> otherConnection = client.connect();

        otherConnection.sync().set(key, value);

        StepVerifier.create(commands.watch(key)).expectNext("OK").verifyComplete();
        StepVerifier.create(commands.multi()).expectNext("OK").verifyComplete();

        commands.set(key, value).toFuture();

        otherConnection.sync().del(key);

        StepVerifier.create(commands.exec()).consumeNextWith(actual -> {
            assertThat(actual).isNotNull();
            assertThat(actual.wasDiscarded()).isTrue();
        }).verifyComplete();

        otherConnection.close();
    }

    @Test
    void execSingular() {

        StepVerifier.create(commands.multi()).expectNext("OK").verifyComplete();

        connection.sync().set(key, value);

        StepVerifier.create(commands.exec()).consumeNextWith(actual -> assertThat(actual).contains("OK")).verifyComplete();
        StepVerifier.create(commands.get(key)).expectNext(value).verifyComplete();
    }

    @Test
    void errorInMulti() {

        StepVerifier.create(commands.multi()).expectNext("OK").verifyComplete();
        commands.set(key, value).toFuture();
        commands.lpop(key).toFuture();
        commands.get(key).toFuture();

        StepVerifier.create(commands.exec()).consumeNextWith(actual -> {

            assertThat((String) actual.get(0)).isEqualTo("OK");
            assertThat((Object) actual.get(1)).isInstanceOf(RedisException.class);
            assertThat((String) actual.get(2)).isEqualTo(value);
        }).verifyComplete();
    }

    @Test
    void resultOfMultiIsContainedInCommandFlux() {

        StepVerifier.create(commands.multi()).expectNext("OK").verifyComplete();

        StepVerifier.Step<String> set1 = StepVerifier.create(commands.set("key1", "value1")).expectNext("OK").thenAwait();
        StepVerifier.Step<String> set2 = StepVerifier.create(commands.set("key2", "value2")).expectNext("OK").thenAwait();
        StepVerifier.Step<KeyValue<String, String>> mget = StepVerifier.create(commands.mget("key1", "key2"))
                .expectNext(KeyValue.just("key1", "value1"), KeyValue.just("key2", "value2")).thenAwait();
        StepVerifier.Step<Long> llen = StepVerifier.create(commands.llen("something")).expectNext(0L).thenAwait();

        StepVerifier.create(commands.exec()).then(() -> {

            set1.verifyComplete();
            set2.verifyComplete();
            mget.verifyComplete();
            llen.verifyComplete();

        }).expectNextCount(1).verifyComplete();
    }

    @Test
    void resultOfMultiIsContainedInExecObservable() {

        StepVerifier.create(commands.multi()).expectNext("OK").verifyComplete();

        commands.set("key1", "value1").toFuture();
        commands.set("key2", "value2").toFuture();
        commands.mget("key1", "key2").collectList().toFuture();
        commands.llen("something").toFuture();

        StepVerifier.create(commands.exec()).consumeNextWith(actual -> {

            assertThat(actual).contains("OK", "OK", list(kv("key1", "value1"), kv("key2", "value2")), 0L);

        }).verifyComplete();
    }

}
