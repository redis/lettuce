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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.protocol.RedisStateMachine;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link TsInfoValueParser}. The RESP2 and RESP3 replies below are the bytes captured from Redis 8.10.0
 * (redislabs/client-libs-test:8.10.0, 2026-10-20) for the commands named in each fixture; the variants with unknown or nil
 * values are derived from them.
 */
@Tag(UNIT_TEST)
class TsInfoValueParserUnitTests {

    // --- captured replies --------------------------------------------------------------------------------------------

    /**
     * {@code TS.CREATE {ts}:x}, then {@code TS.INFO {ts}:x} on RESP2.
     */
    static final String RESP2_DEFAULT = "*28\r\n+totalSamples\r\n:0\r\n+memoryUsage\r\n:4464\r\n+firstTimestamp\r\n:0\r\n"
            + "+lastTimestamp\r\n:0\r\n+retentionTime\r\n:0\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:4096\r\n"
            + "+chunkType\r\n+compressed\r\n+duplicatePolicy\r\n+block\r\n+labels\r\n*0\r\n+sourceKey\r\n$-1\r\n"
            + "+rules\r\n*0\r\n+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n$1\r\n0\r\n";

    /**
     * Same as {@link #RESP2_DEFAULT} on RESP3.
     */
    static final String RESP3_DEFAULT = "%14\r\n+totalSamples\r\n:0\r\n+memoryUsage\r\n:4464\r\n+firstTimestamp\r\n:0\r\n"
            + "+lastTimestamp\r\n:0\r\n+retentionTime\r\n:0\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:4096\r\n"
            + "+chunkType\r\n+compressed\r\n+duplicatePolicy\r\n+block\r\n+labels\r\n%0\r\n+sourceKey\r\n_\r\n"
            + "+rules\r\n%0\r\n+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n,0\r\n";

    /**
     * {@code TS.CREATE {ts}:y RETENTION 5000 ENCODING UNCOMPRESSED CHUNK_SIZE 128 DUPLICATE_POLICY LAST IGNORE 10 0.5 LABELS a
     * 1 b 2}, then {@code TS.INFO {ts}:y DEBUG} on RESP2.
     */
    static final String RESP2_OPTIONS_DEBUG = "*32\r\n+totalSamples\r\n:0\r\n+memoryUsage\r\n:1184\r\n+firstTimestamp\r\n:0\r\n"
            + "+lastTimestamp\r\n:0\r\n+retentionTime\r\n:5000\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:128\r\n"
            + "+chunkType\r\n+uncompressed\r\n+duplicatePolicy\r\n+last\r\n+labels\r\n*2\r\n*2\r\n$1\r\na\r\n$1\r\n1\r\n"
            + "*2\r\n$1\r\nb\r\n$1\r\n2\r\n+sourceKey\r\n$-1\r\n+rules\r\n*0\r\n+ignoreMaxTimeDiff\r\n:10\r\n"
            + "+ignoreMaxValDiff\r\n$3\r\n0.5\r\n+keySelfName\r\n$6\r\n{ts}:y\r\n+Chunks\r\n*1\r\n*10\r\n"
            + "+startTimestamp\r\n:-1\r\n+endTimestamp\r\n:-1\r\n+samples\r\n:0\r\n+size\r\n:128\r\n"
            + "+bytesPerSample\r\n$1\r\n0\r\n";

    /**
     * Same as {@link #RESP2_OPTIONS_DEBUG} on RESP3.
     */
    static final String RESP3_OPTIONS_DEBUG = "%16\r\n+totalSamples\r\n:0\r\n+memoryUsage\r\n:1184\r\n+firstTimestamp\r\n:0\r\n"
            + "+lastTimestamp\r\n:0\r\n+retentionTime\r\n:5000\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:128\r\n"
            + "+chunkType\r\n+uncompressed\r\n+duplicatePolicy\r\n+last\r\n+labels\r\n%2\r\n$1\r\na\r\n$1\r\n1\r\n"
            + "$1\r\nb\r\n$1\r\n2\r\n+sourceKey\r\n_\r\n+rules\r\n%0\r\n+ignoreMaxTimeDiff\r\n:10\r\n"
            + "+ignoreMaxValDiff\r\n,0.5\r\n+keySelfName\r\n$6\r\n{ts}:y\r\n+Chunks\r\n*1\r\n%5\r\n"
            + "+startTimestamp\r\n:-1\r\n+endTimestamp\r\n:-1\r\n+samples\r\n:0\r\n+size\r\n:128\r\n"
            + "+bytesPerSample\r\n,0\r\n";

