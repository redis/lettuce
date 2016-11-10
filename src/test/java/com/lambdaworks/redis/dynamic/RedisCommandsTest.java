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

        api.setSync(key, value, Timeout.create(10, TimeUnit.SECONDS));
        assertThat(api.get("key")).isEqualTo("value");
        assertThat(api.getAsBytes("key")).isEqualTo("value".getBytes());
    }

    @Test
    public void async() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        TestInterface api = factory.getCommands(TestInterface.class);

        Future<String> set = api.set(key, value);
        assertThat(set).isInstanceOf(CompletableFuture.class);
        set.get();
    }

    @Test
    public void reactive() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        TestInterface api = factory.getCommands(TestInterface.class);

        Mono<String> set = api.setReactive(key, value);
        assertThat(set.block()).isEqualTo("OK");
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

    static interface TestInterface extends Commands {

        String get(String key);

        @Command("GET")
        byte[] getAsBytes(String key);

        @Command("SET")
        String setSync(String key, String value, Timeout timeout);

        Future<String> set(String key, String value);

        @Command("SET")
        Mono<String> setReactive(String key, String value);
    }

    static interface TooFewParameters extends Commands {

        String get();

    }

    static interface WithTypo extends Commands {

        String gat(String key);

    }
}
