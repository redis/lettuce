/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsNRangeRow}: equality with {@code Double.compare} semantics (test plan §3 "Value equality"), the
 * per-key slices of {@code getValues(int)} (design §3.5, H17) and the validation of {@code of}.
 */
@Tag(UNIT_TEST)
class TsNRangeRowUnitTests {

    private static TsNRangeRow row(long timestamp, List<Double> values, Integer... columnCounts) {
        return TsNRangeRow.of(timestamp, values, Arrays.asList(columnCounts));
    }

    /**
     * Scenario U05 (equality, test plan §3): NaN equals NaN, with the same hash code, so a row with a missing key compares
     * equal to the same row decoded from the other protocol.
     */
    @Test
    void nanEqualsNan() {

        TsNRangeRow a = row(10, Arrays.asList(100.0, Double.NaN), 1, 1);
        TsNRangeRow b = row(10, Arrays.asList(100.0, Double.NaN), 1, 1);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(b).isEqualTo(a);
    }

    /**
     * Scenario U05 (equality): +0.0 is not equal to −0.0 ({@code Double.compare} semantics).
     */
    @Test
    void positiveAndNegativeZeroDiffer() {
        assertThat(row(0, Arrays.asList(0.0, 1.0), 1, 1)).isNotEqualTo(row(0, Arrays.asList(-0.0, 1.0), 1, 1));
    }

    /**
     * Scenario U05 (equality): rows differ by timestamp, by any value, and by the split of the values into keys.
     */
    @Test
    void differencesBreakEquality() {

        TsNRangeRow base = row(0, Arrays.asList(3.0, 30.0, 2.0), 1, 2);

        assertThat(base).isNotEqualTo(row(10, Arrays.asList(3.0, 30.0, 2.0), 1, 2));
        assertThat(base).isNotEqualTo(row(0, Arrays.asList(3.0, 30.0, 1.0), 1, 2));
        assertThat(base).isNotEqualTo(row(0, Arrays.asList(3.0, 30.0, 2.0), 2, 1));
        assertThat(base).isNotEqualTo(row(0, Arrays.asList(3.0, 30.0, 2.0), 1, 1, 1));
        assertThat(base).isNotEqualTo(null);
        assertThat(base).isNotEqualTo("row");
        assertThat(base).isEqualTo(base);
    }

    /**
     * Scenario U05: {@code getValues()} is flat in key order and then aggregation order; {@code getValues(keyIndex)} slices by
     * the column counts. The F6 flow row {@code [10, [100, 50, 1]]} with counts {@code [1, 2]}: key 0 → [100], key 1 → [50, 1].
     */
    @Test
    void perKeySlices() {

        TsNRangeRow r = row(10, Arrays.asList(100.0, 50.0, 1.0), 1, 2);

        assertThat(r.getTimestamp()).isEqualTo(10);
        assertThat(r.getValues()).containsExactly(100.0, 50.0, 1.0);
        assertThat(r.getValues(0)).containsExactly(100.0);
        assertThat(r.getValues(1)).containsExactly(50.0, 1.0);
    }

    /**
     * Scenario U05: three keys with 2, 1 and 3 values each.
     */
    @Test
    void perKeySlicesUnevenCounts() {

        TsNRangeRow r = row(5, Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), 2, 1, 3);

        assertThat(r.getValues(0)).containsExactly(1.0, 2.0);
        assertThat(r.getValues(1)).containsExactly(3.0);
        assertThat(r.getValues(2)).containsExactly(4.0, 5.0, 6.0);
    }

    /**
     * Scenario U05: a key index that is negative or not less than the number of keys → {@link IndexOutOfBoundsException}
     * (Javadoc of {@code getValues(int)}).
     */
    @Test
    void keyIndexOutOfBounds() {

        TsNRangeRow r = row(0, Arrays.asList(1.0, 3.0), 1, 1);

        assertThatThrownBy(() -> r.getValues(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> r.getValues(2)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    /**
     * Scenario U05: {@link TsNRangeRow} is immutable: the returned lists cannot be changed, and changing the lists passed to
     * {@code of} afterwards does not change the row.
     */
    @Test
    void immutable() {

        List<Double> values = new ArrayList<>(Arrays.asList(1.0, 3.0));
        List<Integer> counts = new ArrayList<>(Arrays.asList(1, 1));
        TsNRangeRow r = TsNRangeRow.of(0, values, counts);

        values.set(0, 9.0);
        counts.set(0, 2);

        assertThat(r.getValues()).containsExactly(1.0, 3.0);
        assertThat(r.getValues(0)).containsExactly(1.0);
        assertThatThrownBy(() -> r.getValues().add(1.0)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> r.getValues(1).set(0, 1.0)).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Scenario U05: the validation of {@code of}: {@code null} values or {@code null} elements, {@code null} or empty column
     * counts, a count that is not positive, and counts that do not add up to the number of values throw
     * {@link IllegalArgumentException}.
     */
    @Test
    void ofValidation() {

        assertThatThrownBy(() -> TsNRangeRow.of(0, null, Arrays.asList(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0, null), Arrays.asList(1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0), Collections.<Integer> emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0), Arrays.asList(1, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0, 2.0), Arrays.asList(-1, 3)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0, 2.0), Arrays.asList(1, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0, 2.0), Arrays.asList(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeRow.of(0, Arrays.asList(1.0), Arrays.asList(1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario U05: {@code toString()} names the timestamp and the values.
     */
    @Test
    void toStringNamesTimestampAndValues() {
        assertThat(row(12, Arrays.asList(Double.NaN, 50.0), 1, 1).toString()).contains("12").contains("NaN").contains("50.0");
    }

}
