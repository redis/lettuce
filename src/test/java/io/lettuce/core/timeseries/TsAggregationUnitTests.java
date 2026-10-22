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

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link TsAggregation} and {@link TsAggregationType}.
 */
@Tag(UNIT_TEST)
class TsAggregationUnitTests {

    private static String wire(TsAggregation aggregation) {
        return new String(aggregation.getBytes(), StandardCharsets.US_ASCII);
    }

    /**
     * Scenario U02: lookup by wire token, ignoring case.
     */
    @Test
    void lookupByWireToken() {

        assertThat(TsAggregation.of("AVG")).isSameAs(TsAggregationType.AVG);
        assertThat(TsAggregation.of("avg")).isSameAs(TsAggregationType.AVG);
        assertThat(TsAggregation.of("std.p")).isSameAs(TsAggregationType.STD_P);
        assertThat(TsAggregation.of("STD.S")).isSameAs(TsAggregationType.STD_S);
        assertThat(TsAggregation.of("var.p")).isSameAs(TsAggregationType.VAR_P);
        assertThat(TsAggregation.of("Var.S")).isSameAs(TsAggregationType.VAR_S);
        assertThat(TsAggregation.of("countnan")).isSameAs(TsAggregationType.COUNTNAN);
        assertThat(TsAggregation.of("countNaN")).isSameAs(TsAggregationType.COUNTNAN);
        assertThat(TsAggregation.of("COUNTALL")).isSameAs(TsAggregationType.COUNTALL);
        assertThat(TsAggregation.of("twa")).isSameAs(TsAggregationType.TWA);
    }

    /**
     * Scenario U02: every constant is found by its own wire token.
     */
    @Test
    void everyConstantFoundByItsWireToken() {

        for (TsAggregationType type : TsAggregationType.values()) {
            assertThat(TsAggregation.of(wire(type))).isSameAs(type);
            assertThat(TsAggregation.of(wire(type).toLowerCase(Locale.ROOT))).isSameAs(type);
        }
    }

    /**
     * Scenario U02: lookup never uses {@code name()}: {@code std_p} is not {@code STD_P}.
     */
    @Test
    void lookupNeverByName() {

        assertThat(TsAggregation.of("std_p")).isNotInstanceOf(TsAggregationType.class);
        assertThat(TsAggregation.of("STD_S")).isNotInstanceOf(TsAggregationType.class);
        assertThat(TsAggregation.of("VAR_P")).isNotInstanceOf(TsAggregationType.class);
    }

    /**
     * Scenario U02: the wire tokens of design §3.1.
     */
    @Test
    void wireTokens() {

        assertThat(wire(TsAggregationType.AVG)).isEqualTo("AVG");
        assertThat(wire(TsAggregationType.STD_P)).isEqualTo("STD.P");
        assertThat(wire(TsAggregationType.STD_S)).isEqualTo("STD.S");
        assertThat(wire(TsAggregationType.VAR_P)).isEqualTo("VAR.P");
        assertThat(wire(TsAggregationType.VAR_S)).isEqualTo("VAR.S");
        assertThat(TsAggregationType.values()).hasSize(15);
    }

    /**
     * Scenario U02: {@code toString()} of every constant is its wire token, not its {@code name()}.
     */
    @Test
    void toStringIsWireToken() {

        assertThat(TsAggregationType.STD_P.toString()).isEqualTo("STD.P");
        assertThat(TsAggregationType.STD_S.toString()).isEqualTo("STD.S");
        assertThat(TsAggregationType.VAR_P.toString()).isEqualTo("VAR.P");
        assertThat(TsAggregationType.VAR_S.toString()).isEqualTo("VAR.S");
        assertThat(TsAggregationType.AVG.toString()).isEqualTo("AVG");
        for (TsAggregationType type : TsAggregationType.values()) {
            assertThat(type.toString()).isEqualTo(wire(type));
        }
    }

    /**
     * Scenario U02: lookup runs correctly under the Turkish locale (dotted/dotless i).
     */
    @Test
    void lookupUnderTurkishLocale() {

        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            assertThat(TsAggregation.of("min")).isSameAs(TsAggregationType.MIN);
            assertThat(TsAggregation.of("MIN")).isSameAs(TsAggregationType.MIN);
            assertThat(TsAggregation.of("first")).isSameAs(TsAggregationType.FIRST);
            assertThat(TsAggregation.of("FIRST")).isSameAs(TsAggregationType.FIRST);
            assertThat(TsAggregation.of("countnan")).isSameAs(TsAggregationType.COUNTNAN);
            assertThat(wire(TsAggregationType.MIN)).isEqualTo("MIN");
            assertThat(wire(TsAggregationType.FIRST)).isEqualTo("FIRST");
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * Scenario U02b: an unknown name becomes a raw value that is sent exactly as given.
     */
    @Test
    void unknownNameBecomesRawValue() {

        TsAggregation median = TsAggregation.of("median");

        assertThat(median).isNotInstanceOf(TsAggregationType.class);
        assertThat(median.toString()).isEqualTo("median");
        assertThat(wire(median)).isEqualTo("median");
    }

    /**
     * Scenario U02b: raw values compare by the exact name; {@code hashCode} is consistent with {@code equals}.
     */
    @Test
    void rawValueEquality() {

        assertThat(TsAggregation.of("median")).isEqualTo(TsAggregation.of("median"));
        assertThat(TsAggregation.of("median").hashCode()).isEqualTo(TsAggregation.of("median").hashCode());
        assertThat(TsAggregation.of("median")).isNotEqualTo(TsAggregation.of("MEDIAN"));
        assertThat(TsAggregation.of("median")).isNotEqualTo(TsAggregation.of("p50"));
        assertThat(TsAggregation.of("median")).isNotEqualTo(TsAggregationType.AVG);
    }

    /**
     * Scenario U02b: {@code of("avg")} returns the enum constant itself.
     */
    @Test
    void knownNameReturnsConstant() {
        assertThat(TsAggregation.of("avg") == TsAggregationType.AVG).isTrue();
    }

    /**
     * Scenario A21: both a constant and a raw value can be passed where a {@link TsAggregation} is expected and encode as their
     * wire token.
     */
    @Test
    void valueTypesEncodeAsParameters() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(TsAggregationType.AVG)
                .add(TsAggregation.of("median")).add(TsAggregation.of("std.p"));

        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("$3\r\nAVG\r\n$6\r\nmedian\r\n$5\r\nSTD.P\r\n");
    }

    @Test
    void nullOrEmptyNameThrows() {

        assertThatThrownBy(() -> TsAggregation.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsAggregation.of("")).isInstanceOf(IllegalArgumentException.class);
    }

}
