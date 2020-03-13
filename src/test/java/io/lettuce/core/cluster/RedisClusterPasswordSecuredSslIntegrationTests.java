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

import static io.lettuce.test.settings.TestSettings.host;
import static io.lettuce.test.settings.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.test.CanConnect;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Mark Paluch
 */
class RedisClusterPasswordSecuredSslIntegrationTests extends TestSupport {

    private static final int CLUSTER_PORT_SSL_1 = 7443;
    private static final int CLUSTER_PORT_SSL_2 = 7444;
    private static final int CLUSTER_PORT_SSL_3 = 7445;

    private static final String SLOT_1_KEY = "8HMdi";
    private static final String SLOT_16352_KEY = "UyAa4KqoWgPGKa";

    private static RedisURI redisURI = RedisURI.Builder.redis(host(), CLUSTER_PORT_SSL_1).withPassword("foobared").withSsl(true)
            .withVerifyPeer(false).build();
    private static RedisClusterClient redisClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

    @BeforeEach
    void before() {
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_1), "Assume that stunnel runs on port 7443");
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_2), "Assume that stunnel runs on port 7444");
        assumeTrue(CanConnect.to(host(), CLUSTER_PORT_SSL_3), "Assume that stunnel runs on port 7445");
        assumeTrue(CanConnect.to(host(), 7479), "Assume that Redis runs on port 7479");
        assumeTrue(CanConnect.to(host(), 7480), "Assume that Redis runs on port 7480");
        assumeTrue(CanConnect.to(host(), 7481), "Assume that Redis runs on port 7481");
    }

    @AfterAll
    static void afterClass() {
        FastShutdown.shutdown(redisClient);
    }

    @Test
    void defaultClusterConnectionShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    void partitionViewShouldContainClusterPorts() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        List<Integer> ports = connection.getPartitions().stream().map(redisClusterNode -> redisClusterNode.getUri().getPort())
                .collect(Collectors.toList());
        connection.close();

        assertThat(ports).contains(CLUSTER_PORT_SSL_1, CLUSTER_PORT_SSL_2, CLUSTER_PORT_SSL_3);
    }

    @Test
    void routedOperationsAreWorking() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        sync.set(SLOT_1_KEY, "value1");
        sync.set(SLOT_16352_KEY, "value2");

        assertThat(sync.get(SLOT_1_KEY)).isEqualTo("value1");
        assertThat(sync.get(SLOT_16352_KEY)).isEqualTo("value2");

        connection.close();
    }

    @Test
    void nodeConnectionsShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        // replica
        StatefulRedisConnection<String, String> node2Connection = connection.getConnection(hostAddr(), 7444);

        try {
            node2Connection.sync().get(SLOT_1_KEY);
        } catch (RedisCommandExecutionException e) {
            assertThat(e).hasMessage("MOVED 1 127.0.0.1:7443");
        }

        connection.close();
    }

    @Test
    void nodeSelectionApiShouldWork() {

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        Executions<String> ping = connection.sync().all().commands().ping();
        assertThat(ping).hasSize(3).contains("PONG");

        connection.close();
    }

    @Test
    void connectionWithoutPasswordShouldFail() {

        RedisURI redisURI = RedisURI.Builder.redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(false).build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

        try {
            redisClusterClient.reloadPartitions();
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Cannot reload Redis Cluster topology");
        } finally {
            FastShutdown.shutdown(redisClusterClient);
        }
    }

    @Test
    void connectionWithoutPasswordShouldFail2() {

        RedisURI redisURI = RedisURI.Builder.redis(host(), CLUSTER_PORT_SSL_1).withSsl(true).withVerifyPeer(false).build();
        RedisClusterClient redisClusterClient = RedisClusterClient.create(TestClientResources.get(), redisURI);

        try {
            redisClusterClient.connect();
        } catch (RedisConnectionException e) {
            assertThat(e).hasMessageContaining("Unable to establish a connection to Redis Cluster");
        } finally {
            FastShutdown.shutdown(redisClusterClient);
        }
    }

    @Test
    void clusterNodeRefreshWorksForMultipleIterations() {

        redisClient.reloadPartitions();
        redisClient.reloadPartitions();
        redisClient.reloadPartitions();
        redisClient.reloadPartitions();
    }
}
