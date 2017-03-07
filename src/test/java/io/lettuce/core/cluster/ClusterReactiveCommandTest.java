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

import static io.lettuce.core.cluster.ClusterTestUtil.getNodeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.*;
import org.junit.runners.MethodSorters;

import reactor.test.StepVerifier;

import io.lettuce.TestClientResources;
import io.lettuce.core.FastShutdown;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.models.slots.ClusterSlotRange;
import io.lettuce.core.cluster.models.slots.ClusterSlotsParser;
import io.lettuce.core.internal.LettuceLists;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class ClusterReactiveCommandTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected RedisClusterReactiveCommands<String, String> reactive;
    protected RedisAsyncCommands<String, String> async;

    @BeforeClass
    public static void setupClient() throws Exception {
        setupClusterClient();
        client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port1).build());
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                LettuceLists.unmodifiableList(RedisURI.Builder.redis(host, port1).build()));

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

        async = client.connect(RedisURI.Builder.redis(host, port1).build()).async();
        reactive = async.getStatefulConnection().reactive();
    }

    @After
    public void after() throws Exception {
        async.getStatefulConnection().close();
    }

    @Test
    public void testClusterBumpEpoch() throws Exception {
        StepVerifier.create(reactive.clusterBumpepoch())
                .consumeNextWith(actual -> assertThat(actual).matches("(BUMPED|STILL).*")).verifyComplete();
    }

    @Test
    public void testClusterInfo() throws Exception {

        StepVerifier.create(reactive.clusterInfo()).consumeNextWith(actual -> {
            assertThat(actual).contains("cluster_known_nodes:");
            assertThat(actual).contains("cluster_slots_fail:0");
            assertThat(actual).contains("cluster_state:");
        }).verifyComplete();
    }

    @Test
    public void testClusterNodes() throws Exception {

        StepVerifier.create(reactive.clusterNodes()).consumeNextWith(actual -> {
            assertThat(actual).contains("connected");
            assertThat(actual).contains("master");
            assertThat(actual).contains("myself");
        }).verifyComplete();
    }

    @Test
    public void testClusterSlaves() throws Exception {
        StepVerifier.create(reactive.waitForReplication(1, 5)).expectNextCount(1).verifyComplete();
    }

    @Test
    public void testAsking() throws Exception {
        StepVerifier.create(reactive.asking()).expectNext("OK").verifyComplete();
    }

    @Test
    public void testClusterSlots() throws Exception {

        List<Object> reply = reactive.clusterSlots().collectList().block();
        assertThat(reply.size()).isGreaterThan(1);

        List<ClusterSlotRange> parse = ClusterSlotsParser.parse(reply);
        assertThat(parse).hasSize(2);

        ClusterSlotRange clusterSlotRange = parse.get(0);
        assertThat(clusterSlotRange.getFrom()).isEqualTo(0);
        assertThat(clusterSlotRange.getTo()).isEqualTo(11999);

        assertThat(clusterSlotRange.toString()).contains(ClusterSlotRange.class.getSimpleName());
    }

    @Test
    public void clusterSlaves() throws Exception {

        String nodeId = getNodeId(async.getStatefulConnection().sync());
        List<String> result = reactive.clusterSlaves(nodeId).collectList().block();

        assertThat(result.size()).isGreaterThan(0);
    }
}
