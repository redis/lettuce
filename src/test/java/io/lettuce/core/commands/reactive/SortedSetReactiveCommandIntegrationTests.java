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

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.commands.SortedSetCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for Sorted Sets via {@link io.lettuce.core.api.reactive.RedisReactiveCommands}.
 *
 * @author Mark Paluch
 */
class SortedSetReactiveCommandIntegrationTests extends SortedSetCommandIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    SortedSetReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
        this.connection = connection;
    }

    @Test
    @EnabledOnCommand("ZMSCORE")
    public void zmscore() {

        connection.sync().zadd("zset1", 1.0, "a", 2.0, "b");

        reactive.zmscore("zset1", "a", "c", "b").as(StepVerifier::create).assertNext(actual -> {
            assertThat(actual).isEqualTo(list(1.0, null, 2.0));
        }).verifyComplete();
    }
}
