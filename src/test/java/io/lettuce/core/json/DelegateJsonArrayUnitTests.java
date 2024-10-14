/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DelegateJsonArray}.
 */
@Tag(UNIT_TEST)
class DelegateJsonArrayUnitTests {

    @Test
    void add() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(parser.createJsonValue("\"test\"")).add(parser.createJsonValue("\"test2\""))
                .add(parser.createJsonValue("\"test3\""));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.get(0).isString()).isTrue();
        assertThat(underTest.get(0).asString()).isEqualTo("test");
        assertThat(underTest.get(1).isString()).isTrue();
        assertThat(underTest.get(1).asString()).isEqualTo("test2");
        assertThat(underTest.get(2).isString()).isTrue();
        assertThat(underTest.get(2).asString()).isEqualTo("test3");
    }

    @Test
    void addCornerCases() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(null).add(parser.createJsonValue("null")).add(parser.createJsonValue("\"test3\""));

        assertThatThrownBy(() -> underTest.addAll(null)).isInstanceOf(IllegalArgumentException.class);

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.get(0).isNull()).isTrue();
        assertThat(underTest.get(1).isNull()).isTrue();
        assertThat(underTest.get(2).isString()).isTrue();
        assertThat(underTest.get(2).asString()).isEqualTo("test3");
    }

    @Test
    void getCornerCases() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(parser.createJsonValue("\"test\"")).add(parser.createJsonValue("\"test2\""))
                .add(parser.createJsonValue("\"test3\""));

        assertThat(underTest.get(3)).isNull();
        assertThat(underTest.get(-1)).isNull();
    }

    @Test
    void addAll() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray array = new DelegateJsonArray();
        array.add(parser.createJsonValue("\"test\"")).add(parser.createJsonValue("\"test2\""))
                .add(parser.createJsonValue("\"test3\""));

        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.addAll(array);
        array.remove(1); // verify source array modifications not propagated

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.get(0).isString()).isTrue();
        assertThat(underTest.get(0).asString()).isEqualTo("test");
        assertThat(underTest.get(1).isString()).isTrue();
        assertThat(underTest.get(1).asString()).isEqualTo("test2");
        assertThat(underTest.get(2).isString()).isTrue();
        assertThat(underTest.get(2).asString()).isEqualTo("test3");
    }

    @Test
    void asList() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.asList()).hasSize(3);
        assertThat(underTest.asList().get(0).isNumber()).isTrue();
        assertThat(underTest.asList().get(0).asNumber()).isEqualTo(1);
    }

    @Test
    void getFirst() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(parser.createJsonValue("\"test\"")).add(parser.createJsonValue("\"test2\""))
                .add(parser.createJsonValue("\"test3\""));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.getFirst().isString()).isTrue();
        assertThat(underTest.getFirst().asString()).isEqualTo("test");
    }

    @Test
    void iterator() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));

        Iterator<JsonValue> iterator = underTest.iterator();
        assertThat(iterator.hasNext()).isTrue();
        while (iterator.hasNext()) {
            assertThat(iterator.next().isNumber()).isTrue();
        }
    }

    @Test
    void remove() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));

        assertThat(underTest.remove(1).asNumber()).isEqualTo(2);
        assertThat(underTest.size()).isEqualTo(2);
        assertThat(underTest.get(0).asNumber()).isEqualTo(1);
        assertThat(underTest.get(1).asNumber()).isEqualTo(3);
    }

    @Test
    void replace() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonArray underTest = new DelegateJsonArray();
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));
        underTest.replace(1, parser.createJsonValue("4"));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.get(0).asNumber()).isEqualTo(1);
        assertThat(underTest.get(1).asNumber()).isEqualTo(4);
        assertThat(underTest.get(2).asNumber()).isEqualTo(3);
    }

    @Test
    void isJsonArray() {
        DelegateJsonArray underTest = new DelegateJsonArray();
        assertThat(underTest.isJsonArray()).isTrue();

        assertThat(underTest.isJsonObject()).isFalse();
        assertThat(underTest.isNull()).isFalse();
        assertThat(underTest.isNumber()).isFalse();
        assertThat(underTest.isString()).isFalse();
    }

    @Test
    void asJsonArray() {
        DelegateJsonArray underTest = new DelegateJsonArray();
        assertThat(underTest.asJsonArray()).isSameAs(underTest);
    }

    @Test
    void asAnythingElse() {
        DelegateJsonArray underTest = new DelegateJsonArray();

        assertThat(underTest.asBoolean()).isNull();
        assertThat(underTest.asJsonObject()).isNull();
        assertThat(underTest.asString()).isNull();
        assertThat(underTest.asNumber()).isNull();
    }

}
