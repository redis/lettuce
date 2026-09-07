/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import javax.inject.Inject;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactive integration tests for Redis TimeSeries commands. Re-runs all tests from {@link RedisTimeSeriesIntegrationTests}
 * routing every call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 *
 * <p>
 * Overrides verify the two scenarios that a wrong wire encoding could break in a way a unit test cannot catch: the
 * {@code LABELS} keyword being emitted last, and {@link TsAggregationType#STD_P} encoding to {@code STD.P}. Both are exercised
 * here directly against {@link RedisReactiveCommands} using {@link StepVerifier}.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesReactiveIntegrationTests extends RedisTimeSeriesIntegrationTests {

    private static final String MY_KEY = "temperature:sensor1";

    private static final String SOURCE_KEY = "{ts-rule}:raw";

    private static final String DEST_KEY = "{ts-rule}:hourly";

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    public RedisTimeSeriesReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
    }

    @Test
    @Override
    void tsCreate() {
        StepVerifier.create(reactive.tsCreate(MY_KEY)).expectNext("OK").verifyComplete();
        StepVerifier.create(reactive.tsAlter(MY_KEY, TsAlterArgs.Builder.retention(1000))).expectNext("OK").verifyComplete();
    }

    @Test
    @Override
    void tsCreateWithIgnoreAfterLabelsOnBuilderStillSucceeds() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("sensor", "1");
        TsCreateArgs args = TsCreateArgs.Builder.labels(labels).ignore(100, 0.1);

        StepVerifier.create(reactive.tsCreate(MY_KEY, args)).expectNext("OK").verifyComplete();

        assertThat(redis.tsInfo(MY_KEY).getLabels()).containsExactlyInAnyOrderEntriesOf(labels);
    }

    @Test
    @Override
    void tsCreateRuleWithDottedAggregationType() {
        StepVerifier.create(reactive.tsCreate(SOURCE_KEY)).expectNext("OK").verifyComplete();
        StepVerifier.create(reactive.tsCreate(DEST_KEY)).expectNext("OK").verifyComplete();

        StepVerifier.create(reactive.tsCreateRule(SOURCE_KEY, DEST_KEY, TsAggregationType.STD_P, 60000)).expectNext("OK")
                .verifyComplete();
    }

    @Test
    @Override
    void tsDelRemovesSamplesInRange() {
        StepVerifier.create(reactive.tsCreate(MY_KEY)).expectNext("OK").verifyComplete();
        redis.tsAdd(MY_KEY, 100, 1.0);
        redis.tsAdd(MY_KEY, 200, 2.0);
        redis.tsAdd(MY_KEY, 300, 3.0);

        StepVerifier.create(reactive.tsDel(MY_KEY, 100, 200)).expectNext(2L).verifyComplete();
        assertThat(redis.tsInfo(MY_KEY).getTotalSamples()).isEqualTo(1L);
        assertSample(redis.tsGet(MY_KEY), 300L, 3.0);
    }

    /**
     * Overridden with a bounded {@link StepVerifier} timeout to document, rather than hang on, a pre-existing bug in
     * {@code io.lettuce.core.RedisPublisher.SubscriptionCommand#doOnComplete()}: it calls {@code getOutput().get()}
     * <em>before</em> checking {@code getOutput().hasError()}. For any {@code EncodedComplexOutput}-backed command whose
     * {@code ComplexDataParser} throws on a {@code null} {@link io.lettuce.core.output.ComplexData} (every parser in this
     * codebase does, including the already-shipped {@code CfInfoValueParser}), that throw happens inside {@code doOnComplete()}
     * itself and is never converted into {@code onError}, so the
     * {@link reactor.core.publisher.Flux}/{@link reactor.core.publisher.Mono} never terminates on a server error reply.
     * Confirmed independently against {@code
     * reactive().cfInfo("does-not-exist")}, which exhibits the identical hang, so this is not specific to {@code TS.MGET} or to
     * this PR. Fixed in {@code redis/lettuce} PR #3851, which reorders {@code doOnComplete()} to check {@code hasError()}
     * before {@code get()}.
     */
    @Test
    @Override
    void tsMGetWithoutEqualityFilterFails() {
        prepareMGetFixture();

        StepVerifier.create(reactive.tsMGet("type!=temp")).expectErrorMessage("ERR TSDB: please provide at least one matcher")
                .verify(Duration.ofSeconds(5));
    }

    /**
     * Overridden because {@code TS.MADD} is {@code Flux<Value<Long>>} on the reactive flavor (Reactive Streams cannot emit
     * {@code null}), so the base assertions against raw {@code Long} elements don't apply here; verified directly against
     * {@link #reactive} instead of through the sync-mirrored {@code redis}.
     */
    @Test
    @Override
    void tsMAddAndGetRoundTrip() {
        String key1 = "{ts-madd}:k1";
        String key2 = "{ts-madd}:k2";
        redis.tsCreate(key1);
        redis.tsCreate(key2);

        StepVerifier.create(reactive.tsMAdd(mAddEntry(key1, 1000, 10.0), mAddEntry(key2, 1000, 20.0)))
                .expectNext(Value.just(1000L)).expectNext(Value.just(1000L)).verifyComplete();

        assertSample(redis.tsGet(key1), 1000L, 10.0);
        assertSample(redis.tsGet(key2), 1000L, 20.0);
    }

    /**
     * Overridden because {@code TS.MADD} is {@code Flux<Value<Long>>} on the reactive flavor: a failed entry is
     * {@link Value#empty()}, not {@code null}, so the base assertion doesn't apply here.
     */
    @Test
    @Override
    void tsMAddOnMissingKeyFailsInsteadOfAutoCreating() {
        StepVerifier.create(reactive.tsMAdd(mAddEntry("series:does-not-exist", 1000, 1.0))).expectNext(Value.empty())
                .verifyComplete();
    }

    /**
     * Overridden because {@code TS.MADD} is {@code Flux<Value<Long>>} on the reactive flavor: a partial failure surfaces as
     * {@link Value#empty()} in place of the failed entry's timestamp, instead of a {@code null} list element. This is the
     * scenario a shared-output regression previously broke: {@code RedisSubscription} rejects a {@code null} emission with
     * {@code IllegalArgumentException: Data must not be null}, masking the server's own error reply.
     */
    @Test
    @Override
    void tsMAddPartialFailureReturnsNullForFailedEntryAndKeepsSuccessfulTimestamps() {
        String key1 = "{ts-madd-partial}:k1";
        String key2 = "{ts-madd-partial}:k2";
        redis.tsCreate(key1);
        redis.tsCreate(key2, TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicy.BLOCK));
        redis.tsAdd(key2, 1000, 5.0);

        StepVerifier.create(reactive.tsMAdd(mAddEntry(key1, 2000, 10.0), mAddEntry(key2, 1000, 20.0)))
                .expectNext(Value.just(2000L)).expectNext(Value.empty()).verifyComplete();

        // The successful entry actually persisted server-side; the failed one left key2's existing sample untouched.
        assertSample(redis.tsGet(key1), 2000L, 10.0);
        assertSample(redis.tsGet(key2), 1000L, 5.0);
    }

}
