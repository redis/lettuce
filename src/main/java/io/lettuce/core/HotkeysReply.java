/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reply from the Redis <a href="https://redis.io/commands/hotkeys">HOTKEYS GET</a> command. Contains tracking session
 * information and top K hotkeys by CPU time and network bytes.
 *
 * @author Aleksandar Todorov
 * @since 7.3
 */
public class HotkeysReply {

    private final boolean trackingActive;

    private final int sampleRatio;

    private final List<Range<Integer>> selectedSlots;

    private final Long sampledCommandSelectedSlotsUs;

    private final Long allCommandsSelectedSlotsUs;

    private final Long allCommandsAllSlotsUs;

    private final Long netBytesSampledCommandsSelectedSlots;

    private final Long netBytesAllCommandsSelectedSlots;

    private final Long netBytesAllCommandsAllSlots;

    private final Long collectionStartTimeUnixMs;

    private final Long collectionDurationMs;

    private final Long totalCpuTimeUserMs;

    private final Long totalCpuTimeSysMs;

    private final Long totalNetBytes;

    private final Map<String, Long> byCpuTimeUs;

    private final Map<String, Long> byNetBytes;

    /**
     * Creates a new {@link HotkeysReply}.
     *
     * @param trackingActive whether tracking is currently active.
     * @param sampleRatio the sampling ratio.
     * @param selectedSlots the selected slot ranges (empty if all slots).
     * @param sampledCommandSelectedSlotsUs CPU time for sampled commands on selected slots in microseconds (nullable).
     * @param allCommandsSelectedSlotsUs CPU time for all commands on selected slots in microseconds (nullable).
     * @param allCommandsAllSlotsUs CPU time for all commands on all slots in microseconds.
     * @param netBytesSampledCommandsSelectedSlots network bytes for sampled commands on selected slots (nullable).
     * @param netBytesAllCommandsSelectedSlots network bytes for all commands on selected slots (nullable).
     * @param netBytesAllCommandsAllSlots network bytes for all commands on all slots.
     * @param collectionStartTimeUnixMs collection start time in Unix milliseconds.
     * @param collectionDurationMs collection duration in milliseconds.
     * @param totalCpuTimeUserMs total CPU user time in milliseconds.
     * @param totalCpuTimeSysMs total CPU system time in milliseconds.
     * @param totalNetBytes total network bytes.
     * @param byCpuTimeUs top K keys by CPU time in microseconds (key -> microseconds).
     * @param byNetBytes top K keys by network bytes (key -> bytes).
     */
    public HotkeysReply(boolean trackingActive, int sampleRatio, List<Range<Integer>> selectedSlots,
            Long sampledCommandSelectedSlotsUs, Long allCommandsSelectedSlotsUs, Long allCommandsAllSlotsUs,
            Long netBytesSampledCommandsSelectedSlots, Long netBytesAllCommandsSelectedSlots, Long netBytesAllCommandsAllSlots,
            Long collectionStartTimeUnixMs, Long collectionDurationMs, Long totalCpuTimeUserMs, Long totalCpuTimeSysMs,
            Long totalNetBytes, Map<String, Long> byCpuTimeUs, Map<String, Long> byNetBytes) {

        this.trackingActive = trackingActive;
        this.sampleRatio = sampleRatio;
        this.selectedSlots = selectedSlots != null ? Collections.unmodifiableList(selectedSlots) : Collections.emptyList();
        this.sampledCommandSelectedSlotsUs = sampledCommandSelectedSlotsUs;
        this.allCommandsSelectedSlotsUs = allCommandsSelectedSlotsUs;
        this.allCommandsAllSlotsUs = allCommandsAllSlotsUs;
        this.netBytesSampledCommandsSelectedSlots = netBytesSampledCommandsSelectedSlots;
        this.netBytesAllCommandsSelectedSlots = netBytesAllCommandsSelectedSlots;
        this.netBytesAllCommandsAllSlots = netBytesAllCommandsAllSlots;
        this.collectionStartTimeUnixMs = collectionStartTimeUnixMs;
        this.collectionDurationMs = collectionDurationMs;
        this.totalCpuTimeUserMs = totalCpuTimeUserMs;
        this.totalCpuTimeSysMs = totalCpuTimeSysMs;
        this.totalNetBytes = totalNetBytes;
        this.byCpuTimeUs = byCpuTimeUs != null ? Collections.unmodifiableMap(byCpuTimeUs) : Collections.emptyMap();
        this.byNetBytes = byNetBytes != null ? Collections.unmodifiableMap(byNetBytes) : Collections.emptyMap();
    }

    public boolean isTrackingActive() {
        return trackingActive;
    }

    public int getSampleRatio() {
        return sampleRatio;
    }

