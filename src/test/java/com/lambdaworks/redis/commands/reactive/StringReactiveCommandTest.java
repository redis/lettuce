/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.KeyValue;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.StringCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

import reactor.core.publisher.Flux;

/**
 * @author Mark Paluch
 */
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
