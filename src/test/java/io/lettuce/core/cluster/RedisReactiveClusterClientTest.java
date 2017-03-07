/*
 * Copyright 2011-2017 the original author or authors.
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
package io.lettuce.core.cluster;

import static io.lettuce.core.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.*;
import org.junit.runners.MethodSorters;

import reactor.test.StepVerifier;

import io.lettuce.TestClientResources;
import io.lettuce.core.FastShutdown;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisReactiveClusterClientTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected StatefulRedisClusterConnection<String, String> connection;
    protected RedisAdvancedClusterCommands<String, String> sync;
    protected RedisAdvancedClusterReactiveCommands<String, String> reactive;

    @BeforeClass
    public static void setupClient() throws Exception {
        setupClusterClient();
        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port1).build());
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                Collections.singletonList(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClient() {
        shutdownClusterClient();
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() throws Exception {

        clusterRule.getClusterClient().reloadPartitions();

        clusterClient.reloadPartitions();
        connection = clusterClient.connect();
        sync = connection.sync();
        reactive = connection.reactive();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void testClusterCommandRedirection() throws Exception {

        // Command on node within the default connection
        StepVerifier.create(reactive.set(KEY_B, "myValue1")).expectNext("OK").verifyComplete();

        // gets redirection to node 3
        StepVerifier.create(reactive.set(KEY_A, "myValue1")).expectNext("OK").verifyComplete();
    }

    @Test
    public void getKeysInSlot() throws Exception {

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        StepVerifier.create(reactive.clusterGetKeysInSlot(SLOT_A, 10)).expectNext(KEY_A).verifyComplete();
        StepVerifier.create(reactive.clusterGetKeysInSlot(SLOT_B, 10)).expectNext(KEY_B).verifyComplete();
    }

    @Test
    public void countKeysInSlot() throws Exception {

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        StepVerifier.create(reactive.clusterCountKeysInSlot(SLOT_A)).expectNext(1L).verifyComplete();
        StepVerifier.create(reactive.clusterCountKeysInSlot(SLOT_B)).expectNext(1L).verifyComplete();

        int slotZZZ = SlotHash.getSlot("ZZZ".getBytes());
        StepVerifier.create(reactive.clusterCountKeysInSlot(slotZZZ)).expectNext(0L).verifyComplete();
    }

    @Test
    public void testClusterCountFailureReports() throws Exception {
        RedisClusterNode ownPartition = getOwnPartition(sync);
        StepVerifier.create(reactive.clusterCountFailureReports(ownPartition.getNodeId())).consumeNextWith(actual -> {
            assertThat(actual).isGreaterThanOrEqualTo(0);
        }).verifyComplete();
    }

    @Test
    public void testClusterKeyslot() throws Exception {
        StepVerifier.create(reactive.clusterKeyslot(KEY_A)).expectNext((long) SLOT_A).verifyComplete();
        assertThat(SlotHash.getSlot(KEY_A)).isEqualTo(SLOT_A);
    }

    @Test
    public void testClusterSaveconfig() throws Exception {
        StepVerifier.create(reactive.clusterSaveconfig()).expectNext("OK").verifyComplete();
    }

    @Test
    public void testClusterSetConfigEpoch() throws Exception {
        StepVerifier.create(reactive.clusterSetConfigEpoch(1L)).consumeErrorWith(e -> {
            assertThat(e).hasMessageContaining("ERR The user can assign a config epoch only");
        }).verify();
    }
}
