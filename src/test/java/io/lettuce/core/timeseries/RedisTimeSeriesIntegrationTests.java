/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsMGetArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisTimeSeriesCommands}.
 * <p>
 * These are round-trip tests: every write is followed by a read (mostly {@code TS.INFO}/{@code TS.GET}/{@code TS.MGET}) that
 * verifies server-side state, not just that the write returned {@code OK}.
 *
 * @author Gyumin Hwang
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.CREATE")
public class RedisTimeSeriesIntegrationTests {

    private static final String MY_KEY = "temperature:sensor1";

    // hash-tagged so both keys route to the same cluster slot for TS.CREATERULE/TS.DELETERULE
    private static final String SOURCE_KEY = "{ts-rule}:raw";

    private static final String DEST_KEY = "{ts-rule}:hourly";

    private static final String DEST_KEY2 = "{ts-rule}:daily";

    protected final RedisCommands<String, String> redis;

    @Inject
    protected RedisTimeSeriesIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void prepare() {
        redis.flushall();
    }

    @AfterAll
    void teardown() {
        redis.flushall();
    }

    // ------------------------------------------------------------------------------------------------------------
    // TS.CREATE / TS.ALTER / TS.INFO
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsCreate() {
        String result = redis.tsCreate(MY_KEY);

        assertThat(result).isEqualTo("OK");
        // TS.ALTER only succeeds against a series that actually exists on the server.
        assertThat(redis.tsAlter(MY_KEY, TsAlterArgs.Builder.retention(1000))).isEqualTo("OK");
    }

    /**
     * Create -> Read round trip. Verifies every {@code TS.CREATE} option actually persisted server-side, including the
     * {@code IGNORE} thresholds and that {@code LABELS} never absorbs {@code IGNORE}'s values as a label pair.
     */
    @Test
    void tsCreateWithAllOptionsRoundTripsThroughInfo() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");
        labels.put("type", "temp");

        TsCreateArgs args = TsCreateArgs.Builder.retention(86400000).chunkSize(4096).duplicatePolicy(TsDuplicatePolicy.LAST)
                .ignore(5, 0.1).labels(labels);

        assertThat(redis.tsCreate(MY_KEY, args)).isEqualTo("OK");

        TsInfoValue<String> info = redis.tsInfo(MY_KEY);
        assertThat(info.getRetentionTime()).isEqualTo(86400000L);
        assertThat(info.getChunkSize()).isEqualTo(4096L);
        assertThat(info.getDuplicatePolicy()).isEqualToIgnoringCase("LAST");
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(5L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(0.1);
        assertThat(info.getTotalSamples()).isEqualTo(0L);
        // The critical regression check: LABELS must never absorb IGNORE's own arguments as a label pair.
        assertThat(info.getLabels()).containsExactlyInAnyOrderEntriesOf(labels);
        assertThat(info.getLabels()).doesNotContainKeys("IGNORE", "5", "0.1");
    }

    @Test
    void tsCreateWithUncompressedEncoding() {
        TsCreateArgs args = TsCreateArgs.Builder.encoding(TsEncodingFormat.UNCOMPRESSED);

        String result = redis.tsCreate(MY_KEY, args);

        assertThat(result).isEqualTo("OK");
    }

    /**
     * When {@code DUPLICATE_POLICY} is never configured on the series, {@code TS.INFO} does not report an absent/{@code null}
     * value: it reports the module's effective default policy (the {@code ts-duplicate-policy} server config, {@code block} by
     * default), lowercased. Confirmed directly against a live server; the module source's {@code DP_NONE} internal state never
     * reaches the client as a blank/absent value.
     */
    @Test
    void tsCreateWithoutDuplicatePolicyReportsServerDefaultPolicy() {
        redis.tsCreate(MY_KEY);

        assertThat(redis.tsInfo(MY_KEY).getDuplicatePolicy()).isEqualToIgnoringCase("block");
    }

    /**
     * Verifies that {@code LABELS} reaches the server last on the wire even when {@code IGNORE} is configured after
     * {@code LABELS} on the builder. The RedisTimeSeries module consumes every remaining token after {@code LABELS} as a label
     * pair, so if {@code IGNORE} were emitted after {@code LABELS} the server would misinterpret it as a label key.
     */
    @Test
    void tsCreateWithIgnoreAfterLabelsOnBuilderStillSucceeds() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("sensor", "1");

