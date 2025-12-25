/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.codec.StringCodec.UTF8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.TestFutures;
import io.lettuce.test.LettuceExtension;

/**
 * 
 * @author Ali Takavci
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class RedisMultiDbClientConnectIntegrationTests extends MultiDbTestSupport {

    @Inject
    RedisMultiDbClientConnectIntegrationTests(MultiDbClient client) {
        super(client);
    }

    @BeforeEach
    void setUp() {
        directClient1.connect().sync().flushall();
        directClient2.connect().sync().flushall();
    }

    @After
    void tearDown() {
        directClient1.shutdown();
        directClient2.shutdown();
    }

    /*
     * Standalone/Stateful
     */
    @Test
    void connectClientUri() {

        StatefulRedisConnection<String, String> connection = multiDbClient.connect();
        assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
        connection.close();
    }

    @Test
    void connectCodecClientUri() {
        StatefulRedisConnection<String, String> connection = multiDbClient.connect(UTF8);
        assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
        connection.close();
    }

    @Test
    void connectAndRunSimpleCommand() throws InterruptedException, ExecutionException {
        StatefulRedisConnection<String, String> connection = multiDbClient.connect();
        RedisFuture futureSet = connection.async().set("key1", "value1");
        TestFutures.awaitOrTimeout(futureSet);
        RedisFuture<String> futureGet = connection.async().get("key1");
        TestFutures.awaitOrTimeout(futureGet);
        assertEquals("value1", futureGet.get());
        connection.close();
    }

    @Test
    void connectAndRunAndSwitchAndRun() throws InterruptedException, ExecutionException {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisFuture futureSet = connection.async().set("key1", "value1");
        TestFutures.awaitOrTimeout(futureSet);
        RedisFuture<String> futureGet = connection.async().get("key1");
        TestFutures.awaitOrTimeout(futureGet);
        assertEquals(futureGet.get(), "value1");
        RedisURI other = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(connection.getCurrentEndpoint())).findFirst().get();
        connection.switchTo(other);
        RedisFuture<String> futureGet2 = connection.async().get("key1");
        TestFutures.awaitOrTimeout(futureGet2);
        assertEquals(null, futureGet2.get());
        connection.close();
    }

}
