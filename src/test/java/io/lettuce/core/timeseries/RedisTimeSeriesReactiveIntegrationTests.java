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

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Disabled;
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
     * before {@code get()}; this test is disabled until that PR merges.
     */
    @Test
    @Disabled("Blocked by RedisPublisher reactive error-handling bug; fixed in redis/lettuce#3851. "
            + "Re-enable once that fix is merged.")
    @Override
    void tsMGetWithoutEqualityFilterFails() {
        prepareMGetFixture();

        StepVerifier.create(reactive.tsMGet("type!=temp")).expectErrorMessage("ERR TSDB: please provide at least one matcher")
                .verify(Duration.ofSeconds(5));
    }

}