        TsCreateArgs args = TsCreateArgs.Builder.labels(labels).ignore(100, 0.1);

        String result = redis.tsCreate(MY_KEY, args);

        assertThat(result).isEqualTo("OK");
        assertThat(redis.tsInfo(MY_KEY).getLabels()).containsExactlyInAnyOrderEntriesOf(labels);
        assertThat(redis.tsInfo(MY_KEY).getLabels()).doesNotContainKeys("IGNORE", "100", "0.1");
    }

    @Test
    void tsAlter() {
        redis.tsCreate(MY_KEY);

        String result = redis.tsAlter(MY_KEY, TsAlterArgs.Builder.retention(5000).duplicatePolicy(TsDuplicatePolicy.MAX));

        assertThat(result).isEqualTo("OK");
    }

    /**
     * Update -> Read round trip. {@code TS.ALTER}'s {@code LABELS} replaces the entire label set, not merges into it.
     */
    @Test
    void tsAlterRoundTripReplacesLabels() {
        Map<String, String> initialLabels = new LinkedHashMap<>();
        initialLabels.put("region", "us");
        initialLabels.put("type", "temp");
        redis.tsCreate(MY_KEY, TsCreateArgs.Builder.labels(initialLabels));

        String result = redis.tsAlter(MY_KEY, TsAlterArgs.Builder.retention(3600000).label("region", "eu"));

        assertThat(result).isEqualTo("OK");
        TsInfoValue<String> info = redis.tsInfo(MY_KEY);
        assertThat(info.getRetentionTime()).isEqualTo(3600000L);
        assertThat(info.getLabels()).containsExactly(new AbstractMap.SimpleEntry<>("region", "eu"));
    }

    @Test
    void tsAlterLabelsReset() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("sensor", "1");
        redis.tsCreate(MY_KEY, TsCreateArgs.Builder.labels(labels));

        String result = redis.tsAlter(MY_KEY, TsAlterArgs.Builder.labelsReset());

        assertThat(result).isEqualTo("OK");
        assertThat(redis.tsInfo(MY_KEY).getLabels()).isEmpty();
    }

    @Test
    void tsAlterOnNonExistentKeyFails() {
        assertThatThrownBy(() -> redis.tsAlter("does-not-exist", TsAlterArgs.Builder.retention(1000)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: the key does not exist");
    }

    @Test
    void tsCreateOnDuplicateKeyFails() {
        redis.tsCreate(MY_KEY);

        assertThatThrownBy(() -> redis.tsCreate(MY_KEY)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: key already exists");
    }

    // ------------------------------------------------------------------------------------------------------------
    // TS.CREATERULE / TS.DELETERULE / TS.INFO rules
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsCreateRuleAndDeleteRule() {
        redis.tsCreate(SOURCE_KEY);
        redis.tsCreate(DEST_KEY);

        assertThat(redis.tsCreateRule(SOURCE_KEY, DEST_KEY, TsAggregationType.AVG, 60000)).isEqualTo("OK");

        List<TsInfoValue.Rule<String>> rules = redis.tsInfo(SOURCE_KEY).getRules();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getDestKey()).isEqualTo(DEST_KEY);
        assertThat(rules.get(0).getBucketDuration()).isEqualTo(60000L);
        assertThat(rules.get(0).getAggregationType()).isEqualTo(TsAggregationType.AVG);
        assertThat(redis.tsInfo(DEST_KEY).getSourceKey()).isEqualTo(SOURCE_KEY);

        assertThat(redis.tsDeleteRule(SOURCE_KEY, DEST_KEY)).isEqualTo("OK");
        assertThat(redis.tsInfo(SOURCE_KEY).getRules()).isEmpty();
    }

    @Test
    void tsCreateRuleWithAlignTimestampAndMultipleRulesShrinksOnDelete() {
        redis.tsCreate(SOURCE_KEY);
        redis.tsCreate(DEST_KEY);
        redis.tsCreate(DEST_KEY2);

        assertThat(redis.tsCreateRule(SOURCE_KEY, DEST_KEY, TsAggregationType.SUM, 60000)).isEqualTo("OK");
        assertThat(redis.tsCreateRule(SOURCE_KEY, DEST_KEY2, TsAggregationType.STD_P, 60000, 0)).isEqualTo("OK");

        List<TsInfoValue.Rule<String>> rules = redis.tsInfo(SOURCE_KEY).getRules();
        assertThat(rules).hasSize(2);
        assertThat(rules).extracting(TsInfoValue.Rule::getAggregationType).containsExactlyInAnyOrder(TsAggregationType.SUM,
                TsAggregationType.STD_P);

        assertThat(redis.tsDeleteRule(SOURCE_KEY, DEST_KEY)).isEqualTo("OK");
        assertThat(redis.tsInfo(SOURCE_KEY).getRules()).hasSize(1);
    }

    /**
     * {@link TsAggregationType#STD_P} encodes to the wire value {@code STD.P}, which cannot be represented as a plain Java enum
     * constant name. A unit test cannot catch a wrong wire value here; only a real server round-trip can.
     */
    @Test
    void tsCreateRuleWithDottedAggregationType() {
        redis.tsCreate(SOURCE_KEY);
        redis.tsCreate(DEST_KEY);

        assertThat(redis.tsCreateRule(SOURCE_KEY, DEST_KEY, TsAggregationType.STD_P, 60000)).isEqualTo("OK");
    }

    // ------------------------------------------------------------------------------------------------------------
    // TS.ADD / TS.MADD / TS.INCRBY / TS.DECRBY / TS.GET round trips
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsAddAndGetRoundTrip() {
        assertThat(redis.tsAdd(MY_KEY, 1000, 23.5)).isEqualTo(1000L);
        assertSample(redis.tsGet(MY_KEY), 1000L, 23.5);

        assertThat(redis.tsAdd(MY_KEY, 2000, 24.5)).isEqualTo(2000L);
        assertSample(redis.tsGet(MY_KEY), 2000L, 24.5);

        long before = System.currentTimeMillis();
        Long autoTimestamp = redis.tsAdd(MY_KEY, 30.0);
        assertThat(autoTimestamp).isCloseTo(before, offset(60000L));
        assertSample(redis.tsGet(MY_KEY), autoTimestamp, 30.0);

        // TS.INCRBY/TS.DECRBY without an explicit timestamp stamp the sample with the server's current time, not the
        // timestamp of the previous sample, so this is a fresh timestamp, not necessarily equal to autoTimestamp.
        Long incrByTimestamp = redis.tsIncrBy(MY_KEY, 1.5);
        assertThat(incrByTimestamp).isCloseTo(before, offset(60000L));
        assertSample(redis.tsGet(MY_KEY), incrByTimestamp, 31.5);

        Long decrByTimestamp = redis.tsDecrBy(MY_KEY, 0.5);
        assertThat(decrByTimestamp).isCloseTo(before, offset(60000L));
        assertSample(redis.tsGet(MY_KEY), decrByTimestamp, 31.0);
    }

    /**
     * Unlike {@code TS.ADD}, {@code TS.MADD} does <b>not</b> auto-create missing series: the server rejects it with
     * {@code TSDB: the key is not a TSDB key} if any of the target keys does not already exist. This contradicts the
     * {@code tsMAdd} Javadoc's claim of implicit creation; see the discovered-bug notes in issues.md.
     * <p>
     * The keys are hash-tagged so both route to the same cluster slot: {@code TS.MADD} sends a single command carrying multiple
     * keys, which Redis Cluster rejects with {@code CROSSSLOT} unless they all hash to the same slot.
     */
    @Test
    void tsMAddAndGetRoundTrip() {
        String key1 = "{ts-madd}:k1";
        String key2 = "{ts-madd}:k2";
        redis.tsCreate(key1);
        redis.tsCreate(key2);

        List<Long> timestamps = redis.tsMAdd(mAddEntry(key1, 1000, 10.0), mAddEntry(key2, 1000, 20.0));

        assertThat(timestamps).containsExactly(1000L, 1000L);
        assertSample(redis.tsGet(key1), 1000L, 10.0);
        assertSample(redis.tsGet(key2), 1000L, 20.0);
    }

    @Test
    void tsMAddOnMissingKeyFailsInsteadOfAutoCreating() {
        assertThatThrownBy(() -> redis.tsMAdd(mAddEntry("series:does-not-exist", 1000, 1.0)))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("TSDB: the key is not a TSDB key");
    }

    /**
     * {@code DUPLICATE_POLICY} only applies when {@code TS.ADD} implicitly creates the series, but it then persists on the
     * series like a {@code TS.CREATE}/{@code TS.ALTER} configured policy would.
     */
    @Test
    void tsAddDuplicatePolicyPersistsOnImplicitCreate() {
        String newKey = "series:implicit-create";

        redis.tsAdd(newKey, 1000, 1.0, TsAddArgs.Builder.duplicatePolicy(TsDuplicatePolicy.MAX));

        assertThat(redis.tsInfo(newKey).getDuplicatePolicy()).isEqualToIgnoringCase("MAX");
    }

    /**
     * {@code ON_DUPLICATE} is a one-shot override for a single {@code TS.ADD} call against an already-existing series: it never
     * changes the policy persisted on the series (compared against whatever {@code TS.CREATE} left it at, which per
     * {@link #tsCreateWithoutDuplicatePolicyReportsServerDefaultPolicy()} is the server's configured default, not
     * {@code null}), but it does take effect for the sample it was passed with.
     */
    @Test
    void tsAddOnDuplicateIsOneShotOverrideNotPersistedButEffective() {
        String key = "series:on-duplicate";
        redis.tsCreate(key);
        String policyAfterCreate = redis.tsInfo(key).getDuplicatePolicy();

        redis.tsAdd(key, 1000, 5.0, TsAddArgs.Builder.onDuplicate(TsDuplicatePolicy.MIN));
        assertThat(redis.tsInfo(key).getDuplicatePolicy()).isEqualTo(policyAfterCreate);

        redis.tsAdd(key, 1000, 3.0, TsAddArgs.Builder.onDuplicate(TsDuplicatePolicy.MIN));
        assertSample(redis.tsGet(key), 1000L, 3.0);
        assertThat(redis.tsInfo(key).getDuplicatePolicy()).isEqualTo(policyAfterCreate);

        redis.tsAdd(key, 1000, 9.0, TsAddArgs.Builder.onDuplicate(TsDuplicatePolicy.MAX));
        assertSample(redis.tsGet(key), 1000L, 9.0);
        assertThat(redis.tsInfo(key).getDuplicatePolicy()).isEqualTo(policyAfterCreate);
    }

    // ------------------------------------------------------------------------------------------------------------
    // TS.DEL
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsDelRemovesSamplesInRange() {
        redis.tsCreate(MY_KEY);
        redis.tsAdd(MY_KEY, 100, 1.0);
        redis.tsAdd(MY_KEY, 200, 2.0);
        redis.tsAdd(MY_KEY, 300, 3.0);

        Long deleted = redis.tsDel(MY_KEY, 100, 200);

        assertThat(deleted).isEqualTo(2L);
        assertThat(redis.tsInfo(MY_KEY).getTotalSamples()).isEqualTo(1L);
        assertSample(redis.tsGet(MY_KEY), 300L, 3.0);
    }

    @Test
    void tsDelOnEmptySeriesReturnsZero() {
        redis.tsCreate(MY_KEY);

        Long deleted = redis.tsDel(MY_KEY, 0, Long.MAX_VALUE);

        assertThat(deleted).isEqualTo(0L);
    }

    // ------------------------------------------------------------------------------------------------------------
    // TS.GET on an empty series
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsGetOnEmptySeriesReturnsNull() {
        redis.tsCreate(MY_KEY);

        assertThat(redis.tsGet(MY_KEY)).isNull();
    }

    // ------------------------------------------------------------------------------------------------------------
    // TS.MGET
    // ------------------------------------------------------------------------------------------------------------

    void prepareMGetFixture() {
        redis.tsCreate("mget:us:temp", TsCreateArgs.Builder.label("region", "us").label("type", "temp"));
        redis.tsAdd("mget:us:temp", 1000, 10.0);

        redis.tsCreate("mget:us:humid", TsCreateArgs.Builder.label("region", "us").label("type", "humid"));
        redis.tsAdd("mget:us:humid", 1000, 20.0);

        redis.tsCreate("mget:eu:temp", TsCreateArgs.Builder.label("region", "eu").label("type", "temp"));
        redis.tsAdd("mget:eu:temp", 1000, 30.0);
    }

    @Test
    void tsMGetWithoutLabelsOptionReturnsEmptyLabelMap() {
        prepareMGetFixture();

        List<TsMGetValue<String>> result = redis.tsMGet("region=us");

        assertThat(result).extracting(TsMGetValue::getKey).containsExactlyInAnyOrder("mget:us:temp", "mget:us:humid");
        assertThat(result).allSatisfy(value -> assertThat(value.getLabels()).isEmpty());
    }

    @Test
    void tsMGetWithLabelsIncludesAllLabels() {
        prepareMGetFixture();

        List<TsMGetValue<String>> result = redis.tsMGet(TsMGetArgs.Builder.withLabels(), "region=us");

        TsMGetValue<String> temp = result.stream().filter(v -> v.getKey().equals("mget:us:temp")).findFirst().get();
        assertThat(temp.getLabels()).containsExactly(new AbstractMap.SimpleEntry<>("region", "us"),
                new AbstractMap.SimpleEntry<>("type", "temp"));
        assertSample(temp.getSample(), 1000L, 10.0);
    }

    @Test
    void tsMGetWithSelectedLabelsIncludesOnlyRequestedLabels() {
        prepareMGetFixture();

        List<TsMGetValue<String>> result = redis.tsMGet(TsMGetArgs.Builder.selectedLabels("type"), "region=us");

        assertThat(result).allSatisfy(value -> assertThat(value.getLabels()).containsOnlyKeys("type"));
    }

    @Test
    void tsMGetWithNonMatchingFilterReturnsEmptyList() {
        prepareMGetFixture();

        List<TsMGetValue<String>> result = redis.tsMGet("region=nonexistent");

        assertThat(result).isEmpty();
    }

    /**
     * Checks whether a series with no samples that still matches the label filter is included by {@code TS.MGET} (with a
     * {@code null} sample slot) or excluded entirely. This was extrapolated, not independently confirmed, when the read-domain
     * types were first built.
     */
    @Test
    void tsMGetIncludesSeriesWithNoSamples() {
        prepareMGetFixture();
        redis.tsCreate("mget:us:empty", TsCreateArgs.Builder.label("region", "us"));

        List<TsMGetValue<String>> result = redis.tsMGet("region=us");

        assertThat(result).extracting(TsMGetValue::getKey).contains("mget:us:empty");
        TsMGetValue<String> empty = result.stream().filter(v -> v.getKey().equals("mget:us:empty")).findFirst().get();
        assertThat(empty.getSample()).isNull();
    }

    @Test
    void tsMGetWithoutEqualityFilterFails() {
        prepareMGetFixture();

        assertThatThrownBy(() -> redis.tsMGet("type!=temp")).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("TSDB: please provide at least one matcher");
    }

    // ------------------------------------------------------------------------------------------------------------
    // TS.QUERYINDEX
    // ------------------------------------------------------------------------------------------------------------

    @Test
    void tsQueryIndexReturnsMatchingKeys() {
        prepareMGetFixture();

        List<String> result = redis.tsQueryIndex("region=us");

        assertThat(result).containsExactlyInAnyOrder("mget:us:temp", "mget:us:humid");
    }

    @Test
    void tsQueryIndexWithMultipleFiltersNarrowsResults() {
        prepareMGetFixture();

        List<String> result = redis.tsQueryIndex("region=us", "type=temp");

        assertThat(result).containsExactly("mget:us:temp");
    }

    @Test
    void tsQueryIndexWithNonMatchingFilterReturnsEmptyList() {
        prepareMGetFixture();

        List<String> result = redis.tsQueryIndex("region=nonexistent");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Test helpers
    // ------------------------------------------------------------------------------------------------------------

    static void assertSample(TsSample sample, long expectedTimestamp, double expectedValue) {
        assertThat(sample).isNotNull();
        assertThat(sample.getTimestamp()).isEqualTo(expectedTimestamp);
        assertThat(sample.getValue()).isEqualTo(expectedValue);
    }

    static Map.Entry<String, TsSample> mAddEntry(String key, long timestamp, double value) {
        return new AbstractMap.SimpleEntry<>(key, new TsSample(timestamp, Collections.singletonList(value)));
    }

}
