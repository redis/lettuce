/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.TestTags;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.vector.VSimScoreAttribs;

@Tag(TestTags.UNIT_TEST)
class VSimScoreAttribsMapOutputUnitTests {

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void output_parses_resp2_triplets() {
        VSimScoreAttribsMapOutput<String, String> output = new VSimScoreAttribsMapOutput<>(StringCodec.UTF8);

        // Simulate RESP2 flat array: element, score-as-string, attributes
        output.multi(3);
        output.set(ByteBuffer.wrap("e1".getBytes(StandardCharsets.UTF_8))); // KEY
        output.set(ByteBuffer.wrap("0.91".getBytes(StandardCharsets.UTF_8))); // SCORE as string
        output.set(ByteBuffer.wrap("{\"a\":1}".getBytes(StandardCharsets.UTF_8))); // ATTRIBS

        Map<String, VSimScoreAttribs> result = output.get();
        assertThat(result).containsKey("e1");
        assertThat(result.get("e1").getScore()).isCloseTo(0.91, within(1e-9));
        assertThat(result.get("e1").getAttributes()).isEqualTo("{\"a\":1}");
    }

    @Test
    void output_parses_resp3_with_double_score() {
        VSimScoreAttribsMapOutput<String, String> output = new VSimScoreAttribsMapOutput<>(StringCodec.UTF8);

        // Simulate RESP3: element key, then score as double, then attributes as bulk string
        output.multi(1);
        output.set(ByteBuffer.wrap("e1".getBytes(StandardCharsets.UTF_8))); // KEY
        output.set(0.91); // SCORE as double
        output.set(ByteBuffer.wrap("{\"a\":1}".getBytes(StandardCharsets.UTF_8))); // ATTRIBS

        Map<String, VSimScoreAttribs> result = output.get();
        assertThat(result).containsKey("e1");
        assertThat(result.get("e1").getScore()).isCloseTo(0.91, within(1e-9));
        assertThat(result.get("e1").getAttributes()).isEqualTo("{\"a\":1}");
    }

    @Test
    void output_skips_entry_on_malformed_score_resp2() {
        VSimScoreAttribsMapOutput<String, String> output = new VSimScoreAttribsMapOutput<>(StringCodec.UTF8);

        // KEY -> SCORE (malformed) -> ATTRIBS should skip this entry and not fail output
        output.multi(3);
        output.set(ByteBuffer.wrap("badkey".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("not-a-double".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("{\"ignored\":true}".getBytes(StandardCharsets.UTF_8)));

        // Then add a valid triplet
        output.set(ByteBuffer.wrap("good".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("0.75".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("{\"ok\":1}".getBytes(StandardCharsets.UTF_8)));

        Map<String, VSimScoreAttribs> result = output.get();
        assertThat(result).doesNotContainKey("badkey");
        assertThat(result).containsKey("good");
        assertThat(result.get("good").getScore()).isCloseTo(0.75, within(1e-9));
        assertThat(result.get("good").getAttributes()).isEqualTo("{\"ok\":1}");
    }

    @Test
    void output_skips_entry_on_resp3_attr_double_mismatch() {
        VSimScoreAttribsMapOutput<String, String> output = new VSimScoreAttribsMapOutput<>(StringCodec.UTF8);

        output.multi(1);
        output.set(buf("x")); // KEY
        output.set(0.33); // SCORE
        output.set(1.23); // Wrong type for ATTRIBS (double instead of bulk-string) -> skip entry

        // Now a valid one
        output.set(buf("y"));
        output.set(0.44);
        output.set(buf("{\"b\":2}"));

        Map<String, VSimScoreAttribs> result = output.get();
        assertThat(result).doesNotContainKey("x");
        assertThat(result).containsKey("y");
        assertThat(result.get("y").getScore()).isCloseTo(0.44, within(1e-9));
        assertThat(result.get("y").getAttributes()).isEqualTo("{\"b\":2}");
    }

}
