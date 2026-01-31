/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link HotkeysArgs}.
 *
 * @author Aleksandar Todorov
 */
@Tag(UNIT_TEST)
class HotkeysArgsUnitTests {

    @Test
    void shouldBuildWithMetricsCpu() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU).build(args);

        assertThat(args.toCommandString()).isEqualTo("METRICS 1 CPU");
    }

    @Test
    void shouldBuildWithMetricsNet() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.NET).build(args);

        assertThat(args.toCommandString()).isEqualTo("METRICS 1 NET");
    }

    @Test
    void shouldBuildWithMetricsBoth() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.metrics(HotkeysArgs.Metric.CPU, HotkeysArgs.Metric.NET).build(args);

        assertThat(args.toCommandString()).contains("METRICS 2");
        assertThat(args.toCommandString()).contains("CPU");
        assertThat(args.toCommandString()).contains("NET");
    }

    @Test
    void shouldRejectEmptyMetrics() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.metrics()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Metrics count must be between 1 and 2");
    }

    @Test
    void shouldBuildWithCount() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.count(20).build(args);

        assertThat(args.toCommandString()).isEqualTo("COUNT 20");
    }

    @Test
    void shouldBuildWithCountMin() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.count(HotkeysArgs.COUNT_MIN).build(args);

        assertThat(args.toCommandString()).isEqualTo("COUNT 10");
    }

    @Test
    void shouldBuildWithCountMax() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.count(HotkeysArgs.COUNT_MAX).build(args);

        assertThat(args.toCommandString()).isEqualTo("COUNT 64");
    }

    @Test
    void shouldRejectCountBelowMin() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.count(9)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Count must be between 10 and 64");
    }

    @Test
    void shouldRejectCountAboveMax() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.count(65)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Count must be between 10 and 64");
    }

    @Test
    void shouldBuildWithDuration() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.duration(60).build(args);

        assertThat(args.toCommandString()).isEqualTo("DURATION 60");
    }

    @Test
    void shouldBuildWithDurationZero() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.duration(0).build(args);

        assertThat(args.toCommandString()).isEqualTo("DURATION 0");
    }

    @Test
    void shouldBuildWithDurationObject() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.duration(Duration.ofMinutes(5)).build(args);

        assertThat(args.toCommandString()).isEqualTo("DURATION 300");
    }

    @Test
    void shouldRejectNegativeDuration() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.duration(-1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must be at least 0");
    }

    @Test
    void shouldBuildWithSample() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.sample(10).build(args);

        assertThat(args.toCommandString()).isEqualTo("SAMPLE 10");
    }

    @Test
    void shouldBuildWithSampleMin() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.sample(HotkeysArgs.SAMPLE_MIN).build(args);

        assertThat(args.toCommandString()).isEqualTo("SAMPLE 1");
    }

    @Test
    void shouldRejectSampleBelowMin() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.sample(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sample ratio must be at least 1");
    }

    @Test
    void shouldBuildWithSlots() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.slots(0, 100, 16383).build(args);

        assertThat(args.toCommandString()).isEqualTo("SLOTS 3 0 100 16383");
    }

    @Test
    void shouldBuildWithSingleSlot() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        HotkeysArgs.Builder.slots(0).build(args);

        assertThat(args.toCommandString()).isEqualTo("SLOTS 1 0");
    }

    @Test
    void shouldRejectEmptySlots() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.slots()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Slots count must be between 1 and 16384");
    }

    @Test
    void shouldRejectSlotBelowMin() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.slots(-1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Slot must be between 0 and 16383");
    }

    @Test
    void shouldRejectSlotAboveMax() {
        assertThatThrownBy(() -> HotkeysArgs.Builder.slots(16384)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Slot must be between 0 and 16383");
    }

    @Test
    void shouldBuildWithAllOptions() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        new HotkeysArgs().metrics(HotkeysArgs.Metric.CPU, HotkeysArgs.Metric.NET).count(20).duration(60).sample(5).slots(0, 100)
                .build(args);

        String commandString = args.toCommandString();
        assertThat(commandString).contains("METRICS 2");
        assertThat(commandString).contains("CPU");
        assertThat(commandString).contains("NET");
        assertThat(commandString).contains("COUNT 20");
        assertThat(commandString).contains("DURATION 60");
        assertThat(commandString).contains("SAMPLE 5");
        assertThat(commandString).contains("SLOTS 2 0 100");
    }

    @Test
    void shouldBuildEmptyArgs() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        new HotkeysArgs().build(args);

        assertThat(args.toCommandString()).isEmpty();
    }

    @Test
    void shouldChainMethods() {
        HotkeysArgs hotkeysArgs = new HotkeysArgs().metrics(HotkeysArgs.Metric.CPU).count(15).duration(30).sample(2);

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        hotkeysArgs.build(args);

        assertThat(args.toCommandString()).contains("METRICS 1 CPU");
        assertThat(args.toCommandString()).contains("COUNT 15");
        assertThat(args.toCommandString()).contains("DURATION 30");
        assertThat(args.toCommandString()).contains("SAMPLE 2");
    }

}
