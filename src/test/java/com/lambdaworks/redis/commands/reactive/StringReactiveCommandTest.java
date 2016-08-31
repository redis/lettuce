package com.lambdaworks.redis.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.KeyValue;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;
import org.junit.Test;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.StringCommandTest;
import reactor.core.publisher.Flux;

public class StringReactiveCommandTest extends StringCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

    @Test
    public void mget() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();

        connection.sync().set(key, value);
        connection.sync().set("key1", value);
        connection.sync().set("key2", value);

        Flux<KeyValue<String, String>> mget = connection.reactive().mget(key, "key1", "key2");
        KeyValue<String, String> first = mget.next().block();
        assertThat(first).isEqualTo(KeyValue.just(key, value));

        connection.close();
    }

    @Test
    public void mgetEmpty() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();

        connection.sync().set(key, value);

        Flux<KeyValue<String, String>> mget = connection.reactive().mget("unknown");
        KeyValue<String, String> first = mget.next().block();
        assertThat(first).isEqualTo(KeyValue.empty("unknown"));

        connection.close();
    }
}
