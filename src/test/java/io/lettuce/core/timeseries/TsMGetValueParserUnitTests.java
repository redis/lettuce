/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.timeseries.TsReplyTestSupport.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link TsMGetValueParser} (design §4, shape P7). The replies are the bytes captured from Redis 8.10.0
 * (redislabs/client-libs-test:8.10.0, 2026-10-20) after {@code TS.CREATE {ts}:a LABELS t x sensor temp area north},
 * {@code TS.CREATE {ts}:b LABELS t x sensor hum}, {@code TS.CREATE {ts}:e LABELS t x}, {@code TS.ADD {ts}:a 1000 1},
 * {@code TS.ADD {ts}:a 2000 2.5}, {@code TS.ADD {ts}:b 1000 3}.
 */
@Tag(UNIT_TEST)
class TsMGetValueParserUnitTests {

    /**
     * {@code TS.MGET FILTER t=x} on RESP2.
     */
    static final String RESP2_PLAIN = "*3\r\n*3\r\n$6\r\n{ts}:a\r\n*0\r\n*2\r\n:2000\r\n+2.5\r\n"
            + "*3\r\n$6\r\n{ts}:b\r\n*0\r\n*2\r\n:1000\r\n+3\r\n*3\r\n$6\r\n{ts}:e\r\n*0\r\n*0\r\n";

    /**
     * {@code TS.MGET FILTER t=x} on RESP3.
     */
    static final String RESP3_PLAIN = "%3\r\n$6\r\n{ts}:a\r\n*2\r\n%0\r\n*2\r\n:2000\r\n,2.5\r\n"
            + "$6\r\n{ts}:b\r\n*2\r\n%0\r\n*2\r\n:1000\r\n,3\r\n$6\r\n{ts}:e\r\n*2\r\n%0\r\n*0\r\n";

    /**
     * {@code TS.MGET WITHLABELS FILTER t=x} on RESP2.
     */
    static final String RESP2_WITHLABELS = "*3\r\n*3\r\n$6\r\n{ts}:a\r\n*3\r\n*2\r\n$1\r\nt\r\n$1\r\nx\r\n"
            + "*2\r\n$6\r\nsensor\r\n$4\r\ntemp\r\n*2\r\n$4\r\narea\r\n$5\r\nnorth\r\n*2\r\n:2000\r\n+2.5\r\n"
            + "*3\r\n$6\r\n{ts}:b\r\n*2\r\n*2\r\n$1\r\nt\r\n$1\r\nx\r\n*2\r\n$6\r\nsensor\r\n$3\r\nhum\r\n*2\r\n:1000\r\n+3\r\n"
            + "*3\r\n$6\r\n{ts}:e\r\n*1\r\n*2\r\n$1\r\nt\r\n$1\r\nx\r\n*0\r\n";

    /**
     * {@code TS.MGET WITHLABELS FILTER t=x} on RESP3.
     */
    static final String RESP3_WITHLABELS = "%3\r\n$6\r\n{ts}:a\r\n*2\r\n%3\r\n$1\r\nt\r\n$1\r\nx\r\n$6\r\nsensor\r\n$4\r\ntemp\r\n"
            + "$4\r\narea\r\n$5\r\nnorth\r\n*2\r\n:2000\r\n,2.5\r\n$6\r\n{ts}:b\r\n*2\r\n%2\r\n$1\r\nt\r\n$1\r\nx\r\n"
            + "$6\r\nsensor\r\n$3\r\nhum\r\n*2\r\n:1000\r\n,3\r\n$6\r\n{ts}:e\r\n*2\r\n%1\r\n$1\r\nt\r\n$1\r\nx\r\n*0\r\n";

    /**
     * {@code TS.MGET SELECTED_LABELS area missing FILTER t=x} on RESP2.
     */
    static final String RESP2_SELECTED = "*3\r\n*3\r\n$6\r\n{ts}:a\r\n*2\r\n*2\r\n$4\r\narea\r\n$5\r\nnorth\r\n"
            + "*2\r\n$7\r\nmissing\r\n$-1\r\n*2\r\n:2000\r\n+2.5\r\n*3\r\n$6\r\n{ts}:b\r\n*2\r\n*2\r\n$4\r\narea\r\n$-1\r\n"
            + "*2\r\n$7\r\nmissing\r\n$-1\r\n*2\r\n:1000\r\n+3\r\n*3\r\n$6\r\n{ts}:e\r\n*2\r\n*2\r\n$4\r\narea\r\n$-1\r\n"
            + "*2\r\n$7\r\nmissing\r\n$-1\r\n*0\r\n";

    /**
     * {@code TS.MGET SELECTED_LABELS area missing FILTER t=x} on RESP3.
     */
    static final String RESP3_SELECTED = "%3\r\n$6\r\n{ts}:a\r\n*2\r\n%2\r\n$4\r\narea\r\n$5\r\nnorth\r\n$7\r\nmissing\r\n_\r\n"
            + "*2\r\n:2000\r\n,2.5\r\n$6\r\n{ts}:b\r\n*2\r\n%2\r\n$4\r\narea\r\n_\r\n$7\r\nmissing\r\n_\r\n"
            + "*2\r\n:1000\r\n,3\r\n$6\r\n{ts}:e\r\n*2\r\n%2\r\n$4\r\narea\r\n_\r\n$7\r\nmissing\r\n_\r\n*0\r\n";

    private static List<TsMGetValue<String>> parseString(String resp) {
        return parse(resp, StringCodec.UTF8, new TsMGetValueParser<>(StringCodec.UTF8));
    }

