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

    private final List<Integer> selectedSlots;

    private final Long sampledCommandSelectedSlotsMs;

    private final Long allCommandsSelectedSlotsMs;

    private final Long allCommandsAllSlotsMs;

    private final Long netBytesSampledCommandsSelectedSlots;

    private final Long netBytesAllCommandsSelectedSlots;

    private final Long netBytesAllCommandsAllSlots;

    private final Long collectionStartTimeUnixMs;

    private final Long collectionDurationMs;

    private final Long totalCpuTimeUserMs;

    private final Long totalCpuTimeSysMs;

    private final Long totalNetBytes;

    private final Map<String, Long> byCpuTime;

    private final Map<String, Long> byNetBytes;

    /**
     * Creates a new {@link HotkeysReply}.
     *
     * @param trackingActive whether tracking is currently active.
     * @param sampleRatio the sampling ratio.
     * @param selectedSlots the selected slots (empty if all slots).
     * @param sampledCommandSelectedSlotsMs CPU time for sampled commands on selected slots in milliseconds (nullable).
     * @param allCommandsSelectedSlotsMs CPU time for all commands on selected slots in milliseconds (nullable).
     * @param allCommandsAllSlotsMs CPU time for all commands on all slots in milliseconds.
     * @param netBytesSampledCommandsSelectedSlots network bytes for sampled commands on selected slots (nullable).
     * @param netBytesAllCommandsSelectedSlots network bytes for all commands on selected slots (nullable).
     * @param netBytesAllCommandsAllSlots network bytes for all commands on all slots.
     * @param collectionStartTimeUnixMs collection start time in Unix milliseconds.
     * @param collectionDurationMs collection duration in milliseconds.
     * @param totalCpuTimeUserMs total CPU user time in milliseconds.
     * @param totalCpuTimeSysMs total CPU system time in milliseconds.
     * @param totalNetBytes total network bytes.
     * @param byCpuTime top K keys by CPU time.
     * @param byNetBytes top K keys by network bytes (key -> bytes).
     */
    public HotkeysReply(boolean trackingActive, int sampleRatio, List<Integer> selectedSlots,
            Long sampledCommandSelectedSlotsMs, Long allCommandsSelectedSlotsMs, Long allCommandsAllSlotsMs,
            Long netBytesSampledCommandsSelectedSlots, Long netBytesAllCommandsSelectedSlots, Long netBytesAllCommandsAllSlots,
            Long collectionStartTimeUnixMs, Long collectionDurationMs, Long totalCpuTimeUserMs, Long totalCpuTimeSysMs,
            Long totalNetBytes, Map<String, Long> byCpuTime, Map<String, Long> byNetBytes) {

        this.trackingActive = trackingActive;
        this.sampleRatio = sampleRatio;
        this.selectedSlots = selectedSlots != null ? Collections.unmodifiableList(selectedSlots) : Collections.emptyList();
        this.sampledCommandSelectedSlotsMs = sampledCommandSelectedSlotsMs;
        this.allCommandsSelectedSlotsMs = allCommandsSelectedSlotsMs;
        this.allCommandsAllSlotsMs = allCommandsAllSlotsMs;
        this.netBytesSampledCommandsSelectedSlots = netBytesSampledCommandsSelectedSlots;
        this.netBytesAllCommandsSelectedSlots = netBytesAllCommandsSelectedSlots;
        this.netBytesAllCommandsAllSlots = netBytesAllCommandsAllSlots;
        this.collectionStartTimeUnixMs = collectionStartTimeUnixMs;
        this.collectionDurationMs = collectionDurationMs;
        this.totalCpuTimeUserMs = totalCpuTimeUserMs;
        this.totalCpuTimeSysMs = totalCpuTimeSysMs;
        this.totalNetBytes = totalNetBytes;
        this.byCpuTime = byCpuTime != null ? Collections.unmodifiableMap(byCpuTime) : Collections.emptyMap();
        this.byNetBytes = byNetBytes != null ? Collections.unmodifiableMap(byNetBytes) : Collections.emptyMap();
    }

    public boolean isTrackingActive() {
        return trackingActive;
    }

    public int getSampleRatio() {
        return sampleRatio;
    }

    public List<Integer> getSelectedSlots() {
        return selectedSlots;
    }

    public Long getSampledCommandSelectedSlotsMs() {
        return sampledCommandSelectedSlotsMs;
    }

    public Long getAllCommandsSelectedSlotsMs() {
        return allCommandsSelectedSlotsMs;
    }

    public Long getAllCommandsAllSlotsMs() {
        return allCommandsAllSlotsMs;
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

    public Map<String, Long> getByCpuTime() {
        return byCpuTime;
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
                && Objects.equals(sampledCommandSelectedSlotsMs, that.sampledCommandSelectedSlotsMs)
                && Objects.equals(allCommandsSelectedSlotsMs, that.allCommandsSelectedSlotsMs)
                && Objects.equals(allCommandsAllSlotsMs, that.allCommandsAllSlotsMs)
                && Objects.equals(netBytesSampledCommandsSelectedSlots, that.netBytesSampledCommandsSelectedSlots)
                && Objects.equals(netBytesAllCommandsSelectedSlots, that.netBytesAllCommandsSelectedSlots)
                && Objects.equals(netBytesAllCommandsAllSlots, that.netBytesAllCommandsAllSlots)
                && Objects.equals(collectionStartTimeUnixMs, that.collectionStartTimeUnixMs)
                && Objects.equals(collectionDurationMs, that.collectionDurationMs)
                && Objects.equals(totalCpuTimeUserMs, that.totalCpuTimeUserMs)
                && Objects.equals(totalCpuTimeSysMs, that.totalCpuTimeSysMs)
                && Objects.equals(totalNetBytes, that.totalNetBytes) && Objects.equals(byCpuTime, that.byCpuTime)
                && Objects.equals(byNetBytes, that.byNetBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackingActive, sampleRatio, selectedSlots, sampledCommandSelectedSlotsMs,
                allCommandsSelectedSlotsMs, allCommandsAllSlotsMs, netBytesSampledCommandsSelectedSlots,
                netBytesAllCommandsSelectedSlots, netBytesAllCommandsAllSlots, collectionStartTimeUnixMs, collectionDurationMs,
                totalCpuTimeUserMs, totalCpuTimeSysMs, totalNetBytes, byCpuTime, byNetBytes);
    }

    @Override
    public String toString() {
        return "HotkeysReply{" + "trackingActive=" + trackingActive + ", sampleRatio=" + sampleRatio + ", selectedSlots="
                + selectedSlots + ", sampledCommandSelectedSlotsMs=" + sampledCommandSelectedSlotsMs
                + ", allCommandsSelectedSlotsMs=" + allCommandsSelectedSlotsMs + ", allCommandsAllSlotsMs="
                + allCommandsAllSlotsMs + ", netBytesSampledCommandsSelectedSlots=" + netBytesSampledCommandsSelectedSlots
                + ", netBytesAllCommandsSelectedSlots=" + netBytesAllCommandsSelectedSlots + ", netBytesAllCommandsAllSlots="
                + netBytesAllCommandsAllSlots + ", collectionStartTimeUnixMs=" + collectionStartTimeUnixMs
                + ", collectionDurationMs=" + collectionDurationMs + ", totalCpuTimeUserMs=" + totalCpuTimeUserMs
                + ", totalCpuTimeSysMs=" + totalCpuTimeSysMs + ", totalNetBytes=" + totalNetBytes + ", byCpuTime=" + byCpuTime
                + ", byNetBytes=" + byNetBytes + '}';
    }

}
