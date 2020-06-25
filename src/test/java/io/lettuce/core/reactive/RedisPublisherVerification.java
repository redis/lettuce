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
package io.lettuce.core.reactive;

import static io.lettuce.core.protocol.CommandType.LRANGE;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestRedisPublisher;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.ValueListOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Reactive Streams TCK for {@link io.lettuce.core.RedisPublisher}.
 *
 * @author Mark Paluch
 */
public class RedisPublisherVerification extends PublisherVerification<String> {

    private static RedisClient client;

    private static StatefulRedisConnection<String, String> connection;

    public RedisPublisherVerification() {
        super(new TestEnvironment(1000));
    }

    @BeforeClass
    private static void beforeClass() {
        client = RedisClient.create(TestClientResources.get(), RedisURI.create(TestSettings.host(), TestSettings.port()));
        connection = client.connect();
        connection.sync().flushall();
    }

    @AfterClass
    private static void afterClass() {
        connection.close();
        FastShutdown.shutdown(client);
    }

    @Override
    public Publisher<String> createPublisher(long elements) {

        RedisCommands<String, String> sync = connection.sync();

        if (elements == Long.MAX_VALUE) {
            return null;
        }

        String id = UUID.randomUUID().toString();
        String key = "PublisherVerification-" + id;

        for (int i = 0; i < elements; i++) {
            sync.lpush(key, "element-" + i);
        }

        Supplier<Command<String, String, List<String>>> supplier = () -> {
            CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).addKey(key).add(0).add(-1);
            return new Command<>(LRANGE, new ValueListOutput<>(StringCodec.UTF8), args);
        };

        return new TestRedisPublisher(supplier, connection, true);
    }

    @Override
    public long maxElementsFromPublisher() {
        return 100;
    }

    @Override
    public Publisher<String> createFailedPublisher() {
        return null;
    }

}
