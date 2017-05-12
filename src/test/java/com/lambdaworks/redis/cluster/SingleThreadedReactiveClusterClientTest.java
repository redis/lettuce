/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollectorOptions;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.DefaultEventLoopGroupProvider;

import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * @author Mark Paluch
 */
public class SingleThreadedReactiveClusterClientTest {

    RedisClusterClient client;

    @Before
    public void before() {

        DefaultClientResources clientResources = DefaultClientResources.builder()
                .eventExecutorGroup(ImmediateEventExecutor.INSTANCE)
                .eventLoopGroupProvider(new DefaultEventLoopGroupProvider(1))
                .commandLatencyCollectorOptions(DefaultCommandLatencyCollectorOptions.disabled()).build();

        client = RedisClusterClient.create(clientResources, RedisURI.create("localhost", 7379));
    }

    @After
    public void tearDown() {

        client.shutdown();
        client.getResources().shutdown();
    }

    @Test
    public void shouldPropagateAsynchronousConnections() {

        StatefulRedisClusterConnection<String, String> connect = client.connect();
        connect.sync().flushall();

        List<String> keys = connect.reactive().set("key", "value").flatMap(s -> connect.reactive().set("foo", "bar"))
                .flatMap(s -> connect.reactive().keys("*")) //
                .doOnError(Throwable::printStackTrace) //
                .toList() //
                .toBlocking() //
                .last();

        assertThat(keys).contains("key", "foo");
    }
}
