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
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.commands.TransactionCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
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

        commands.set(key, value).toProcessor();

        StepVerifier.create(commands.discard()).expectNext("OK").verifyComplete();
        StepVerifier.create(commands.get(key)).verifyComplete();
    }

    @Test
    void watchRollback() {

        StatefulRedisConnection<String, String> otherConnection = client.connect();

        otherConnection.sync().set(key, value);

        StepVerifier.create(commands.watch(key)).expectNext("OK").verifyComplete();
        StepVerifier.create(commands.multi()).expectNext("OK").verifyComplete();

        commands.set(key, value).toProcessor();

        otherConnection.sync().del(key);

        StepVerifier.create(commands.exec()).consumeNextWith(actual -> {
            assertThat(actual).isNotNull();
            assertThat(actual.wasDiscarded()).isTrue();
        });

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

        commands.multi().toProcessor();
        commands.set(key, value).toProcessor();
        commands.lpop(key).toProcessor();
        commands.get(key).toProcessor();

        StepVerifier.create(commands.exec()).consumeNextWith(actual -> {

            assertThat((String) actual.get(0)).isEqualTo("OK");
            assertThat((Object) actual.get(1)).isInstanceOf(RedisException.class);
            assertThat((String) actual.get(2)).isEqualTo(value);
        }).verifyComplete();
    }

    @Test
    void resultOfMultiIsContainedInCommandFlux() {

        commands.multi().toProcessor();

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

        commands.multi().toProcessor();
        commands.set("key1", "value1").toProcessor();
        commands.set("key2", "value2").toProcessor();
        commands.mget("key1", "key2").collectList().toProcessor();
        commands.llen("something").toProcessor();

        StepVerifier.create(commands.exec()).consumeNextWith(actual -> {

            assertThat(actual).contains("OK", "OK", list(kv("key1", "value1"), kv("key2", "value2")), 0L);

        }).verifyComplete();
    }

}