    /**
     * {@code TS.CREATE {ts}:src LABELS t x}, {@code TS.CREATE {ts}:dst}, {@code TS.CREATERULE {ts}:src {ts}:dst AGGREGATION avg
     * 1000 500}, {@code TS.ADD {ts}:src 1000 10}, {@code TS.ADD {ts}:src 1500 20}, then {@code TS.INFO {ts}:src DEBUG} on
     * RESP2.
     */
    static final String RESP2_SOURCE_DEBUG = "*32\r\n+totalSamples\r\n:2\r\n+memoryUsage\r\n:4968\r\n+firstTimestamp\r\n:1000\r\n"
            + "+lastTimestamp\r\n:1500\r\n+retentionTime\r\n:0\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:4096\r\n"
            + "+chunkType\r\n+compressed\r\n+duplicatePolicy\r\n+block\r\n+labels\r\n*1\r\n*2\r\n$1\r\nt\r\n$1\r\nx\r\n"
            + "+sourceKey\r\n$-1\r\n+rules\r\n*1\r\n*4\r\n$8\r\n{ts}:dst\r\n:1000\r\n+AVG\r\n:500\r\n"
            + "+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n$1\r\n0\r\n+keySelfName\r\n$8\r\n{ts}:src\r\n"
            + "+Chunks\r\n*1\r\n*10\r\n+startTimestamp\r\n:1000\r\n+endTimestamp\r\n:1500\r\n+samples\r\n:2\r\n"
            + "+size\r\n:4096\r\n+bytesPerSample\r\n$4\r\n2048\r\n";

    /**
     * Same as {@link #RESP2_SOURCE_DEBUG} on RESP3.
     */
    static final String RESP3_SOURCE_DEBUG = "%16\r\n+totalSamples\r\n:2\r\n+memoryUsage\r\n:4968\r\n+firstTimestamp\r\n:1000\r\n"
            + "+lastTimestamp\r\n:1500\r\n+retentionTime\r\n:0\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:4096\r\n"
            + "+chunkType\r\n+compressed\r\n+duplicatePolicy\r\n+block\r\n+labels\r\n%1\r\n$1\r\nt\r\n$1\r\nx\r\n"
            + "+sourceKey\r\n_\r\n+rules\r\n%1\r\n$8\r\n{ts}:dst\r\n*3\r\n:1000\r\n+AVG\r\n:500\r\n"
            + "+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n,0\r\n+keySelfName\r\n$8\r\n{ts}:src\r\n"
            + "+Chunks\r\n*1\r\n%5\r\n+startTimestamp\r\n:1000\r\n+endTimestamp\r\n:1500\r\n+samples\r\n:2\r\n"
            + "+size\r\n:4096\r\n+bytesPerSample\r\n,2048\r\n";

    /**
     * Same setup as {@link #RESP2_SOURCE_DEBUG}, then {@code TS.INFO {ts}:dst} on RESP2.
     */
    static final String RESP2_DESTINATION = "*28\r\n+totalSamples\r\n:1\r\n+memoryUsage\r\n:4464\r\n+firstTimestamp\r\n:500\r\n"
            + "+lastTimestamp\r\n:500\r\n+retentionTime\r\n:0\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:4096\r\n"
            + "+chunkType\r\n+compressed\r\n+duplicatePolicy\r\n+block\r\n+labels\r\n*0\r\n"
            + "+sourceKey\r\n$8\r\n{ts}:src\r\n+rules\r\n*0\r\n+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n$1\r\n0\r\n";

    /**
     * Same as {@link #RESP2_DESTINATION} on RESP3.
     */
    static final String RESP3_DESTINATION = "%14\r\n+totalSamples\r\n:1\r\n+memoryUsage\r\n:4464\r\n+firstTimestamp\r\n:500\r\n"
            + "+lastTimestamp\r\n:500\r\n+retentionTime\r\n:0\r\n+chunkCount\r\n:1\r\n+chunkSize\r\n:4096\r\n"
            + "+chunkType\r\n+compressed\r\n+duplicatePolicy\r\n+block\r\n+labels\r\n%0\r\n"
            + "+sourceKey\r\n$8\r\n{ts}:src\r\n+rules\r\n%0\r\n+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n,0\r\n";

