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

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import reactor.test.StepVerifier;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.slots.ClusterSlotRange;
import io.lettuce.core.cluster.models.slots.ClusterSlotsParser;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class ClusterReactiveCommandIntegrationTests {

    private final RedisClusterClient clusterClient;
    private final RedisClusterReactiveCommands<String, String> reactive;
    private final RedisClusterCommands<String, String> sync;

    @Inject
    ClusterReactiveCommandIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {
        this.clusterClient = clusterClient;

        this.reactive = connection.reactive();
        this.sync = connection.sync();
    }

    @Test
    void testClusterBumpEpoch() {
        StepVerifier.create(reactive.clusterBumpepoch())
                .consumeNextWith(actual -> assertThat(actual).matches("(BUMPED|STILL).*")).verifyComplete();
    }

    @Test
    void testClusterInfo() {

        StepVerifier.create(reactive.clusterInfo()).consumeNextWith(actual -> {
            assertThat(actual).contains("cluster_known_nodes:");
            assertThat(actual).contains("cluster_slots_fail:0");
            assertThat(actual).contains("cluster_state:");
        }).verifyComplete();
    }

    @Test
    void testClusterNodes() {

        StepVerifier.create(reactive.clusterNodes()).consumeNextWith(actual -> {
            assertThat(actual).contains("connected");
            assertThat(actual).contains("master");
            assertThat(actual).contains("myself");
        }).verifyComplete();
    }

    @Test
    void testAsking() {
        StepVerifier.create(reactive.asking()).expectNext("OK").verifyComplete();
    }

    @Test
    void testClusterSlots() {

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
    void clusterSlaves() {

        RedisClusterNode master = clusterClient.getPartitions().stream().filter(it -> it.is(RedisClusterNode.NodeFlag.UPSTREAM))
                .findFirst().get();

        List<String> result = reactive.clusterSlaves(master.getNodeId()).collectList().block();

        assertThat(result.size()).isGreaterThan(0);
    }
}