    public List<Range<Integer>> getSelectedSlots() {
        return selectedSlots;
    }

    public Long getSampledCommandSelectedSlotsUs() {
        return sampledCommandSelectedSlotsUs;
    }

    public Long getAllCommandsSelectedSlotsUs() {
        return allCommandsSelectedSlotsUs;
    }

    public Long getAllCommandsAllSlotsUs() {
        return allCommandsAllSlotsUs;
    }

    public Long getNetBytesSampledCommandsSelectedSlots() {
        return netBytesSampledCommandsSelectedSlots;
    }

    public Long getNetBytesAllCommandsSelectedSlots() {
        return netBytesAllCommandsSelectedSlots;
    }

    public Long getNetBytesAllCommandsAllSlots() {
        return netBytesAllCommandsAllSlots;
    }

    public Long getCollectionStartTimeUnixMs() {
        return collectionStartTimeUnixMs;
    }

    public Long getCollectionDurationMs() {
        return collectionDurationMs;
    }

    public Long getTotalCpuTimeUserMs() {
        return totalCpuTimeUserMs;
    }

    public Long getTotalCpuTimeSysMs() {
        return totalCpuTimeSysMs;
    }

    public Long getTotalNetBytes() {
        return totalNetBytes;
    }

    public Map<String, Long> getByCpuTimeUs() {
        return byCpuTimeUs;
    }

    public Map<String, Long> getByNetBytes() {
        return byNetBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HotkeysReply)) {
            return false;
        }
        HotkeysReply that = (HotkeysReply) o;
        return trackingActive == that.trackingActive && sampleRatio == that.sampleRatio
                && Objects.equals(selectedSlots, that.selectedSlots)
                && Objects.equals(sampledCommandSelectedSlotsUs, that.sampledCommandSelectedSlotsUs)
                && Objects.equals(allCommandsSelectedSlotsUs, that.allCommandsSelectedSlotsUs)
                && Objects.equals(allCommandsAllSlotsUs, that.allCommandsAllSlotsUs)
                && Objects.equals(netBytesSampledCommandsSelectedSlots, that.netBytesSampledCommandsSelectedSlots)
                && Objects.equals(netBytesAllCommandsSelectedSlots, that.netBytesAllCommandsSelectedSlots)
                && Objects.equals(netBytesAllCommandsAllSlots, that.netBytesAllCommandsAllSlots)
                && Objects.equals(collectionStartTimeUnixMs, that.collectionStartTimeUnixMs)
                && Objects.equals(collectionDurationMs, that.collectionDurationMs)
                && Objects.equals(totalCpuTimeUserMs, that.totalCpuTimeUserMs)
                && Objects.equals(totalCpuTimeSysMs, that.totalCpuTimeSysMs)
                && Objects.equals(totalNetBytes, that.totalNetBytes) && Objects.equals(byCpuTimeUs, that.byCpuTimeUs)
                && Objects.equals(byNetBytes, that.byNetBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackingActive, sampleRatio, selectedSlots, sampledCommandSelectedSlotsUs,
                allCommandsSelectedSlotsUs, allCommandsAllSlotsUs, netBytesSampledCommandsSelectedSlots,
                netBytesAllCommandsSelectedSlots, netBytesAllCommandsAllSlots, collectionStartTimeUnixMs, collectionDurationMs,
                totalCpuTimeUserMs, totalCpuTimeSysMs, totalNetBytes, byCpuTimeUs, byNetBytes);
    }

    @Override
    public String toString() {
        return "HotkeysReply{" + "trackingActive=" + trackingActive + ", sampleRatio=" + sampleRatio + ", selectedSlots="
                + selectedSlots + ", sampledCommandSelectedSlotsUs=" + sampledCommandSelectedSlotsUs
                + ", allCommandsSelectedSlotsUs=" + allCommandsSelectedSlotsUs + ", allCommandsAllSlotsUs="
                + allCommandsAllSlotsUs + ", netBytesSampledCommandsSelectedSlots=" + netBytesSampledCommandsSelectedSlots
                + ", netBytesAllCommandsSelectedSlots=" + netBytesAllCommandsSelectedSlots + ", netBytesAllCommandsAllSlots="
                + netBytesAllCommandsAllSlots + ", collectionStartTimeUnixMs=" + collectionStartTimeUnixMs
                + ", collectionDurationMs=" + collectionDurationMs + ", totalCpuTimeUserMs=" + totalCpuTimeUserMs
                + ", totalCpuTimeSysMs=" + totalCpuTimeSysMs + ", totalNetBytes=" + totalNetBytes + ", byCpuTimeUs="
                + byCpuTimeUs + ", byNetBytes=" + byNetBytes + '}';
    }

}
