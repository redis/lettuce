/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.StringCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
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
