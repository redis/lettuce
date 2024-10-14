package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.StringCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class StringReactiveCommandIntegrationTests extends StringCommandIntegrationTests {

    private final RedisCommands<String, String> redis;

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    StringReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.redis = connection.sync();
        this.reactive = connection.reactive();
    }

    @Test
    void mget() {

        redis.set(key, value);
        redis.set("key1", value);
        redis.set("key2", value);

        Flux<KeyValue<String, String>> mget = reactive.mget(key, "key1", "key2");
        StepVerifier.create(mget.next()).expectNext(KeyValue.just(key, value)).verifyComplete();
    }

    @Test
    void mgetEmpty() {

        redis.set(key, value);

        Flux<KeyValue<String, String>> mget = reactive.mget("unknown");
        StepVerifier.create(mget.next()).expectNext(KeyValue.empty("unknown")).verifyComplete();
    }

}
