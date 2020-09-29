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

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import io.lettuce.core.KeyValue;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HashCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
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
}
