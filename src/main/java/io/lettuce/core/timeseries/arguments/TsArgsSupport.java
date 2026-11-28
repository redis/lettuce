/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsBucketTimestamp;

/**
 * Validation and conversion shared by the TimeSeries argument classes. Static methods only; the argument classes don't extend a
 * common base.
 *
 * @author Aleksandar Todorov
 * @since 8.0
 */
final class TsArgsSupport {

    private TsArgsSupport() {
    }

    /**
     * Convert a retention period to whole milliseconds.
     *
     * @throws IllegalArgumentException if {@code retention} is {@code null}, negative, or not zero but shorter than one
     *         millisecond.
     */
    static long retentionMillis(Duration retention) {

        LettuceAssert.notNull(retention, "Retention must not be null");
        LettuceAssert.isTrue(!retention.isNegative(), "Retention must not be negative");
        LettuceAssert.isTrue(retention.isZero() || retention.toMillis() > 0,
                "Retention must be zero or at least one millisecond");

        return retention.toMillis();
    }

    /**
     * Copy labels in iteration order.
     *
     * @throws IllegalArgumentException if {@code labels} is {@code null} or contains a {@code null} name or value.
     */
    static Map<String, String> copyLabels(Map<String, String> labels) {

        LettuceAssert.notNull(labels, "Labels must not be null");

        Map<String, String> copy = new LinkedHashMap<>();
        labels.forEach((name, value) -> {
            LettuceAssert.notNull(name, "Label name must not be null");
            LettuceAssert.notNull(value, "Label value must not be null");
            copy.put(name, value);
        });

        return copy;
    }

    /**
     * Check a bucket duration in milliseconds.
     *
     * @return {@code bucketDuration}.
     * @throws IllegalArgumentException if {@code bucketDuration} is not positive.
     */
    static long bucketDuration(long bucketDuration) {

        LettuceAssert.isTrue(bucketDuration > 0, "Bucket duration must be at least one millisecond");

        return bucketDuration;
    }

    /**
     * Convert a bucket duration to whole milliseconds. A duration shorter than one millisecond counts as zero and is rejected.
     *
     * @throws IllegalArgumentException if {@code bucket} is {@code null}, shorter than one millisecond or longer than
     *         {@code Long.MAX_VALUE} milliseconds.
     */
    static long bucketMillis(Duration bucket) {

        LettuceAssert.notNull(bucket, "Bucket duration must not be null");

        long millis;
        try {
            millis = bucket.toMillis();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Bucket duration is too large", e);
        }

        return bucketDuration(millis);
    }

    /**
     * Copy aggregations into an unmodifiable list.
     *
     * @throws IllegalArgumentException if {@code aggregations} is {@code null}, empty or contains {@code null} elements.
     */
    static List<TsAggregation> aggregations(TsAggregation... aggregations) {

        LettuceAssert.notEmpty(aggregations, "Aggregations must not be empty");
        LettuceAssert.noNullElements(aggregations, "Aggregations must not contain null elements");

        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(aggregations)));
    }

    /**
     * Check the range options that require an aggregation.
     *
     * @throws IllegalArgumentException if {@code ALIGN}, {@code BUCKETTIMESTAMP} or {@code EMPTY} is set without an
     *         aggregation.
     */
    static void assertAggregationOptions(boolean hasAggregation, String align, TsBucketTimestamp bucketTimestamp,
            boolean empty) {

        if (!hasAggregation) {
            LettuceAssert.isTrue(align == null, "ALIGN requires an aggregation");
            LettuceAssert.isTrue(bucketTimestamp == null, "BUCKETTIMESTAMP requires an aggregation");
            LettuceAssert.isTrue(!empty, "EMPTY requires an aggregation");
        }
    }

    /**
     * Join aggregation tokens with commas and no spaces, for example {@code MIN,AVG,MAX}.
     */
    static String aggregationToken(List<TsAggregation> aggregations) {

        StringBuilder token = new StringBuilder();
        for (TsAggregation aggregation : aggregations) {
            if (token.length() > 0) {
                token.append(',');
            }
            token.append(new String(aggregation.getBytes(), StandardCharsets.UTF_8));
        }
        return token.toString();
    }

}
