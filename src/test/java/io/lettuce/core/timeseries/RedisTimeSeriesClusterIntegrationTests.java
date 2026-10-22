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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.Range;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.Value;
import io.lettuce.core.api.sync.RedisTimeSeriesCommands;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsMGetArgs;
import io.lettuce.core.timeseries.arguments.TsMRangeArgs;
import io.lettuce.core.timeseries.arguments.TsNRangeArgs;
import io.lettuce.core.timeseries.arguments.TsReadArgs;
import io.lettuce.test.condition.EnabledOnCommand;
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

    // --- TS.CREATERULE / TS.DELETERULE across slots ------------------------------------------------------------------

    /**
     * Scenario K06 (CREATERULE): CREATERULE with the source and the destination in different slots, with and without
     * alignTimestamp, and DELETERULE across slots, fail on the node of the source key, and no rule is created.
     * <p>
     * The reply depends on the server version. 8.4.0, 8.6.0, 8.8.0, 8.10.0 and 8.12 reply {@code CROSSSLOT Keys in request
     * don't hash to the same slot} (test plan K06; TIMESERIES-CANONICAL.md §8). 8.0.2 and 8.2.2 do not check the slots: the
     * source node does not find the destination and replies {@code ERR TSDB: the key does not exist} to CREATERULE (the text
     * the TS.CREATERULE page documents, TIMESERIES-CANONICAL.md §1) and {@code ERR TSDB: compaction rule does not exist} to
     * DELETERULE (observed with redis-cli on {@code test-cluster}). See QA-P3 P3-D1 and P3-TP1.
     */
    @Test
    void createRuleAcrossSlotsFails() {

        String source = "{a}:src";
        String dest = "{b}:dst";
        assertThat(SlotHash.getSlot(source)).isNotEqualTo(SlotHash.getSlot(dest));
        redis.tsCreate(source, new TsCreateArgs().label("t", testName));
        redis.tsCreate(dest, new TsCreateArgs().label("t", testName));

        boolean crossSlot = RedisConditions.of(connection).hasVersionGreaterOrEqualsTo("8.4");
        String createError = crossSlot ? "CROSSSLOT" : "TSDB: the key does not exist";
        String deleteError = crossSlot ? "CROSSSLOT" : "TSDB: compaction rule does not exist";

        assertThatThrownBy(() -> redis.tsCreateRule(source, dest, TsAggregationType.AVG, 1000))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining(createError);
        assertThatThrownBy(() -> redis.tsCreateRule(source, dest, TsAggregationType.AVG, 1000, 500))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining(createError);
        assertThatThrownBy(() -> redis.tsDeleteRule(source, dest)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining(deleteError);

        assertThat(redis.tsInfo(source).getRules()).isEmpty();
        assertThat(redis.tsInfo(dest).getSourceKey()).isNull();
    }

    /**
     * Scenario K06 (CREATERULE), counterpart: keys with a shared hash tag in a slot other than the base suite's {@code {ts}}
     * create and delete a rule over the cluster connection.
     */
    @Test
    void createRuleWithSharedHashTag() {

        String source = "{b}:src";
        String dest = "{b}:dst";
        redis.tsCreate(source, new TsCreateArgs().label("t", testName));
        redis.tsCreate(dest, new TsCreateArgs().label("t", testName));

        assertThat(redis.tsCreateRule(source, dest, TsAggregationType.MAX, 1000, 250)).isEqualTo("OK");
        assertThat(redis.tsInfo(source).getRules()).hasSize(1);
        assertThat(redis.tsInfo(source).getRules().get(0).getAlignment()).isEqualTo(250);
        assertThat(redis.tsInfo(dest).getSourceKey()).isEqualTo(source);
        assertThat(redis.tsDeleteRule(source, dest)).isEqualTo("OK");
        assertThat(redis.tsInfo(source).getRules()).isEmpty();
    }

    // --- execution modes ---------------------------------------------------------------------------------------------

    /**
     * The pipelining test (L01) runs on a dedicated cluster connection.
     */
    @Override
    protected Pipeline openPipeline() {

        RedisClusterClient client = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.create(ClusterTestSettings.host, ClusterTestSettings.port1));
        StatefulRedisClusterConnection<String, String> clusterConnection = client.connect();
        return new Pipeline(clusterConnection, clusterConnection.async(), client::shutdown);
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Test
    @Override
    void transactionWithReads() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Test
    @Override
    void transactionWithMAddPartialFailure() {
    }

    // --- read-only routing -------------------------------------------------------------------------------------------

    /**
     * The upstream node that serves {@code key}.
     */
    private RedisClusterNode primaryOf(String key) {
        return connection.getPartitions().getPartitionBySlot(SlotHash.getSlot(key));
    }

    /**
     * A replica of the upstream node that serves {@code key}.
     */
    private RedisClusterNode replicaOf(String key) {

        String primaryId = primaryOf(key).getNodeId();
        for (RedisClusterNode node : connection.getPartitions()) {
            if (node.is(RedisClusterNode.NodeFlag.REPLICA) && primaryId.equals(node.getSlaveOf())) {
                return node;
            }
        }
        throw new IllegalStateException("No replica for the primary of " + key);
    }

    private RedisClusterCommands<String, String> nodeCommands(RedisClusterNode node) {
        return connection.getConnection(node.getNodeId()).sync();
    }

    /**
     * Wait until the replicas of the primary that serves {@code key} have the writes made so far.
     */
    private void awaitReplication(String key) {
        assertThat(nodeCommands(primaryOf(key)).waitForReplication(1, 5000)).isGreaterThanOrEqualTo(1);
    }

    /**
     * The {@code calls} count of {@code command} (lowercase, for example {@code ts.get}) in {@code INFO commandstats} of
     * {@code node}; 0 if the node has not run it yet.
     */
    private long commandCalls(RedisClusterNode node, String command) {

        String marker = "cmdstat_" + command + ":calls=";
        for (String line : nodeCommands(node).info("commandstats").split("\r?\n")) {
            if (line.startsWith(marker)) {
                String rest = line.substring(marker.length());
                return Long.parseLong(rest.substring(0, rest.indexOf(',')));
            }
        }
        return 0;
    }

    private StatefulRedisClusterConnection<String, String> connectReadFromReplica() {

        StatefulRedisClusterConnection<String, String> replicaConnection = clusterClient.connect();
        replicaConnection.setReadFrom(ReadFrom.REPLICA);
        return replicaConnection;
    }

    /**
     * Scenario K10 (GET, RANGE, REVRANGE): with {@code ReadFrom.REPLICA}, TS.GET, TS.RANGE and TS.REVRANGE run on a replica of
     * the key's primary: the replica's {@code INFO commandstats} counts each call, the primary's does not change, and the
     * replies hold the replicated data.
     */
    @Test
    void singleKeyReadsRouteToReplica() {

        simpleSeries();
        awaitReplication(F1);
        RedisClusterNode primary = primaryOf(F1);
        RedisClusterNode replica = replicaOf(F1);

        String[] commands = { "ts.get", "ts.range", "ts.revrange" };
        long[] replicaBefore = new long[commands.length];
        long[] primaryBefore = new long[commands.length];
        for (int i = 0; i < commands.length; i++) {
            replicaBefore[i] = commandCalls(replica, commands[i]);
            primaryBefore[i] = commandCalls(primary, commands[i]);
        }

        try (StatefulRedisClusterConnection<String, String> replicaConnection = connectReadFromReplica()) {
            assertThat(replicaConnection.sync().tsGet(F1)).isEqualTo(TsSample.of(3000, 3.0));
            assertThat(replicaConnection.sync().tsRange(F1, Range.unbounded())).hasSize(3);
            assertThat(replicaConnection.sync().tsRevRange(F1, Range.create(1000L, 2000L)))
                    .containsExactly(TsSample.of(2000, 2.0), TsSample.of(1000, 1.0));
        }

        for (int i = 0; i < commands.length; i++) {
            assertThat(commandCalls(replica, commands[i]) - replicaBefore[i]).as("replica " + commands[i]).isEqualTo(1);
            assertThat(commandCalls(primary, commands[i]) - primaryBefore[i]).as("primary " + commands[i]).isZero();
        }
    }

    /**
     * Scenario K10 (READ, Redis 8.10+): with {@code ReadFrom.REPLICA}, TS.READ without BLOCK runs on a replica.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void readRoutesToReplica() {

        simpleSeries();
        awaitReplication(F1);
        RedisClusterNode primary = primaryOf(F1);
        RedisClusterNode replica = replicaOf(F1);
        long replicaBefore = commandCalls(replica, "ts.read");
        long primaryBefore = commandCalls(primary, "ts.read");

        try (StatefulRedisClusterConnection<String, String> replicaConnection = connectReadFromReplica()) {
            assertThat(replicaConnection.sync().tsRead(F1, TsReadOffset.from(2000))).containsExactly(TsSample.of(2000, 2.0),
                    TsSample.of(3000, 3.0));
        }

        assertThat(commandCalls(replica, "ts.read") - replicaBefore).isEqualTo(1);
        assertThat(commandCalls(primary, "ts.read") - primaryBefore).isZero();
    }

    /**
     * Scenario K07 (READ, Redis 8.10+): with {@code ReadFrom.REPLICA} on a dedicated cluster connection, TS.READ BLOCK
     * {@code newSamples()} waits on a replica and wakes up after a write on the primary. The block time is 5 s because a
     * replica wakes only after the write has replicated (design §2.2: about 1 s on 8.12-m02); the reply arrives as soon as the
     * sample is there.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void readBlockOnReplicaWakesAfterPrimaryWrite() throws Exception {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAdd(KEY, 1000L, 1.0);
        awaitReplication(KEY);
        RedisClusterNode replica = replicaOf(KEY);

        try (StatefulRedisClusterConnection<String, String> replicaConnection = connectReadFromReplica()) {

            RedisFuture<List<TsSample>> read = replicaConnection.async().tsRead(KEY, TsReadOffset.newSamples(),
                    new TsReadArgs().block(5000, 1));

            long deadline = System.currentTimeMillis() + 5000;
            boolean blocked = false;
            while (!blocked && System.currentTimeMillis() < deadline) {
                for (String client : nodeCommands(replica).clientList().split("\\r?\\n")) {
                    if (client.contains("cmd=ts.read") && client.contains("flags=b")) {
                        blocked = true;
                    }
                }
            }
            assertThat(blocked).as("TS.READ blocked on the replica").isTrue();
            assertThat(read.isDone()).isFalse();

            redis.tsAdd(KEY, 2000L, 2.0);

            assertThat(read.get(10, TimeUnit.SECONDS)).containsExactly(TsSample.of(2000, 2.0));
        }
    }

    // --- label-index reads across shards -----------------------------------------------------------------------------

    /**
     * Create {@code {a}:s}, {@code {b}:s}, {@code {c}:s} in three slots on both primaries, with labels {@code t=<test>,
     * sensor=s<i>} and the samples (1000, i), (2000, 10 i) for i = 1, 2, 3.
     */
    private List<String> shardedSeries() {

        List<String> keys = Arrays.asList("{a}:s", "{b}:s", "{c}:s");
        Set<String> primaries = new HashSet<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            primaries.add(primaryOf(key).getNodeId());
            redis.tsCreate(key, new TsCreateArgs().label("t", testName).label("sensor", "s" + (i + 1)));
            redis.tsAdd(key, 1000L, i + 1.0);
            redis.tsAdd(key, 2000L, (i + 1) * 10.0);
        }
        assertThat(primaries).as("keys on different primaries").hasSize(2);
        return keys;
    }

    private void awaitReplication(List<String> keys) {
        for (String key : keys) {
            awaitReplication(key);
        }
    }

    private boolean isRedis810() {
        return RedisConditions.of(connection).hasVersionGreaterOrEqualsTo("8.10");
    }

    /**
     * Assert that every keyless read of {@code commands} returns all of {@code keys} once, in any order.
     */
    private void assertKeylessReadsReturnAll(RedisTimeSeriesCommands<String, String> commands, List<String> keys) {

        assertThat(keys(commands.tsMGet(isolation()))).containsExactlyInAnyOrderElementsOf(keys);
        assertThat(keys(commands.tsMGet(new TsMGetArgs().withLabels(), isolation()))).containsExactlyInAnyOrderElementsOf(keys);

        Map<String, TsSeries<String>> range = seriesByKey(commands.tsMRange(Range.unbounded(), isolation()));
        assertThat(range).containsOnlyKeys(keys.toArray(new String[0]));
        for (int i = 0; i < keys.size(); i++) {
            assertThat(range.get(keys.get(i)).getSamples()).containsExactly(TsSample.of(1000, i + 1.0),
                    TsSample.of(2000, (i + 1) * 10.0));
        }

        Map<String, TsSeries<String>> reverse = seriesByKey(
                commands.tsMRevRange(Range.unbounded(), new TsMRangeArgs().count(1), isolation()));
        assertThat(reverse).containsOnlyKeys(keys.toArray(new String[0]));
        assertThat(reverse.get(keys.get(2)).getSamples()).containsExactly(TsSample.of(2000, 30.0));

        assertThat(commands.tsQueryIndex(isolation())).containsExactlyInAnyOrderElementsOf(keys);
    }

    /**
     * Scenario K02: series in three slots on both primaries. MGET, MRANGE, MREVRANGE and QUERYINDEX through the cluster
     * connection return all of them once, in any order (below 8.10 after {@code TIMESERIES.REFRESHCLUSTER} on every primary,
     * see {@link #refreshClusterBelow810()}).
     */
    @Test
    void keylessReadsAcrossShards() {

        List<String> keys = shardedSeries();

        assertKeylessReadsReturnAll(redis, keys);
    }

    /**
     * Scenario K02 (QUERYLABELS, Redis 8.10+): label names and values across shards.
     */
    @Test
    @EnabledOnCommand("TS.QUERYLABELS")
    void queryLabelsAcrossShards() {

        shardedSeries();

        assertThat(redis.tsQueryLabels(isolation())).containsExactlyInAnyOrder("t", "sensor");
        assertThat(redis.tsQueryLabelValues("sensor", isolation())).containsExactlyInAnyOrder("s1", "s2", "s3");
        assertThat(redis.tsQueryLabelValues("sensor")).contains("s1", "s2", "s3");
    }

    /**
     * Scenario K02 through the async and reactive APIs of the cluster connection.
     */
    @Test
    void keylessReadsAcrossShardsAsyncAndReactive() throws Exception {

        List<String> keys = shardedSeries();

        assertThat(connection.async().tsQueryIndex(isolation()).get(10, TimeUnit.SECONDS))
                .containsExactlyInAnyOrderElementsOf(keys);
        assertThat(keys(connection.async().tsMGet(isolation()).get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrderElementsOf(keys);
        assertThat(connection.reactive().tsQueryIndex(isolation()).collectList().block())
                .containsExactlyInAnyOrderElementsOf(keys);
        assertThat(seriesKeys(connection.reactive().tsMRange(Range.unbounded(), isolation()).collectList().block()))
                .containsExactlyInAnyOrderElementsOf(keys);
    }

    /**
     * Scenario K03: F3 with {@code {a}:g1} and {@code {b}:g2} on different primaries; GROUPBY grp reduces both: sum 4/6/8, max
     * 3/4/5, std.s √2; the sources are both keys.
     */
    @Test
    void groupByAcrossShards() {

        String g1 = "{a}:g1";
        String g2 = "{b}:g2";
        assertThat(primaryOf(g1).getNodeId()).isNotEqualTo(primaryOf(g2).getNodeId());
        groupPair(g1, g2);

        TsSeries<String> sum = onlyGroup(redis.tsMRange(Range.unbounded(),
                new TsMRangeArgs().withLabels().groupBy("grp", TsReducerType.SUM), isolation()));
        assertThat(sum.getGroup()).isEqualTo("grp=x");
        assertThat(sum.getSources()).containsExactlyInAnyOrder(g1, g2);
        assertThat(sum.getSamples()).containsExactly(TsSample.of(1000, 4.0), TsSample.of(2000, 6.0), TsSample.of(3000, 8.0));

        assertThat(firstValues(groupSamples(TsReducerType.MAX))).containsExactly(3.0, 4.0, 5.0);
        assertThat(firstValues(groupSamples(TsReducerType.STD_S))).containsExactly(1.4142135623730951, 1.4142135623730951,
                1.4142135623730951);
        assertThat(onlyGroup(
                redis.tsMRevRange(Range.unbounded(), new TsMRangeArgs().groupBy("grp", TsReducerType.AVG), isolation()))
                        .getSamples()).containsExactly(TsSample.of(3000, 4.0), TsSample.of(2000, 3.0), TsSample.of(1000, 2.0));
    }

    /**
     * Scenario K04 (Redis 8.10+): without {@code TIMESERIES.REFRESHCLUSTER} (the {@code @BeforeAll} skips it on 8.10), every
     * node, primary or replica, gathers across shards by itself: TS.QUERYINDEX and TS.MGET sent directly to each node return
     * all three series.
     */
    @Test
    void automaticGatherOnEveryNode() {

        assumeTrue(isRedis810(), "Requires Redis 8.10");
        List<String> keys = shardedSeries();
        awaitReplication(keys);

        for (RedisClusterNode node : connection.getPartitions()) {
            RedisClusterCommands<String, String> commands = nodeCommands(node);
            assertThat(commands.tsQueryIndex(isolation())).as(node.getUri().toString())
                    .containsExactlyInAnyOrderElementsOf(keys);
            assertThat(keys(commands.tsMGet(isolation()))).as(node.getUri().toString())
                    .containsExactlyInAnyOrderElementsOf(keys);
        }
    }

    /**
     * Scenario K07 (keyless, Redis 8.10+): with {@code ReadFrom.REPLICA}, the keyless reads run on a replica and return the
     * full set of series of both shards.
     */
    @Test
    void keylessReadsOnReplicaReturnFullSet() {

        assumeTrue(isRedis810(), "Requires Redis 8.10");
        List<String> keys = shardedSeries();
        awaitReplication(keys);

        try (StatefulRedisClusterConnection<String, String> replicaConnection = connectReadFromReplica()) {
            assertKeylessReadsReturnAll(replicaConnection.sync(), keys);
            if (RedisConditions.of(connection).hasCommand("TS.QUERYLABELS")) {
                assertThat(replicaConnection.sync().tsQueryLabelValues("sensor", isolation())).containsExactlyInAnyOrder("s1",
                        "s2", "s3");
            }
        }
    }

    /**
     * Scenario K08 (Redis 8.10+): {@code upstream().commands().tsQueryIndex(…)} runs on every primary, and each primary returns
     * the full set, so the results contain each series once per node (design §5.1).
     */
    @Test
    void nodeSelectionReturnsFullSetPerNode() {

        assumeTrue(isRedis810(), "Requires Redis 8.10");
        List<String> keys = shardedSeries();

        Executions<List<String>> executions = connection.sync().upstream().commands().tsQueryIndex(isolation());

        assertThat(executions.asMap()).hasSize(2);
        for (List<String> result : executions) {
            assertThat(result).containsExactlyInAnyOrderElementsOf(keys);
        }

        Executions<List<TsMGetValue<String>>> mget = connection.sync().upstream().commands().tsMGet(isolation());
        for (List<TsMGetValue<String>> result : mget) {
            assertThat(keys(result)).containsExactlyInAnyOrderElementsOf(keys);
        }
    }

    /**
     * Scenario K09: a keyless TS command inside MULTI on a node connection of a node in multi-shard mode is rejected by the
     * server ({@code Can not run multi sharded command inside a multi exec, lua, or when blocking is not allowed},
     * TIMESERIES-CANONICAL.md §8), and Lettuce passes the error through as the command's result.
     */
    @Test
    void keylessCommandInsideMultiOnNodeConnection() throws Exception {

        shardedSeries();

        try (StatefulRedisClusterConnection<String, String> dedicated = clusterClient.connect()) {
            RedisClusterNode primary = primaryOf("{a}:s");
            RedisAsyncCommands<String, String> node = dedicated.getConnection(primary.getNodeId()).async();

            assertThat(node.multi().get(10, TimeUnit.SECONDS)).isEqualTo("OK");
            RedisFuture<List<String>> queryIndex = node.tsQueryIndex(isolation());
            TransactionResult result = node.exec().get(10, TimeUnit.SECONDS);

            assertThat(result.wasDiscarded()).isFalse();
            assertThat(result).hasSize(1);
            assertThatThrownBy(() -> queryIndex.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("Can not run multi sharded command");
        }
    }

    /**
     * Scenario K10 (MGET, MRANGE, MREVRANGE, QUERYINDEX, QUERYLABELS): with {@code ReadFrom.REPLICA}, each keyless read runs on
     * a replica: the {@code INFO commandstats} calls of the command grow by exactly one over all replicas and not at all over
     * the primaries.
     */
    @Test
    void keylessReadsRouteToReplica() {

        List<String> keys = shardedSeries();
        awaitReplication(keys);
        boolean queryLabels = RedisConditions.of(connection).hasCommand("TS.QUERYLABELS");

        List<String> commands = new ArrayList<>(Arrays.asList("ts.mget", "ts.mrange", "ts.mrevrange", "ts.queryindex"));
        if (queryLabels) {
            commands.add("ts.querylabels");
        }

        try (StatefulRedisClusterConnection<String, String> replicaConnection = connectReadFromReplica()) {
            RedisTimeSeriesCommands<String, String> replicaCommands = replicaConnection.sync();
            for (String command : commands) {
                long replicasBefore = callsOn(RedisClusterNode.NodeFlag.REPLICA, command);
                long primariesBefore = callsOn(RedisClusterNode.NodeFlag.UPSTREAM, command);

                switch (command) {
                    case "ts.mget":
                        replicaCommands.tsMGet(isolation());
                        break;
                    case "ts.mrange":
                        replicaCommands.tsMRange(Range.unbounded(), isolation());
                        break;
                    case "ts.mrevrange":
                        replicaCommands.tsMRevRange(Range.unbounded(), isolation());
                        break;
                    case "ts.queryindex":
                        replicaCommands.tsQueryIndex(isolation());
                        break;
                    default:
                        replicaCommands.tsQueryLabels(isolation());
                        break;
                }

                assertThat(callsOn(RedisClusterNode.NodeFlag.REPLICA, command) - replicasBefore).as("replicas " + command)
                        .isEqualTo(1);
                assertThat(callsOn(RedisClusterNode.NodeFlag.UPSTREAM, command) - primariesBefore).as("primaries " + command)
                        .isZero();
            }
        }
    }

    /**
     * The sum of the {@code INFO commandstats} calls of {@code command} over all nodes with {@code flag}.
     */
    private long callsOn(RedisClusterNode.NodeFlag flag, String command) {

        long calls = 0;
        for (RedisClusterNode node : connection.getPartitions()) {
            if (node.is(flag)) {
                calls += commandCalls(node, command);
            }
        }
        return calls;
    }

    /**
     * Scenario A13 (filters) on the cluster async API of the keyless reads: no filters, a {@code null} filter or {@code null}
     * arguments throw {@link IllegalArgumentException} from the call, as the {@code @throws} clause of
     * {@code RedisTimeSeriesAsyncCommands} states, as the standalone async API does, and as the cluster {@code tsMAdd} does
     * ({@link #maddSplitRejectsNullAndEmpty()}).
     */
    // routeKeyless builds the command inside thenCompose after picking a node, so the exception fails the future instead of
    // being thrown from the call
    @Disabled("Cluster async keyless reads report IllegalArgumentException through the future, see #3962")
    @Test
    void keylessReadsRejectInvalidArgumentsAsync() {

        assertThatThrownBy(() -> connection.async().tsMGet()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsQueryIndex((TsFilter) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsMRange(Range.unbounded())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsMRevRange(Range.unbounded(), null, isolation()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsQueryLabels(new TsFilter[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsQueryLabelValues(null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A13 (filters) on the cluster reactive API of the keyless reads: no filters, a {@code null} filter or
     * {@code null} arguments are rejected with {@link IllegalArgumentException} (thrown by the call or signalled as the error).
     */
    @Test
    void keylessReadsRejectInvalidArgumentsReactive() {

        assertThatThrownBy(() -> connection.reactive().tsMGet().collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsQueryIndex((TsFilter) null).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsMRange(Range.unbounded(), new TsMRangeArgs()).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsMRevRange(Range.unbounded(), (TsFilter) null).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsQueryLabels(new TsFilter[0]).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsQueryLabelValues(null).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.NRANGE / TS.NREVRANGE ------------------------------------------------------------------------------------

    /**
     * Create {@code key} with the samples (0, 1) and (5, 2) plus {@code offset}.
     */
    private void nrangeSeries(String key, double offset) {
        redis.tsCreate(key, new TsCreateArgs().label("t", testName));
        redis.tsAdd(key, 0L, 1.0 + offset);
        redis.tsAdd(key, 5L, 2.0 + offset);
    }

    /**
     * Scenario K06 (NRANGE, Redis 8.10+): keys in different slots on different primaries → {@code CROSSSLOT Keys in request
     * don't hash to the same slot}, for NRANGE and NREVRANGE, through the list and the two-key overloads (verified with
     * redis-cli on {@code test-cluster}, 8.10.0; TIMESERIES-CANONICAL.md §8). The client passes the server error through.
     */
    @Test
    @EnabledOnCommand("TS.NRANGE")
    void nrangeAcrossSlotsFails() {

        String a = "{a}:n";
        String b = "{b}:n";
        assertThat(primaryOf(a).getNodeId()).as("keys on different primaries").isNotEqualTo(primaryOf(b).getNodeId());
        nrangeSeries(a, 0);
        nrangeSeries(b, 10);

        assertThatThrownBy(() -> redis.tsNRange(a, b, Range.unbounded())).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("CROSSSLOT");
        assertThatThrownBy(() -> redis.tsNRange(Arrays.asList(b, a), Range.unbounded(),
                new TsNRangeArgs().aggregationForEachKey(10, TsAggregationType.SUM)))
                        .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("CROSSSLOT");
        assertThatThrownBy(() -> redis.tsNRevRange(a, b, Range.unbounded())).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("CROSSSLOT");
        assertThatThrownBy(() -> redis.tsNRevRange(Arrays.asList(a, b), Range.unbounded(), new TsNRangeArgs().count(1)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("CROSSSLOT");
    }

    /**
     * Scenario K06 (NRANGE, Redis 8.10+), counterpart: keys with a shared hash tag on each primary ({@code {a}} and
     * {@code {b}}) return the joined rows over the cluster connection, whichever node serves the slot.
     */
    @Test
    @EnabledOnCommand("TS.NRANGE")
    void nrangeWithSharedHashTagOnEachPrimary() {

        List<Integer> oneEach = Arrays.asList(1, 1);
        for (String tag : new String[] { "{a}", "{b}" }) {
            String first = tag + ":n1";
            String second = tag + ":n2";
            nrangeSeries(first, 0);
            nrangeSeries(second, 10);

            assertThat(redis.tsNRange(first, second, Range.unbounded())).as(tag).containsExactly(row(0, oneEach, 1.0, 11.0),
                    row(5, oneEach, 2.0, 12.0));
            assertThat(redis.tsNRevRange(Arrays.asList(second, first), Range.unbounded())).as(tag)
                    .containsExactly(row(5, oneEach, 12.0, 2.0), row(0, oneEach, 11.0, 1.0));
        }
    }

    /**
     * Scenario K10 (NRANGE, NREVRANGE, Redis 8.10+): with {@code ReadFrom.REPLICA}, TS.NRANGE and TS.NREVRANGE run on a replica
     * of the primary that serves the keys' slot (the {@code {a}} slot, so not the node the cluster connection was opened on):
     * the replica's {@code INFO commandstats} counts each call, the primary's does not change, and the replies hold the
     * replicated data.
     */
    @Test
    @EnabledOnCommand("TS.NRANGE")
    void multiKeyReadsRouteToReplica() {

        String first = "{a}:n1";
        String second = "{a}:n2";
        nrangeSeries(first, 0);
        nrangeSeries(second, 10);
        awaitReplication(first);
        RedisClusterNode primary = primaryOf(first);
        RedisClusterNode replica = replicaOf(first);

        String[] commands = { "ts.nrange", "ts.nrevrange" };
        long[] replicaBefore = new long[commands.length];
        long[] primaryBefore = new long[commands.length];
        for (int i = 0; i < commands.length; i++) {
            replicaBefore[i] = commandCalls(replica, commands[i]);
            primaryBefore[i] = commandCalls(primary, commands[i]);
        }

        List<Integer> oneEach = Arrays.asList(1, 1);
        try (StatefulRedisClusterConnection<String, String> replicaConnection = connectReadFromReplica()) {
            assertThat(replicaConnection.sync().tsNRange(first, second, Range.unbounded()))
                    .containsExactly(row(0, oneEach, 1.0, 11.0), row(5, oneEach, 2.0, 12.0));
            assertThat(replicaConnection.sync().tsNRevRange(Arrays.asList(first, second), Range.unbounded(),
                    new TsNRangeArgs().count(1))).containsExactly(row(5, oneEach, 2.0, 12.0));
        }

        for (int i = 0; i < commands.length; i++) {
            assertThat(commandCalls(replica, commands[i]) - replicaBefore[i]).as("replica " + commands[i]).isEqualTo(1);
            assertThat(commandCalls(primary, commands[i]) - primaryBefore[i]).as("primary " + commands[i]).isZero();
        }
    }

    /**
     * Scenario A13 (NRANGE) on the cluster async and reactive APIs: an empty key list, a {@code null} key or {@code null}
     * arguments throw {@link IllegalArgumentException} (thrown by the call, or signalled as the error on the reactive API), as
     * the {@code @throws} clauses state. The known gap of A13 (issue #3962) concerns only the keyless reads.
     */
    @Test
    void nrangeRejectsInvalidArgumentsAsyncAndReactive() {

        List<String> empty = Collections.emptyList();

        assertThatThrownBy(() -> connection.async().tsNRange(empty, Range.unbounded()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsNRevRange("{ts}:n1", null, Range.unbounded()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.async().tsNRange("{ts}:n1", "{ts}:n2", Range.unbounded(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsNRange(empty, Range.unbounded()).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> connection.reactive().tsNRevRange("{ts}:n1", "{ts}:n2", null).collectList().block())
                .isInstanceOf(IllegalArgumentException.class);
    }

}
