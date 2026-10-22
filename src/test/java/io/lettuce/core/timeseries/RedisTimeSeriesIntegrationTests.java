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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisTimeSeriesCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for {@link RedisTimeSeriesCommands}: the series lifecycle commands (TS.CREATE, TS.ALTER, TS.INFO). The
 * subclasses run the same tests on RESP2, through the reactive API and over Redis Cluster.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.ADD")
public class RedisTimeSeriesIntegrationTests {

    protected static final String KEY = "{ts}:k";

    protected static final String MISSING = "{ts}:missing";

    protected final RedisCommands<String, String> redis;

    /**
     * The test method name, used as the value of the isolation label {@code t}.
     */
    protected String testName;

    @Inject
    protected RedisTimeSeriesIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testName = testInfo.getTestMethod().map(m -> m.getName()).orElse("unknown");
        redis.flushall();
    }

    protected static Map<String, String> labels(String... pairs) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            labels.put(pairs[i], pairs[i + 1]);
        }
        return labels;
    }

    /**
     * Fixture F1 labels {@code t=<test>, sensor=temp, area=north}.
     */
    protected Map<String, String> f1Labels() {
        return labels("t", testName, "sensor", "temp", "area", "north");
    }

    // --- TS.CREATE ---------------------------------------------------------------------------------------------------

    /**
     * Scenario B01: CREATE with defaults, then INFO.
     */
    @Test
    void createWithDefaults() {

        assertThat(redis.tsCreate(KEY)).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(KEY);

        assertThat(info.getTotalSamples()).isEqualTo(0);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.COMPRESSED);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.BLOCK);
        assertThat(info.getRetentionTime()).isEqualTo(0);
        assertThat(info.getChunkSize()).isEqualTo(4096);
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(0L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(0.0);
        assertThat(info.getLabels()).isEmpty();
        assertThat(info.getRules()).isEmpty();
        assertThat(info.getSourceKey()).isNull();
    }

    /**
     * Scenario B01: CREATE with empty arguments behaves like CREATE without arguments.
     */
    @Test
    void createWithEmptyArgs() {

        assertThat(redis.tsCreate(KEY, new TsCreateArgs())).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.COMPRESSED);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.BLOCK);
        assertThat(info.getChunkSize()).isEqualTo(4096);
        assertThat(info.getLabels()).isEmpty();
    }

    /**
     * Scenario B02: CREATE with every option, then INFO: every field round-trips.
     */
    @Test
    void createWithEveryOption() {

        TsCreateArgs args = new TsCreateArgs().retention(5000).encoding(TsEncodingType.UNCOMPRESSED).chunkSize(128)
                .duplicatePolicy(TsDuplicatePolicyType.LAST).ignore(10, 0.5).labels(f1Labels());

        assertThat(redis.tsCreate(KEY, args)).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(KEY);

        assertThat(info.getTotalSamples()).isEqualTo(0);
        assertThat(info.getRetentionTime()).isEqualTo(5000);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.UNCOMPRESSED);
        assertThat(info.getChunkSize()).isEqualTo(128);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.LAST);
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(10L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(0.5);
        assertThat(info.getLabels()).containsExactlyEntriesOf(f1Labels());
        assertThat(info.getRules()).isEmpty();
        assertThat(info.getSourceKey()).isNull();
    }

    /**
     * Scenario B02: each duplicate policy round-trips through CREATE and INFO.
     */
    @Test
    void createWithEachDuplicatePolicy() {

        for (TsDuplicatePolicyType policy : TsDuplicatePolicyType.values()) {
            String key = "{ts}:policy:" + policy.name();
            redis.tsCreate(key, new TsCreateArgs().duplicatePolicy(policy).label("t", testName));
            assertThat(redis.tsInfo(key).getDuplicatePolicy()).as(policy.name()).isSameAs(policy);
        }
    }

    /**
     * Scenario B03: CREATE twice fails with {@code TSDB: key already exists}.
     */
    @Test
    void createTwice() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));

        assertThatThrownBy(() -> redis.tsCreate(KEY)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: key already exists");
        assertThatThrownBy(() -> redis.tsCreate(KEY, new TsCreateArgs().retention(1)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: key already exists");
    }

    /**
     * Scenario B04 (CREATE): with every option set, INFO labels are exactly the given ones; no option token is stored as a
     * label.
     */
    @Test
    void createStoresLabelsExactly() {

        Map<String, String> labels = labels("t", testName, "sensor", "temp", "area", "north");
        TsCreateArgs args = new TsCreateArgs().labels(labels).ignore(11, 12).duplicatePolicy(TsDuplicatePolicyType.FIRST)
                .encoding(TsEncodingType.UNCOMPRESSED).chunkSize(256).retention(1000);

        redis.tsCreate(KEY, args);

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getLabels()).containsExactlyEntriesOf(labels);
        assertThat(info.getLabels()).doesNotContainKeys("IGNORE", "UNCOMPRESSED", "DUPLICATE_POLICY", "ENCODING", "CHUNK_SIZE",
                "RETENTION", "FIRST");
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.FIRST);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.UNCOMPRESSED);
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(11L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(12.0);
    }

    /**
     * Scenario B04 (CREATE): non-ASCII label names and values are stored and returned exactly (design §3.5: UTF-8).
     */
    @Test
    void createStoresUtf8Labels() {

        Map<String, String> labels = labels("t", testName, "température", "°C", "город", "Москва");
        redis.tsCreate(KEY, new TsCreateArgs().labels(labels));

        assertThat(redis.tsInfo(KEY).getLabels()).containsExactlyEntriesOf(labels);
    }

    /**
     * Scenario B04 (CREATE): an empty label map creates a series without labels.
     */
    @Test
    void createWithEmptyLabels() {

        redis.tsCreate(KEY, new TsCreateArgs().labels(Collections.emptyMap()));

        assertThat(redis.tsInfo(KEY).getLabels()).isEmpty();
    }

    // --- TS.ALTER ----------------------------------------------------------------------------------------------------

    /**
     * Scenario B05: ALTER with labels replaces all labels.
     */
    @Test
    void alterReplacesLabels() {

        redis.tsCreate(KEY, new TsCreateArgs().labels(f1Labels()));

        Map<String, String> replaced = labels("t", testName, "sensor", "hum");
        assertThat(redis.tsAlter(KEY, new TsAlterArgs().labels(replaced))).isEqualTo("OK");

        assertThat(redis.tsInfo(KEY).getLabels()).containsExactlyEntriesOf(replaced);
    }

    /**
     * Scenario B05: ALTER without LABELS keeps the labels.
     */
    @Test
    void alterWithoutLabelsKeepsLabels() {

        redis.tsCreate(KEY, new TsCreateArgs().labels(f1Labels()));

        assertThat(redis.tsAlter(KEY, new TsAlterArgs().retention(1000))).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getLabels()).containsExactlyEntriesOf(f1Labels());
        assertThat(info.getRetentionTime()).isEqualTo(1000);
    }

    /**
     * Scenario B05: ALTER with an empty label map removes all labels.
     */
    @Test
    void alterWithEmptyLabelsRemovesAllLabels() {

        redis.tsCreate(KEY, new TsCreateArgs().labels(f1Labels()));

        assertThat(redis.tsAlter(KEY, new TsAlterArgs().labels(Collections.emptyMap()))).isEqualTo("OK");

        assertThat(redis.tsInfo(KEY).getLabels()).isEmpty();
    }

    /**
     * Scenario B06: ALTER retention, chunk size, duplicate policy and IGNORE updates the INFO fields; other fields are kept.
     */
    @Test
    void alterOptions() {

        redis.tsCreate(KEY, new TsCreateArgs().encoding(TsEncodingType.UNCOMPRESSED).labels(f1Labels()));

        TsAlterArgs args = new TsAlterArgs().retention(60000).chunkSize(256).duplicatePolicy(TsDuplicatePolicyType.MAX)
                .ignore(3, 4.5);
        assertThat(redis.tsAlter(KEY, args)).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getRetentionTime()).isEqualTo(60000);
        assertThat(info.getChunkSize()).isEqualTo(256);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.MAX);
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(3L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(4.5);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.UNCOMPRESSED);
        assertThat(info.getLabels()).containsExactlyEntriesOf(f1Labels());
    }

    /**
     * Scenario B06: ALTER of a single option changes only that option.
     */
    @Test
    void alterSingleOption() {

        redis.tsCreate(KEY,
                new TsCreateArgs().retention(5000).duplicatePolicy(TsDuplicatePolicyType.LAST).label("t", testName));

        redis.tsAlter(KEY, TsAlterArgs.Builder.duplicatePolicy(TsDuplicatePolicyType.SUM));

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.SUM);
        assertThat(info.getRetentionTime()).isEqualTo(5000);
        assertThat(info.getChunkSize()).isEqualTo(4096);
    }

    // --- TS.INFO -----------------------------------------------------------------------------------------------------

    /**
     * Scenario G01: INFO full model of a series without samples.
     */
    @Test
    void infoFullModel() {

        redis.tsCreate(KEY, new TsCreateArgs().retention(3000).encoding(TsEncodingType.COMPRESSED).chunkSize(1024)
                .duplicatePolicy(TsDuplicatePolicyType.MIN).ignore(5, 1.5).labels(f1Labels()));

        TsInfoValue<String> info = redis.tsInfo(KEY);

        assertThat(info.getTotalSamples()).isEqualTo(0);
        assertThat(info.getMemoryUsage()).isGreaterThan(0);
        assertThat(info.getFirstTimestamp()).isEqualTo(0);
        assertThat(info.getLastTimestamp()).isEqualTo(0);
        assertThat(info.getRetentionTime()).isEqualTo(3000);
        assertThat(info.getChunkCount()).isEqualTo(1);
        assertThat(info.getChunkSize()).isEqualTo(1024);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.COMPRESSED);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.MIN);
        assertThat(info.getLabels()).containsExactlyEntriesOf(f1Labels());
        assertThat(info.getSourceKey()).isNull();
        assertThat(info.getRules()).isEmpty();
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(5L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(1.5);
        assertThat(info.getKeySelfName()).isNull();
        assertThat(info.getChunks()).isNull();
    }

    /**
     * Scenario G01 / J02 (INFO): INFO on a missing key fails with {@code TSDB: the key does not exist}.
     */
    @Test
    void infoMissingKey() {

        assertThatThrownBy(() -> redis.tsInfo(MISSING)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key does not exist");
        assertThatThrownBy(() -> redis.tsInfo(MISSING, true)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key does not exist");
    }

    /**
     * Scenario G02: {@code keySelfName} and the chunks are populated only with {@code debug = true}.
     */
    @Test
    void infoDebug() {

        redis.tsCreate(KEY, new TsCreateArgs().labels(f1Labels()));

        TsInfoValue<String> debug = redis.tsInfo(KEY, true);

        assertThat(debug.getKeySelfName()).isEqualTo(KEY);
        assertThat(debug.getChunks()).hasSize((int) debug.getChunkCount());
        TsInfoValue.Chunk chunk = debug.getChunks().get(0);
        assertThat(chunk.getSize()).isGreaterThan(0).isEqualTo(debug.getChunkSize());
        assertThat(chunk.getSamples()).isEqualTo(0);
        assertThat(chunk.getBytesPerSample()).isEqualTo(0.0);
        assertThat(debug.getLabels()).containsExactlyEntriesOf(f1Labels());

        TsInfoValue<String> plain = redis.tsInfo(KEY, false);
        assertThat(plain.getKeySelfName()).isNull();
        assertThat(plain.getChunks()).isNull();
        assertThat(plain.getLabels()).containsExactlyEntriesOf(f1Labels());
        assertThat(plain.getChunkSize()).isEqualTo(debug.getChunkSize());
    }

    /**
     * Scenario G02: DEBUG on an uncompressed series with a custom chunk size.
     */
    @Test
    void infoDebugUncompressed() {

        redis.tsCreate(KEY, new TsCreateArgs().encoding(TsEncodingType.UNCOMPRESSED).chunkSize(128).label("t", testName));

        TsInfoValue<String> debug = redis.tsInfo(KEY, true);

        assertThat(debug.getKeySelfName()).isEqualTo(KEY);
        assertThat(debug.getChunks()).hasSize(1);
        assertThat(debug.getChunks().get(0).getSize()).isEqualTo(128);
    }

    /**
     * Scenario G07: {@code getRawInfo()} contains every top-level field the server sent.
     */
    @Test
    void infoRawInfo() {

        redis.tsCreate(KEY, new TsCreateArgs().labels(f1Labels()));

        String[] fields = { "totalSamples", "memoryUsage", "firstTimestamp", "lastTimestamp", "retentionTime", "chunkCount",
                "chunkSize", "chunkType", "duplicatePolicy", "labels", "sourceKey", "rules", "ignoreMaxTimeDiff",
                "ignoreMaxValDiff" };

        assertThat(redis.tsInfo(KEY).getRawInfo()).containsKeys(fields);
        assertThat(redis.tsInfo(KEY, true).getRawInfo()).containsKeys(fields).containsKeys("keySelfName", "Chunks");
        assertThat(redis.tsInfo(KEY).getRawInfo()).doesNotContainKeys("keySelfName", "Chunks");
    }

    // --- errors ------------------------------------------------------------------------------------------------------

    /**
     * Scenario J01 (INFO): INFO on a string key fails with WRONGTYPE.
     */
    @Test
    void infoWrongType() {

        redis.set(KEY, "value");

        assertThatThrownBy(() -> redis.tsInfo(KEY)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("WRONGTYPE");
    }

    /**
     * Scenario J02 (ALTER): ALTER on a missing key fails with {@code TSDB: the key does not exist}.
     */
    @Test
    void alterMissingKey() {

        assertThatThrownBy(() -> redis.tsAlter(MISSING, new TsAlterArgs().retention(1)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: the key does not exist");
    }

    /**
     * Scenario J05: a user with {@code +@timeseries} runs TS commands; with {@code -ts.info} TS.INFO fails with NOPERM.
     * <p>
     * The users are created with raw ACL rules, because {@code AclSetuserArgs.removeCommand(CommandType)} sends the enum name
     * ({@code -TS_INFO}), which the server rejects.
     */
    @Test
    void aclTimeseriesCategory() {

        String allowed = "ts-acl-" + testName;
        String denied = "ts-acl-noinfo-" + testName;

        createAclUser(allowed, "on", "nopass", "~*", "+@connection", "+@timeseries");
        createAclUser(denied, "on", "nopass", "~*", "+@connection", "+@timeseries", "-ts.info");
        try {
            runAs(allowed, ts -> {
                assertThat(ts.tsCreate("{ts}:acl", new TsCreateArgs().label("t", testName))).isEqualTo("OK");
                assertThat(ts.tsAlter("{ts}:acl", new TsAlterArgs().retention(10))).isEqualTo("OK");
                assertThat(ts.tsInfo("{ts}:acl").getRetentionTime()).isEqualTo(10);
                return null;
            });
            runAs(denied, ts -> {
                assertThat(ts.tsCreate("{ts}:acl2", new TsCreateArgs().label("t", testName))).isEqualTo("OK");
                assertThatThrownBy(() -> ts.tsInfo("{ts}:acl2")).isInstanceOf(RedisCommandExecutionException.class)
                        .hasMessageContaining("NOPERM");
                return null;
            });
        } finally {
            deleteAclUser(allowed);
            deleteAclUser(denied);
        }
    }

    /**
     * Create an ACL user with raw {@code ACL SETUSER} rules on every node that serves the tests.
     */
    protected void createAclUser(String user, String... rules) {
        assertThat(aclSetuser(redis, user, rules)).isEqualTo("OK");
    }

    protected static String aclSetuser(BaseRedisCommands<String, String> commands, String user, String... rules) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add("SETUSER").add(user);
        for (String rule : rules) {
            args.add(rule);
        }
        return commands.dispatch(CommandType.ACL, new StatusOutput<>(StringCodec.UTF8), args);
    }

    protected void deleteAclUser(String user) {
        redis.aclDeluser(user);
    }

    /**
     * Run {@code action} on a dedicated connection authenticated as {@code user}.
     */
    protected void runAs(String user, Function<RedisTimeSeriesCommands<String, String>, Void> action) {

        RedisURI uri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).withAuthentication(user, "unused")
                .build();
        RedisClient client = RedisClient.create(TestClientResources.get(), uri);
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            action.apply(connection.sync());
        } finally {
            client.shutdown();
        }
    }

}
