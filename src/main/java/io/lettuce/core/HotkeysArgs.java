/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/hotkeys">HOTKEYS START</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code metrics(Metric.CPU, Metric.NET).count(20)}.
 * <p>
 * {@link HotkeysArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 7.4
 */
@Experimental
public class HotkeysArgs implements CompositeArgument {

    /**
     * Metrics to track for hotkeys.
     */
    public enum Metric {

        CPU(CommandKeyword.CPU), NET(CommandKeyword.NET);

        private final CommandKeyword keyword;

        Metric(CommandKeyword keyword) {
            this.keyword = keyword;
        }

        /**
         * Returns the protocol keyword for this metric.
         *
         * @return the {@link CommandKeyword} for this metric.
         */
        public CommandKeyword getKeyword() {
            return keyword;
        }

    }

    /**
     * Minimum value for COUNT option.
     */
    public static final int COUNT_MIN = 10;

    /**
     * Maximum value for COUNT option.
     */
    public static final int COUNT_MAX = 64;

    /**
     * Minimum value for DURATION option (seconds). 0 means no auto-stop.
     */
    public static final long DURATION_MIN = 0;

    /**
     * Minimum value for SAMPLE option.
     */
    public static final int SAMPLE_MIN = 1;

    /**
     * Minimum slot number (inclusive).
     */
    public static final int SLOT_MIN = 0;

    /**
     * Maximum slot number (inclusive).
     */
    public static final int SLOT_MAX = 16383;

    /**
     * Maximum number of slots that can be specified.
     */
    public static final int SLOTS_MAX_COUNT = 16384;

    private Set<Metric> metrics;

    private Integer count;

    private Long duration;

    private Integer sample;

    private List<Integer> slots;

    /**
     * Builder entry points for {@link HotkeysArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link HotkeysArgs} with {@literal METRICS} set.
         *
         * @param metrics the metrics to track.
         * @return new {@link HotkeysArgs} with {@literal METRICS} set.
         * @see HotkeysArgs#metrics(Metric...)
         */
        public static HotkeysArgs metrics(Metric... metrics) {
            return new HotkeysArgs().metrics(metrics);
        }

        /**
         * Creates new {@link HotkeysArgs} with {@literal COUNT} set.
         *
         * @param count number of top keys to report (min: {@value #COUNT_MIN}, max: {@value #COUNT_MAX}).
         * @return new {@link HotkeysArgs} with {@literal COUNT} set.
         * @see HotkeysArgs#count(int)
         */
        public static HotkeysArgs count(int count) {
            return new HotkeysArgs().count(count);
        }

        /**
         * Creates new {@link HotkeysArgs} with {@literal DURATION} set.
         *
         * @param duration auto-stop duration in seconds (min: {@value #DURATION_MIN}, 0 = no auto-stop).
         * @return new {@link HotkeysArgs} with {@literal DURATION} set.
         * @see HotkeysArgs#duration(long)
         */
        public static HotkeysArgs duration(long duration) {
            return new HotkeysArgs().duration(duration);
        }

        /**
         * Creates new {@link HotkeysArgs} with {@literal DURATION} set.
         *
         * @param duration auto-stop duration (min: {@value #DURATION_MIN}s).
         * @return new {@link HotkeysArgs} with {@literal DURATION} set.
         * @see HotkeysArgs#duration(Duration)
         */
        public static HotkeysArgs duration(Duration duration) {
            return new HotkeysArgs().duration(duration);
        }

        /**
         * Creates new {@link HotkeysArgs} with {@literal SAMPLE} set.
         *
         * @param ratio sampling ratio (min: {@value #SAMPLE_MIN}).
         * @return new {@link HotkeysArgs} with {@literal SAMPLE} set.
         * @see HotkeysArgs#sample(int)
         */
        public static HotkeysArgs sample(int ratio) {
            return new HotkeysArgs().sample(ratio);
        }

        /**
         * Creates new {@link HotkeysArgs} with {@literal SLOTS} set.
         * <p>
         * Specifies which hash slots to track for hotkeys collection (cluster mode only). In the response, Redis groups
         * consecutive slots into ranges. For example, if you specify slots 0, 1, 2, and 100, the response will contain
         * {@code [[0, 2], [100]]} - consecutive slots 0-2 are grouped, while slot 100 remains separate.
         *
         * @param slots the slot numbers to track (each: {@value #SLOT_MIN}-{@value #SLOT_MAX}, max count:
         *        {@value #SLOTS_MAX_COUNT}).
         * @return new {@link HotkeysArgs} with {@literal SLOTS} set.
         * @see HotkeysArgs#slots(int...)
         * @see io.lettuce.core.HotkeysReply#getSelectedSlots()
         */
        public static HotkeysArgs slots(int... slots) {
            return new HotkeysArgs().slots(slots);
        }

    }

    /**
     * Set the metrics to track. At least one metric must be specified, maximum two (CPU and NET).
     *
     * @param metrics the metrics to track (CPU and/or NET).
     * @return {@code this} {@link HotkeysArgs}.
     */
    public HotkeysArgs metrics(Metric... metrics) {

        LettuceAssert.notNull(metrics, "Metrics must not be null");
        LettuceAssert.isTrue(metrics.length >= 1 && metrics.length <= 2, "Metrics count must be between 1 and 2");

        this.metrics = EnumSet.noneOf(Metric.class);
        for (Metric metric : metrics) {
            this.metrics.add(metric);
        }
        return this;
    }

    /**
     * Set the number of top keys to report.
     *
     * @param count number of keys (min: {@value #COUNT_MIN}, max: {@value #COUNT_MAX}).
     * @return {@code this} {@link HotkeysArgs}.
     */
    public HotkeysArgs count(int count) {

        LettuceAssert.isTrue(count >= COUNT_MIN && count <= COUNT_MAX,
                "Count must be between " + COUNT_MIN + " and " + COUNT_MAX);

        this.count = count;
        return this;
    }

    /**
     * Set the auto-stop duration in seconds. Use 0 to disable auto-stop.
     *
     * @param duration duration in seconds (min: {@value #DURATION_MIN}, 0 = no auto-stop).
     * @return {@code this} {@link HotkeysArgs}.
     */
    public HotkeysArgs duration(long duration) {

        LettuceAssert.isTrue(duration >= DURATION_MIN, "Duration must be at least " + DURATION_MIN);

        this.duration = duration;
        return this;
    }

    /**
     * Set the auto-stop duration.
     *
     * @param duration duration (min: {@value #DURATION_MIN}s).
     * @return {@code this} {@link HotkeysArgs}.
     */
    public HotkeysArgs duration(Duration duration) {

        LettuceAssert.notNull(duration, "Duration must not be null");

        return duration(duration.getSeconds());
    }

    /**
     * Set the sampling ratio.
     *
     * @param ratio sampling ratio (min: {@value #SAMPLE_MIN}, 1 = track every key, higher values = sample less frequently).
     * @return {@code this} {@link HotkeysArgs}.
     */
    public HotkeysArgs sample(int ratio) {

        LettuceAssert.isTrue(ratio >= SAMPLE_MIN, "Sample ratio must be at least " + SAMPLE_MIN);

        this.sample = ratio;
        return this;
    }

    /**
     * Set the slot numbers to track (cluster mode only). Each slot must be between {@value #SLOT_MIN} and {@value #SLOT_MAX}
     * inclusive. Maximum {@value #SLOTS_MAX_COUNT} slots can be specified.
     * <p>
     * In the response, Redis groups consecutive slots into ranges. For example, if you specify slots 0, 1, 2, and 100, the
     * response's {@link HotkeysReply#getSelectedSlots()} will contain two entries: a range {@code [0, 2]} for the consecutive
     * slots and {@code [100, 100]} for the single slot.
     *
     * @param slots the slot numbers.
     * @return {@code this} {@link HotkeysArgs}.
     * @see HotkeysReply#getSelectedSlots()
     */
    public HotkeysArgs slots(int... slots) {

        LettuceAssert.notNull(slots, "Slots must not be null");
        LettuceAssert.isTrue(slots.length >= 1 && slots.length <= SLOTS_MAX_COUNT,
                "Slots count must be between 1 and " + SLOTS_MAX_COUNT);

        this.slots = new ArrayList<>(slots.length);
        for (int slot : slots) {
            LettuceAssert.isTrue(slot >= SLOT_MIN && slot <= SLOT_MAX, "Slot must be between " + SLOT_MIN + " and " + SLOT_MAX);
            this.slots.add(slot);
        }
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (metrics != null && !metrics.isEmpty()) {
            args.add(CommandKeyword.METRICS).add(metrics.size());
            for (Metric metric : metrics) {
                args.add(metric.getKeyword());
            }
        }

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }

        if (duration != null) {
            args.add(CommandKeyword.DURATION).add(duration);
        }

        if (sample != null) {
            args.add(CommandKeyword.SAMPLE).add(sample);
        }

        if (slots != null && !slots.isEmpty()) {
            args.add(CommandKeyword.SLOTS).add(slots.size());
            for (Integer slot : slots) {
                args.add(slot);
            }
        }
    }

}