    // --- derived replies ---------------------------------------------------------------------------------------------

    /**
     * {@link #RESP2_DEFAULT} with a nil duplicate policy, as older servers report it (TIMESERIES-CANONICAL.md §3, §9).
     */
    static final String RESP2_NIL_POLICY = RESP2_DEFAULT.replace("+duplicatePolicy\r\n+block\r\n",
            "+duplicatePolicy\r\n$-1\r\n");

    static final String RESP3_NIL_POLICY = RESP3_DEFAULT.replace("+duplicatePolicy\r\n+block\r\n", "+duplicatePolicy\r\n_\r\n");

    /**
     * {@link #RESP2_SOURCE_DEBUG} with values Lettuce does not model: rule aggregator {@code MEDIAN}, chunk type {@code zstd},
     * duplicate policy {@code keep}, and an extra top-level field {@code newField}.
     */
    static final String RESP2_UNKNOWN = RESP2_SOURCE_DEBUG.replace("*32\r\n", "*34\r\n").replace("+AVG\r\n", "+MEDIAN\r\n")
            .replace("+compressed\r\n", "+zstd\r\n").replace("+duplicatePolicy\r\n+block\r\n", "+duplicatePolicy\r\n+keep\r\n")
            .replace("+keySelfName\r\n", "+newField\r\n:42\r\n+keySelfName\r\n");

    static final String RESP3_UNKNOWN = RESP3_SOURCE_DEBUG.replace("%16\r\n", "%17\r\n").replace("+AVG\r\n", "+MEDIAN\r\n")
            .replace("+compressed\r\n", "+zstd\r\n").replace("+duplicatePolicy\r\n+block\r\n", "+duplicatePolicy\r\n+keep\r\n")
            .replace("+keySelfName\r\n", "+newField\r\n:42\r\n+keySelfName\r\n");

    private static TsInfoValue<String> parse(String resp) {

        RedisStateMachine rsm = new RedisStateMachine(ByteBufAllocator.DEFAULT);
        EncodedComplexOutput<String, String, TsInfoValue<String>> output = new EncodedComplexOutput<>(StringCodec.UTF8,
                new TsInfoValueParser<>(StringCodec.UTF8));
        boolean complete = rsm.decode(Unpooled.wrappedBuffer(resp.getBytes(StandardCharsets.UTF_8)), output, e -> {
            throw new IllegalStateException(e);
        });
        assertThat(complete).isTrue();
        assertThat(output.getError()).isNull();
        return output.get();
    }

