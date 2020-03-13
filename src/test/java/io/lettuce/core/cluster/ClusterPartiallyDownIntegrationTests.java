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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
class ClusterPartiallyDownIntegrationTests extends TestSupport {

    private static ClientResources clientResources;

    private static int port1 = 7579;
    private static int port2 = 7580;
    private static int port3 = 7581;
    private static int port4 = 7582;

    private static final RedisURI URI_1 = RedisURI.create(TestSettings.host(), port1);
    private static final RedisURI URI_2 = RedisURI.create(TestSettings.host(), port2);
    private static final RedisURI URI_3 = RedisURI.create(TestSettings.host(), port3);
    private static final RedisURI URI_4 = RedisURI.create(TestSettings.host(), port4);

    private RedisClusterClient redisClusterClient;

    @BeforeAll
    static void beforeClass() {
        clientResources = TestClientResources.get();
    }

    @AfterEach
    void after() {
        redisClusterClient.shutdown();
    }

    @Test
    void connectToPartiallyDownCluster() {

        List<RedisURI> seed = LettuceLists.unmodifiableList(URI_1, URI_2, URI_3, URI_4);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);
        StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    void operateOnPartiallyDownCluster() {

        List<RedisURI> seed = LettuceLists.unmodifiableList(URI_1, URI_2, URI_3, URI_4);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);
        StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();

        String key_10439 = "aaa";
        assertThat(SlotHash.getSlot(key_10439)).isEqualTo(10439);

        try {
            connection.sync().get(key_10439);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasRootCauseInstanceOf(IOException.class);
        }

        connection.close();
    }

    @Test
    void seedNodesAreOffline() {

        List<RedisURI> seed = LettuceLists.unmodifiableList(URI_1, URI_2, URI_3);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);

        try {
            redisClusterClient.connect();
            fail("Missing RedisException");
        } catch (RedisException e) {

            assertThat(e).isInstanceOf(RedisConnectionException.class)
                    .hasMessageStartingWith("Unable to establish a connection to Redis Cluster");
        }
    }

    @Test
    void partitionNodesAreOffline() {

        List<RedisURI> seed = LettuceLists.unmodifiableList(URI_1, URI_2, URI_3);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);

        Partitions partitions = new Partitions();
        partitions.addPartition(new RedisClusterNode(URI_1, "a", true, null, 0, 0, 0, new ArrayList<>(), new HashSet<>()));
        partitions.addPartition(new RedisClusterNode(URI_2, "b", true, null, 0, 0, 0, new ArrayList<>(), new HashSet<>()));

        redisClusterClient.setPartitions(partitions);

        try {
            redisClusterClient.connect();
            fail("Missing RedisConnectionException");
        } catch (RedisConnectionException e) {
            assertThat(e).hasRootCauseInstanceOf(IOException.class);
        }
    }
}
