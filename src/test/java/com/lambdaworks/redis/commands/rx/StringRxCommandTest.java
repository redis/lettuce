package com.lambdaworks.redis.commands.rx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.StringCommandTest;

import rx.Observable;

public class StringRxCommandTest extends StringCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
    }

    @Test
    public void mget() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();

        connection.sync().set(key, value);
        connection.sync().set("key1", value);
        connection.sync().set("key2", value);

        Observable<String> mget = connection.reactive().mget(key, "key1", "key2");
        String first = mget.toBlocking().first();
        assertThat(first).isEqualTo(value);

        connection.close();
    }
}
