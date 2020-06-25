/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DefaultEventLoopGroupProvider;
import io.lettuce.test.resource.FastShutdown;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * @author Mark Paluch
 */
class SingleThreadedReactiveClusterClientIntegrationTests {

    private RedisClusterClient client;

    @BeforeEach
    void before() {

        DefaultClientResources clientResources = DefaultClientResources.builder()
                .eventExecutorGroup(ImmediateEventExecutor.INSTANCE)
                .eventLoopGroupProvider(new DefaultEventLoopGroupProvider(1))
                .commandLatencyCollectorOptions(DefaultCommandLatencyCollectorOptions.disabled()).build();

        client = RedisClusterClient.create(clientResources, RedisURI.create("localhost", 7379));
    }

    @AfterEach
    void tearDown() {

        FastShutdown.shutdown(client);
        FastShutdown.shutdown(client.getResources());
    }

    @Test
    void shouldPropagateAsynchronousConnections() {

        StatefulRedisClusterConnection<String, String> connect = client.connect();
        connect.sync().flushall();

        List<String> keys = connect.reactive().set("key", "value").flatMap(s -> connect.reactive().set("foo", "bar"))
                .flatMapMany(s -> connect.reactive().keys("*")) //
                .doOnError(Throwable::printStackTrace) //
                .collectList() //
                .block();

        assertThat(keys).contains("key", "foo");
    }

}
