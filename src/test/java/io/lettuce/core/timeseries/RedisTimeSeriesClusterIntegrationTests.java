/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.Value;
import io.lettuce.core.api.sync.RedisTimeSeriesCommands;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.resource.TestClientResources;
import reactor.test.StepVerifier;

/**
 * Runs {@link RedisTimeSeriesIntegrationTests} over Redis Cluster ({@code test-cluster}), with the hash-tagged keys of the base
 * suite.
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesClusterIntegrationTests extends RedisTimeSeriesIntegrationTests {

    private static final ProtocolKeyword REFRESHCLUSTER = new ProtocolKeyword() {

        private final byte[] bytes = "TIMESERIES.REFRESHCLUSTER".getBytes(StandardCharsets.US_ASCII);

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return "TIMESERIES.REFRESHCLUSTER";
        }

    };

    private final StatefulRedisClusterConnection<String, String> connection;

    private final RedisClusterClient clusterClient;

    @Inject
    RedisTimeSeriesClusterIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
        this.clusterClient = clusterClient;
        this.connection = connection;
    }

    /**
     * Below 8.10, keyless commands gather across shards only after {@code TIMESERIES.REFRESHCLUSTER} ran on the answering node
     * (test plan §3).
     */
    @BeforeAll
    void refreshClusterBelow810() {

        if (RedisConditions.of(connection).hasVersionGreaterOrEqualsTo("8.10")) {
            return;
        }
        for (RedisClusterNode node : connection.getPartitions()) {
            if (node.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                String reply = connection.getConnection(node.getNodeId()).sync().dispatch(REFRESHCLUSTER,
                        new StatusOutput<>(StringCodec.UTF8));
                assertThat(reply).isEqualTo("OK");
            }
        }
    }

    /**
     * ACL users are per node, so create them on every node of the cluster. The cluster client also needs {@code CLUSTER} to
     * read the topology.
     */
    @Override
    protected void createAclUser(String user, String... rules) {

        String[] withCluster = Arrays.copyOf(rules, rules.length + 1);
        withCluster[rules.length] = "+cluster";
        for (RedisClusterNode node : connection.getPartitions()) {
            assertThat(aclSetuser(connection.getConnection(node.getNodeId()).sync(), user, withCluster)).isEqualTo("OK");
        }
    }

    @Override
    protected void deleteAclUser(String user) {
        for (RedisClusterNode node : connection.getPartitions()) {
            connection.getConnection(node.getNodeId()).sync().aclDeluser(user);
        }
    }

    /**
     * Run {@code action} on a dedicated cluster connection authenticated as {@code user}.
     */
    @Override
    protected void runAs(String user, Function<RedisTimeSeriesCommands<String, String>, Void> action) {

        RedisURI uri = RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1)
                .withAuthentication(user, "unused").build();
        RedisClusterClient client = RedisClusterClient.create(TestClientResources.get(), uri);
        try (StatefulRedisClusterConnection<String, String> clusterConnection = client.connect()) {
            action.apply(clusterConnection.sync());
        } finally {
            client.shutdown();
        }
    }

    // --- TS.MADD split by slot ---------------------------------------------------------------------------------------

    /**
     * Create the series {@code {a}:<suffix>}, {@code {b}:<suffix>}, {@code {c}:<suffix>} in three slots on both primaries, with
     * (1000, 5) on the b series so that a second sample at 1000 is rejected under policy BLOCK.
     */
    private List<String> splitSeries(String suffix) {

        List<String> keys = Arrays.asList("{a}:" + suffix, "{b}:" + suffix, "{c}:" + suffix);
        Set<String> primaries = new HashSet<>();
        for (String key : keys) {
            primaries.add(connection.getPartitions().getPartitionBySlot(SlotHash.getSlot(key)).getNodeId());
            redis.tsCreate(key, new TsCreateArgs().label("t", testName));
        }
        assertThat(primaries).as("keys on different primaries").hasSizeGreaterThanOrEqualTo(2);
        redis.tsAdd(keys.get(1), 1000L, 5.0);
        return keys;
    }

    private static List<TsMAddValue<String>> splitSamples(List<String> keys) {
        return Arrays.asList(TsMAddValue.of(keys.get(0), 1000L, 1.0), TsMAddValue.of(keys.get(1), 1000L, 6.0),
                TsMAddValue.of(keys.get(2), 1500L, 2.0), TsMAddValue.of(keys.get(0), 2000L, 3.0),
                TsMAddValue.of(keys.get(1), 2000L, 7.0));
    }

    private void assertSplitStored(List<String> keys) {
        assertThat(storedSamples(keys.get(0))).isEqualTo(samples(1000, 1.0, 2000, 3.0));
        assertThat(storedSamples(keys.get(1))).isEqualTo(samples(1000, 5.0, 2000, 7.0));
        assertThat(storedSamples(keys.get(2))).isEqualTo(samples(1500, 2.0));
    }

    /**
     * Scenario K05: MADD over 3 slots on both primaries, with a repeated key and one rejected sample, keeps the caller's order
     * and puts {@code null} at the rejected position (sync API).
     */
    @Test
    void maddSplitAcrossSlots() {

        List<String> keys = splitSeries("sync");

        assertThat(madd(splitSamples(keys))).containsExactly(1000L, null, 1500L, 2000L, 2000L);
        assertSplitStored(keys);
    }

    /**
     * Scenario K05: the same split through the async API of the cluster connection.
     */
    @Test
    void maddSplitAcrossSlotsAsync() throws Exception {

        List<String> keys = splitSeries("async");

        assertThat(connection.async().tsMAdd(splitSamples(keys)).get(10, TimeUnit.SECONDS)).containsExactly(1000L, null, 1500L,
                2000L, 2000L);
        assertSplitStored(keys);
    }

    /**
     * Scenario K05: the same split through the reactive API of the cluster connection emits {@code Value.empty()} at the
     * rejected position, in the caller's order.
     */
    @Test
    void maddSplitAcrossSlotsReactive() {

        List<String> keys = splitSeries("reactive");

        StepVerifier.create(connection.reactive().tsMAdd(splitSamples(keys))).expectNext(Value.just(1000L))
                .expectNext(Value.empty()).expectNext(Value.just(1500L)).expectNext(Value.just(2000L))
                .expectNext(Value.just(2000L)).verifyComplete();
        assertSplitStored(keys);
    }

    /**
     * Scenario K05: a missing key in one slot gives {@code null} at its position; the other slots are applied.
     */
    @Test
    void maddSplitWithMissingKey() {

        List<String> keys = splitSeries("missing");

        assertThat(madd(TsMAddValue.of(keys.get(0), 1000L, 1.0), TsMAddValue.of("{c}:absent", 1000L, 2.0),
                TsMAddValue.of(keys.get(2), 1000L, 3.0))).containsExactly(1000L, null, 1000L);
        assertThat(redis.exists("{c}:absent")).isEqualTo(0);
    }

    /**
     * Scenario A13 (MADD) on the cluster async and reactive APIs: a {@code null} element among keys of several slots, and an
     * empty list, are rejected with {@link IllegalArgumentException} (async: thrown by the call, as on standalone; reactive:
     * thrown by the call or signalled as the error, as for other reactive commands).
     */
    @Test
    void maddSplitRejectsNullAndEmpty() {

        List<TsMAddValue<String>> withNull = Arrays.asList(TsMAddValue.of("{a}:k", 1000L, 1.0), null,
                TsMAddValue.of("{b}:k", 1000L, 1.0));

        assertThatThrownBy(() -> connection.async().tsMAdd(withNull)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsMAdd(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsMAdd(withNull).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsMAdd(Collections.emptyList()).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario K05 with a binary codec: the split partitions by the encoded key, so keys of three slots on both primaries keep
     * the caller's order with a {@code byte[]} key type.
     */
    @Test
    void maddSplitAcrossSlotsWithByteArrayCodec() {

        List<String> keys = splitSeries("bytes");

        try (StatefulRedisClusterConnection<byte[], byte[]> binary = clusterClient.connect(ByteArrayCodec.INSTANCE)) {
            List<TsMAddValue<byte[]>> samples = new ArrayList<>();
            for (TsMAddValue<String> sample : splitSamples(keys)) {
                samples.add(TsMAddValue.of(sample.getKey().getBytes(StandardCharsets.UTF_8), sample.getTimestamp(),
                        sample.getValue()));
            }
            assertThat(binary.sync().tsMAdd(samples)).containsExactly(1000L, null, 1500L, 2000L, 2000L);
        }
        assertSplitStored(keys);
    }

}
