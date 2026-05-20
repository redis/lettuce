/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.array.*;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisArrayCommandBuilder}.
 *
 * @author Aleksandar Todorov
 */
@Tag(UNIT_TEST)
class RedisArrayCommandBuilderUnitTests {

    private static final String KEY = "myarray";

    RedisArrayCommandBuilder<String, String> builder = new RedisArrayCommandBuilder<>(StringCodec.UTF8);

    private String encode(Command<?, ?, ?> command) {
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldCorrectlyConstructArset() {
        assertThat(encode(builder.arset(KEY, 0, "value")))
                .isEqualTo("*4\r\n$5\r\nARSET\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$5\r\nvalue\r\n");
    }

    @Test
    void shouldCorrectlyConstructArsetVarargs() {
        assertThat(encode(builder.arset(KEY, 0, "v1", "v2", "v3")))
                .isEqualTo("*6\r\n$5\r\nARSET\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$2\r\nv1\r\n$2\r\nv2\r\n$2\r\nv3\r\n");
    }

    @Test
    void shouldCorrectlyConstructArmset() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(0L, "v0");
        map.put(5L, "v5");
        assertThat(encode(builder.armset(KEY, map)))
                .isEqualTo("*6\r\n$6\r\nARMSET\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$2\r\nv0\r\n$1\r\n5\r\n$2\r\nv5\r\n");
    }

    @Test
    void shouldCorrectlyConstructArget() {
        assertThat(encode(builder.arget(KEY, 0))).isEqualTo("*3\r\n$5\r\nARGET\r\n$7\r\nmyarray\r\n$1\r\n0\r\n"); // ARGET = 5
                                                                                                                  // chars
    }

    @Test
    void shouldCorrectlyConstructArmget() {
        assertThat(encode(builder.armget(KEY, 0, 1, 5)))
                .isEqualTo("*5\r\n$6\r\nARMGET\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$1\r\n1\r\n$1\r\n5\r\n");
    }

    @Test
    void shouldCorrectlyConstructArdel() {
        assertThat(encode(builder.ardel(KEY, 0L))).isEqualTo("*3\r\n$5\r\nARDEL\r\n$7\r\nmyarray\r\n$1\r\n0\r\n");
    }

    @Test
    void shouldCorrectlyConstructArdelMultiple() {
        assertThat(encode(builder.ardel(KEY, new long[] { 0, 5, 100 })))
                .isEqualTo("*5\r\n$5\r\nARDEL\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$1\r\n5\r\n$3\r\n100\r\n");
    }

    @Test
    void shouldCorrectlyConstructArdelrangeSingle() {
        assertThat(encode(builder.ardelrange(KEY, 0, 10)))
                .isEqualTo("*4\r\n$10\r\nARDELRANGE\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$2\r\n10\r\n");
    }

    @Test
    void shouldCorrectlyConstructArdelrange() {
        assertThat(encode(builder.ardelrange(KEY, ArrayIndexRange.of(0, 10))))
                .isEqualTo("*4\r\n$10\r\nARDELRANGE\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$2\r\n10\r\n");
    }

    @Test
    void shouldCorrectlyConstructArdelrangeMultiple() {
        assertThat(encode(builder.ardelrange(KEY, ArrayIndexRange.of(0, 1), ArrayIndexRange.of(4, 5))))
                .isEqualTo("*6\r\n$10\r\nARDELRANGE\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$1\r\n1\r\n$1\r\n4\r\n$1\r\n5\r\n");
    }

    @Test
    void shouldCorrectlyConstructArlen() {
        assertThat(encode(builder.arlen(KEY))).isEqualTo("*2\r\n$5\r\nARLEN\r\n$7\r\nmyarray\r\n");
    }

    @Test
    void shouldCorrectlyConstructArcount() {
        assertThat(encode(builder.arcount(KEY))).isEqualTo("*2\r\n$7\r\nARCOUNT\r\n$7\r\nmyarray\r\n");
    }

    @Test
    void shouldCorrectlyConstructArgetrange() {
        assertThat(encode(builder.argetrange(KEY, 0, 10)))
                .isEqualTo("*4\r\n$10\r\nARGETRANGE\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$2\r\n10\r\n");
    }

    @Test
    void shouldCorrectlyConstructArscan() {
        assertThat(encode(builder.arscan(KEY, 0, 100)))
                .isEqualTo("*4\r\n$6\r\nARSCAN\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$3\r\n100\r\n");
    }

    @Test
    void shouldCorrectlyConstructArscanWithLimit() {
        assertThat(encode(builder.arscan(KEY, 0, 100, 50)))
                .isEqualTo("*6\r\n$6\r\nARSCAN\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$3\r\n100\r\n$5\r\nLIMIT\r\n$2\r\n50\r\n");
    }

    @Test
    void shouldCorrectlyConstructArnext() {
        assertThat(encode(builder.arnext(KEY))).isEqualTo("*2\r\n$6\r\nARNEXT\r\n$7\r\nmyarray\r\n");
    }

    @Test
    void shouldCorrectlyConstructArseek() {
        assertThat(encode(builder.arseek(KEY, 5))).isEqualTo("*3\r\n$6\r\nARSEEK\r\n$7\r\nmyarray\r\n$1\r\n5\r\n");
    }

    @Test
    void shouldCorrectlyConstructArlastitems() {
        assertThat(encode(builder.arlastitems(KEY, 10))).isEqualTo("*3\r\n$11\r\nARLASTITEMS\r\n$7\r\nmyarray\r\n$2\r\n10\r\n");
    }

