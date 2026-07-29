/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.List;

import javax.inject.Inject;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.timeseries.RedisTimeSeriesIntegrationTests.assertSample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the "practically common" edge cases of {@code DUPLICATE_POLICY}, {@code IGNORE},
 * {@code TS.INCRBY}/{@code TS.DECRBY} and compaction timing.
 * <p>
 * These scenarios reproduce shapes real callers hit in production (retried writes, near-duplicate ingestion, and
 * createrule-then-add ordering) that {@link RedisTimeSeriesIntegrationTests} does not cover: it only exercises
 * {@code ON_DUPLICATE} as a one-shot override, never the create-time {@code DUPLICATE_POLICY} merge effect for every policy,
 * and never {@code IGNORE}'s actual suppression behavior.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.CREATE")
public class RedisTimeSeriesPolicyIntegrationTests {

    // hash-tagged so both keys route to the same cluster slot for TS.CREATERULE/TS.DELETERULE
    private static final String SOURCE_KEY = "{ts-policy-rule}:raw";

    private static final String DEST_KEY = "{ts-policy-rule}:hourly";

    protected final RedisCommands<String, String> redis;

    @Inject
    protected RedisTimeSeriesPolicyIntegrationTests(RedisCommands<String, String> redis) {
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
    // P2-1: DUPLICATE_POLICY, all six values, reinserting at the same timestamp
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Given a series created with {@code DUPLICATE_POLICY=BLOCK}, when a second sample is added at a timestamp that already has
     * a sample, then the server rejects the write instead of silently keeping or replacing the existing value.
     */
    @Test
    void tsDuplicatePolicyBlockRejectsReinsertAtSameTimestamp() {
        String key = "policy:block";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.BLOCK));
        redis.tsAdd(key, 1000, 5.0);

        assertThatThrownBy(() -> redis.tsAdd(key, 1000, 10.0)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("BLOCK mode");
        assertSample(redis.tsGet(key), 1000L, 5.0);
    }

