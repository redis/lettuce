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
 * Information about a TimeSeries, as returned by the Redis <a href="https://redis.io/commands/ts.info/">TS.INFO</a> command.
 * <p>
 * RESP2 and RESP3 replies produce equal values. {@link #getChunks()} and {@link #getKeySelfName()} are only populated when the
 * information was requested with {@code DEBUG}. Fields that this Lettuce version does not model are available through
 * {@link #getRawInfo()}.
 *
 * @param <K> Key type.
 * @since 8.0
 */
public class TsInfoValue<K> {

    private final long totalSamples;

    private final long memoryUsage;

    private final long firstTimestamp;

    private final long lastTimestamp;

    private final long retentionTime;

    private final long chunkCount;

    private final long chunkSize;

    private final TsEncoding chunkType;

    private final TsDuplicatePolicy duplicatePolicy;

    private final Map<String, String> labels;

    private final K sourceKey;

    private final List<Rule<K>> rules;

    private final Long ignoreMaxTimeDiff;

    private final Double ignoreMaxValDiff;

    private final K keySelfName;

    private final List<Chunk> chunks;

    private final Map<String, Object> rawInfo;

    TsInfoValue(long totalSamples, long memoryUsage, long firstTimestamp, long lastTimestamp, long retentionTime,
            long chunkCount, long chunkSize, TsEncoding chunkType, TsDuplicatePolicy duplicatePolicy,
            Map<String, String> labels, K sourceKey, List<Rule<K>> rules, Long ignoreMaxTimeDiff, Double ignoreMaxValDiff,
            K keySelfName, List<Chunk> chunks, Map<String, Object> rawInfo) {
        this.totalSamples = totalSamples;
        this.memoryUsage = memoryUsage;
        this.firstTimestamp = firstTimestamp;
        this.lastTimestamp = lastTimestamp;
        this.retentionTime = retentionTime;
        this.chunkCount = chunkCount;
        this.chunkSize = chunkSize;
        this.chunkType = chunkType;
        this.duplicatePolicy = duplicatePolicy;
        this.labels = Collections.unmodifiableMap(labels);
        this.sourceKey = sourceKey;
        this.rules = Collections.unmodifiableList(rules);
        this.ignoreMaxTimeDiff = ignoreMaxTimeDiff;
        this.ignoreMaxValDiff = ignoreMaxValDiff;
        this.keySelfName = keySelfName;
        this.chunks = chunks == null ? null : Collections.unmodifiableList(chunks);
        this.rawInfo = Collections.unmodifiableMap(rawInfo);
    }

    /**
     * Get the number of samples in the series.
     *
     * @return the number of samples.
     */
    public long getTotalSamples() {
        return totalSamples;
    }

    /**
     * Get the memory used by the series, including its configuration, rules, labels and chunks.
     *
     * @return the memory usage in bytes.
     */
    public long getMemoryUsage() {
        return memoryUsage;
    }

    /**
     * Get the timestamp of the first sample.
     *
     * @return the first timestamp in milliseconds, {@code 0} when the series has no samples.
     */
    public long getFirstTimestamp() {
        return firstTimestamp;
    }

    /**
     * Get the timestamp of the last sample.
     *
     * @return the last timestamp in milliseconds, {@code 0} when the series has no samples.
     */
    public long getLastTimestamp() {
        return lastTimestamp;
    }

    /**
     * Get the retention period of the series.
     *
     * @return the retention period in milliseconds, {@code 0} when samples never expire.
     */
    public long getRetentionTime() {
        return retentionTime;
    }

    /**
     * Get the number of data chunks of the series.
     *
     * @return the number of chunks.
     */
    public long getChunkCount() {
        return chunkCount;
    }

    /**
     * Get the memory size of a data chunk of the series.
     *
     * @return the chunk size in bytes.
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * Get the encoding of the data chunks.
     *
     * @return the chunk encoding: a {@link TsEncodingType} constant when this Lettuce version knows the value, otherwise a
     *         {@link TsEncoding} that keeps the server's name; {@code null} if the server did not report it.
     */
    public TsEncoding getChunkType() {
        return chunkType;
    }

    /**
     * Get the duplicate policy of the series.
     *
     * @return the duplicate policy: a {@link TsDuplicatePolicyType} constant when this Lettuce version knows the value,
     *         otherwise a {@link TsDuplicatePolicy} that keeps the server's name; {@code null} if the series has no policy of
     *         its own.
     */
    public TsDuplicatePolicy getDuplicatePolicy() {
        return duplicatePolicy;
    }

    /**
     * Get the labels of the series, in the order the server reported them.
     *
     * @return the labels, never {@code null}; empty when the series has no labels.
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Get the key of the series this series is a compaction of.
     *
     * @return the source key, or {@code null} if this series is not the destination of a compaction rule.
     */
    public K getSourceKey() {
        return sourceKey;
    }

    /**
     * Get the compaction rules whose source is this series.
     *
     * @return the compaction rules, never {@code null}.
     */
    public List<Rule<K>> getRules() {
        return rules;
    }

    /**
     * Get the maximum time difference under which a new sample is ignored as a duplicate ({@code IGNORE}).
     *
     * @return the maximum time difference in milliseconds, or {@code null} if the server did not report it.
     */
    public Long getIgnoreMaxTimeDiff() {
        return ignoreMaxTimeDiff;
    }

    /**
     * Get the maximum value difference under which a new sample is ignored as a duplicate ({@code IGNORE}).
     *
     * @return the maximum value difference, or {@code null} if the server did not report it.
     */
    public Double getIgnoreMaxValDiff() {
        return ignoreMaxValDiff;
    }

    /**
     * Get the key of the series as stored by the server. Only populated when the information was requested with {@code DEBUG}.
     *
     * @return the key, or {@code null} if the information was not requested with {@code DEBUG}.
     */
    public K getKeySelfName() {
        return keySelfName;
    }

    /**
     * Get the data chunks of the series. Only populated when the information was requested with {@code DEBUG}.
     *
     * @return the chunks, or {@code null} if the information was not requested with {@code DEBUG}.
     */
    public List<Chunk> getChunks() {
        return chunks;
    }

    /**
     * Get every field the server reported, including fields this Lettuce version does not model. Field names are the server's
     * names. Values are {@link String}, {@link Long}, {@link Double}, {@link Boolean}, {@link List}, {@link Map}, {@code Set}
     * or {@code null}, in the shape the server sent them, so the values can differ between RESP2 and RESP3.
     *
     * @return the fields by name, never {@code null}.
     */
    public Map<String, Object> getRawInfo() {
        return rawInfo;
    }

    /**
     * Compare the typed fields of this value with another one. {@link #getRawInfo()} is not compared, because its shape depends
     * on the protocol.
     *
     * @param o the object to compare with.
     * @return {@code true} if all typed fields are equal.
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsInfoValue)) {
            return false;
        }

        TsInfoValue<?> that = (TsInfoValue<?>) o;
        return totalSamples == that.totalSamples && memoryUsage == that.memoryUsage && firstTimestamp == that.firstTimestamp
                && lastTimestamp == that.lastTimestamp && retentionTime == that.retentionTime && chunkCount == that.chunkCount
                && chunkSize == that.chunkSize && Objects.equals(chunkType, that.chunkType)
                && Objects.equals(duplicatePolicy, that.duplicatePolicy) && Objects.equals(labels, that.labels)
                && Objects.equals(sourceKey, that.sourceKey) && Objects.equals(rules, that.rules)
                && Objects.equals(ignoreMaxTimeDiff, that.ignoreMaxTimeDiff)
                && Objects.equals(ignoreMaxValDiff, that.ignoreMaxValDiff) && Objects.equals(keySelfName, that.keySelfName)
                && Objects.equals(chunks, that.chunks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalSamples, memoryUsage, firstTimestamp, lastTimestamp, retentionTime, chunkCount, chunkSize,
                chunkType, duplicatePolicy, labels, sourceKey, rules, ignoreMaxTimeDiff, ignoreMaxValDiff, keySelfName, chunks);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [totalSamples=" + totalSamples + ", memoryUsage=" + memoryUsage
                + ", firstTimestamp=" + firstTimestamp + ", lastTimestamp=" + lastTimestamp + ", retentionTime=" + retentionTime
                + ", chunkCount=" + chunkCount + ", chunkSize=" + chunkSize + ", chunkType=" + chunkType + ", duplicatePolicy="
                + duplicatePolicy + ", labels=" + labels + ", sourceKey=" + sourceKey + ", rules=" + rules
                + ", ignoreMaxTimeDiff=" + ignoreMaxTimeDiff + ", ignoreMaxValDiff=" + ignoreMaxValDiff + ", keySelfName="
                + keySelfName + ", chunks=" + chunks + "]";
    }

    /**
     * Compaction rule of a TimeSeries, as reported by TS.INFO.
     *
     * @param <K> Key type.
     * @since 8.0
     */
    public static class Rule<K> {

        private final K destinationKey;

        private final long bucketDuration;

        private final TsAggregation aggregation;

        private final long alignment;

        Rule(K destinationKey, long bucketDuration, TsAggregation aggregation, long alignment) {
            this.destinationKey = destinationKey;
            this.bucketDuration = bucketDuration;
            this.aggregation = aggregation;
            this.alignment = alignment;
        }

        /**
         * Get the key of the series that receives the compacted samples.
         *
         * @return the destination key.
         */
        public K getDestinationKey() {
            return destinationKey;
        }

        /**
         * Get the duration of each compaction bucket.
         *
         * @return the bucket duration in milliseconds.
         */
        public long getBucketDuration() {
            return bucketDuration;
        }

        /**
         * Get the aggregation of the rule.
         *
         * @return the aggregation: a {@link TsAggregationType} constant when this Lettuce version knows the value, otherwise a
         *         {@link TsAggregation} that keeps the server's name.
         */
        public TsAggregation getAggregation() {
            return aggregation;
        }

        /**
         * Get the timestamp the compaction buckets are aligned to.
         *
         * @return the alignment timestamp in milliseconds.
         */
        public long getAlignment() {
            return alignment;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof Rule)) {
                return false;
            }

            Rule<?> that = (Rule<?>) o;
            return bucketDuration == that.bucketDuration && alignment == that.alignment
                    && Objects.equals(destinationKey, that.destinationKey) && Objects.equals(aggregation, that.aggregation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(destinationKey, bucketDuration, aggregation, alignment);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [destinationKey=" + destinationKey + ", bucketDuration=" + bucketDuration
                    + ", aggregation=" + aggregation + ", alignment=" + alignment + "]";
        }

    }

    /**
     * Data chunk of a TimeSeries, as reported by TS.INFO with {@code DEBUG}.
     *
     * @since 8.0
     */
    public static class Chunk {

        private final long startTimestamp;

        private final long endTimestamp;

        private final long samples;

        private final long size;

        private final double bytesPerSample;

        Chunk(long startTimestamp, long endTimestamp, long samples, long size, double bytesPerSample) {
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
            this.samples = samples;
            this.size = size;
            this.bytesPerSample = bytesPerSample;
        }

        /**
         * Get the timestamp of the first sample in the chunk.
         *
         * @return the start timestamp in milliseconds.
         */
        public long getStartTimestamp() {
            return startTimestamp;
        }

        /**
         * Get the timestamp of the last sample in the chunk.
         *
         * @return the end timestamp in milliseconds.
         */
        public long getEndTimestamp() {
            return endTimestamp;
        }

        /**
         * Get the number of samples in the chunk.
         *
         * @return the number of samples.
         */
        public long getSamples() {
            return samples;
        }

        /**
         * Get the size of the chunk's data, without overhead.
         *
         * @return the data size in bytes.
         */
        public long getSize() {
            return size;
        }

        /**
         * Get the average number of bytes per sample in the chunk.
         *
         * @return the bytes per sample.
         */
        public double getBytesPerSample() {
            return bytesPerSample;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof Chunk)) {
                return false;
            }

            Chunk that = (Chunk) o;
            return startTimestamp == that.startTimestamp && endTimestamp == that.endTimestamp && samples == that.samples
                    && size == that.size && Double.compare(bytesPerSample, that.bytesPerSample) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startTimestamp, endTimestamp, samples, size, bytesPerSample);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [startTimestamp=" + startTimestamp + ", endTimestamp=" + endTimestamp
                    + ", samples=" + samples + ", size=" + size + ", bytesPerSample=" + bytesPerSample + "]";
        }

    }

}
