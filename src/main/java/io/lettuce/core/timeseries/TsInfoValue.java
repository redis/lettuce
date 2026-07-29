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

/**
 * Represents the result of the Redis <a href="https://redis.io/commands/ts.info/">TS.INFO</a> command.
 *
 * @param <K> Key type
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsInfoValue<K> {

    private final Map<String, Object> rawInfo;

    private final Long totalSamples;

    private final Long memoryUsage;

    private final Long firstTimestamp;

    private final Long lastTimestamp;

    private final Long retentionTime;

    private final Long chunkCount;

    private final Long chunkSize;

    private final String chunkType;

    private final String duplicatePolicy;

    private final Map<String, String> labels;

    private final K sourceKey;

    private final List<Rule<K>> rules;

    private final Long ignoreMaxTimeDiff;

    private final Double ignoreMaxValDiff;

    private final K keySelfName;

    private final List<Chunk> chunks;

    @SuppressWarnings("unchecked")
    public TsInfoValue(Map<String, Object> rawInfo) {
        this.rawInfo = rawInfo;
        this.totalSamples = (Long) rawInfo.get("totalSamples");
        this.memoryUsage = (Long) rawInfo.get("memoryUsage");
        this.firstTimestamp = (Long) rawInfo.get("firstTimestamp");
        this.lastTimestamp = (Long) rawInfo.get("lastTimestamp");
        this.retentionTime = (Long) rawInfo.get("retentionTime");
        this.chunkCount = (Long) rawInfo.get("chunkCount");
        this.chunkSize = (Long) rawInfo.get("chunkSize");
        this.chunkType = (String) rawInfo.get("chunkType");
        this.duplicatePolicy = (String) rawInfo.get("duplicatePolicy");
        this.labels = (Map<String, String>) rawInfo.get("labels");
        this.sourceKey = (K) rawInfo.get("sourceKey");
        this.rules = (List<Rule<K>>) rawInfo.get("rules");
        this.ignoreMaxTimeDiff = (Long) rawInfo.get("ignoreMaxTimeDiff");
        this.ignoreMaxValDiff = (Double) rawInfo.get("ignoreMaxValDiff");
        this.keySelfName = (K) rawInfo.get("keySelfName");
        this.chunks = (List<Chunk>) rawInfo.get("Chunks");
    }

    /**
     * Returns the raw info map returned by the Redis server.
     *
     * @return the raw info map returned by the Redis server
     */
    public Map<String, Object> getRawInfo() {
        return rawInfo;
    }

    /**
     * Returns the total number of samples in the series.
     *
     * @return the total number of samples in the series
     */
    public Long getTotalSamples() {
        return totalSamples;
    }

    /**
     * Returns the total memory usage of the series in bytes.
     *
     * @return the total memory usage of the series in bytes
     */
    public Long getMemoryUsage() {
        return memoryUsage;
    }

    /**
     * Returns the first timestamp present in the series.
     *
     * @return the first timestamp present in the series
     */
    public Long getFirstTimestamp() {
        return firstTimestamp;
    }

    /**
     * Returns the last timestamp present in the series.
     *
     * @return the last timestamp present in the series
     */
    public Long getLastTimestamp() {
        return lastTimestamp;
    }

    /**
     * Returns the retention time, in milliseconds, for the series.
     *
     * @return the retention time, in milliseconds, for the series
     */
    public Long getRetentionTime() {
        return retentionTime;
    }

    /**
     * Returns the number of chunks used by the series.
     *
     * @return the number of chunks used by the series
     */
    public Long getChunkCount() {
        return chunkCount;
    }

    /**
     * Returns the initial chunk size, in bytes, used by the series.
     *
     * @return the initial chunk size, in bytes, used by the series
     */
    public Long getChunkSize() {
        return chunkSize;
    }

    /**
     * Returns the chunk encoding used by the series ({@code "compressed"} or {@code "uncompressed"}).
     *
     * @return the chunk encoding used by the series
     */
    public String getChunkType() {
        return chunkType;
    }

    /**
     * Returns the duplicate sample handling policy configured for the series, or {@code null} if none was configured.
     *
     * @return the duplicate sample handling policy configured for the series, or {@code null} if none was configured
     */
    public String getDuplicatePolicy() {
        return duplicatePolicy;
    }

    /**
     * Returns the labels associated with the series.
     *
     * @return the labels associated with the series, never {@code null}
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Returns the source key of the series, if it is the destination of a compaction rule, or {@code null} otherwise.
     *
     * @return the source key of the series, or {@code null} otherwise
     */
    public K getSourceKey() {
        return sourceKey;
    }

    /**
     * Returns the compaction rules attached to the series.
     *
     * @return the compaction rules attached to the series, never {@code null}
     */
    public List<Rule<K>> getRules() {
        return rules;
    }

    /**
     * Returns the maximum time difference, in milliseconds, allowed for out-of-order samples.
     *
     * @return the maximum time difference, in milliseconds, allowed for out-of-order samples
     */
    public Long getIgnoreMaxTimeDiff() {
        return ignoreMaxTimeDiff;
    }

    /**
     * Returns the maximum value difference allowed for out-of-order samples.
     *
     * @return the maximum value difference allowed for out-of-order samples
     */
    public Double getIgnoreMaxValDiff() {
        return ignoreMaxValDiff;
    }

    /**
     * Returns the key name of the series, as reported by {@code TS.INFO ... DEBUG}, or {@code null} if the reply was not
     * produced by the {@code DEBUG} modifier.
     *
     * @return the key name of the series, or {@code null} if not available
     */
    public K getKeySelfName() {
        return keySelfName;
    }

    /**
     * Returns the chunks of the series, as reported by {@code TS.INFO ... DEBUG}, or {@code null} if the reply was not produced
     * by the {@code DEBUG} modifier.
     *
     * @return the chunks of the series, or {@code null} if not available
     */
    public List<Chunk> getChunks() {
        return chunks;
    }

    /**
     * Represents a single compaction rule attached to a series, as reported by {@code TS.INFO}.
     *
     * @param <K> Key type
     * @author Gyumin Hwang
     * @since 7.7
     */
    public static class Rule<K> {

        private final K destKey;

        private final long bucketDuration;

        private final TsAggregationType aggregationType;

        private final long timestampAlignment;

        public Rule(K destKey, long bucketDuration, TsAggregationType aggregationType, long timestampAlignment) {
            this.destKey = destKey;
            this.bucketDuration = bucketDuration;
            this.aggregationType = aggregationType;
            this.timestampAlignment = timestampAlignment;
        }

        /**
         * Returns the destination key of the compaction rule.
         *
         * @return the destination key of the compaction rule
         */
        public K getDestKey() {
            return destKey;
        }

        /**
         * Returns the bucket duration, in milliseconds, of the compaction rule.
         *
         * @return the bucket duration, in milliseconds, of the compaction rule
         */
        public long getBucketDuration() {
            return bucketDuration;
        }

        /**
         * Returns the aggregation type of the compaction rule.
         *
         * @return the aggregation type of the compaction rule
         */
        public TsAggregationType getAggregationType() {
            return aggregationType;
        }

        /**
         * Returns the timestamp alignment of the compaction rule.
         *
         * @return the timestamp alignment of the compaction rule
         */
        public long getTimestampAlignment() {
            return timestampAlignment;
        }

    }

    /**
     * Represents a single chunk of a series, as reported by {@code TS.INFO ... DEBUG}.
     *
     * @author Gyumin Hwang
     * @since 7.7
     */
    public static class Chunk {

        private final long startTimestamp;

        private final long endTimestamp;

        private final long samples;

        private final long size;

        private final double bytesPerSample;

        public Chunk(long startTimestamp, long endTimestamp, long samples, long size, double bytesPerSample) {
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
            this.samples = samples;
            this.size = size;
            this.bytesPerSample = bytesPerSample;
        }

        /**
         * Returns the start timestamp of the chunk.
         *
         * @return the start timestamp of the chunk
         */
        public long getStartTimestamp() {
            return startTimestamp;
        }

        /**
         * Returns the end timestamp of the chunk.
         *
         * @return the end timestamp of the chunk
         */
        public long getEndTimestamp() {
            return endTimestamp;
        }

        /**
         * Returns the number of samples in the chunk.
         *
         * @return the number of samples in the chunk
         */
        public long getSamples() {
            return samples;
        }

        /**
         * Returns the size, in bytes, of the chunk.
         *
         * @return the size, in bytes, of the chunk
         */
        public long getSize() {
            return size;
        }

        /**
         * Returns the average bytes per sample of the chunk.
         *
         * @return the average bytes per sample of the chunk
         */
        public double getBytesPerSample() {
            return bytesPerSample;
        }

    }

}