    private static Map<String, String> labels(String... pairs) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            labels.put(pairs[i], pairs[i + 1]);
        }
        return labels;
    }

    // --- U11 ---------------------------------------------------------------------------------------------------------

    /**
     * Scenario U11: RESP2 flat list, a series with default options.
     */
    @Test
    void resp2Defaults() {
        assertDefaults(parse(RESP2_DEFAULT));
    }

    /**
     * Scenario U11: RESP3 map, a series with default options.
     */
    @Test
    void resp3Defaults() {
        assertDefaults(parse(RESP3_DEFAULT));
    }

    private static void assertDefaults(TsInfoValue<String> info) {

        assertThat(info.getTotalSamples()).isEqualTo(0);
        assertThat(info.getMemoryUsage()).isEqualTo(4464);
        assertThat(info.getFirstTimestamp()).isEqualTo(0);
        assertThat(info.getLastTimestamp()).isEqualTo(0);
        assertThat(info.getRetentionTime()).isEqualTo(0);
        assertThat(info.getChunkCount()).isEqualTo(1);
        assertThat(info.getChunkSize()).isEqualTo(4096);
        assertThat(info.getChunkType()).isSameAs(TsEncodingType.COMPRESSED);
        assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.BLOCK);
        assertThat(info.getLabels()).isNotNull().isEmpty();
        assertThat(info.getSourceKey()).isNull();
        assertThat(info.getRules()).isNotNull().isEmpty();
        assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(0L);
        assertThat(info.getIgnoreMaxValDiff()).isEqualTo(0.0);
        assertThat(info.getKeySelfName()).isNull();
        assertThat(info.getChunks()).isNull();
    }

    /**
     * Scenario U11: every option round-trips, with DEBUG chunks (RESP2 flat chunk arrays, {@code ignoreMaxValDiff} as bulk
     * string; RESP3 chunk maps, {@code ignoreMaxValDiff} as double).
     */
    @Test
    void optionsWithDebugBothProtocols() {

        for (String resp : new String[] { RESP2_OPTIONS_DEBUG, RESP3_OPTIONS_DEBUG }) {

            TsInfoValue<String> info = parse(resp);

            assertThat(info.getRetentionTime()).isEqualTo(5000);
            assertThat(info.getChunkSize()).isEqualTo(128);
            assertThat(info.getChunkType()).isSameAs(TsEncodingType.UNCOMPRESSED);
            assertThat(info.getDuplicatePolicy()).isSameAs(TsDuplicatePolicyType.LAST);
            assertThat(info.getLabels()).containsExactlyEntriesOf(labels("a", "1", "b", "2"));
            assertThat(info.getIgnoreMaxTimeDiff()).isEqualTo(10L);
            assertThat(info.getIgnoreMaxValDiff()).isEqualTo(0.5);
            assertThat(info.getKeySelfName()).isEqualTo("{ts}:y");
            assertThat(info.getChunks()).hasSize(1);

            TsInfoValue.Chunk chunk = info.getChunks().get(0);
            assertThat(chunk.getStartTimestamp()).isEqualTo(-1);
            assertThat(chunk.getEndTimestamp()).isEqualTo(-1);
            assertThat(chunk.getSamples()).isEqualTo(0);
            assertThat(chunk.getSize()).isEqualTo(128);
            assertThat(chunk.getBytesPerSample()).isEqualTo(0.0);
        }
    }

    /**
     * Scenario U11: rules as list of 4-arrays (RESP2) and as map dest → 3-array (RESP3); DEBUG chunks under {@code Chunks}.
     */
    @Test
    void rulesAndChunksBothProtocols() {

        for (String resp : new String[] { RESP2_SOURCE_DEBUG, RESP3_SOURCE_DEBUG }) {

            TsInfoValue<String> info = parse(resp);

            assertThat(info.getTotalSamples()).isEqualTo(2);
            assertThat(info.getFirstTimestamp()).isEqualTo(1000);
            assertThat(info.getLastTimestamp()).isEqualTo(1500);
            assertThat(info.getLabels()).containsExactlyEntriesOf(labels("t", "x"));
            assertThat(info.getRules()).hasSize(1);

            TsInfoValue.Rule<String> rule = info.getRules().get(0);
            assertThat(rule.getDestinationKey()).isEqualTo("{ts}:dst");
            assertThat(rule.getBucketDuration()).isEqualTo(1000);
            assertThat(rule.getAggregation()).isSameAs(TsAggregationType.AVG);
            assertThat(rule.getAlignment()).isEqualTo(500);

            assertThat(info.getKeySelfName()).isEqualTo("{ts}:src");
            assertThat(info.getChunks()).hasSize(1);
            TsInfoValue.Chunk chunk = info.getChunks().get(0);
            assertThat(chunk.getStartTimestamp()).isEqualTo(1000);
            assertThat(chunk.getEndTimestamp()).isEqualTo(1500);
            assertThat(chunk.getSamples()).isEqualTo(2);
            assertThat(chunk.getSize()).isEqualTo(4096);
            assertThat(chunk.getBytesPerSample()).isEqualTo(2048.0);
        }
    }

    /**
     * Scenario U11: a compaction destination reports its source key (bulk string on both protocols).
     */
    @Test
    void sourceKeyBothProtocols() {

        assertThat(parse(RESP2_DESTINATION).getSourceKey()).isEqualTo("{ts}:src");
        assertThat(parse(RESP3_DESTINATION).getSourceKey()).isEqualTo("{ts}:src");
    }

    /**
     * Scenario U11: a nil duplicate policy (older servers) gives {@code null}.
     */
    @Test
    void nilDuplicatePolicyIsNull() {

        assertThat(parse(RESP2_NIL_POLICY).getDuplicatePolicy()).isNull();
        assertThat(parse(RESP3_NIL_POLICY).getDuplicatePolicy()).isNull();
    }

    /**
     * Scenario U11: nil labels give an empty map (design §3.5: "non-null (empty when nil)").
     */
    @Test
    void nilLabelsAreEmpty() {

        String resp2 = RESP2_DEFAULT.replace("+labels\r\n*0\r\n", "+labels\r\n*-1\r\n");
        String resp3 = RESP3_DEFAULT.replace("+labels\r\n%0\r\n", "+labels\r\n_\r\n");

        assertThat(parse(resp2).getLabels()).isNotNull().isEmpty();
        assertThat(parse(resp3).getLabels()).isNotNull().isEmpty();
    }

    /**
     * Scenario U11: the ignore fields are {@code null} when the server omits them.
     */
    @Test
    void missingIgnoreFieldsAreNull() {

        String resp2 = RESP2_DEFAULT.replace("*28\r\n", "*24\r\n")
                .replace("+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n$1\r\n0\r\n", "");
        String resp3 = RESP3_DEFAULT.replace("%14\r\n", "%12\r\n")
                .replace("+ignoreMaxTimeDiff\r\n:0\r\n+ignoreMaxValDiff\r\n,0\r\n", "");

        for (String resp : new String[] { resp2, resp3 }) {
            TsInfoValue<String> info = parse(resp);
            assertThat(info.getIgnoreMaxTimeDiff()).isNull();
            assertThat(info.getIgnoreMaxValDiff()).isNull();
            assertThat(info.getChunkSize()).isEqualTo(4096);
        }
    }

    /**
     * Scenario U11: {@code bytesPerSample} is documented as {@code "inf"} for a chunk with no samples (TIMESERIES-CANONICAL.md
     * §3, TS.INFO); RESP3 sends it as a double.
     */
    @Test
    void bytesPerSampleInfinity() {

        String resp2 = RESP2_OPTIONS_DEBUG.replace("+bytesPerSample\r\n$1\r\n0\r\n", "+bytesPerSample\r\n$3\r\ninf\r\n");
        String resp3 = RESP3_OPTIONS_DEBUG.replace("+bytesPerSample\r\n,0\r\n", "+bytesPerSample\r\n,inf\r\n");

        assertThat(parse(resp2).getChunks().get(0).getBytesPerSample()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(parse(resp3).getChunks().get(0).getBytesPerSample()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    // --- U11b --------------------------------------------------------------------------------------------------------

    /**
     * Scenario U11b: unknown value-type names become raw values that keep the server's name.
     */
    @Test
    void unknownValuesBecomeRawValues() {

        for (String resp : new String[] { RESP2_UNKNOWN, RESP3_UNKNOWN }) {

            TsInfoValue<String> info = parse(resp);

            assertThat(info.getChunkType()).isNotInstanceOf(TsEncodingType.class).isEqualTo(TsEncoding.of("zstd"));
            assertThat(info.getChunkType().toString()).isEqualTo("zstd");
            assertThat(info.getDuplicatePolicy()).isNotInstanceOf(TsDuplicatePolicyType.class)
                    .isEqualTo(TsDuplicatePolicy.of("keep"));
            assertThat(info.getDuplicatePolicy().toString()).isEqualTo("keep");

            TsAggregation aggregation = info.getRules().get(0).getAggregation();
            assertThat(aggregation).isNotInstanceOf(TsAggregationType.class).isEqualTo(TsAggregation.of("MEDIAN"));
            assertThat(aggregation.toString()).isEqualTo("MEDIAN");
        }
    }

    /**
     * Scenario U11b: an unknown top-level field is ignored by the getters and present in {@code getRawInfo()}.
     */
    @Test
    void unknownFieldOnlyInRawInfo() {

        for (String resp : new String[] { RESP2_UNKNOWN, RESP3_UNKNOWN }) {

            TsInfoValue<String> info = parse(resp);

            assertThat(info.getRawInfo()).containsKey("newField");
            assertThat(info.getRawInfo().get("newField")).isEqualTo(42L);
            assertThat(info.getTotalSamples()).isEqualTo(2);
            assertThat(info.getKeySelfName()).isEqualTo("{ts}:src");
        }
    }

    /**
     * Scenario U11b / G07: {@code getRawInfo()} holds every field the server sent, under the server's names.
     */
    @Test
    void rawInfoHasEveryField() {

        String[] fields = { "totalSamples", "memoryUsage", "firstTimestamp", "lastTimestamp", "retentionTime", "chunkCount",
                "chunkSize", "chunkType", "duplicatePolicy", "labels", "sourceKey", "rules", "ignoreMaxTimeDiff",
                "ignoreMaxValDiff" };

        assertThat(parse(RESP2_DEFAULT).getRawInfo()).containsOnlyKeys(fields);
        assertThat(parse(RESP3_DEFAULT).getRawInfo()).containsOnlyKeys(fields);
        assertThat(parse(RESP2_SOURCE_DEBUG).getRawInfo()).containsKeys(fields).containsKeys("keySelfName", "Chunks")
                .hasSize(16);
        assertThat(parse(RESP3_SOURCE_DEBUG).getRawInfo()).containsKeys(fields).containsKeys("keySelfName", "Chunks")
                .hasSize(16);
        assertThat(parse(RESP2_DEFAULT).getRawInfo().get("totalSamples")).isEqualTo(0L);
    }

    // --- U12 ---------------------------------------------------------------------------------------------------------

    /**
     * Scenario U12 (INFO): every RESP2/RESP3 pair produces equal models.
     */
    @Test
    void resp2AndResp3AreEqual() {

        assertThat(parse(RESP2_DEFAULT)).isEqualTo(parse(RESP3_DEFAULT));
        assertThat(parse(RESP2_DEFAULT).hashCode()).isEqualTo(parse(RESP3_DEFAULT).hashCode());
        assertThat(parse(RESP2_OPTIONS_DEBUG)).isEqualTo(parse(RESP3_OPTIONS_DEBUG));
        assertThat(parse(RESP2_SOURCE_DEBUG)).isEqualTo(parse(RESP3_SOURCE_DEBUG));
        assertThat(parse(RESP2_DESTINATION)).isEqualTo(parse(RESP3_DESTINATION));
        assertThat(parse(RESP2_NIL_POLICY)).isEqualTo(parse(RESP3_NIL_POLICY));
        assertThat(parse(RESP2_UNKNOWN)).isEqualTo(parse(RESP3_UNKNOWN));
        assertThat(parse(RESP2_SOURCE_DEBUG).getRules()).isEqualTo(parse(RESP3_SOURCE_DEBUG).getRules());
        assertThat(parse(RESP2_SOURCE_DEBUG).getChunks()).isEqualTo(parse(RESP3_SOURCE_DEBUG).getChunks());
    }

    /**
     * Scenario U12: different series are not equal.
     */
    @Test
    void differentSeriesAreNotEqual() {

        assertThat(parse(RESP2_DEFAULT)).isNotEqualTo(parse(RESP2_DESTINATION));
        assertThat(parse(RESP3_SOURCE_DEBUG)).isNotEqualTo(parse(RESP3_UNKNOWN));
    }

    /**
     * Design §3.5: labels are decoded as UTF-8 independent of the codec; keys are decoded with the codec.
     */
    @Test
    void labelsAreUtf8KeysUseCodec() {

        String resp = RESP2_DESTINATION.replace("+labels\r\n*0\r\n",
                "+labels\r\n*1\r\n*2\r\n$12\r\ntempérature\r\n$3\r\n°C\r\n");

        RedisStateMachine rsm = new RedisStateMachine(ByteBufAllocator.DEFAULT);
        EncodedComplexOutput<byte[], byte[], TsInfoValue<byte[]>> output = new EncodedComplexOutput<>(ByteArrayCodec.INSTANCE,
                new TsInfoValueParser<>(ByteArrayCodec.INSTANCE));
        rsm.decode(Unpooled.wrappedBuffer(resp.getBytes(StandardCharsets.UTF_8)), output, e -> {
            throw new IllegalStateException(e);
        });
        TsInfoValue<byte[]> info = output.get();

        assertThat(info.getLabels()).containsExactlyEntriesOf(labels("température", "°C"));
        assertThat(info.getSourceKey()).isEqualTo("{ts}:src".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void nullDataThrows() {

        assertThatThrownBy(() -> new TsInfoValueParser<>(StringCodec.UTF8).parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsInfoValueParser<>(null)).isInstanceOf(IllegalArgumentException.class);
    }

}