    @Test
    void shouldCorrectlyConstructArlastitemsRev() {
        assertThat(encode(builder.arlastitems(KEY, 10, true)))
                .isEqualTo("*4\r\n$11\r\nARLASTITEMS\r\n$7\r\nmyarray\r\n$2\r\n10\r\n$3\r\nREV\r\n");
    }

    @Test
    void shouldCorrectlyConstructArgrepExact() {
        assertThat(encode(builder.argrep(KEY, ArGrepArgs.unbounded().exact("foo"))))
                .isEqualTo("*6\r\n$6\r\nARGREP\r\n$7\r\nmyarray\r\n$1\r\n-\r\n$1\r\n+\r\n$5\r\nEXACT\r\n$3\r\nfoo\r\n");
    }

    @Test
    void shouldCorrectlyConstructArgrepMultiple() {
        assertThat(encode(builder.argrep(KEY, ArGrepArgs.range(0, 100).re("p1").glob("p2").and().limit(10).nocase())))
                .isEqualTo(
                        "*12\r\n$6\r\nARGREP\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$3\r\n100\r\n$2\r\nRE\r\n$2\r\np1\r\n$4\r\nGLOB\r\n$2\r\np2\r\n$3\r\nAND\r\n$5\r\nLIMIT\r\n$2\r\n10\r\n$6\r\nNOCASE\r\n");
    }

    @Test
    void shouldCorrectlyConstructArgrepFrom() {
        assertThat(encode(builder.argrep(KEY, ArGrepArgs.from(5).exact("foo"))))
                .isEqualTo("*6\r\n$6\r\nARGREP\r\n$7\r\nmyarray\r\n$1\r\n5\r\n$1\r\n+\r\n$5\r\nEXACT\r\n$3\r\nfoo\r\n");
    }

    @Test
    void shouldCorrectlyConstructArgrepTo() {
        assertThat(encode(builder.argrep(KEY, ArGrepArgs.to(10).exact("foo"))))
                .isEqualTo("*6\r\n$6\r\nARGREP\r\n$7\r\nmyarray\r\n$1\r\n-\r\n$2\r\n10\r\n$5\r\nEXACT\r\n$3\r\nfoo\r\n");
    }

    @Test
    void shouldCorrectlyConstructArgrepWithValues() {
        assertThat(encode(builder.argrepWithValues(KEY, ArGrepArgs.unbounded().exact("foo")))).isEqualTo(
                "*7\r\n$6\r\nARGREP\r\n$7\r\nmyarray\r\n$1\r\n-\r\n$1\r\n+\r\n$5\r\nEXACT\r\n$3\r\nfoo\r\n$10\r\nWITHVALUES\r\n");
    }

    @Test
    void shouldCorrectlyConstructAropSum() {
        assertThat(encode(builder.aropAggregate(KEY, 0, 100, ArAggregateType.SUM)))
                .isEqualTo("*5\r\n$4\r\nAROP\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$3\r\n100\r\n$3\r\nSUM\r\n");
    }

    @Test
    void shouldCorrectlyConstructAropBitwise() {
        assertThat(encode(builder.aropBitwise(KEY, 0, 100, ArBitwiseType.AND)))
                .isEqualTo("*5\r\n$4\r\nAROP\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$3\r\n100\r\n$3\r\nAND\r\n");
    }

    @Test
    void shouldCorrectlyConstructAropCount() {
        assertThat(encode(builder.aropCount(KEY, 0, 100)))
                .isEqualTo("*5\r\n$4\r\nAROP\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$3\r\n100\r\n$4\r\nUSED\r\n");
    }

    @Test
    void shouldCorrectlyConstructAropCountMatch() {
        assertThat(encode(builder.aropCount(KEY, 0, 100, "hello")))
                .isEqualTo("*6\r\n$4\r\nAROP\r\n$7\r\nmyarray\r\n$1\r\n0\r\n$3\r\n100\r\n$5\r\nMATCH\r\n$5\r\nhello\r\n");
    }

    @Test
    void shouldCorrectlyConstructArinsertSingle() {
        assertThat(encode(builder.arinsert(KEY, "v1"))).isEqualTo("*3\r\n$8\r\nARINSERT\r\n$7\r\nmyarray\r\n$2\r\nv1\r\n");
    }

    @Test
    void shouldCorrectlyConstructArinsert() {
        assertThat(encode(builder.arinsert(KEY, "v1", "v2", "v3")))
                .isEqualTo("*5\r\n$8\r\nARINSERT\r\n$7\r\nmyarray\r\n$2\r\nv1\r\n$2\r\nv2\r\n$2\r\nv3\r\n");
    }

    @Test
    void shouldCorrectlyConstructArringSingle() {
        assertThat(encode(builder.arring(KEY, 10, "v1")))
                .isEqualTo("*4\r\n$6\r\nARRING\r\n$7\r\nmyarray\r\n$2\r\n10\r\n$2\r\nv1\r\n");
    }

    @Test
    void shouldCorrectlyConstructArring() {
        assertThat(encode(builder.arring(KEY, 10, "v1", "v2")))
                .isEqualTo("*5\r\n$6\r\nARRING\r\n$7\r\nmyarray\r\n$2\r\n10\r\n$2\r\nv1\r\n$2\r\nv2\r\n");
    }

    @Test
    void shouldCorrectlyConstructArinfo() {
        assertThat(encode(builder.arinfo(KEY))).isEqualTo("*2\r\n$6\r\nARINFO\r\n$7\r\nmyarray\r\n");
    }

    @Test
    void shouldCorrectlyConstructArinfoFull() {
        assertThat(encode(builder.arinfoFull(KEY))).isEqualTo("*3\r\n$6\r\nARINFO\r\n$7\r\nmyarray\r\n$4\r\nFULL\r\n");
    }

}