    /**
     * Scenario U08: RESP2 list without labels: keys, empty labels, samples, and {@code null} for the empty series.
     */
    @Test
    void resp2Plain() {
        assertPlain(parseString(RESP2_PLAIN));
    }

    /**
     * Scenario U08: RESP3 map without labels gives the same model.
     */
    @Test
    void resp3Plain() {
        assertPlain(parseString(RESP3_PLAIN));
    }

    private static void assertPlain(List<TsMGetValue<String>> values) {

        assertThat(values).hasSize(3);
        assertThat(values.get(0).getKey()).isEqualTo("{ts}:a");
        assertThat(values.get(0).getLabels()).isNotNull().isEmpty();
        assertThat(values.get(0).getSample()).isEqualTo(TsSample.of(2000, 2.5));
        assertThat(values.get(1).getKey()).isEqualTo("{ts}:b");
        assertThat(values.get(1).getSample()).isEqualTo(TsSample.of(1000, 3.0));
        assertThat(values.get(2).getKey()).isEqualTo("{ts}:e");
        assertThat(values.get(2).getLabels()).isNotNull().isEmpty();
        assertThat(values.get(2).getSample()).isNull();
    }

    /**
     * Scenario U08: WITHLABELS, RESP2 and RESP3: all labels, in server order.
     */
    @Test
    void withLabels() {

        for (String resp : new String[] { RESP2_WITHLABELS, RESP3_WITHLABELS }) {
            List<TsMGetValue<String>> values = parseString(resp);
            assertThat(values).hasSize(3);
            assertThat(values.get(0).getLabels()).containsExactly(entry("t", "x"), entry("sensor", "temp"),
                    entry("area", "north"));
            assertThat(values.get(1).getLabels()).containsExactly(entry("t", "x"), entry("sensor", "hum"));
            assertThat(values.get(2).getLabels()).containsExactly(entry("t", "x"));
            assertThat(values.get(2).getSample()).isNull();
        }
    }

    /**
     * Scenario U08: SELECTED_LABELS, RESP2 and RESP3: a selected label the series does not have has a {@code null} value.
     */
    @Test
    void selectedLabelsMissingLabelIsNull() {

        for (String resp : new String[] { RESP2_SELECTED, RESP3_SELECTED }) {
            List<TsMGetValue<String>> values = parseString(resp);
            assertThat(values.get(0).getLabels()).containsExactly(entry("area", "north"), entry("missing", null));
            assertThat(values.get(1).getLabels()).containsExactly(entry("area", null), entry("missing", null));
            assertThat(values.get(2).getLabels()).containsOnlyKeys("area", "missing");
        }
    }

    /**
     * Scenario U12 (MGET): each RESP2/RESP3 pair gives equal models with equal hash codes (NF.1).
     */
    @Test
    void resp2AndResp3Equal() {

        String[][] pairs = { { RESP2_PLAIN, RESP3_PLAIN }, { RESP2_WITHLABELS, RESP3_WITHLABELS },
                { RESP2_SELECTED, RESP3_SELECTED } };
        for (String[] pair : pairs) {
            List<TsMGetValue<String>> fromResp2 = parseString(pair[0]);
            List<TsMGetValue<String>> fromResp3 = parseString(pair[1]);
            assertThat(fromResp2).isEqualTo(fromResp3);
            assertThat(fromResp2.hashCode()).isEqualTo(fromResp3.hashCode());
        }
        assertThat(parseString(RESP2_PLAIN)).isNotEqualTo(parseString(RESP2_WITHLABELS));
    }

    /**
     * Scenario U08: an empty reply (no series matches) → an empty list on both protocols.
     */
    @Test
    void emptyReply() {

        assertThat(parseString("*0\r\n")).isEmpty();
        assertThat(parseString("%0\r\n")).isEmpty();
    }

    /**
     * Scenario U08 with values outside the captured set (NF.9): NaN and Infinity samples on both protocols, and a UTF-8 label.
     * Keys are decoded with the connection codec, labels as UTF-8 regardless of the codec (design §3.5).
     */
    @Test
    void specialValuesAndCodec() {

        String resp2 = "*2\r\n*3\r\n$1\r\nk\r\n*1\r\n*2\r\n$4\r\ncity\r\n$6\r\nSofiа\r\n*2\r\n:1\r\n+NaN\r\n"
                + "*3\r\n$1\r\nm\r\n*0\r\n*2\r\n:2\r\n+-Infinity\r\n";
        String resp3 = "%2\r\n$1\r\nk\r\n*2\r\n%1\r\n$4\r\ncity\r\n$6\r\nSofiа\r\n*2\r\n:1\r\n,nan\r\n"
                + "$1\r\nm\r\n*2\r\n%0\r\n*2\r\n:2\r\n,-inf\r\n";

        for (String resp : new String[] { resp2, resp3 }) {
            List<TsMGetValue<byte[]>> values = parse(resp, ByteArrayCodec.INSTANCE,
                    new TsMGetValueParser<>(ByteArrayCodec.INSTANCE));
            assertThat(values).hasSize(2);
            assertThat(values.get(0).getKey()).isEqualTo("k".getBytes(StandardCharsets.UTF_8));
            assertThat(values.get(0).getLabels()).containsExactly(entry("city", "Sofiа"));
            assertThat(values.get(0).getSample().getValue()).isNaN();
            assertThat(values.get(1).getSample()).isEqualTo(TsSample.of(2, Double.NEGATIVE_INFINITY));
        }
    }

    /**
     * The parser rejects a {@code null} codec.
     */
    @Test
    void nullCodecThrows() {
        assertThatThrownBy(() -> new TsMGetValueParser<>(null)).isInstanceOf(IllegalArgumentException.class);
    }

}
