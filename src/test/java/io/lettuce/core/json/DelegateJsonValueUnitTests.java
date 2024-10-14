/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DelegateJsonValue}.
 */
@Tag(UNIT_TEST)
class DelegateJsonValueUnitTests {

    @Test
    void testString() {
        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue underTest = parser.createJsonValue("\"test\"");

        assertThat(underTest.toString()).isEqualTo("\"test\"");
        assertThat(underTest.asByteBuffer().array()).isEqualTo("\"test\"".getBytes());

        assertThat(underTest.isJsonArray()).isFalse();
        assertThat(underTest.asJsonArray()).isNull();

        assertThat(underTest.isJsonObject()).isFalse();
        assertThat(underTest.asJsonObject()).isNull();

        assertThat(underTest.isString()).isTrue();
        assertThat(underTest.asString()).isEqualTo("test");

        assertThat(underTest.isNumber()).isFalse();
        assertThat(underTest.asNumber()).isNull();

        assertThat(underTest.isBoolean()).isFalse();
        assertThat(underTest.asBoolean()).isNull();

        assertThat(underTest.isNull()).isFalse();
    }

    @Test
    void testNumber() {
        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue underTest = parser.createJsonValue("1");

        assertThat(underTest.toString()).isEqualTo("1");
        assertThat(underTest.asByteBuffer().array()).isEqualTo("1".getBytes());

        assertThat(underTest.isJsonArray()).isFalse();
        assertThat(underTest.asJsonArray()).isNull();

        assertThat(underTest.isJsonObject()).isFalse();
        assertThat(underTest.asJsonObject()).isNull();

        assertThat(underTest.isNumber()).isTrue();
        assertThat(underTest.asNumber()).isEqualTo(1);

        assertThat(underTest.isString()).isFalse();
        assertThat(underTest.asString()).isNull();

        assertThat(underTest.isBoolean()).isFalse();
        assertThat(underTest.asBoolean()).isNull();

        assertThat(underTest.isNull()).isFalse();
    }

    @Test
    void testNumberExtended() {
        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue underTest = parser.createJsonValue("1");

        assertThat(underTest.isNumber()).isTrue();
        assertThat(underTest.asNumber()).isEqualTo(1);
        assertThat(underTest.asNumber()).isInstanceOf(Integer.class);

        underTest = parser.createJsonValue(String.valueOf(Long.MAX_VALUE));

        assertThat(underTest.isNumber()).isTrue();
        assertThat(underTest.asNumber()).isEqualTo(Long.MAX_VALUE);
        assertThat(underTest.asNumber()).isInstanceOf(Long.class);

        underTest = parser.createJsonValue(String.valueOf(Double.MAX_VALUE));

        assertThat(underTest.isNumber()).isTrue();
        assertThat(underTest.asNumber()).isEqualTo(Double.MAX_VALUE);
        assertThat(underTest.asNumber()).isInstanceOf(Double.class);
    }

    @Test
    void testBoolean() {
        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue underTest = parser.createJsonValue("true");

        assertThat(underTest.toString()).isEqualTo("true");
        assertThat(underTest.asByteBuffer().array()).isEqualTo("true".getBytes());

        assertThat(underTest.isJsonArray()).isFalse();
        assertThat(underTest.asJsonArray()).isNull();

        assertThat(underTest.isJsonObject()).isFalse();
        assertThat(underTest.asJsonObject()).isNull();

        assertThat(underTest.isBoolean()).isTrue();
        assertThat(underTest.asBoolean()).isTrue();

        assertThat(underTest.isString()).isFalse();
        assertThat(underTest.asString()).isNull();

        assertThat(underTest.isNumber()).isFalse();
        assertThat(underTest.asNumber()).isNull();

        assertThat(underTest.isNull()).isFalse();
    }

    @Test
    void testNull() {
        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue underTest = parser.createJsonValue("null");

        assertThat(underTest.toString()).isEqualTo("null");
        assertThat(underTest.asByteBuffer().array()).isEqualTo("null".getBytes());

        assertThat(underTest.isJsonArray()).isFalse();
        assertThat(underTest.asJsonArray()).isNull();

        assertThat(underTest.isJsonObject()).isFalse();
        assertThat(underTest.asJsonObject()).isNull();

        assertThat(underTest.isNumber()).isFalse();
        assertThat(underTest.asNumber()).isNull();

        assertThat(underTest.isString()).isFalse();
        assertThat(underTest.asString()).isNull();

        assertThat(underTest.isBoolean()).isFalse();
        assertThat(underTest.asBoolean()).isNull();

        assertThat(underTest.isNull()).isTrue();
    }

}