    /**
     * Given a series created with {@code DUPLICATE_POLICY=FIRST}, when a second sample is added at an existing timestamp, then
     * the original value is kept. Our existing suite never exercises {@code FIRST} at all.
     */
    @Test
    void tsDuplicatePolicyFirstKeepsExistingValueOnReinsert() {
        String key = "policy:first";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.FIRST));
        redis.tsAdd(key, 1000, 5.0);

        redis.tsAdd(key, 1000, 10.0);

        assertSample(redis.tsGet(key), 1000L, 5.0);
    }

    /**
     * Given a series created with {@code DUPLICATE_POLICY=LAST}, when a second sample is added at an existing timestamp, then
     * the new value overwrites the old one.
     */
    @Test
    void tsDuplicatePolicyLastOverwritesValueOnReinsert() {
        String key = "policy:last";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.LAST));
        redis.tsAdd(key, 1000, 5.0);

        redis.tsAdd(key, 1000, 10.0);

        assertSample(redis.tsGet(key), 1000L, 10.0);
    }

    /**
     * Given a series created with {@code DUPLICATE_POLICY=MIN}, when a second, larger sample is added at an existing timestamp,
     * then the smaller of the two values is kept.
     */
    @Test
    void tsDuplicatePolicyMinKeepsSmallerValueOnReinsert() {
        String key = "policy:min";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.MIN));
        redis.tsAdd(key, 1000, 5.0);

        redis.tsAdd(key, 1000, 10.0);

        assertSample(redis.tsGet(key), 1000L, 5.0);
    }

    /**
     * Given a series created with {@code DUPLICATE_POLICY=MAX}, when a second, larger sample is added at an existing timestamp,
     * then the larger of the two values is kept.
     */
    @Test
    void tsDuplicatePolicyMaxKeepsLargerValueOnReinsert() {
        String key = "policy:max";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.MAX));
        redis.tsAdd(key, 1000, 5.0);

        redis.tsAdd(key, 1000, 10.0);

        assertSample(redis.tsGet(key), 1000L, 10.0);
    }

    /**
     * Given a series created with {@code DUPLICATE_POLICY=SUM}, when repeated samples are added at the same timestamp, then
     * every reinsertion accumulates onto the stored value. This is <b>not idempotent</b>: retrying the exact same write (as a
     * caller recovering from a timeout might) inflates the stored value every time, which is the realistic incident shape this
     * test locks in.
     */
    @Test
    void tsDuplicatePolicySumAccumulatesAndIsNotIdempotentOnRetry() {
        String key = "policy:sum";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.SUM));
        redis.tsAdd(key, 1000, 5.0);

        redis.tsAdd(key, 1000, 10.0);
        assertSample(redis.tsGet(key), 1000L, 15.0);

        // A third write at the very same timestamp keeps accumulating instead of converging - a naive retry doubles the
        // damage rather than being a safe no-op.
        redis.tsAdd(key, 1000, 3.0);
        assertSample(redis.tsGet(key), 1000L, 18.0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // P2-2: IGNORE, only active under DUPLICATE_POLICY=LAST, suppressed writes return the previous timestamp
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Given a series created with {@code DUPLICATE_POLICY=LAST} and {@code IGNORE 5 10.0}, when a sample arrives within both
     * the time and value thresholds of the last stored sample, then the server silently discards it and reports the
     * <b>previous</b> sample's timestamp back to the caller - not the timestamp that was just submitted. A caller checking only
     * "did the call throw?" will believe the write succeeded and lose the sample.
     * <p>
     * Both thresholds are an AND: violating either one (a large enough time gap, or a large enough value gap) still inserts.
     */
    @Test
    void tsIgnoreSuppressesNearDuplicateAndReturnsPreviousTimestamp() {
        String key = "ignore:last-policy";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.LAST).ignore(5, 10.0));

        assertThat(redis.tsAdd(key, 1000, 1.0)).isEqualTo(1000L);
        // time gap 10 > 5: inserted normally.
        assertThat(redis.tsAdd(key, 1010, 11.0)).isEqualTo(1010L);
        // time gap 3 <= 5 AND value gap |10.0 - 11.0| = 1.0 <= 10.0: both thresholds satisfied, so this sample is ignored and
        // the previous timestamp (1010) is echoed back instead of the new one (1013).
        assertThat(redis.tsAdd(key, 1013, 10.0)).isEqualTo(1010L);
        assertSample(redis.tsGet(key), 1010L, 11.0);

        // time gap 10 > 5: inserted normally even though close in value.
        assertThat(redis.tsAdd(key, 1020, 11.5)).isEqualTo(1020L);
        // time gap 1 <= 5 BUT value gap |22.0 - 11.5| = 10.5 > 10.0: one threshold violated is enough to force insertion.
        assertThat(redis.tsAdd(key, 1021, 22.0)).isEqualTo(1021L);
        assertSample(redis.tsGet(key), 1021L, 22.0);
    }

    /**
     * Given a series created with {@code DUPLICATE_POLICY=BLOCK} (not {@code LAST}) and {@code IGNORE 5 10.0}, when a sample
     * arrives within both thresholds of the previous sample, then {@code IGNORE} does not activate at all: the sample is
     * inserted as a normal, distinct data point. {@code IGNORE} is not a general near-duplicate filter; it is a behavior
     * specific to {@code DUPLICATE_POLICY=LAST}.
     */
    @Test
    void tsIgnoreDoesNotActivateUnlessDuplicatePolicyIsLast() {
        String key = "ignore:block-policy";
        redis.tsCreate(key, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.BLOCK).ignore(5, 10.0));
        redis.tsAdd(key, 1000, 1.0);

        // Within both IGNORE thresholds of the previous sample, but the policy is BLOCK, so IGNORE never engages and this is
        // inserted as an ordinary new sample rather than being silently dropped.
        Long timestamp = redis.tsAdd(key, 1002, 1.5);

        assertThat(timestamp).isEqualTo(1002L);
        assertThat(redis.tsInfo(key).getTotalSamples()).isEqualTo(2L);
        assertSample(redis.tsGet(key), 1002L, 1.5);
    }

    // ------------------------------------------------------------------------------------------------------------
    // P2-3: TS.INCRBY/TS.DECRBY reject past timestamps on an existing series, unlike TS.ADD's upsert semantics
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Given an existing series, when {@code TS.INCRBY} is called with a timestamp older than the series' current maximum
     * timestamp, then the server rejects the call. {@code TS.ADD} allows exactly this (an upsert into the past); treating
     * {@code INCRBY}/{@code DECRBY} as "the same write family" as {@code TS.ADD} is the trap this test guards against.
     */
    @Test
    void tsIncrByRejectsTimestampOlderThanExistingMaximumOnExistingSeries() {
        String key = "incrby:existing-series";
        redis.tsCreate(key);
        redis.tsIncrBy(key, 1.0, TsIncrByArgs.Builder.timestamp(1000));

        assertThatThrownBy(() -> redis.tsIncrBy(key, 1.0, TsIncrByArgs.Builder.timestamp(500)))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("must be equal to or higher than the maximum existing timestamp");
        assertSample(redis.tsGet(key), 1000L, 1.0);
    }

    /**
     * Given an existing series, when {@code TS.DECRBY} is called with a timestamp older than the series' current maximum
     * timestamp, then the server rejects it exactly like {@code TS.INCRBY} does; both commands share the same handler.
     */
    @Test
    void tsDecrByRejectsTimestampOlderThanExistingMaximumOnExistingSeries() {
        String key = "decrby:existing-series";
        redis.tsCreate(key);
        redis.tsDecrBy(key, 1.0, TsIncrByArgs.Builder.timestamp(1000));

        assertThatThrownBy(() -> redis.tsDecrBy(key, 1.0, TsIncrByArgs.Builder.timestamp(500)))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("must be equal to or higher than the maximum existing timestamp");
        assertSample(redis.tsGet(key), 1000L, -1.0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // P2-4: compaction only reflects closed buckets; a backfilled sample never re-triggers compaction
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Given a source series with a compaction rule to a destination series, when a sample lands inside the still-open bucket,
     * then the destination is not updated until a later sample closes that bucket by crossing its boundary. Once the bucket
     * closes, a further backfilled sample landing in an <b>earlier, already-closed</b> bucket does not retroactively update the
     * destination: compaction is only ever driven forward by new writes, never re-evaluated for the past.
     */
    @Test
    void tsCreateRuleReflectsOnlyClosedBucketsAndIgnoresBackfill() {
        redis.tsCreate(SOURCE_KEY);
        redis.tsCreate(DEST_KEY);
        redis.tsCreateRule(SOURCE_KEY, DEST_KEY, TsAggregationType.AVG, 1000);

        // Both timestamps fall in different 1000ms buckets (5000-5999 and 6500-7499), so writing the second one closes the
        // first bucket and its average (10) is flushed to the destination.
        redis.tsAdd(SOURCE_KEY, 5000, 10);
        redis.tsAdd(SOURCE_KEY, 6500, 20);
        assertSample(redis.tsGet(DEST_KEY), 5000L, 10.0);

        // A backfilled sample landing in a bucket that is already far in the past does not retrigger compaction: the
        // destination still reflects only the bucket that was actually closed above.
        redis.tsAdd(SOURCE_KEY, 3000, 99);
        assertSample(redis.tsGet(DEST_KEY), 5000L, 10.0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // P2-6: TS.QUERYINDEX filter syntax, all six forms, including the two that read backwards
    // ------------------------------------------------------------------------------------------------------------

    void prepareFilterSyntaxFixture() {
        redis.tsCreate("filter:us-temp", TsCreateArgs.Builder.label("region", "us").label("type", "temp"));
        redis.tsCreate("filter:eu-temp", TsCreateArgs.Builder.label("region", "eu").label("type", "temp"));
        redis.tsCreate("filter:humid-only", TsCreateArgs.Builder.label("type", "humid"));
    }

    /**
     * Given three series with different label shapes, when each of the six {@code TS.QUERYINDEX} filter grammar forms is used,
     * then every form matches exactly what the RedisTimeSeries filter grammar defines - including the two forms whose meaning
     * reads backwards from the equals/not-equals operator: {@code label=} means the label is <b>absent</b>, and {@code label!=}
     * means the label <b>is present</b>. Every query still includes at least one {@code EQ}/{@code LIST_MATCH} filter, since
     * the server requires one.
     */
    @Test
    void tsQueryIndexSupportsAllSixFilterSyntaxForms() {
        prepareFilterSyntaxFixture();

        // l=v (EQ): matches series whose label equals the given value.
        List<String> eq = redis.tsQueryIndex("region=us");
        assertThat(eq).containsExactly("filter:us-temp");

        // l= (label absent): counter-intuitively means the label does not exist on the series at all, not "equals empty
        // string".
        List<String> labelAbsent = redis.tsQueryIndex("type=humid", "region=");
        assertThat(labelAbsent).containsExactly("filter:humid-only");

        // l!=v (NEQ): matches series where the label exists and differs from the given value.
        List<String> neq = redis.tsQueryIndex("type=temp", "region!=us");
        assertThat(neq).containsExactly("filter:eu-temp");

        // l!= (label present): counter-intuitively means the label exists on the series, not "not equal to empty string".
        List<String> labelPresent = redis.tsQueryIndex("type=temp", "region!=");
        assertThat(labelPresent).containsExactlyInAnyOrder("filter:us-temp", "filter:eu-temp");

        // l=(v1,v2) (LIST_MATCH): matches series whose label equals any value in the list.
        List<String> listMatch = redis.tsQueryIndex("region=(us,eu)");
        assertThat(listMatch).containsExactlyInAnyOrder("filter:us-temp", "filter:eu-temp");

        // l!=(v1,v2) (LIST_NOTMATCH): excludes series whose label equals any value in the list.
        List<String> listNotMatch = redis.tsQueryIndex("type=temp", "region!=(us,eu)");
        assertThat(listNotMatch).isEmpty();
    }

}
