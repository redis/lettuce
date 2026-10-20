/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Range;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import reactor.test.StepVerifier;

/**
 * Runs {@link RedisTimeSeriesIntegrationTests} through the reactive API.
 * <p>
 * The reactive {@code tsMAdd} emits {@code Value<Long>}, with {@link Value#empty()} for a rejected sample (design §2.3), so the
 * base suite's TS.MADD calls are unwrapped into nullable elements, and the reactive shape has its own tests.
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesReactiveIntegrationTests extends RedisTimeSeriesIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    RedisTimeSeriesReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
    }

    @Override
    protected List<Long> madd(List<TsMAddValue<String>> samples) {

        List<Value<Long>> values = reactive.tsMAdd(samples).collectList().block();
        List<Long> result = new ArrayList<>();
        for (Value<Long> value : values) {
            result.add(value.getValueOrElse(null));
        }
        return result;
    }

    /**
     * Scenario L03 (MADD): the reactive {@code tsMAdd} emits {@code Value.empty()} for a rejected sample and
     * {@code Value.just(ts)} for an accepted one, in input order.
     */
    @Test
    void maddReactiveEmitsValueEmptyForRejectedSample() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        redis.tsAdd(KEY, 1000L, 5.0);

        StepVerifier
                .create(reactive.tsMAdd(Arrays.asList(TsMAddValue.of(KEY, 1000L, 6.0), TsMAddValue.of(KEY, 2000L, 7.0),
                        TsMAddValue.of(MISSING, 1000L, 1.0))))
                .expectNext(Value.empty()).expectNext(Value.just(2000L)).expectNext(Value.empty()).verifyComplete();

        assertThat(storedSamples(KEY)).isEqualTo(samples(1000, 5.0, 2000, 7.0));
    }

    /**
     * Scenario L03 (MADD): every sample accepted emits only {@code Value.just(ts)}.
     */
    @Test
    void maddReactiveAllAccepted() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));

        StepVerifier.create(reactive.tsMAdd(Arrays.asList(TsMAddValue.of(KEY, 1000L, 1.0), TsMAddValue.of(KEY, 2000L, 2.0))))
                .expectNext(Value.just(1000L)).expectNext(Value.just(2000L)).verifyComplete();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Long> maddResultInTransaction(Object element) {

        List<Long> result = new ArrayList<>();
        for (Value<Long> value : (List<Value<Long>>) element) {
            result.add(value.getValueOrElse(null));
        }
        return result;
    }

    /**
     * Scenario L03 (GET): {@code tsGet} on a series without samples completes empty (design §2.3); on F1 it emits (3000, 3.0).
     */
    @Test
    void getReactiveEmptySeriesCompletesEmpty() {

        redis.tsCreate(KEY, new TsCreateArgs().label("t", testName));
        StepVerifier.create(reactive.tsGet(KEY)).verifyComplete();
        StepVerifier.create(reactive.tsGet(KEY, true)).verifyComplete();

        simpleSeries();
        StepVerifier.create(reactive.tsGet(F1)).expectNext(TsSample.of(3000, 3.0)).verifyComplete();
    }

    /**
     * Scenario L03 (GET): {@code tsGet} on a missing key fails with the server error.
     */
    @Test
    void getReactiveMissingKeyErrors() {

        StepVerifier.create(reactive.tsGet(MISSING))
                .expectErrorSatisfies(e -> assertThat(e).hasMessageContaining("TSDB: the key does not exist")).verify();
    }

    /**
     * Scenario L03 (RANGE): {@code tsRange} emits one element per row, in order; {@code tsRevRange} in reverse order; an empty
     * range completes without elements.
     */
    @Test
    void rangeReactiveEmitsRows() {

        simpleSeries();

        StepVerifier.create(reactive.tsRange(F1, Range.unbounded())).expectNext(TsSample.of(1000, 1.0))
                .expectNext(TsSample.of(2000, 2.0)).expectNext(TsSample.of(3000, 3.0)).verifyComplete();
        StepVerifier.create(reactive.tsRevRange(F1, Range.unbounded())).expectNext(TsSample.of(3000, 3.0))
                .expectNext(TsSample.of(2000, 2.0)).expectNext(TsSample.of(1000, 1.0)).verifyComplete();
        StepVerifier.create(reactive.tsRange(F1, Range.create(5000L, 6000L))).verifyComplete();
    }

    /**
     * Scenario L03 (RANGE): an aggregated range emits one bucket per element.
     */
    @Test
    void rangeReactiveEmitsBuckets() {

        statsSeries();

        StepVerifier.create(reactive.tsRange(F2, Range.unbounded(), agg(3000, TsAggregationType.AVG)))
                .expectNext(TsSample.of(0, 3.0)).expectNext(TsSample.of(3000, 13.0 / 3)).expectNext(TsSample.of(6000, 7.0))
                .verifyComplete();
    }

}
