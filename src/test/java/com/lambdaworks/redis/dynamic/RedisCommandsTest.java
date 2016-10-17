package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.dynamic.annotation.Command;
import com.lambdaworks.redis.dynamic.domain.Timeout;

import reactor.core.publisher.Mono;

/**
 * @author Mark Paluch
 */
public class RedisCommandsTest extends AbstractRedisClientTest {

    @Test
    public void sync() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        TestInterface api = factory.getCommands(TestInterface.class);

        api.setSync("key", "value", Timeout.create(10, TimeUnit.SECONDS));
        assertThat(api.get("key")).isEqualTo("value");
        assertThat(api.getAsBytes("key")).isEqualTo("value".getBytes());
    }

    @Test
    public void async() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        TestInterface api = factory.getCommands(TestInterface.class);

        Future<String> key = api.set("key", "value");
        assertThat(key).isInstanceOf(CompletableFuture.class);
        key.get();
    }

    @Test
    public void reactive() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        TestInterface api = factory.getCommands(TestInterface.class);

        Mono<String> key = api.setReactive("key", "value");
        assertThat(key.block()).isEqualTo("OK");
    }

    @Test
    public void verifierShouldCatchBuggyDeclarations() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        try {
            factory.getCommands(TooFewParameters.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Command GET accepts 1 parameters but method declares 0 parameter");
        }

        try {
            factory.getCommands(WithTypo.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Command GAT does not exist.");
        }

    }

    static interface TestInterface {

        String get(String key);

        @Command("GET")
        byte[] getAsBytes(String key);

        @Command("SET")
        String setSync(String key, String value, Timeout timeout);

        Future<String> set(String key, String value);

        @Command("SET")
        Mono<String> setReactive(String key, String value);
    }

    static interface TooFewParameters {

        String get();

    }

    static interface WithTypo {

        String gat(String key);

    }
}
