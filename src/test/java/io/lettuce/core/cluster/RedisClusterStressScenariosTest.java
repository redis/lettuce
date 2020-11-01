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

import static io.lettuce.core.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import io.lettuce.category.SlowTests;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

@TestMethodOrder(MethodOrderer.MethodName.class)
@SuppressWarnings("unchecked")
@SlowTests
public class RedisClusterStressScenariosTest extends TestSupport {

    private static final String host = TestSettings.hostAddr();

    private static RedisClient client;
    private static RedisClusterClient clusterClient;
    private static ClusterTestHelper clusterHelper;

    private Logger log = LogManager.getLogger(getClass());

    private StatefulRedisConnection<String, String> redis5;
    private StatefulRedisConnection<String, String> redis6;

    private RedisCommands<String, String> redissync5;
    private RedisCommands<String, String> redissync6;

    protected String key = "key";
    protected String value = "value";

    @BeforeAll
    public static void setupClient() {
        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, ClusterTestSettings.port5).build());
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                Collections.singletonList(RedisURI.Builder.redis(host, ClusterTestSettings.port5).build()));
        clusterHelper = new ClusterTestHelper(clusterClient, ClusterTestSettings.port5, ClusterTestSettings.port6);
    }

    @AfterAll
    public static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    @BeforeEach
    public void before() {
        clusterHelper.flushdb();
        ClusterSetup.setupMasterWithReplica(clusterHelper);

        redis5 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port5).build());
        redis6 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port6).build());

        redissync5 = redis5.sync();
        redissync6 = redis6.sync();
        clusterClient.refreshPartitions();

        Wait.untilTrue(clusterHelper::isStable).waitOrTimeout();

    }

    @AfterEach
    public void after() {
        redis5.close();

        redissync5.getStatefulConnection().close();
        redissync6.getStatefulConnection().close();
    }

    @Test
    public void testClusterFailover() {

        log.info("Cluster node 5 is master");
        log.info("Cluster nodes seen from node 5:\n" + redissync5.clusterNodes());
        log.info("Cluster nodes seen from node 6:\n" + redissync6.clusterNodes());

        Wait.untilTrue(() -> getOwnPartition(redissync5).is(RedisClusterNode.NodeFlag.UPSTREAM)).waitOrTimeout();
        Wait.untilTrue(() -> getOwnPartition(redissync6).is(RedisClusterNode.NodeFlag.REPLICA)).waitOrTimeout();

        String failover = redissync6.clusterFailover(true);
        assertThat(failover).isEqualTo("OK");

        Wait.untilTrue(() -> getOwnPartition(redissync6).is(RedisClusterNode.NodeFlag.UPSTREAM)).waitOrTimeout();
        Wait.untilTrue(() -> getOwnPartition(redissync5).is(RedisClusterNode.NodeFlag.REPLICA)).waitOrTimeout();

        log.info("Cluster nodes seen from node 5 after clusterFailover:\n" + redissync5.clusterNodes());
        log.info("Cluster nodes seen from node 6 after clusterFailover:\n" + redissync6.clusterNodes());

        RedisClusterNode redis5Node = getOwnPartition(redissync5);
        RedisClusterNode redis6Node = getOwnPartition(redissync6);

        assertThat(redis5Node.is(RedisClusterNode.NodeFlag.REPLICA)).isTrue();
        assertThat(redis6Node.is(RedisClusterNode.NodeFlag.UPSTREAM)).isTrue();

    }

    @Test
    public void testClusterConnectionStability() {

        RedisAdvancedClusterAsyncCommandsImpl<String, String> connection = (RedisAdvancedClusterAsyncCommandsImpl<String, String>) clusterClient
                .connect().async();

        RedisChannelHandler<String, String> statefulConnection = (RedisChannelHandler) connection.getStatefulConnection();

        connection.set("a", "b");
        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) statefulConnection.getChannelWriter();

        StatefulRedisConnectionImpl<Object, Object> statefulSlotConnection = (StatefulRedisConnectionImpl) writer
                .getClusterConnectionProvider().getConnection(ClusterConnectionProvider.Intent.WRITE, 3300);

        final RedisAsyncCommands<Object, Object> slotConnection = statefulSlotConnection.async();

        slotConnection.set("a", "b");
        slotConnection.getStatefulConnection().close();

        Wait.untilTrue(() -> !slotConnection.isOpen()).waitOrTimeout();

        assertThat(statefulSlotConnection.isClosed()).isTrue();
        assertThat(statefulSlotConnection.isOpen()).isFalse();

        assertThat(connection.isOpen()).isTrue();
        assertThat(statefulConnection.isOpen()).isTrue();
        assertThat(statefulConnection.isClosed()).isFalse();

        try {
            connection.set("a", "b");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Connection is closed");
        }

        connection.getStatefulConnection().close();

    }

}
