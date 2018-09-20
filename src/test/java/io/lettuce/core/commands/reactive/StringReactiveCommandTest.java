/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.commands.reactive;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.StringCommandTest;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class StringReactiveCommandTest extends StringCommandTest {

    @Override
    public RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

    @Test
    void mget() {

        StatefulRedisConnection<String, String> connection = client.connect();

        connection.sync().set(key, value);
        connection.sync().set("key1", value);
        connection.sync().set("key2", value);

        Flux<KeyValue<String, String>> mget = connection.reactive().mget(key, "key1", "key2");
        StepVerifier.create(mget.next()).expectNext(KeyValue.just(key, value)).verifyComplete();

        connection.close();
    }

    @Test
    void mgetEmpty() {

        StatefulRedisConnection<String, String> connection = client.connect();

        connection.sync().set(key, value);

        Flux<KeyValue<String, String>> mget = connection.reactive().mget("unknown");
        StepVerifier.create(mget.next()).expectNext(KeyValue.empty("unknown")).verifyComplete();

        connection.close();
    }
}
