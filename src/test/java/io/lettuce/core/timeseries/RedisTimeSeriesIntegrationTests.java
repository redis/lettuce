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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.Range;
import io.lettuce.core.Range.Boundary;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisTimeSeriesAsyncCommands;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisTimeSeriesCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.NestedMultiOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.lettuce.core.timeseries.arguments.TsRangeArgs;
import io.lettuce.core.timeseries.arguments.TsReadArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for {@link RedisTimeSeriesCommands}: the series lifecycle commands (TS.CREATE, TS.ALTER, TS.DEL, TS.INFO),
 * the write commands (TS.ADD, TS.MADD, TS.INCRBY, TS.DECRBY), the compaction rule commands (TS.CREATERULE, TS.DELETERULE) and
 * the single-key reads (TS.GET, TS.RANGE, TS.REVRANGE, TS.READ). The subclasses run the same tests on RESP2, through the
 * reactive API and over Redis Cluster.
 * <p>
 * Tests written before the read commands existed read stored samples back with a raw {@code TS.RANGE key - +} sent through
 * {@code dispatch} ({@link #storedSamples(String)}).
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.ADD")
public class RedisTimeSeriesIntegrationTests {

    protected static final String KEY = "{ts}:k";

    protected static final String MISSING = "{ts}:missing";

    protected static final String F1 = "{ts}:f1";

    protected static final String F2 = "{ts}:f2";

    protected static final String SRC = "{ts}:src";

    protected static final String DST = "{ts}:dst";

    /**
     * The message of a rejected duplicate under policy BLOCK, as far as it is the same on 8.0.2 and 8.10.0 (test plan B08).
     */
    protected static final String BLOCK_DUPLICATE = "TSDB: Error at upsert, update is not supported when DUPLICATE_POLICY is set to BLOCK mode";

    private static final ProtocolKeyword RAW_TS_RANGE = new ProtocolKeyword() {

        private final byte[] bytes = "TS.RANGE".getBytes(StandardCharsets.US_ASCII);

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return "TS.RANGE";
        }

    };

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

    /**
     * Fixture F1 {@code simpleSeries}: {@code {ts}:f1} with labels {@code t=<test>, sensor=temp, area=north} and samples (1000,
     * 1.0), (2000, 2.0), (3000, 3.0).
     */
    protected void simpleSeries() {
        redis.tsCreate(F1, new TsCreateArgs().labels(f1Labels()));
        redis.tsAdd(F1, 1000L, 1.0);
        redis.tsAdd(F1, 2000L, 2.0);
        redis.tsAdd(F1, 3000L, 3.0);
    }

    /**
     * Fixture F2 {@code statsSeries}: {@code {ts}:f2} with label {@code t=<test>} and values 2, 4, 4, 4, 5, 5, 7, 9 at
     * timestamps 1000, 2000, …, 8000.
     */
    protected void statsSeries() {
        redis.tsCreate(F2, new TsCreateArgs().label("t", testName));
        double[] values = { 2, 4, 4, 4, 5, 5, 7, 9 };
        for (int i = 0; i < values.length; i++) {
            redis.tsAdd(F2, (i + 1) * 1000L, values[i]);
        }
    }

    /**
     * Fixture F4 {@code compactionPair}: source {@code {ts}:src} and destination {@code {ts}:dst}, rule AVG with bucket 1000,
     * then the source samples (1000, 10), (1500, 20), (2000, 30). The closed bucket 1000 on the destination is 15; the open
     * bucket 2000 (30) is not written yet.
     */
    protected void compactionPair() {
        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        redis.tsCreate(DST, new TsCreateArgs().label("t", testName));
        assertThat(redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, 1000)).isEqualTo("OK");
        redis.tsAdd(SRC, 1000L, 10.0);
        redis.tsAdd(SRC, 1500L, 20.0);
        redis.tsAdd(SRC, 2000L, 30.0);
    }

    /**
     * Send TS.MADD. The reactive subclass overrides this to unwrap {@code Value<Long>} into nullable elements.
     */
    protected List<Long> madd(List<TsMAddValue<String>> samples) {
        return redis.tsMAdd(samples);
    }

    @SafeVarargs
    protected final List<Long> madd(TsMAddValue<String>... samples) {
        return madd(Arrays.asList(samples));
    }

    /**
     * Read every stored sample of {@code key} with a raw {@code TS.RANGE key - +}, in timestamp order. The values come as RESP2
     * simple strings or RESP3 doubles and are normalized to {@link Double}.
     */
    protected Map<Long, Double> storedSamples(String key) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).addKey(key).add("-").add("+");
        List<Object> reply = redis.dispatch(RAW_TS_RANGE, new NestedMultiOutput<>(StringCodec.UTF8), args);

        Map<Long, Double> samples = new LinkedHashMap<>();
        for (Object row : reply) {
            List<?> sample = (List<?>) row;
            Object value = sample.get(1);
            samples.put((Long) sample.get(0),
                    value instanceof Double ? (Double) value : Double.valueOf(Double.parseDouble(value.toString())));
        }
        return samples;
    }

    protected static Map<Long, Double> samples(Object... pairs) {
        Map<Long, Double> samples = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            samples.put(((Number) pairs[i]).longValue(), ((Number) pairs[i + 1]).doubleValue());
        }
        return samples;
    }

    /**
     * The server clock in milliseconds ({@code TIME}). Used as the lower bound for {@code *} timestamps instead of the client
     * clock, because the Docker VM clock can lag the host clock by a millisecond or more (observed on 8.10.0).
     */
    protected long serverClockMillis() {
        List<String> time = redis.time();
        return Long.parseLong(time.get(0)) * 1000 + Long.parseLong(time.get(1)) / 1000;
    }

    protected void assumeVersion(String version) {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo(version), "Requires Redis " + version);
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

    // --- TS.ADD ------------------------------------------------------------------------------------------------------

    /**
     * Scenario B04 (ADD): a series created by ADD with every option has exactly the given labels; no option token is stored as
     * a label.
     */
    @Test
    void addStoresLabelsExactly() {

        Map<String, String> labels = labels("sensor", "temp", "area", "north");
        TsAddArgs args = new TsAddArgs().labels(labels).ignore(11, 12).onDuplicate(TsDuplicatePolicyType.MAX)
                .duplicatePolicy(TsDuplicatePolicyType.FIRST).encoding(TsEncodingType.UNCOMPRESSED).chunkSize(256)
                .retention(1000);

        assertThat(redis.tsAdd(KEY, 1000L, 1.0, args)).isEqualTo(1000L);

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getLabels()).containsExactlyEntriesOf(labels);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.FIRST);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.UNCOMPRESSED);
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(11L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(12.0);
    }

    /**
     * Scenario B04 (INCRBY / DECRBY): a series created by INCRBY or DECRBY with every option has exactly the given labels.
     */
    @Test
    void incrByDecrByStoreLabelsExactly() {

        Map<String, String> labels = labels("sensor", "temp", "area", "north");
        TsIncrByArgs args = new TsIncrByArgs().labels(labels).ignore(11, 12).duplicatePolicy(TsDuplicatePolicyType.MIN)
                .encoding(TsEncodingType.UNCOMPRESSED).chunkSize(256).retention(1000).timestamp(1000);

        assertThat(redis.tsIncrBy("{ts}:incr", 1.0, args)).isEqualTo(1000L);
        assertThat(redis.tsDecrBy("{ts}:decr", 1.0, args)).isEqualTo(1000L);

        for (String key : Arrays.asList("{ts}:incr", "{ts}:decr")) {
            TsInfoValue<String> info = redis.tsInfo(key);
            assertThat(info.getLabels()).as(key).containsExactlyEntriesOf(labels);
            assertThat(info.getDuplicatePolicy()).as(key).isSameAs(TsDuplicatePolicyType.MIN);
            assertThat(info.getChunkType()).as(key).isSameAs(TsEncodingType.UNCOMPRESSED);
            assertThat(info.getChunkSize()).as(key).isEqualTo(256);
            assertThat(info.getRetentionTime()).as(key).isEqualTo(1000);
            assertThat(info.getIgnoreMaxTimeDiff()).as(key).isEqualTo(11L);
            assertThat(info.getIgnoreMaxValDiff()).as(key).isEqualTo(12.0);
        }
    }

    /**
     * Scenario B06 (IGNORE effect): after ALTER sets IGNORE (with policy LAST), a following close ADD is suppressed and returns
     * the highest timestamp.
     */
    @Test
    void alterIgnoreSuppressesCloseAdd() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAlter(KEY, new TsAlterArgs().duplicatePolicy(TsDuplicatePolicyType.LAST).ignore(100, 0.5));

        assertThat(redis.tsAdd(KEY, 1000L, 1.0)).isEqualTo(1000L);
        assertThat(redis.tsAdd(KEY, 1050L, 1.2)).isEqualTo(1000L);

        assertThat(redis.tsInfo(KEY).getTotalSamples()).isEqualTo(1);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0));
    }

    /**
     * Scenario B07: ADD on a missing key creates the series with the given options; INFO reflects them.
     */
    @Test
    void addAutoCreatesWithOptions() {

        TsAddArgs args = new TsAddArgs().retention(5000).encoding(TsEncodingType.UNCOMPRESSED).chunkSize(128)
                .duplicatePolicy(TsDuplicatePolicyType.LAST).ignore(10, 0.5).labels(f1Labels());

        assertThat(redis.tsAdd(KEY, 1000L, 1.5, args)).isEqualTo(1000L);

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getTotalSamples()).isEqualTo(1);
        assertThat(info.getRetentionTime()).isEqualTo(5000);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.UNCOMPRESSED);
        assertThat(info.getChunkSize()).isEqualTo(128);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.LAST);
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(10L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(0.5);
        assertThat(info.getLabels()).containsExactlyEntriesOf(f1Labels());
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.5));
    }

    /**
     * Scenario B07: ADD without options on a missing key creates a series with the defaults; series options given for an
     * existing series are ignored (TS.ADD Javadoc).
     */
    @Test
    void addAutoCreatesWithDefaultsAndIgnoresOptionsLater() {

        assertThat(redis.tsAdd(KEY, 1000L, 1.0)).isEqualTo(1000L);

        TsInfoValue<String> created = redis.tsInfo(KEY);
        assertThat(created.getRetentionTime()).isEqualTo(0);
        assertThat(created.getChunkType()).isSameAs(TsEncodingType.COMPRESSED);
        assertThat(created.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.BLOCK);
        assertThat(created.getLabels()).isEmpty();

        redis.tsAdd(KEY, 2000L, 2.0,
                new TsAddArgs().retention(5000).duplicatePolicy(TsDuplicatePolicyType.LAST).labels(labels("t", testName)));

        TsInfoValue<String> after = redis.tsInfo(KEY);
        assertThat(after.getRetentionTime()).isEqualTo(0);
        assertThat(after.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.BLOCK);
        assertThat(after.getLabels()).isEmpty();
        assertThat(after.getTotalSamples()).isEqualTo(2);
    }

    /**
     * Scenario B08: with the default policy BLOCK, a second ADD at the same timestamp fails; the stored value is unchanged.
     */
    @Test
    void addDuplicateUnderDefaultBlockPolicy() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAdd(KEY, 1000L, 5.0);

        assertThatThrownBy(() -> redis.tsAdd(KEY, 1000L, 6.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining(BLOCK_DUPLICATE);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 5.0));
    }

    /**
     * Scenario B09: ON_DUPLICATE matrix. Each case on a fresh key holding (1000, 5): MIN 3 → 3; MIN 7 → 5; MAX 7 → 7; MAX 3 →
     * 5; SUM 2 → 7; FIRST 9 → 5; LAST 9 → 9. The key's duplicate policy is unchanged afterwards.
     */
    @Test
    void addOnDuplicateMatrix() {

        Object[][] cases = { { TsDuplicatePolicyType.MIN, 3.0, 3.0 }, { TsDuplicatePolicyType.MIN, 7.0, 5.0 },
                { TsDuplicatePolicyType.MAX, 7.0, 7.0 }, { TsDuplicatePolicyType.MAX, 3.0, 5.0 },
                { TsDuplicatePolicyType.SUM, 2.0, 7.0 }, { TsDuplicatePolicyType.FIRST, 9.0, 5.0 },
                { TsDuplicatePolicyType.LAST, 9.0, 9.0 } };

        int i = 0;
        for (Object[] c : cases) {
            String key = "{ts}:od:" + i++;
            TsDuplicatePolicyType policy = (TsDuplicatePolicyType) c[0];
            redis.tsAdd(key, 1000L, 5.0);

            assertThat(redis.tsAdd(key, 1000L, (Double) c[1], new TsAddArgs().onDuplicate(policy))).isEqualTo(1000L);

            assertThat(storedSamples(key)).as(policy + " " + c[1]).isEqualTo(samples(1000, c[2]));
            assertThat(redis.tsInfo(key).getDuplicatePolicy()).as(policy + " " + c[1]).isSameAs(TsDuplicatePolicyType.BLOCK);
        }
    }

    /**
     * Scenario B09: ON_DUPLICATE has no effect when ADD creates the series; the key gets the default policy.
     */
    @Test
    void addOnDuplicateOnCreationDoesNotSetPolicy() {

        redis.tsAdd(KEY, 1000L, 5.0, new TsAddArgs().onDuplicate(TsDuplicatePolicyType.SUM));

        assertThat(redis.tsInfo(KEY).getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.BLOCK);
    }

    /**
     * Scenario B10: DUPLICATE_POLICY on the key. Each policy on a fresh key holding (1000, 5), then ADD (1000, 3): BLOCK →
     * error, 5 kept; FIRST → 5; LAST → 3; MIN → 3; MAX → 5; SUM → 8.
     */
    @Test
    void duplicatePolicyOnKey() {

        Object[][] cases = { { TsDuplicatePolicyType.FIRST, 5.0 }, { TsDuplicatePolicyType.LAST, 3.0 },
                { TsDuplicatePolicyType.MIN, 3.0 }, { TsDuplicatePolicyType.MAX, 5.0 }, { TsDuplicatePolicyType.SUM, 8.0 } };

        for (Object[] c : cases) {
            TsDuplicatePolicyType policy = (TsDuplicatePolicyType) c[0];
            String key = "{ts}:dp:" + policy.name();
            redis.tsCreate(key, new TsCreateArgs().duplicatePolicy(policy).label("t", testName));
            redis.tsAdd(key, 1000L, 5.0);

            assertThat(redis.tsAdd(key, 1000L, 3.0)).as(policy.name()).isEqualTo(1000L);
            assertThat(storedSamples(key)).as(policy.name()).isEqualTo(samples(1000, c[1]));
        }

        String block = "{ts}:dp:BLOCK";
        redis.tsCreate(block, new TsCreateArgs().duplicatePolicy(TsDuplicatePolicyType.BLOCK).label("t", testName));
        redis.tsAdd(block, 1000L, 5.0);
        assertThatThrownBy(() -> redis.tsAdd(block, 1000L, 3.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining(BLOCK_DUPLICATE);
        assertThat(storedSamples(block)).isEqualTo(samples(1000, 5.0));
    }

    /**
     * Scenario B10: a duplicate policy set by ADD on creation applies to later writes.
     */
    @Test
    void duplicatePolicyFromAddCreation() {

        redis.tsAdd(KEY, 1000L, 5.0, new TsAddArgs().duplicatePolicy(TsDuplicatePolicyType.SUM));
        redis.tsAdd(KEY, 1000L, 3.0);

        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 8.0));
    }

    /**
     * Scenario B11: IGNORE with policy LAST: (1000, 1.0) → 1000; (1050, 1.2) → 1000 and nothing stored; (1200, 1.2) → 1200 and
     * stored.
     */
    @Test
    void ignoreSuppressionWithLastPolicy() {

        redis.tsCreate(KEY,
                new TsCreateArgs().ignore(100, 0.5).duplicatePolicy(TsDuplicatePolicyType.LAST).label("t", testName));

        assertThat(redis.tsAdd(KEY, 1000L, 1.0)).isEqualTo(1000L);
        assertThat(redis.tsAdd(KEY, 1050L, 1.2)).isEqualTo(1000L);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0));

        assertThat(redis.tsAdd(KEY, 1200L, 1.2)).isEqualTo(1200L);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0, 1200, 1.2));
    }

    /**
     * Scenario B11: IGNORE with the default policy BLOCK: (1050, 1.2) is stored, because IGNORE only applies with LAST.
     */
    @Test
    void ignoreHasNoEffectWithBlockPolicy() {

        redis.tsCreate(KEY, new TsCreateArgs().ignore(100, 0.5).label("t", testName));

        assertThat(redis.tsAdd(KEY, 1000L, 1.0)).isEqualTo(1000L);
        assertThat(redis.tsAdd(KEY, 1050L, 1.2)).isEqualTo(1050L);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0, 1050, 1.2));
    }

    /**
     * Scenario B11: the IGNORE settings also apply to TS.MADD (TIMESTAMP-CANONICAL §2.3): the suppressed sample returns the
     * highest timestamp.
     */
    @Test
    void ignoreSuppressionInMAdd() {

        redis.tsCreate(KEY,
                new TsCreateArgs().ignore(100, 0.5).duplicatePolicy(TsDuplicatePolicyType.LAST).label("t", testName));

        assertThat(madd(TsMAddValue.of(KEY, 1000L, 1.0), TsMAddValue.of(KEY, 1050L, 1.2), TsMAddValue.of(KEY, 1200L, 1.2)))
                .containsExactly(1000L, 1000L, 1200L);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0, 1200, 1.2));
    }

    /**
     * Scenario B12: an out-of-order ADD is accepted and returns its own timestamp.
     */
    @Test
    void addOutOfOrder() {

        assertThat(redis.tsAdd(KEY, 2000L, 2.0)).isEqualTo(2000L);
        assertThat(redis.tsAdd(KEY, 1000L, 1.0)).isEqualTo(1000L);

        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0, 2000, 2.0));
        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getFirstTimestamp()).isEqualTo(1000);
        assertThat(info.getLastTimestamp()).isEqualTo(2000);
    }

    /**
     * Scenario B13: ADD without a timestamp ({@code *}), with and without options, returns a timestamp ≥ the server clock read
     * before the call, and stores the value (not the timestamp).
     */
    @Test
    void addWithServerClock() {

        long start = serverClockMillis();

        Long first = redis.tsAdd(KEY, 7.0);
        assertThat(first).isGreaterThanOrEqualTo(start);

        Long second = redis.tsAdd("{ts}:k2", 8.0, new TsAddArgs().labels(labels("t", testName)));
        assertThat(second).isGreaterThanOrEqualTo(start);

        assertThat(storedSamples(KEY)).isEqualTo(samples(first, 7.0));
        assertThat(storedSamples("{ts}:k2")).isEqualTo(samples(second, 8.0));
        assertThat(redis.tsInfo("{ts}:k2").getLabels()).containsExactlyEntriesOf(labels("t", testName));
    }

    /**
     * Scenario B13: {@code TsMAddValue.of(k, v)} returns a timestamp ≥ the server clock read before the call.
     */
    @Test
    void maddWithServerClock() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        long start = serverClockMillis();

        List<Long> result = madd(TsMAddValue.of(KEY, 4.0));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNotNull().isGreaterThanOrEqualTo(start);
        assertThat(storedSamples(KEY)).isEqualTo(samples(result.get(0), 4.0));
    }

    /**
     * Scenario B14 (INFO part): retention 1000, samples at 1000, 1500 and 3000: samples older than 3000 − 1000 are removed;
     * INFO shows one sample starting at 3000.
     */
    @Test
    void retentionTrimming() {

        redis.tsCreate(KEY, new TsCreateArgs().retention(1000).label("t", testName));
        redis.tsAdd(KEY, 1000L, 1.0);
        redis.tsAdd(KEY, 1500L, 2.0);
        redis.tsAdd(KEY, 3000L, 3.0);

        TsInfoValue<String> info = redis.tsInfo(KEY);
        assertThat(info.getTotalSamples()).isEqualTo(1);
        assertThat(info.getFirstTimestamp()).isEqualTo(3000);
        assertThat(info.getLastTimestamp()).isEqualTo(3000);
    }

    // --- TS.MADD -----------------------------------------------------------------------------------------------------

    /**
     * Scenario B15: MADD for {@code {ts}:a}, {@code {ts}:b}, {@code {ts}:a} returns the timestamps in input order and stores
     * every sample.
     */
    @Test
    void maddReturnsTimestampsInInputOrder() {

        redis.tsCreate("{ts}:a", new TsCreateArgs().label("t", testName));
        redis.tsCreate("{ts}:b", new TsCreateArgs().label("t", testName));

        List<Long> result = madd(TsMAddValue.of("{ts}:a", 2000L, 1.0), TsMAddValue.of("{ts}:b", 1500L, 2.0),
                TsMAddValue.of("{ts}:a", 1000L, 3.0));

        assertThat(result).containsExactly(2000L, 1500L, 1000L);
        assertThat(storedSamples("{ts}:a")).isEqualTo(samples(1000, 3.0, 2000, 1.0));
        assertThat(storedSamples("{ts}:b")).isEqualTo(samples(1500, 2.0));
    }

    /**
     * Scenario B16: MADD partial failure. Under policy BLOCK with an existing sample, the duplicate gives {@code null}, the
     * accepted sample is stored, and the next command on the connection is unaffected.
     */
    @Test
    void maddPartialFailure() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAdd(KEY, 1000L, 5.0);

        List<Long> result = madd(TsMAddValue.of(KEY, 1000L, 6.0), TsMAddValue.of(KEY, 2000L, 7.0));

        assertThat(result).containsExactly(null, 2000L);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 5.0, 2000, 7.0));

        assertThat(redis.tsAdd(KEY, 3000L, 8.0)).isEqualTo(3000L);
        assertThat(redis.tsInfo(KEY).getTotalSamples()).isEqualTo(3);
    }

    /**
     * Scenario B17: MADD on a missing key gives {@code [ts, null]}; the other sample is stored and the missing key is not
     * created.
     */
    @Test
    void maddOnMissingKey() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));

        List<Long> result = madd(TsMAddValue.of(KEY, 1000L, 1.0), TsMAddValue.of(MISSING, 1000L, 2.0));

        assertThat(result).containsExactly(1000L, null);
        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0));
        assertThat(redis.exists(MISSING)).isEqualTo(0);
    }

    /**
     * Scenario B16 / B17: every sample rejected gives a list of {@code null}s, not an error.
     */
    @Test
    void maddAllRejected() {

        assertThat(madd(TsMAddValue.of(MISSING, 1000L, 1.0), TsMAddValue.of("{ts}:missing2", 1000L, 2.0))).containsExactly(null,
                null);
        assertThat(redis.exists(MISSING, "{ts}:missing2")).isEqualTo(0);
    }

    /**
     * Scenario A13 (MADD), through the API of every flavor and topology: a {@code null} or empty list, or a {@code null}
     * element, throws {@link IllegalArgumentException} and sends nothing.
     */
    @Test
    void maddRejectsNullAndEmptyThroughTheApi() {

        assertThatThrownBy(() -> madd((List<TsMAddValue<String>>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> madd(Collections.emptyList())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> madd(Arrays.asList(TsMAddValue.of(KEY, 1000L, 1.0), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> madd(Arrays.asList(TsMAddValue.of("{a}:k", 1000L, 1.0), null, TsMAddValue.of("{b}:k", 1.0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(redis.exists(KEY)).isEqualTo(0);
    }

    /**
     * Scenario A13: a {@code null} key or arguments object on the write commands throws {@link IllegalArgumentException}.
     */
    @Test
    void writesRejectNullArgumentsThroughTheApi() {

        assertThatThrownBy(() -> redis.tsAdd(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsAdd(KEY, 1000L, 1.0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsIncrBy(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsDecrBy(KEY, 1.0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsDel(KEY, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(redis.exists(KEY)).isEqualTo(0);
    }

    // --- TS.INCRBY / TS.DECRBY ---------------------------------------------------------------------------------------

    /**
     * Scenario B18: INCRBY and DECRBY without TIMESTAMP use the server clock (timestamp ≥ the server clock read before the
     * call); on an empty series the value is ± the operand.
     */
    @Test
    void incrByDecrByImplicitTimestamp() {

        long start = serverClockMillis();

        Long incr = redis.tsIncrBy("{ts}:incr", 2.5);
        Long decr = redis.tsDecrBy("{ts}:decr", 2.5);

        assertThat(incr).isGreaterThanOrEqualTo(start);
        assertThat(decr).isGreaterThanOrEqualTo(start);
        assertThat(storedSamples("{ts}:incr")).isEqualTo(samples(incr, 2.5));
        assertThat(storedSamples("{ts}:decr")).isEqualTo(samples(decr, -2.5));
    }

    /**
     * Scenario B18: explicit TIMESTAMP. A higher timestamp adds a sample with the increased value; an equal timestamp updates
     * the latest sample in place.
     */
    @Test
    void incrByDecrByExplicitTimestamp() {

        assertThat(redis.tsIncrBy(KEY, 10, TsIncrByArgs.Builder.timestamp(1000))).isEqualTo(1000L);
        assertThat(redis.tsIncrBy(KEY, 5, TsIncrByArgs.Builder.timestamp(2000))).isEqualTo(2000L);
        assertThat(redis.tsDecrBy(KEY, 3, TsIncrByArgs.Builder.timestamp(2000))).isEqualTo(2000L);
        assertThat(redis.tsDecrBy(KEY, 2, TsIncrByArgs.Builder.timestamp(3000))).isEqualTo(3000L);

        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 10.0, 2000, 12.0, 3000, 10.0));
    }

    /**
     * Scenario B18: an explicit TIMESTAMP below the highest timestamp fails with
     * {@code TSDB: timestamp must be equal to or higher than the maximum existing timestamp}. The test does not assert that an
     * invalid INCRBY leaves no key (servers before 8.8 create it first, RedisTimeSeries #1910).
     */
    @Test
    void incrByDecrByTimestampBelowMaximum() {

        redis.tsIncrBy(KEY, 1, TsIncrByArgs.Builder.timestamp(2000));

        assertThatThrownBy(() -> redis.tsIncrBy(KEY, 1, TsIncrByArgs.Builder.timestamp(1000)))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: timestamp must be equal to or higher than the maximum existing timestamp");
        assertThatThrownBy(() -> redis.tsDecrBy(KEY, 1, TsIncrByArgs.Builder.timestamp(1000)))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: timestamp must be equal to or higher than the maximum existing timestamp");
        assertThat(storedSamples(KEY)).isEqualTo(samples(2000, 1.0));
    }

    /**
     * Scenario B18: a negative operand. INCRBY −5 decreases, DECRBY −1 increases.
     */
    @Test
    void incrByDecrByNegativeOperand() {

        redis.tsIncrBy(KEY, 10, TsIncrByArgs.Builder.timestamp(1000));
        redis.tsIncrBy(KEY, -5, TsIncrByArgs.Builder.timestamp(2000));
        redis.tsDecrBy(KEY, -1, TsIncrByArgs.Builder.timestamp(3000));

        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 10.0, 2000, 5.0, 3000, 6.0));
    }

    /**
     * Scenario B18: fractional addends. 0.1 + 0.2 is stored as the server's double 0.30000000000000004 (verified on 8.10.0:
     * RESP2 {@code 3.0000000000000004E-1}, RESP3 {@code 0.30000000000000004}).
     */
    @Test
    void incrByFractionalAddends() {

        redis.tsIncrBy(KEY, 0.1, TsIncrByArgs.Builder.timestamp(1000));
        redis.tsIncrBy(KEY, 0.2, TsIncrByArgs.Builder.timestamp(1000));

        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 0.30000000000000004));
        assertThat(storedSamples(KEY).get(1000L)).isEqualTo(0.1 + 0.2);
    }

    /**
     * Scenario B18: INCRBY and DECRBY auto-create the series with labels.
     */
    @Test
    void incrByDecrByAutoCreateWithLabels() {

        redis.tsIncrBy("{ts}:incr", 1, new TsIncrByArgs().labels(f1Labels()));
        redis.tsDecrBy("{ts}:decr", 1, new TsIncrByArgs().label("t", testName).label("kind", "decr"));

        assertThat(redis.tsInfo("{ts}:incr").getLabels()).containsExactlyEntriesOf(f1Labels());
        assertThat(redis.tsInfo("{ts}:decr").getLabels()).containsExactlyEntriesOf(labels("t", testName, "kind", "decr"));
    }

    /**
     * Scenario B18: IGNORE applies to INCRBY and DECRBY: with policy LAST, a close increase is suppressed and returns the
     * highest timestamp; a larger one is stored.
     */
    @Test
    void incrByDecrByIgnore() {

        TsIncrByArgs create = new TsIncrByArgs().timestamp(1000).ignore(100, 0.5).duplicatePolicy(TsDuplicatePolicyType.LAST)
                .label("t", testName);
        assertThat(redis.tsIncrBy(KEY, 1, create)).isEqualTo(1000L);

        assertThat(redis.tsIncrBy(KEY, 0.1, TsIncrByArgs.Builder.timestamp(1050))).isEqualTo(1000L);
        assertThat(redis.tsDecrBy(KEY, 0.1, TsIncrByArgs.Builder.timestamp(1060))).isEqualTo(1000L);
        assertThat(redis.tsIncrBy(KEY, 1, TsIncrByArgs.Builder.timestamp(1070))).isEqualTo(1070L);

        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 1.0, 1070, 2.0));
    }

    // --- TS.DEL ------------------------------------------------------------------------------------------------------

    /**
     * Scenario B19: DEL on F2 with {@code Range.create(2000, 4000)} deletes 3 samples (bounds inclusive); then
     * {@code Range.unbounded()} deletes the remaining 5.
     */
    @Test
    void delInclusiveAndUnbounded() {

        statsSeries();

        assertThat(redis.tsDel(F2, Range.create(2000L, 4000L))).isEqualTo(3);
        assertThat(storedSamples(F2).keySet()).containsExactly(1000L, 5000L, 6000L, 7000L, 8000L);

        assertThat(redis.tsDel(F2, Range.unbounded())).isEqualTo(5);
        assertThat(storedSamples(F2)).isEmpty();
        assertThat(redis.tsInfo(F2).getTotalSamples()).isEqualTo(0);
    }

    /**
     * Scenario B19: an exclusive lower bound {@code excluding(2000)..4000} deletes 2 samples (3000, 4000).
     */
    @Test
    void delExclusiveBound() {

        statsSeries();

        assertThat(redis.tsDel(F2, Range.from(Boundary.excluding(2000L), Boundary.including(4000L)))).isEqualTo(2);
        assertThat(storedSamples(F2).keySet()).containsExactly(1000L, 2000L, 5000L, 6000L, 7000L, 8000L);

        assertThat(redis.tsDel(F2, Range.from(Boundary.including(5000L), Boundary.excluding(7000L)))).isEqualTo(2);
        assertThat(storedSamples(F2).keySet()).containsExactly(1000L, 2000L, 7000L, 8000L);
    }

    /**
     * Scenario B19: one unbounded side, and a range without samples.
     */
    @Test
    void delHalfUnboundedAndEmptyRange() {

        statsSeries();

        assertThat(redis.tsDel(F2, Range.from(Boundary.unbounded(), Boundary.including(2000L)))).isEqualTo(2);
        assertThat(redis.tsDel(F2, Range.from(Boundary.including(7000L), Boundary.unbounded()))).isEqualTo(2);
        assertThat(redis.tsDel(F2, Range.create(2001L, 2999L))).isEqualTo(0);
        assertThat(storedSamples(F2).keySet()).containsExactly(3000L, 4000L, 5000L, 6000L);
    }

    /**
     * Scenario B19 / J02 (DEL): DEL on a missing key fails with {@code TSDB: the key does not exist}.
     */
    @Test
    void delMissingKey() {

        assertThatThrownBy(() -> redis.tsDel(MISSING, Range.create(1L, 2L))).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key does not exist");
    }

    // --- TS.INFO after writes ----------------------------------------------------------------------------------------

    /**
     * Scenario B20 / G01 (stats): INFO of F1 has totalSamples 3, firstTimestamp 1000, lastTimestamp 3000 and memoryUsage &gt;
     * 0.
     */
    @Test
    void infoStatsAfterWrites() {

        simpleSeries();

        TsInfoValue<String> info = redis.tsInfo(F1);
        assertThat(info.getTotalSamples()).isEqualTo(3);
        assertThat(info.getFirstTimestamp()).isEqualTo(1000);
        assertThat(info.getLastTimestamp()).isEqualTo(3000);
        assertThat(info.getMemoryUsage()).isGreaterThan(0);
        assertThat(info.getChunkCount()).isEqualTo(1);
        assertThat(info.getLabels()).containsExactlyEntriesOf(f1Labels());
    }

    /**
     * Scenario G02b: INFO DEBUG of F1 has one chunk with start timestamp 1000, end timestamp 3000, 3 samples, size &gt; 0 and
     * bytesPerSample &gt; 0.
     */
    @Test
    void infoDebugWithSamples() {

        simpleSeries();

        TsInfoValue<String> debug = redis.tsInfo(F1, true);
        assertThat(debug.getKeySelfName()).isEqualTo(F1);
        assertThat(debug.getChunks()).hasSize(1);

        TsInfoValue.Chunk chunk = debug.getChunks().get(0);
        assertThat(chunk.getStartTimestamp()).isEqualTo(1000);
        assertThat(chunk.getEndTimestamp()).isEqualTo(3000);
        assertThat(chunk.getSamples()).isEqualTo(3);
        assertThat(chunk.getSize()).isGreaterThan(0);
        assertThat(chunk.getBytesPerSample()).isGreaterThan(0);
    }

    // --- NaN and Infinity --------------------------------------------------------------------------------------------

    /**
     * Scenario H01 (writes): ADD and MADD accept NaN (Redis 8.6+); the stored value is NaN.
     */
    @Test
    void nanWrites() {

        assumeVersion("8.6");

        assertThat(redis.tsAdd(KEY, 1000L, Double.NaN)).isEqualTo(1000L);
        assertThat(madd(TsMAddValue.of(KEY, 2000L, Double.NaN), TsMAddValue.of(KEY, 3000L, 3.0))).containsExactly(2000L, 3000L);

        Map<Long, Double> stored = storedSamples(KEY);
        assertThat(stored.keySet()).containsExactly(1000L, 2000L, 3000L);
        assertThat(stored.get(1000L)).isNaN();
        assertThat(stored.get(2000L)).isNaN();
        assertThat(stored.get(3000L)).isEqualTo(3.0);
    }

    /**
     * Scenario H03: NaN with MIN/SUM duplicates and NaN INCRBY (Redis 8.6+). MIN policy, (1000, 1) then (1000, NaN) → the B08
     * message; SUM policy, (1000, NaN) accepted, then (1000, 1) → the B08 message; INCRBY NaN →
     * {@code TSDB: invalid increase/decrease value}.
     */
    @Test
    void nanDuplicatesAndIncrBy() {

        assumeVersion("8.6");

        redis.tsCreate("{ts}:min", new TsCreateArgs().duplicatePolicy(TsDuplicatePolicyType.MIN).label("t", testName));
        redis.tsAdd("{ts}:min", 1000L, 1.0);
        assertThatThrownBy(() -> redis.tsAdd("{ts}:min", 1000L, Double.NaN)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining(BLOCK_DUPLICATE);

        redis.tsCreate("{ts}:sum", new TsCreateArgs().duplicatePolicy(TsDuplicatePolicyType.SUM).label("t", testName));
        assertThat(redis.tsAdd("{ts}:sum", 1000L, Double.NaN)).isEqualTo(1000L);
        assertThatThrownBy(() -> redis.tsAdd("{ts}:sum", 1000L, 1.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining(BLOCK_DUPLICATE);

        assertThatThrownBy(() -> redis.tsIncrBy("{ts}:incr", Double.NaN)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: invalid increase/decrease value");
        assertThatThrownBy(() -> redis.tsDecrBy("{ts}:incr", Double.NaN)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: invalid increase/decrease value");
    }

    /**
     * Scenario H04: overflow gives ±Infinity. ADD 1.7e308, INCRBY 1.7e308 → +Infinity; DECRBY on a −1.7e308 series → −Infinity.
     * The base suite runs on RESP3 and on RESP2 with the same asserts.
     */
    @Test
    void overflowToInfinity() {

        redis.tsAdd("{ts}:pos", 1000L, 1.7e308);
        assertThat(redis.tsIncrBy("{ts}:pos", 1.7e308, TsIncrByArgs.Builder.timestamp(2000))).isEqualTo(2000L);

        redis.tsAdd("{ts}:neg", 1000L, -1.7e308);
        assertThat(redis.tsDecrBy("{ts}:neg", 1.7e308, TsIncrByArgs.Builder.timestamp(2000))).isEqualTo(2000L);

        assertThat(storedSamples("{ts}:pos")).isEqualTo(samples(1000, 1.7e308, 2000, Double.POSITIVE_INFINITY));
        assertThat(storedSamples("{ts}:neg")).isEqualTo(samples(1000, -1.7e308, 2000, Double.NEGATIVE_INFINITY));
    }

    /**
     * Scenario H05: an infinite input value fails with {@code TSDB: invalid value} (ADD); MADD rejects that sample only.
     */
    @Test
    void infiniteInputRejected() {

        assertThatThrownBy(() -> redis.tsAdd(KEY, 1000L, Double.POSITIVE_INFINITY))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: invalid value");
        assertThatThrownBy(() -> redis.tsAdd(KEY, 1000L, Double.NEGATIVE_INFINITY))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: invalid value");
        assertThat(redis.exists(KEY)).isEqualTo(0);

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        assertThat(madd(TsMAddValue.of(KEY, 1000L, Double.POSITIVE_INFINITY), TsMAddValue.of(KEY, 2000L, 2.0)))
                .containsExactly(null, 2000L);
        assertThat(storedSamples(KEY)).isEqualTo(samples(2000, 2.0));
    }

    // --- write errors ------------------------------------------------------------------------------------------------

    /**
     * Scenario J01 (ADD): writes on a string key fail. Observed server messages (8.10.0): TS.ADD →
     * {@code TSDB: the key is not a TSDB key}; TS.INCRBY, TS.DECRBY, TS.DEL → {@code WRONGTYPE}; TS.MADD → a {@code null}
     * element.
     */
    @Test
    void writesOnWrongType() {

        redis.set(KEY, "value");

        assertThatThrownBy(() -> redis.tsAdd(KEY, 1L, 1.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key is not a TSDB key");
        assertThatThrownBy(() -> redis.tsIncrBy(KEY, 1.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("WRONGTYPE");
        assertThatThrownBy(() -> redis.tsDecrBy(KEY, 1.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("WRONGTYPE");
        assertThatThrownBy(() -> redis.tsDel(KEY, Range.unbounded())).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("WRONGTYPE");
        assertThat(madd(TsMAddValue.of(KEY, 1L, 1.0))).containsExactly((Long) null);
        assertThat(redis.get(KEY)).isEqualTo("value");
    }

    /**
     * Scenario J03: retention 1000, a sample at 5000, then ADD at 1000 fails with {@code TSDB: Timestamp is older than
     * retention}; in MADD only that sample is rejected.
     */
    @Test
    void addOlderThanRetention() {

        redis.tsAdd(KEY, 5000L, 1.0, new TsAddArgs().retention(1000).label("t", testName));

        assertThatThrownBy(() -> redis.tsAdd(KEY, 1000L, 1.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: Timestamp is older than retention");
        assertThat(madd(TsMAddValue.of(KEY, 1000L, 1.0), TsMAddValue.of(KEY, 6000L, 2.0))).containsExactly(null, 6000L);
        assertThat(storedSamples(KEY)).isEqualTo(samples(5000, 1.0, 6000, 2.0));
    }

    // --- TS.CREATERULE / TS.DELETERULE -------------------------------------------------------------------------------

    /**
     * Find the rule of {@code info} whose destination is {@code destKey}.
     */
    protected static TsInfoValue.Rule<String> ruleTo(TsInfoValue<String> info, String destKey) {
        for (TsInfoValue.Rule<String> rule : info.getRules()) {
            if (destKey.equals(rule.getDestinationKey())) {
                return rule;
            }
        }
        throw new AssertionError("No rule to " + destKey + " in " + info.getRules());
    }

    /**
     * Scenario G03: F4. The source INFO has exactly one rule (destination {@code {ts}:dst}, bucket 1000, aggregation AVG,
     * alignment 0) and no source key; the destination INFO has {@code sourceKey} = the source and no rules. The closed bucket
     * 1000 on the destination holds 15 (fixture F4), read back with a raw TS.RANGE.
     */
    @Test
    void createRuleInfo() {

        compactionPair();

        TsInfoValue<String> source = redis.tsInfo(SRC);
        assertThat(source.getRules()).hasSize(1);
        TsInfoValue.Rule<String> rule = source.getRules().get(0);
        assertThat(rule.getDestinationKey()).isEqualTo(DST);
        assertThat(rule.getBucketDuration()).isEqualTo(1000);
        assertThat(rule.getAggregation()).isSameAs(TsAggregationType.AVG);
        assertThat(rule.getAlignment()).isEqualTo(0);
        assertThat(source.getSourceKey()).isNull();

        TsInfoValue<String> destination = redis.tsInfo(DST);
        assertThat(destination.getSourceKey()).isEqualTo(SRC);
        assertThat(destination.getRules()).isEmpty();

        assertThat(storedSamples(DST)).isEqualTo(samples(1000, 15.0));
    }

    /**
     * Scenario G03: with the F4 rule in place, a sample in the next bucket closes bucket 2000, so the destination gets (2000,
     * 30). This is the counterpart of G05, where the same write after DELETERULE adds nothing.
     */
    @Test
    void createRuleCompactsClosedBuckets() {

        compactionPair();
        redis.tsAdd(SRC, 3000L, 40.0);

        assertThat(storedSamples(DST)).isEqualTo(samples(1000, 15.0, 2000, 30.0));
    }

    /**
     * Scenario G03: every aggregation of design §3.1 that the 8.0 floor supports is accepted by CREATERULE and comes back in
     * the source INFO as the same {@link TsAggregationType} constant (TS.INFO sends the token in uppercase). One source can
     * have several rules.
     */
    @Test
    void createRuleEveryAggregationRoundTrips() {

        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        List<TsAggregationType> aggregations = new ArrayList<>();
        for (TsAggregationType aggregation : TsAggregationType.values()) {
            if (aggregation != TsAggregationType.COUNTNAN && aggregation != TsAggregationType.COUNTALL) {
                aggregations.add(aggregation);
            }
        }
        for (TsAggregationType aggregation : aggregations) {
            String dest = "{ts}:dst:" + aggregation.name();
            redis.tsCreate(dest, new TsCreateArgs().label("t", testName));
            assertThat(redis.tsCreateRule(SRC, dest, aggregation, 1000)).isEqualTo("OK");
        }

        TsInfoValue<String> info = redis.tsInfo(SRC);
        assertThat(info.getRules()).hasSize(aggregations.size());
        for (TsAggregationType aggregation : aggregations) {
            TsInfoValue.Rule<String> rule = ruleTo(info, "{ts}:dst:" + aggregation.name());
            assertThat(rule.getAggregation()).as(aggregation.name()).isSameAs(aggregation);
            assertThat(rule.getBucketDuration()).isEqualTo(1000);
            assertThat(rule.getAlignment()).isEqualTo(0);
        }
    }

    /**
     * Scenario G03: the aggregations {@code countNaN} and {@code countAll} (Redis 8.6+, Javadoc of {@code tsCreateRule})
     * round-trip through CREATERULE and INFO.
     */
    @Test
    void createRuleCountNanAndCountAll() {

        assumeVersion("8.6");

        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        redis.tsCreate("{ts}:dst:nan", new TsCreateArgs().label("t", testName));
        redis.tsCreate("{ts}:dst:all", new TsCreateArgs().label("t", testName));
        assertThat(redis.tsCreateRule(SRC, "{ts}:dst:nan", TsAggregationType.COUNTNAN, 1000)).isEqualTo("OK");
        assertThat(redis.tsCreateRule(SRC, "{ts}:dst:all", TsAggregationType.COUNTALL, 1000)).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(SRC);
        assertThat(ruleTo(info, "{ts}:dst:nan").getAggregation()).isSameAs(TsAggregationType.COUNTNAN);
        assertThat(ruleTo(info, "{ts}:dst:all").getAggregation()).isSameAs(TsAggregationType.COUNTALL);
    }

    /**
     * Scenario G04 (INFO): CREATERULE with alignTimestamp 500 → the source INFO reports alignment 500, with the given bucket
     * and aggregation.
     */
    @Test
    void createRuleWithAlignment() {

        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        redis.tsCreate(DST, new TsCreateArgs().label("t", testName));

        assertThat(redis.tsCreateRule(SRC, DST, TsAggregationType.MIN, 1000, 500)).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(SRC);
        assertThat(info.getRules()).hasSize(1);
        TsInfoValue.Rule<String> rule = info.getRules().get(0);
        assertThat(rule.getDestinationKey()).isEqualTo(DST);
        assertThat(rule.getBucketDuration()).isEqualTo(1000);
        assertThat(rule.getAggregation()).isSameAs(TsAggregationType.MIN);
        assertThat(rule.getAlignment()).isEqualTo(500);
        assertThat(redis.tsInfo(DST).getSourceKey()).isEqualTo(SRC);
    }

    /**
     * Scenario G05: F4, then DELETERULE → {@code OK}; the source has no rules, the destination no source key, both series are
     * kept, and later source samples that close bucket 2000 add nothing to the destination.
     */
    @Test
    void deleteRule() {

        compactionPair();
        assertThat(storedSamples(DST)).isEqualTo(samples(1000, 15.0));

        assertThat(redis.tsDeleteRule(SRC, DST)).isEqualTo("OK");

        assertThat(redis.tsInfo(SRC).getRules()).isEmpty();
        assertThat(redis.tsInfo(DST).getSourceKey()).isNull();
        assertThat(redis.exists(SRC, DST)).isEqualTo(2);

        redis.tsAdd(SRC, 3000L, 40.0);
        redis.tsAdd(SRC, 4000L, 50.0);

        assertThat(storedSamples(DST)).isEqualTo(samples(1000, 15.0));
        assertThat(storedSamples(SRC)).isEqualTo(samples(1000, 10.0, 1500, 20.0, 2000, 30.0, 3000, 40.0, 4000, 50.0));
    }

    /**
     * Scenario G05: after DELETERULE the destination is free again, so the same rule can be created anew.
     */
    @Test
    void deleteRuleThenCreateAgain() {

        compactionPair();
        assertThat(redis.tsDeleteRule(SRC, DST)).isEqualTo("OK");

        assertThat(redis.tsCreateRule(SRC, DST, TsAggregationType.SUM, 2000)).isEqualTo("OK");

        TsInfoValue.Rule<String> rule = ruleTo(redis.tsInfo(SRC), DST);
        assertThat(rule.getAggregation()).isSameAs(TsAggregationType.SUM);
        assertThat(rule.getBucketDuration()).isEqualTo(2000);
    }

    /**
     * Scenario G06: creating the same rule twice, or a rule to a destination that already has one (even with another
     * aggregation), fails with {@code TSDB: the destination key already has a src rule} (server message, 8.10.0).
     */
    @Test
    void createRuleDuplicate() {

        compactionPair();

        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, 1000))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the destination key already has a src rule");
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregationType.SUM, 2000))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the destination key already has a src rule");
        assertThat(redis.tsInfo(SRC).getRules()).hasSize(1);
    }

    /**
     * Scenario G06: the invalid topologies of TIMESERIES-CANONICAL.md §5 are rejected with the server's messages (8.10.0):
     * source = destination → {@code TSDB: the source key and destination key should be different}; a source that is already a
     * destination → {@code TSDB: the source key already has a source rule}; a destination that is already a source →
     * {@code TSDB: the destination key already has a dst rule}; a destination of another source →
     * {@code TSDB: the destination key already has a src rule}.
     */
    @Test
    void createRuleInvalidTopology() {

        compactionPair();
        redis.tsCreate("{ts}:other", new TsCreateArgs().label("t", testName));
        redis.tsCreate("{ts}:free", new TsCreateArgs().label("t", testName));

        assertThatThrownBy(() -> redis.tsCreateRule("{ts}:other", "{ts}:other", TsAggregationType.AVG, 1000))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the source key and destination key should be different");
        assertThatThrownBy(() -> redis.tsCreateRule(DST, "{ts}:free", TsAggregationType.AVG, 1000))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the source key already has a source rule");
        assertThatThrownBy(() -> redis.tsCreateRule("{ts}:other", SRC, TsAggregationType.AVG, 1000))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the destination key already has a dst rule");
        assertThatThrownBy(() -> redis.tsCreateRule("{ts}:other", DST, TsAggregationType.AVG, 1000))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the destination key already has a src rule");
    }

    /**
     * Scenario G06 / A18: an unknown aggregation from {@code TsAggregation.of} (the Javadoc: Lettuce does not check names) is
     * passed through and the server rejects it with {@code TSDB: Unknown aggregation type} (server message, 8.10.0). A bucket
     * duration of 0 or less is rejected by the client with {@link IllegalArgumentException} before anything is sent (design
     * §2.2, H19; test plan A18), with and without {@code alignTimestamp}. No rule is created.
     */
    @Test
    void createRuleInvalidArguments() {

        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        redis.tsCreate(DST, new TsCreateArgs().label("t", testName));

        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregation.of("median"), 1000))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: Unknown aggregation type");
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(redis.tsInfo(SRC).getRules()).isEmpty();
    }

    /**
     * Scenario G06: DELETERULE of a rule that does not exist fails with {@code TSDB: compaction rule does not exist} (server
     * message, 8.10.0): between two series without a rule, in the reverse direction of an existing rule, and a second time
     * after the rule was deleted. The existing rule is kept.
     */
    @Test
    void deleteRuleMissingRule() {

        compactionPair();
        redis.tsCreate("{ts}:other", new TsCreateArgs().label("t", testName));

        assertThatThrownBy(() -> redis.tsDeleteRule("{ts}:other", DST)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: compaction rule does not exist");
        assertThatThrownBy(() -> redis.tsDeleteRule(DST, SRC)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: compaction rule does not exist");
        assertThat(redis.tsInfo(SRC).getRules()).hasSize(1);

        assertThat(redis.tsDeleteRule(SRC, DST)).isEqualTo("OK");
        assertThatThrownBy(() -> redis.tsDeleteRule(SRC, DST)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: compaction rule does not exist");
    }

    /**
     * Scenario G06 / J02 (CREATERULE): CREATERULE with a missing source or a missing destination fails with
     * {@code TSDB: the key does not exist}, and the missing key is not created. DELETERULE with a missing source fails with the
     * same message.
     */
    @Test
    void createRuleMissingKey() {

        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        redis.tsCreate(DST, new TsCreateArgs().label("t", testName));

        assertThatThrownBy(() -> redis.tsCreateRule(MISSING, DST, TsAggregationType.AVG, 1000))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: the key does not exist");
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, MISSING, TsAggregationType.AVG, 1000, 500))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: the key does not exist");
        assertThatThrownBy(() -> redis.tsDeleteRule(MISSING, DST)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key does not exist");

        assertThat(redis.exists(MISSING)).isEqualTo(0);
        assertThat(redis.tsInfo(SRC).getRules()).isEmpty();
        assertThat(redis.tsInfo(DST).getSourceKey()).isNull();
    }

    /**
     * Scenario A18, through the API of every flavor and topology: a {@code null} key or aggregation throws
     * {@link IllegalArgumentException} (Javadoc of {@code tsCreateRule} / {@code tsDeleteRule}).
     */
    @Test
    void createRuleAndDeleteRuleRejectNullArgumentsThroughTheApi() {

        assertThatThrownBy(() -> redis.tsCreateRule(null, DST, TsAggregationType.AVG, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, null, TsAggregationType.AVG, 1000, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, null, 1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsDeleteRule(SRC, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsDeleteRule(null, DST)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A18b, through the API of every flavor and topology: a negative alignTimestamp throws
     * {@link IllegalArgumentException} on the client, not the server's {@code TSDB: Couldn't parse alignTimestamp}, so no
     * command is sent and no rule is created; alignment 0 is accepted.
     */
    @Test
    void createRuleRejectsNegativeAlignmentThroughTheApi() {

        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        redis.tsCreate(DST, new TsCreateArgs().label("t", testName));

        assertThatThrownBy(() -> redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, 1000, -1))
                .isInstanceOf(IllegalArgumentException.class).isNotInstanceOf(RedisCommandExecutionException.class);
        assertThat(redis.tsInfo(SRC).getRules()).isEmpty();
        assertThat(redis.tsInfo(DST).getSourceKey()).isNull();

        assertThat(redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, 1000, 0)).isEqualTo("OK");
        assertThat(ruleTo(redis.tsInfo(SRC), DST).getAlignment()).isEqualTo(0);
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

    // --- single-key reads: helpers -----------------------------------------------------------------------------------

    protected static final String EMPTY_BUCKETS = "{ts}:e";

    protected static final String NAN = "{ts}:nan";

    protected static final String STRING_KEY = "{ts}:string";

    /**
     * The WRONGTYPE message of a TS read on a string key (observed on 8.10.0: TS.GET, TS.RANGE and TS.REVRANGE reply
     * {@code ERR WRONGTYPE …}, TS.READ replies {@code WRONGTYPE …}).
     */
    protected static final String WRONGTYPE = "WRONGTYPE Operation against a key holding the wrong kind of value";

    protected static TsSample sample(long timestamp, double... values) {

        List<Double> list = new ArrayList<>();
        for (double value : values) {
            list.add(value);
        }
        return TsSample.of(timestamp, list);
    }

    protected static List<TsSample> rows(TsSample... samples) {
        return Arrays.asList(samples);
    }

    protected static List<Long> timestamps(List<TsSample> samples) {

        List<Long> result = new ArrayList<>();
        for (TsSample sample : samples) {
            result.add(sample.getTimestamp());
        }
        return result;
    }

    protected static List<Double> firstValues(List<TsSample> samples) {

        List<Double> result = new ArrayList<>();
        for (TsSample sample : samples) {
            result.add(sample.getValue());
        }
        return result;
    }

    /**
     * Series {@code {ts}:e} with the samples (1000, 1) and (7000, 7), so that bucket 3000 has an empty middle bucket (test plan
     * C10).
     */
    protected void emptyBucketSeries() {
        redis.tsCreate(EMPTY_BUCKETS, new TsCreateArgs().label("t", testName));
        redis.tsAdd(EMPTY_BUCKETS, 1000L, 1.0);
        redis.tsAdd(EMPTY_BUCKETS, 7000L, 7.0);
    }

    /**
     * Fixture F7 {@code nanSeries} (Redis 8.6+): {@code {ts}:nan} with the values 1, NaN, 3, NaN at 1000, 2000, 3000, 4000.
     */
    protected void nanSeries() {
        redis.tsCreate(NAN, new TsCreateArgs().label("t", testName));
        redis.tsAdd(NAN, 1000L, 1.0);
        redis.tsAdd(NAN, 2000L, Double.NaN);
        redis.tsAdd(NAN, 3000L, 3.0);
        redis.tsAdd(NAN, 4000L, Double.NaN);
    }

    protected static TsRangeArgs agg(long bucket, TsAggregation... aggregations) {
        return new TsRangeArgs().aggregation(bucket, aggregations);
    }

    /**
     * Read the TS.MADD element of a {@link TransactionResult}. The reactive subclass overrides this, because the reactive
     * {@code tsMAdd} produces {@code Value<Long>} elements (design §2.3).
     */
    @SuppressWarnings("unchecked")
    protected List<Long> maddResultInTransaction(Object element) {
        return (List<Long>) element;
    }

    /**
     * A dedicated connection with its async API, for the pipelining test (L01). The subclasses open it with their protocol and
     * topology.
     */
    protected static final class Pipeline implements AutoCloseable {

        final StatefulConnection<String, String> connection;

        final RedisTimeSeriesAsyncCommands<String, String> async;

        private final Runnable shutdown;

        protected Pipeline(StatefulConnection<String, String> connection, RedisTimeSeriesAsyncCommands<String, String> async,
                Runnable shutdown) {
            this.connection = connection;
            this.async = async;
            this.shutdown = shutdown;
        }

        @Override
        public void close() {
            connection.close();
            shutdown.run();
        }

    }

    protected Pipeline openPipeline() {

        RedisClient client = RedisClient.create(TestClientResources.get(),
                RedisURI.create(TestSettings.host(), TestSettings.port()));
        StatefulRedisConnection<String, String> connection = client.connect();
        return new Pipeline(connection, connection.async(), client::shutdown);
    }

    // --- TS.GET ------------------------------------------------------------------------------------------------------

    /**
     * Scenario C01 / J02 (GET): GET on a missing key fails with {@code TSDB: the key does not exist}, with and without LATEST.
     */
    @Test
    void getMissingKey() {

        assertThatThrownBy(() -> redis.tsGet(MISSING)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key does not exist");
        assertThatThrownBy(() -> redis.tsGet(MISSING, true)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key does not exist");
    }

    /**
     * Scenario C01: GET on a series without samples returns {@code null}, with and without LATEST.
     */
    @Test
    void getEmptySeries() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));

        assertThat(redis.tsGet(KEY)).isNull();
        assertThat(redis.tsGet(KEY, true)).isNull();
        assertThat(redis.tsGet(KEY, false)).isNull();
    }

    /**
     * Scenario C01: GET on F1 returns the latest sample (3000, 3.0); LATEST has no effect on a series that is not a compaction.
     */
    @Test
    void getLatestSample() {

        simpleSeries();

        TsSample sample = redis.tsGet(F1);
        assertThat(sample).isEqualTo(TsSample.of(3000, 3.0));
        assertThat(sample.getTimestamp()).isEqualTo(3000);
        assertThat(sample.getValue()).isEqualTo(3.0);
        assertThat(sample.getValues()).containsExactly(3.0);
        assertThat(redis.tsGet(F1, true)).isEqualTo(TsSample.of(3000, 3.0));
        assertThat(redis.tsGet(F1, false)).isEqualTo(TsSample.of(3000, 3.0));
    }

    /**
     * Scenario C02: F4. GET on the destination without LATEST → (1000, 15), the closed bucket; with LATEST → (2000, 30), the
     * open bucket.
     */
    @Test
    void getLatestOnCompaction() {

        compactionPair();

        assertThat(redis.tsGet(DST)).isEqualTo(TsSample.of(1000, 15.0));
        assertThat(redis.tsGet(DST, false)).isEqualTo(TsSample.of(1000, 15.0));
        assertThat(redis.tsGet(DST, true)).isEqualTo(TsSample.of(2000, 30.0));
    }

    /**
     * Scenario G03b: F4. GET on the destination returns the compacted value of the closed bucket, (1000, 15).
     */
    @Test
    void getCompactedValue() {

        compactionPair();

        assertThat(redis.tsGet(DST)).isEqualTo(TsSample.of(1000, 15.0));
        assertThat(redis.tsGet(SRC)).isEqualTo(TsSample.of(2000, 30.0));
    }

    /**
     * Scenario C02: F4. RANGE and REVRANGE on the destination include the open bucket only with LATEST.
     */
    @Test
    void rangeLatestOnCompaction() {

        compactionPair();

        assertThat(redis.tsRange(DST, Range.unbounded())).containsExactly(TsSample.of(1000, 15.0));
        assertThat(redis.tsRange(DST, Range.unbounded(), new TsRangeArgs().latest())).containsExactly(TsSample.of(1000, 15.0),
                TsSample.of(2000, 30.0));
        assertThat(redis.tsRevRange(DST, Range.unbounded())).containsExactly(TsSample.of(1000, 15.0));
        assertThat(redis.tsRevRange(DST, Range.unbounded(), new TsRangeArgs().latest()))
                .containsExactly(TsSample.of(2000, 30.0), TsSample.of(1000, 15.0));
    }

    /**
     * Scenario G04 (values): CREATERULE AVG, bucket 1000, alignTimestamp 500 shifts the buckets to start at 500, 1500, 2500.
     * Source samples (1000, 10), (1500, 20), (2000, 30), (3000, 40) → closed buckets (500, 10) and (1500, 25); the open bucket
     * (2500, 40) is visible with LATEST (verified on 8.10.0 with redis-cli).
     */
    @Test
    void createRuleAlignmentShiftsBuckets() {

        redis.tsCreate(SRC, new TsCreateArgs().label("t", testName));
        redis.tsCreate(DST, new TsCreateArgs().label("t", testName));
        assertThat(redis.tsCreateRule(SRC, DST, TsAggregationType.AVG, 1000, 500)).isEqualTo("OK");
        redis.tsAdd(SRC, 1000L, 10.0);
        redis.tsAdd(SRC, 1500L, 20.0);
        redis.tsAdd(SRC, 2000L, 30.0);
        redis.tsAdd(SRC, 3000L, 40.0);

        assertThat(redis.tsRange(DST, Range.unbounded())).containsExactly(TsSample.of(500, 10.0), TsSample.of(1500, 25.0));
        assertThat(redis.tsGet(DST)).isEqualTo(TsSample.of(1500, 25.0));
        assertThat(redis.tsGet(DST, true)).isEqualTo(TsSample.of(2500, 40.0));
        assertThat(redis.tsRange(DST, Range.unbounded(), new TsRangeArgs().latest())).containsExactly(TsSample.of(500, 10.0),
                TsSample.of(1500, 25.0), TsSample.of(2500, 40.0));
    }

    // --- TS.RANGE / TS.REVRANGE --------------------------------------------------------------------------------------

    /**
     * Scenario C03: F1. {@code Range.create(1000, 2000)} → 2 samples; unbounded → 3; REVRANGE returns the same samples in
     * descending order.
     */
    @Test
    void rangeInclusiveAndUnbounded() {

        simpleSeries();

        assertThat(redis.tsRange(F1, Range.create(1000L, 2000L))).containsExactly(TsSample.of(1000, 1.0),
                TsSample.of(2000, 2.0));
        assertThat(redis.tsRange(F1, Range.unbounded())).containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0),
                TsSample.of(3000, 3.0));
        assertThat(redis.tsRevRange(F1, Range.create(1000L, 2000L))).containsExactly(TsSample.of(2000, 2.0),
                TsSample.of(1000, 1.0));
        assertThat(redis.tsRevRange(F1, Range.unbounded())).containsExactly(TsSample.of(3000, 3.0), TsSample.of(2000, 2.0),
                TsSample.of(1000, 1.0));
    }

    /**
     * Scenario C03: F1. An exclusive lower bound {@code excluding(1000)..unbounded} → (2000), (3000); one inclusive side with
     * the other unbounded.
     */
    @Test
    void rangeExclusiveLowerAndHalfUnbounded() {

        simpleSeries();

        assertThat(redis.tsRange(F1, Range.from(Boundary.excluding(1000L), Boundary.unbounded())))
                .containsExactly(TsSample.of(2000, 2.0), TsSample.of(3000, 3.0));
        assertThat(redis.tsRange(F1, Range.from(Boundary.unbounded(), Boundary.including(2000L))))
                .containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0));
        assertThat(redis.tsRevRange(F1, Range.from(Boundary.excluding(1000L), Boundary.unbounded())))
                .containsExactly(TsSample.of(3000, 3.0), TsSample.of(2000, 2.0));
    }

    /**
     * Scenario C03: from &gt; to, and a range without samples, return an empty list.
     */
    @Test
    void rangeEmptyResults() {

        simpleSeries();

        assertThat(redis.tsRange(F1, Range.create(3000L, 1000L))).isEmpty();
        assertThat(redis.tsRevRange(F1, Range.create(3000L, 1000L))).isEmpty();
        assertThat(redis.tsRange(F1, Range.create(4000L, 5000L))).isEmpty();
        assertThat(redis.tsRange(F1, Range.from(Boundary.excluding(1000L), Boundary.excluding(2000L)))).isEmpty();
    }

    /**
     * Scenario C03: RANGE on a series without samples returns an empty list.
     */
    @Test
    void rangeOnEmptySeries() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));

        assertThat(redis.tsRange(KEY, Range.unbounded())).isEmpty();
        assertThat(redis.tsRevRange(KEY, Range.unbounded())).isEmpty();
        assertThat(redis.tsRange(KEY, Range.unbounded(), agg(1000, TsAggregationType.AVG))).isEmpty();
    }

    /**
     * Scenario C13: F2. RANGE {@code excluding(2000)..excluding(5000)} → ts 3000, 4000; REVRANGE → 4000, 3000.
     */
    @Test
    void rangeExclusiveBothBounds() {

        statsSeries();

        Range<Long> range = Range.from(Boundary.excluding(2000L), Boundary.excluding(5000L));
        assertThat(redis.tsRange(F2, range)).containsExactly(TsSample.of(3000, 4.0), TsSample.of(4000, 4.0));
        assertThat(redis.tsRevRange(F2, range)).containsExactly(TsSample.of(4000, 4.0), TsSample.of(3000, 4.0));
    }

    /**
     * Scenario C04: F2 with COUNT 3. RANGE → ts 1000, 2000, 3000; REVRANGE → 8000, 7000, 6000.
     */
    @Test
    void rangeCount() {

        statsSeries();

        assertThat(redis.tsRange(F2, Range.unbounded(), new TsRangeArgs().count(3))).containsExactly(TsSample.of(1000, 2.0),
                TsSample.of(2000, 4.0), TsSample.of(3000, 4.0));
        assertThat(redis.tsRevRange(F2, Range.unbounded(), new TsRangeArgs().count(3))).containsExactly(TsSample.of(8000, 9.0),
                TsSample.of(7000, 7.0), TsSample.of(6000, 5.0));
    }

    /**
     * Scenario C05: F2. FILTER_BY_TS 1000 3000 5000 → values 2, 4, 5; FILTER_BY_VALUE 4 5 → 4, 4, 4, 5, 5; both → 4, 5;
     * fractional bounds 4.5 5.5 → 5, 5.
     */
    @Test
    void rangeFilters() {

        statsSeries();

        assertThat(redis.tsRange(F2, Range.unbounded(), new TsRangeArgs().filterByTimestamps(1000, 3000, 5000)))
                .containsExactly(TsSample.of(1000, 2.0), TsSample.of(3000, 4.0), TsSample.of(5000, 5.0));
        assertThat(redis.tsRange(F2, Range.unbounded(), new TsRangeArgs().filterByValue(4, 5))).containsExactly(
                TsSample.of(2000, 4.0), TsSample.of(3000, 4.0), TsSample.of(4000, 4.0), TsSample.of(5000, 5.0),
                TsSample.of(6000, 5.0));
        assertThat(redis.tsRange(F2, Range.unbounded(),
                new TsRangeArgs().filterByTimestamps(1000, 3000, 5000).filterByValue(4, 5)))
                        .containsExactly(TsSample.of(3000, 4.0), TsSample.of(5000, 5.0));
        assertThat(redis.tsRange(F2, Range.unbounded(), new TsRangeArgs().filterByValue(4.5, 5.5)))
                .containsExactly(TsSample.of(5000, 5.0), TsSample.of(6000, 5.0));
        assertThat(redis.tsRevRange(F2, Range.unbounded(), new TsRangeArgs().filterByValue(4.5, 5.5)))
                .containsExactly(TsSample.of(6000, 5.0), TsSample.of(5000, 5.0));
    }

    /**
     * Scenario C06: F2, one bucket (10000, range {@code -}..{@code +}): every aggregation of the 8.0 floor gives the exact
     * value of test plan §4 (TWA verified on 8.0.2, 8.8.0 and 8.10.0).
     */
    @Test
    void rangeEveryAggregationOneBucket() {

        statsSeries();

        Map<TsAggregation, Double> expected = new LinkedHashMap<>();
        expected.put(TsAggregationType.AVG, 5.0);
        expected.put(TsAggregationType.SUM, 40.0);
        expected.put(TsAggregationType.MIN, 2.0);
        expected.put(TsAggregationType.MAX, 9.0);
        expected.put(TsAggregationType.RANGE, 7.0);
        expected.put(TsAggregationType.COUNT, 8.0);
        expected.put(TsAggregationType.FIRST, 2.0);
        expected.put(TsAggregationType.LAST, 9.0);
        expected.put(TsAggregationType.STD_P, 2.0);
        expected.put(TsAggregationType.STD_S, 2.138089935299395);
        expected.put(TsAggregationType.VAR_P, 4.0);
        expected.put(TsAggregationType.VAR_S, 4.571428571428571);
        expected.put(TsAggregationType.TWA, 4.928571428571429);

        for (Map.Entry<TsAggregation, Double> entry : expected.entrySet()) {
            assertThat(redis.tsRange(F2, Range.unbounded(), agg(10000, entry.getKey()))).as(entry.getKey().toString())
                    .containsExactly(TsSample.of(0, entry.getValue()));
        }
    }

    /**
     * Scenario C06: F2, bucket 3000: TWA → 3.5, 4.5, 7; avg, sum, min, max, count per bucket as in test plan §4.
     */
    @Test
    void rangeAggregationsBucket3000() {

        statsSeries();

        assertThat(redis.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.TWA))).containsExactly(TsSample.of(0, 3.5),
                TsSample.of(3000, 4.5), TsSample.of(6000, 7.0));
        assertThat(redis.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.AVG))).containsExactly(TsSample.of(0, 3.0),
                TsSample.of(3000, 13.0 / 3), TsSample.of(6000, 7.0));
        assertThat(firstValues(redis.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.SUM)))).containsExactly(6.0,
                13.0, 21.0);
        assertThat(firstValues(redis.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.MIN)))).containsExactly(2.0,
                4.0, 5.0);
        assertThat(firstValues(redis.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.MAX)))).containsExactly(4.0,
                5.0, 9.0);
        assertThat(firstValues(redis.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.COUNT)))).containsExactly(2.0,
                3.0, 3.0);
    }

    /**
     * Scenario C07 (Redis 8.8+): F2, bucket 3000, {@code min,avg,max} → rows (0, [2, 3, 4]), (3000, [4, 4.333333333333333, 5]),
     * (6000, [5, 7, 9]); {@code getValue()} is the first value; REVRANGE returns the rows reversed; an empty window returns an
     * empty list.
     */
    @Test
    void rangeSeveralAggregations() {

        assumeVersion("8.8");
        statsSeries();

        TsRangeArgs args = agg(3000, TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX);
        List<TsSample> range = redis.tsRange(F2, Range.unbounded(), args);

        assertThat(range).containsExactly(sample(0, 2, 3, 4), sample(3000, 4, 4.333333333333333, 5), sample(6000, 5, 7, 9));
        assertThat(firstValues(range)).containsExactly(2.0, 4.0, 5.0);
        assertThat(range.get(1).getValues()).containsExactly(4.0, 4.333333333333333, 5.0);

        assertThat(redis.tsRevRange(F2, Range.unbounded(),
                agg(3000, TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX)))
                        .containsExactly(sample(6000, 5, 7, 9), sample(3000, 4, 4.333333333333333, 5), sample(0, 2, 3, 4));
        assertThat(redis.tsRange(F2, Range.create(10000L, 20000L),
                agg(3000, TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX))).isEmpty();
    }

    /**
     * Scenario C08: F2, bucket 3000, avg. Alignment 0 → ts 0, 3000, 6000; {@code alignStart()} over 1000..8000 → 1000, 4000,
     * 7000 with avg 3.3333333333333335, 4.666666666666667, 8 (count 3/3/2); {@code alignEnd()} over 1000..8000 → 0, 2000, 5000,
     * 8000 with avg 2, 4, 5.666666666666667, 9 (count 1/3/3/1).
     */
    @Test
    void rangeAlign() {

        statsSeries();
        Range<Long> range = Range.create(1000L, 8000L);

        assertThat(redis.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.AVG).align(0)))
                .containsExactly(TsSample.of(0, 3.0), TsSample.of(3000, 13.0 / 3), TsSample.of(6000, 7.0));

        assertThat(redis.tsRange(F2, range, agg(3000, TsAggregationType.AVG).alignStart())).containsExactly(
                TsSample.of(1000, 3.3333333333333335), TsSample.of(4000, 4.666666666666667), TsSample.of(7000, 8.0));
        assertThat(firstValues(redis.tsRange(F2, range, agg(3000, TsAggregationType.COUNT).alignStart()))).containsExactly(3.0,
                3.0, 2.0);

        assertThat(redis.tsRange(F2, range, agg(3000, TsAggregationType.AVG).alignEnd())).containsExactly(TsSample.of(0, 2.0),
                TsSample.of(2000, 4.0), TsSample.of(5000, 5.666666666666667), TsSample.of(8000, 9.0));
        assertThat(firstValues(redis.tsRange(F2, range, agg(3000, TsAggregationType.COUNT).alignEnd()))).containsExactly(1.0,
                3.0, 3.0, 1.0);
    }

    /**
     * Scenario C08: a numeric alignment equal to the start of the range gives the same buckets as {@code alignStart()}.
     */
    @Test
    void rangeAlignNumeric() {

        statsSeries();

        assertThat(redis.tsRange(F2, Range.create(1000L, 8000L), agg(3000, TsAggregationType.AVG).align(1000))).containsExactly(
                TsSample.of(1000, 3.3333333333333335), TsSample.of(4000, 4.666666666666667), TsSample.of(7000, 8.0));
    }

    /**
     * Scenario C09: F2, bucket 3000, avg: BUCKETTIMESTAMP {@code START} → 0, 3000, 6000; {@code END} → 3000, 6000, 9000;
     * {@code MID} → 1500, 4500, 7500. The values are unchanged.
     */
    @Test
    void rangeBucketTimestamp() {

        statsSeries();

        List<TsSample> start = redis.tsRange(F2, Range.unbounded(),
                agg(3000, TsAggregationType.AVG).bucketTimestamp(TsBucketTimestamp.START));
        List<TsSample> end = redis.tsRange(F2, Range.unbounded(),
                agg(3000, TsAggregationType.AVG).bucketTimestamp(TsBucketTimestamp.END));
        List<TsSample> mid = redis.tsRange(F2, Range.unbounded(),
                agg(3000, TsAggregationType.AVG).bucketTimestamp(TsBucketTimestamp.MID));

        assertThat(timestamps(start)).containsExactly(0L, 3000L, 6000L);
        assertThat(timestamps(end)).containsExactly(3000L, 6000L, 9000L);
        assertThat(timestamps(mid)).containsExactly(1500L, 4500L, 7500L);
        assertThat(firstValues(start)).isEqualTo(firstValues(end)).isEqualTo(firstValues(mid)).containsExactly(3.0, 13.0 / 3,
                7.0);
    }

    /**
     * Scenario C10: samples (1000, 1) and (7000, 7), bucket 3000. Without EMPTY 2 buckets; with EMPTY 3 buckets (0, 3000,
     * 6000): avg → 1, NaN, 7; last → 1, 1, 7; twa → 2, 4.5, 6.5; sum → 1, 0, 7; count → 1, 0, 1 (verified on 8.0.2, 8.8.0 and
     * 8.10.0).
     */
    @Test
    void rangeEmpty() {

        emptyBucketSeries();

        assertThat(redis.tsRange(EMPTY_BUCKETS, Range.unbounded(), agg(3000, TsAggregationType.AVG)))
                .containsExactly(TsSample.of(0, 1.0), TsSample.of(6000, 7.0));

        assertThat(redis.tsRange(EMPTY_BUCKETS, Range.unbounded(), agg(3000, TsAggregationType.AVG).empty()))
                .containsExactly(TsSample.of(0, 1.0), TsSample.of(3000, Double.NaN), TsSample.of(6000, 7.0));
        assertThat(redis.tsRange(EMPTY_BUCKETS, Range.unbounded(), agg(3000, TsAggregationType.LAST).empty()))
                .containsExactly(TsSample.of(0, 1.0), TsSample.of(3000, 1.0), TsSample.of(6000, 7.0));
        assertThat(redis.tsRange(EMPTY_BUCKETS, Range.unbounded(), agg(3000, TsAggregationType.TWA).empty()))
                .containsExactly(TsSample.of(0, 2.0), TsSample.of(3000, 4.5), TsSample.of(6000, 6.5));
        assertThat(redis.tsRange(EMPTY_BUCKETS, Range.unbounded(), agg(3000, TsAggregationType.SUM).empty()))
                .containsExactly(TsSample.of(0, 1.0), TsSample.of(3000, 0.0), TsSample.of(6000, 7.0));
        assertThat(redis.tsRange(EMPTY_BUCKETS, Range.unbounded(), agg(3000, TsAggregationType.COUNT).empty()))
                .containsExactly(TsSample.of(0, 1.0), TsSample.of(3000, 0.0), TsSample.of(6000, 1.0));
    }

    /**
     * Scenario C11: F2, REVRANGE with avg, bucket 3000 → (6000, 7), (3000, 4.333…), (0, 3).
     */
    @Test
    void revRangeWithAggregation() {

        statsSeries();

        assertThat(redis.tsRevRange(F2, Range.unbounded(), agg(3000, TsAggregationType.AVG)))
                .containsExactly(TsSample.of(6000, 7.0), TsSample.of(3000, 13.0 / 3), TsSample.of(0, 3.0));
    }

    /**
     * Scenario C12: a series named {@code {ts}:align} (and one named exactly {@code align}, the key of RedisTimeSeries #2052),
     * with F2's data and {@code ALIGN start} over 1000..8000: the key is not taken for an option, and the buckets are those of
     * C08.
     */
    @Test
    void rangeKeyNamedAlign() {

        double[] values = { 2, 4, 4, 4, 5, 5, 7, 9 };
        for (String key : new String[] { "{ts}:align", "align" }) {
            redis.tsCreate(key, new TsCreateArgs().label("t", testName));
            for (int i = 0; i < values.length; i++) {
                redis.tsAdd(key, (i + 1) * 1000L, values[i]);
            }

            assertThat(redis.tsRange(key, Range.create(1000L, 8000L), agg(3000, TsAggregationType.AVG).alignStart())).as(key)
                    .containsExactly(TsSample.of(1000, 3.3333333333333335), TsSample.of(4000, 4.666666666666667),
                            TsSample.of(7000, 8.0));
        }
    }

    /**
     * Scenario B14 (range part): retention 1000, samples at 1000, 1500 and 3000 → RANGE returns only (3000, 3.0).
     */
    @Test
    void retentionTrimmingRange() {

        redis.tsCreate(KEY, new TsCreateArgs().retention(1000).label("t", testName));
        redis.tsAdd(KEY, 1000L, 1.0);
        redis.tsAdd(KEY, 1500L, 2.0);
        redis.tsAdd(KEY, 3000L, 3.0);

        assertThat(redis.tsRange(KEY, Range.unbounded())).containsExactly(TsSample.of(3000, 3.0));
        assertThat(redis.tsRange(KEY, Range.create(0L, 5000L))).containsExactly(TsSample.of(3000, 3.0));
    }

    /**
     * Scenario J02 (RANGE): RANGE and REVRANGE on a missing key fail with {@code TSDB: the key does not exist}
     * (TIMESERIES-CANONICAL.md §5; observed on 8.10.0).
     */
    @Test
    void rangeMissingKey() {

        assertThatThrownBy(() -> redis.tsRange(MISSING, Range.unbounded())).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: the key does not exist");
        assertThatThrownBy(() -> redis.tsRevRange(MISSING, Range.unbounded(), new TsRangeArgs().count(1)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: the key does not exist");
    }

    /**
     * Scenario J01 (RANGE): GET, RANGE and REVRANGE on a string key fail with WRONGTYPE (observed on 8.10.0:
     * {@code ERR WRONGTYPE Operation against a key holding the wrong kind of value}).
     */
    @Test
    void readsOnWrongType() {

        redis.set(STRING_KEY, "x");

        assertThatThrownBy(() -> redis.tsRange(STRING_KEY, Range.unbounded()))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining(WRONGTYPE);
        assertThatThrownBy(() -> redis.tsRevRange(STRING_KEY, Range.unbounded()))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining(WRONGTYPE);
        assertThatThrownBy(() -> redis.tsGet(STRING_KEY)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining(WRONGTYPE);
    }

    /**
     * Scenario A15, through the API of every flavor and topology: ALIGN, BUCKETTIMESTAMP or EMPTY without AGGREGATION throws
     * {@link IllegalArgumentException}, not a server error, so nothing is sent.
     */
    @Test
    void rangeRejectsAggregationOnlyOptionsThroughTheApi() {

        simpleSeries();

        assertThatThrownBy(() -> redis.tsRange(F1, Range.unbounded(), new TsRangeArgs().empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRange(F1, Range.unbounded(), new TsRangeArgs().align(0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                () -> redis.tsRevRange(F1, Range.unbounded(), new TsRangeArgs().bucketTimestamp(TsBucketTimestamp.END)))
                        .isInstanceOf(IllegalArgumentException.class);
        assertThat(redis.tsRange(F1, Range.unbounded())).hasSize(3);
    }

    /**
     * Scenario A08 / A19, through the API of every flavor and topology: {@code null} arguments and an exclusive bound at the
     * {@code Long} edge throw {@link IllegalArgumentException}.
     */
    @Test
    void readsRejectNullArgumentsThroughTheApi() {

        assertThatThrownBy(() -> redis.tsGet(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsGet(null, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRange(KEY, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRange(KEY, Range.unbounded(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRevRange(null, Range.unbounded())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRange(KEY, Range.from(Boundary.excluding(Long.MAX_VALUE), Boundary.unbounded())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRead(KEY, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRead(KEY, TsReadOffset.earliest(), null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- NaN (Redis 8.6+) --------------------------------------------------------------------------------------------

    /**
     * Scenario H01 (reads, Redis 8.6+): F7. GET, RANGE and REVRANGE return the stored NaN values.
     */
    @Test
    void nanReads() {

        assumeVersion("8.6");
        nanSeries();

        TsSample latest = redis.tsGet(NAN);
        assertThat(latest.getTimestamp()).isEqualTo(4000);
        assertThat(latest.getValue()).isNaN();
        assertThat(redis.tsRange(NAN, Range.unbounded())).containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, Double.NaN),
                TsSample.of(3000, 3.0), TsSample.of(4000, Double.NaN));
        assertThat(redis.tsRevRange(NAN, Range.unbounded())).containsExactly(TsSample.of(4000, Double.NaN),
                TsSample.of(3000, 3.0), TsSample.of(2000, Double.NaN), TsSample.of(1000, 1.0));
    }

    /**
     * Scenario H02 (Redis 8.6+): F7, one bucket: countNaN 2, countAll 4, count 2, avg 2, sum 4 (NaN skipped). With bucket 2000,
     * the bucket [0, 2000) has no NaN sample and gets no countNaN row (observed on 8.8.0 and 8.10.0).
     */
    @Test
    void countNanAndCountAll() {

        assumeVersion("8.6");
        nanSeries();

        assertThat(redis.tsRange(NAN, Range.unbounded(), agg(10000, TsAggregationType.COUNTNAN)))
                .containsExactly(TsSample.of(0, 2.0));
        assertThat(redis.tsRange(NAN, Range.unbounded(), agg(10000, TsAggregationType.COUNTALL)))
                .containsExactly(TsSample.of(0, 4.0));
        assertThat(redis.tsRange(NAN, Range.unbounded(), agg(10000, TsAggregationType.COUNT)))
                .containsExactly(TsSample.of(0, 2.0));
        assertThat(redis.tsRange(NAN, Range.unbounded(), agg(10000, TsAggregationType.AVG)))
                .containsExactly(TsSample.of(0, 2.0));
        assertThat(redis.tsRange(NAN, Range.unbounded(), agg(10000, TsAggregationType.SUM)))
                .containsExactly(TsSample.of(0, 4.0));

        assertThat(redis.tsRange(NAN, Range.unbounded(), agg(2000, TsAggregationType.COUNTNAN)))
                .containsExactly(TsSample.of(2000, 1.0), TsSample.of(4000, 1.0));
    }

    // --- TS.READ (non-blocking) --------------------------------------------------------------------------------------

    /**
     * Scenario F01 (Redis 8.10+): F1. {@code from(0)} → all 3; {@code from(2000)} (inclusive) → 2; {@code earliest()} → all;
     * {@code latest()} → (3000); {@code newSamples()} without BLOCK → empty.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void readOffsets() {

        simpleSeries();
        List<TsSample> all = rows(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0), TsSample.of(3000, 3.0));

        assertThat(redis.tsRead(F1, TsReadOffset.from(0))).isEqualTo(all);
        assertThat(redis.tsRead(F1, TsReadOffset.from(2000))).containsExactly(TsSample.of(2000, 2.0), TsSample.of(3000, 3.0));
        assertThat(redis.tsRead(F1, TsReadOffset.earliest())).isEqualTo(all);
        assertThat(redis.tsRead(F1, TsReadOffset.latest())).containsExactly(TsSample.of(3000, 3.0));
        assertThat(redis.tsRead(F1, TsReadOffset.newSamples())).isEmpty();
        assertThat(redis.tsRead(F1, TsReadOffset.from(0), new TsReadArgs())).isEqualTo(all);
    }

    /**
     * Scenario F02 (Redis 8.10+): F2 with {@code maxCount(3)}, starting at {@code earliest()} and continuing with
     * {@code from(last + 1)} → pages of 3, 3, 2, then an empty page; no gaps, no duplicates.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void readPaging() {

        statsSeries();

        List<List<TsSample>> pages = new ArrayList<>();
        List<TsSample> page = redis.tsRead(F2, TsReadOffset.earliest(), new TsReadArgs().maxCount(3));
        while (!page.isEmpty() && pages.size() < 10) {
            pages.add(page);
            long last = page.get(page.size() - 1).getTimestamp();
            page = redis.tsRead(F2, TsReadOffset.from(last + 1), new TsReadArgs().maxCount(3));
        }

        assertThat(pages).hasSize(3);
        assertThat(timestamps(pages.get(0))).containsExactly(1000L, 2000L, 3000L);
        assertThat(timestamps(pages.get(1))).containsExactly(4000L, 5000L, 6000L);
        assertThat(timestamps(pages.get(2))).containsExactly(7000L, 8000L);

        List<TsSample> joined = new ArrayList<>();
        pages.forEach(joined::addAll);
        assertThat(joined).isEqualTo(redis.tsRange(F2, Range.unbounded()));
    }

    /**
     * Scenario F03 (Redis 8.10+): reading past the newest sample, or a missing key, returns an empty list, not an error.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void readPastNewestAndMissingKey() {

        statsSeries();

        assertThat(redis.tsRead(F2, TsReadOffset.from(8001))).isEmpty();
        assertThat(redis.tsRead(F2, TsReadOffset.from(9000), new TsReadArgs().maxCount(5))).isEmpty();
        assertThat(redis.tsRead(MISSING, TsReadOffset.from(0))).isEmpty();
        assertThat(redis.tsRead(MISSING, TsReadOffset.earliest())).isEmpty();
        assertThat(redis.tsRead(MISSING, TsReadOffset.latest())).isEmpty();
        assertThat(redis.tsRead(MISSING, TsReadOffset.newSamples())).isEmpty();
        assertThat(redis.exists(MISSING)).isZero();
    }

    /**
     * Scenario F11 (Redis 8.10+): values the client never sends are rejected by the server and passed through, sent via
     * {@code dispatch}: a negative timestamp → {@code TSDB: invalid timestamp}; {@code min_count > MAX_COUNT} →
     * {@code TSDB: BLOCK min_count must be <= MAX_COUNT} (observed on 8.10.0).
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void readServerValidation() {

        statsSeries();

        CommandArgs<String, String> negative = new CommandArgs<>(StringCodec.UTF8).addKey(F2).add("-1");
        assertThatThrownBy(() -> redis.dispatch(CommandType.TS_READ, new NestedMultiOutput<>(StringCodec.UTF8), negative))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: invalid timestamp");

        CommandArgs<String, String> counts = new CommandArgs<>(StringCodec.UTF8).addKey(F2).add("0").add("BLOCK").add("10")
                .add("5").add("MAX_COUNT").add("3");
        assertThatThrownBy(() -> redis.dispatch(CommandType.TS_READ, new NestedMultiOutput<>(StringCodec.UTF8), counts))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: BLOCK min_count must be <= MAX_COUNT");
    }

    /**
     * Scenario A11 / A17 (READ), through the API of every flavor and topology: invalid TS.READ arguments throw
     * {@link IllegalArgumentException} before anything is sent.
     */
    @Test
    void readRejectsInvalidArgumentsThroughTheApi() {

        assertThatThrownBy(() -> TsReadOffset.from(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().block(100, 5).maxCount(3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.tsRead(null, TsReadOffset.earliest())).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario J01 (READ, Redis 8.10+): TS.READ on a string key fails with WRONGTYPE (observed on 8.10.0:
     * {@code WRONGTYPE Operation against a key holding the wrong kind of value}).
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void readOnWrongType() {

        redis.set(STRING_KEY, "x");

        assertThatThrownBy(() -> redis.tsRead(STRING_KEY, TsReadOffset.from(0)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining(WRONGTYPE);
        assertThatThrownBy(() -> redis.tsRead(STRING_KEY, TsReadOffset.earliest(), new TsReadArgs().maxCount(1)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining(WRONGTYPE);
    }

    // --- execution modes ---------------------------------------------------------------------------------------------

    /**
     * Scenario L01: async pipeline with auto-flush off. A TS.MADD with a rejected sample, then TS.GET, TS.RANGE and TS.ADD are
     * queued and flushed together; each future gets its own reply.
     */
    @Test
    void asyncPipelineAlignment() throws Exception {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAdd(KEY, 1000L, 5.0);

        try (Pipeline pipeline = openPipeline()) {

            pipeline.connection.setAutoFlushCommands(false);
            RedisTimeSeriesAsyncCommands<String, String> async = pipeline.async;

            RedisFuture<List<Long>> madd = async
                    .tsMAdd(Arrays.asList(TsMAddValue.of(KEY, 1000L, 9.0), TsMAddValue.of(KEY, 2000L, 2.0)));
            RedisFuture<TsSample> get = async.tsGet(KEY);
            RedisFuture<List<TsSample>> range = async.tsRange(KEY, Range.unbounded());
            RedisFuture<Long> add = async.tsAdd(KEY, 3000L, 3.0);
            RedisFuture<List<TsSample>> rangeAfter = async.tsRevRange(KEY, Range.unbounded(), new TsRangeArgs().count(1));

            assertThat(madd.isDone()).isFalse();
            pipeline.connection.flushCommands();

            assertThat(madd.get(10, TimeUnit.SECONDS)).containsExactly(null, 2000L);
            assertThat(get.get(10, TimeUnit.SECONDS)).isEqualTo(TsSample.of(2000, 2.0));
            assertThat(range.get(10, TimeUnit.SECONDS)).containsExactly(TsSample.of(1000, 5.0), TsSample.of(2000, 2.0));
            assertThat(add.get(10, TimeUnit.SECONDS)).isEqualTo(3000L);
            assertThat(rangeAfter.get(10, TimeUnit.SECONDS)).containsExactly(TsSample.of(3000, 3.0));
        }
    }

    /**
     * Scenario L02: MULTI/EXEC on standalone. CREATE, ADD, MADD, RANGE and GET in one transaction → the
     * {@link TransactionResult} has one entry per command, in order, with the type of each command's result.
     */
    @Test
    void transactionWithReads() {

        redis.multi();
        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAdd(KEY, 1000L, 1.0);
        redis.tsMAdd(Arrays.asList(TsMAddValue.of(KEY, 2000L, 2.0), TsMAddValue.of(KEY, 3000L, 3.0)));
        redis.tsRange(KEY, Range.unbounded());
        redis.tsGet(KEY);
        TransactionResult result = redis.exec();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(5);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((Long) result.get(1)).isEqualTo(1000L);
        assertThat(maddResultInTransaction(result.get(2))).containsExactly(2000L, 3000L);
        assertThat(result.<List<TsSample>> get(3)).containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0),
                TsSample.of(3000, 3.0));
        assertThat((TsSample) result.get(4)).isEqualTo(TsSample.of(3000, 3.0));
    }

    /**
     * Scenario L02 (MADD partial failure): inside MULTI/EXEC, a TS.MADD with a sample rejected under policy BLOCK gives
     * {@code [null, ts]} at its position, as outside a transaction (design §2.2 and H2: a {@code null} element means the server
     * rejected that sample); the commands after it get their own replies.
     */
    @Test
    void transactionWithMAddPartialFailure() {

        redis.multi();
        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAdd(KEY, 1000L, 1.0);
        redis.tsMAdd(Arrays.asList(TsMAddValue.of(KEY, 1000L, 2.0), TsMAddValue.of(KEY, 2000L, 3.0)));
        redis.tsRange(KEY, Range.unbounded());
        redis.tsGet(KEY);
        TransactionResult result = redis.exec();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(5);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((Long) result.get(1)).isEqualTo(1000L);
        assertThat(result.<List<TsSample>> get(3)).containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 3.0));
        assertThat((TsSample) result.get(4)).isEqualTo(TsSample.of(2000, 3.0));
        assertThat((Object) result.get(2)).isNotInstanceOf(Throwable.class);
        assertThat(maddResultInTransaction(result.get(2))).containsExactly(null, 2000L);
    }

}
