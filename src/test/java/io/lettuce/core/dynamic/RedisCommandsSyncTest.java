/*
 * Copyright 2016 the original author or authors.
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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.domain.Timeout;

/**
 * @author Mark Paluch
 */
public class RedisCommandsSyncTest extends AbstractRedisClientTest {

    @Test
    public void sync() throws Exception {

        StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE);
        RedisCommandFactory factory = new RedisCommandFactory(connection);

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        api.setSync(key, value, Timeout.create(10, TimeUnit.SECONDS));
        assertThat(api.get("key")).isEqualTo("value");
        assertThat(api.getAsBytes("key")).isEqualTo("value".getBytes());

        connection.close();
    }

    @Test
    public void mgetAsValues() throws Exception {

        redis.set(key, value);

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        List<Value<String>> values = api.mgetAsValues(key, "key2");
        assertThat(values).hasSize(2);
        assertThat(values.get(0)).isEqualTo(Value.just(value));
        assertThat(values.get(1)).isEqualTo(Value.empty());
    }

    static interface MultipleExecutionModels extends Commands {

        String get(String key);

        @Command("GET")
        byte[] getAsBytes(String key);

        @Command("SET")
        String setSync(String key, String value, Timeout timeout);

        @Command("MGET")
        List<Value<String>> mgetAsValues(String... keys);
    }

}
