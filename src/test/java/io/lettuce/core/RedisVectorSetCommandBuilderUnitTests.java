/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.vector.RawVector;
import io.lettuce.core.vector.VectorMetadata;
import io.lettuce.core.vector.QuantizationType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisVectorSetCommandBuilder}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class RedisVectorSetCommandBuilderUnitTests {

    private static final String KEY = "vector:set";
    private static final String ELEMENT = "element1";
    private static final Double[] VECTORS = new Double[] { 0.1d, 0.2d, 0.3d };
    private static final String JSON = "{\"attribute\":\"value\"}";

    RedisVectorSetCommandBuilder<String, String> builder = new RedisVectorSetCommandBuilder<>(StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructVadd() {
        Command<String, String, Boolean> command = builder.vadd(KEY, ELEMENT, VECTORS);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*8\r\n" +
                "$4\r\n" + "VADD\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$6\r\n" + "VALUES\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$3\r\n" + "0.1\r\n" + 
                "$3\r\n" + "0.2\r\n" + 
                "$3\r\n" + "0.3\r\n" + 
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVaddWithOneVector() {
        Command<String, String, Boolean> command = builder.vadd(KEY, ELEMENT, new Double[] { 0.1d });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" +
                "$4\r\n" + "VADD\r\n" +
                "$10\r\n" + "vector:set\r\n" +
                "$3\r\n" + "0.1\r\n" +
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVaddWithDimensionality() {
        Command<String, String, Boolean> command = builder.vadd(KEY, 3, ELEMENT, VECTORS);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*10\r\n" +
                "$4\r\n" + "VADD\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$6\r\n" + "REDUCE\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$6\r\n" + "VALUES\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$3\r\n" + "0.1\r\n" + 
                "$3\r\n" + "0.2\r\n" + 
                "$3\r\n" + "0.3\r\n" + 
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVaddWithArgs() {
        VAddArgs args = new VAddArgs();
        args.checkAndSet(true);
        args.quantizationType(QuantizationType.NO_QUANTIZATION);
        args.explorationFactor(100L);
        args.attributes(JSON);
        args.maxNodes(100L);
        
        Command<String, String, Boolean> command = builder.vadd(KEY, ELEMENT, args, VECTORS);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*16\r\n" +
                "$4\r\n" + "VADD\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$6\r\n" + "VALUES\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$3\r\n" + "0.1\r\n" + 
                "$3\r\n" + "0.2\r\n" + 
                "$3\r\n" + "0.3\r\n" + 
                "$8\r\n" + "element1\r\n" + 
                "$3\r\n" + "CAS\r\n" + 
                "$7\r\n" + "NOQUANT\r\n" +
                "$2\r\n" + "EF\r\n" +
                "$3\r\n" + "100\r\n" +
                "$7\r\n" + "SETATTR\r\n" +
                "$21\r\n" + "{\"attribute\":\"value\"}\r\n"+
                "$1\r\n" + "M\r\n" +
                "$3\r\n" + "100\r\n");;
    }

    @Test
    void shouldCorrectlyConstructVcard() {
        Command<String, String, Long> command = builder.vcard(KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*2\r\n" + 
                "$5\r\n" + "VCARD\r\n" + 
                "$10\r\n" + "vector:set\r\n");
    }

    @Test
    void shouldCorrectlyConstructVdim() {
        Command<String, String, Long> command = builder.vdim(KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*2\r\n" + 
                "$4\r\n" + "VDIM\r\n" + 
                "$10\r\n" + "vector:set\r\n");
    }

    @Test
    void shouldCorrectlyConstructVemb() {
        Command<String, String, List<Double>> command = builder.vemb(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + 
                "$4\r\n" + "VEMB\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVembRaw() {
        Command<String, String, RawVector> command = builder.vembRaw(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + 
                "$4\r\n" + "VEMB\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$8\r\n" + "element1\r\n" + 
                "$3\r\n" + "RAW\r\n");
    }

    @Test
    void shouldCorrectlyConstructVgetattr() {
        Command<String, String, String> command = builder.vgetattr(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + 
                "$8\r\n" + "VGETATTR\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVinfo() {
        Command<String, String, VectorMetadata> command = builder.vinfo(KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*2\r\n" + 
                "$5\r\n" + "VINFO\r\n" + 
                "$10\r\n" + "vector:set\r\n");
    }

    @Test
    void shouldCorrectlyConstructVlinks() {
        Command<String, String, List<String>> command = builder.vlinks(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + 
                "$6\r\n" + "VLINKS\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVlinksWithScores() {
        Command<String, String, Map<String, Double>> command = builder.vlinksWithScores(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + 
                "$6\r\n" + "VLINKS\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$8\r\n" + "element1\r\n" + 
                "$10\r\n" + "WITHSCORES\r\n");
    }

    @Test
    void shouldCorrectlyConstructVrandmember() {
        Command<String, String, String> command = builder.vrandmember(KEY);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*2\r\n" + 
                "$11\r\n" + "VRANDMEMBER\r\n" +
                "$10\r\n" + "vector:set\r\n");
    }

    @Test
    void shouldCorrectlyConstructVrandmemberWithCount() {
        Command<String, String, List<String>> command = builder.vrandmember(KEY, 3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + 
                "$11\r\n" + "VRANDMEMBER\r\n" +
                "$10\r\n" + "vector:set\r\n" + 
                "$1\r\n" + "3\r\n");
    }

    @Test
    void shouldCorrectlyConstructVrem() {
        Command<String, String, Boolean> command = builder.vrem(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + 
                "$4\r\n" + "VREM\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVsetattr() {
        Command<String, String, Boolean> command = builder.vsetattr(KEY, ELEMENT, JSON);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + 
                "$8\r\n" + "VSETATTR\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$8\r\n" + "element1\r\n" + 
                "$21\r\n" + "{\"attribute\":\"value\"}\r\n");
    }

    @Test
    void shouldCorrectlyConstructVsim() {
        Command<String, String, List<String>> command = builder.vsim(KEY, VECTORS);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*7\r\n" +
                "$4\r\n" + "VSIM\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$6\r\n" + "VALUES\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$3\r\n" + "0.1\r\n" + 
                "$3\r\n" + "0.2\r\n" + 
                "$3\r\n" + "0.3\r\n");
    }

    @Test
    void shouldCorrectlyConstructVsimWithElement() {
        Command<String, String, List<String>> command = builder.vsim(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" +
                "$4\r\n" + "VSIM\r\n" + 
                "$10\r\n" + "vector:set\r\n" +
                "$3\r\n" + "ELE\r\n" +
                "$8\r\n" + "element1\r\n");
    }

    @Test
    void shouldCorrectlyConstructVsimWithArgs() {
        VSimArgs args = new VSimArgs();
        args.count(5L);
        args.explorationFactor(200L);
        
        Command<String, String, List<String>> command = builder.vsim(KEY, args, VECTORS);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*11\r\n" +
                "$4\r\n" + "VSIM\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$6\r\n" + "VALUES\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$3\r\n" + "0.1\r\n" + 
                "$3\r\n" + "0.2\r\n" + 
                "$3\r\n" + "0.3\r\n" + 
                "$5\r\n" + "COUNT\r\n" + 
                "$1\r\n" + "5\r\n" + 
                "$2\r\n" + "EF\r\n" + 
                "$3\r\n" + "200\r\n");
    }

    @Test
    void shouldCorrectlyConstructVsimWithScores() {
        Command<String, String, Map<String, Double>> command = builder.vsimWithScore(KEY, VECTORS);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*8\r\n" +
                "$4\r\n" + "VSIM\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$6\r\n" + "VALUES\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$3\r\n" + "0.1\r\n" + 
                "$3\r\n" + "0.2\r\n" + 
                "$3\r\n" + "0.3\r\n" + 
                "$10\r\n" + "WITHSCORES\r\n");
    }

    @Test
    void shouldCorrectlyConstructVsimWithElementAndScores() {
        Command<String, String, Map<String, Double>> command = builder.vsimWithScore(KEY, ELEMENT);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" +
                "$4\r\n" + "VSIM\r\n" + 
                "$10\r\n" + "vector:set\r\n" +
                "$3\r\n" + "ELE\r\n" +
                "$8\r\n" + "element1\r\n" + 
                "$10\r\n" + "WITHSCORES\r\n");
    }

    @Test
    void shouldCorrectlyConstructVsimWithArgsAndScores() {
        VSimArgs args = new VSimArgs();
        args.count(5L);
        args.truth(true);
        
        Command<String, String, Map<String, Double>> command = builder.vsimWithScore(KEY, args, VECTORS);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*11\r\n" +
                "$4\r\n" + "VSIM\r\n" + 
                "$10\r\n" + "vector:set\r\n" + 
                "$6\r\n" + "VALUES\r\n" + 
                "$1\r\n" + "3\r\n" + 
                "$3\r\n" + "0.1\r\n" + 
                "$3\r\n" + "0.2\r\n" + 
                "$3\r\n" + "0.3\r\n" + 
                "$10\r\n" + "WITHSCORES\r\n" + 
                "$5\r\n" + "COUNT\r\n" + 
                "$1\r\n" + "5\r\n" + 
                "$5\r\n" + "TRUTH\r\n");
    }
}
