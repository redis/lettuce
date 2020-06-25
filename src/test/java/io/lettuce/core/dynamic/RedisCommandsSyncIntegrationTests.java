/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class RedisCommandsSyncIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    RedisCommandsSyncIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.redis = connection.sync();
    }

    @Test
    void sync() {

        StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE);
        RedisCommandFactory factory = new RedisCommandFactory(connection);

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        api.setSync(key, value, Timeout.create(10, TimeUnit.SECONDS));
        assertThat(api.get("key")).isEqualTo("value");
        assertThat(api.getAsBytes("key")).isEqualTo("value".getBytes());

        connection.close();
    }

    @Test
    void defaultMethod() {

        StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE);
        RedisCommandFactory factory = new RedisCommandFactory(connection);

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        api.setSync(key, value, Timeout.create(10, TimeUnit.SECONDS));

        assertThat(api.getAsBytes()).isEqualTo("value".getBytes());

        connection.close();
    }

    @Test
    void mgetAsValues() {

        redis.set(key, value);

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        List<Value<String>> values = api.mgetAsValues(key, "key2");
        assertThat(values).hasSize(2);
        assertThat(values.get(0)).isEqualTo(Value.just(value));
        assertThat(values.get(1)).isEqualTo(Value.empty());
    }

    @Test
    void mgetByteArray() {

        redis.set(key, value);

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        List<byte[]> values = api.mget(Collections.singleton(key.getBytes()));
        assertThat(values).hasSize(1).contains(value.getBytes());
    }

    interface MultipleExecutionModels extends Commands {

        List<byte[]> mget(Iterable<byte[]> keys);

        String get(String key);

        default byte[] getAsBytes() {
            return getAsBytes("key");
        }

        @Command("GET ?0")
        byte[] getAsBytes(String key);

        @Command("SET")
        String setSync(String key, String value, Timeout timeout);

        @Command("MGET")
        List<Value<String>> mgetAsValues(String... keys);

    }

}
