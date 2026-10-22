/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One time series of a <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a> or
 * <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a> reply: a matching series, or with {@code GROUPBY} one
 * group of series combined by the reducer.
 * <p>
 * {@link #getAggregations()} and {@link #getReducer()} come from the request, so they are the same on RESP2 and RESP3. RESP2
 * and RESP3 replies produce equal values, except {@link #getSources()}: on RESP2 it is only available when the request has
 * {@code WITHLABELS}. {@link TsSeries} is immutable.
 *
 * @param <K> Key type.
 * @since 8.0
 */
public final class TsSeries<K> {

    private final K key;

    private final String group;

    private final Map<String, String> labels;

    private final List<TsAggregation> aggregations;

    private final TsReducer reducer;

    private final List<K> sources;

    private final List<TsSample> samples;

    TsSeries(K key, String group, Map<String, String> labels, List<TsAggregation> aggregations, TsReducer reducer,
            List<K> sources, List<TsSample> samples) {
        this.key = key;
        this.group = group;
        this.labels = Collections.unmodifiableMap(labels);
        this.aggregations = Collections.unmodifiableList(aggregations);
        this.reducer = reducer;
        this.sources = sources == null ? null : Collections.unmodifiableList(sources);
        this.samples = Collections.unmodifiableList(samples);
    }

    /**
     * Whether this entry is a group of series ({@code GROUPBY}).
     *
     * @return {@code true} if the request had {@code GROUPBY}.
     * @since 8.0
     */
    public boolean isGrouped() {
        return group != null;
    }

    /**
     * Get the key of the series.
     *
     * @return the key, or {@code null} for a group.
     * @since 8.0
     */
    public K getKey() {
        return key;
    }

    /**
     * Get the name of the group, {@code label=value}.
     *
     * @return the group name, or {@code null} if the request had no {@code GROUPBY}.
     * @since 8.0
     */
    public String getGroup() {
        return group;
    }

    /**
     * Get the labels, in server order. Empty unless labels were requested with {@code WITHLABELS} or {@code SELECTED_LABELS}. A
     * selected label that is missing has a {@code null} value. The synthetic labels {@code __reducer__} and {@code __source__}
     * of a group are not included.
     *
     * @return the labels, never {@code null}.
     * @since 8.0
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Get the aggregations of the request, in request order.
     *
     * @return the aggregations, never {@code null}; empty if the request had no aggregation.
     * @since 8.0
     */
    public List<TsAggregation> getAggregations() {
        return aggregations;
    }

    /**
     * Get the reducer of the request.
     *
     * @return the reducer, or {@code null} if the request had no {@code GROUPBY}.
     * @since 8.0
     */
    public TsReducer getReducer() {
        return reducer;
    }

    /**
     * Get the keys of the series combined into this group. On RESP3 they are always available for a group. On RESP2 they are
     * only available when the request has {@code WITHLABELS}; they are read from the {@code __source__} label, so a key that
     * contains a comma is split wrongly.
     *
     * @return the source keys, or {@code null} if this entry is not a group or the keys are not available.
     * @since 8.0
     */
    public List<K> getSources() {
        return sources;
    }

    /**
     * Get the samples, or buckets with an aggregation, in the order of the command (descending for TS.MREVRANGE).
     *
     * @return the samples, never {@code null}; empty if the series has no samples in the range.
     * @since 8.0
     */
    public List<TsSample> getSamples() {
        return samples;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsSeries)) {
            return false;
        }

        TsSeries<?> that = (TsSeries<?>) o;
        return Objects.equals(key, that.key) && Objects.equals(group, that.group) && labels.equals(that.labels)
                && aggregations.equals(that.aggregations) && Objects.equals(reducer, that.reducer)
                && Objects.equals(sources, that.sources) && samples.equals(that.samples);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, group, labels, aggregations, reducer, sources, samples);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + (isGrouped() ? "group=" + group : "key=" + key) + ", labels=" + labels
                + ", aggregations=" + aggregations + ", reducer=" + reducer + ", sources=" + sources + ", samples=" + samples
                + "]";
    }

}
