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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link DelegateJsonArray}.
 */
@Tag(UNIT_TEST)
class DelegateJsonArrayUnitTests {

    @Test
    void add() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
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
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
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
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        underTest.add(parser.createJsonValue("\"test\"")).add(parser.createJsonValue("\"test2\""))
                .add(parser.createJsonValue("\"test3\""));

        assertThat(underTest.get(3)).isNull();
        assertThat(underTest.get(-1)).isNull();
    }

    @Test
    void addAll() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray array = new DelegateJsonArray(objectMapper);
        array.add(parser.createJsonValue("\"test\"")).add(parser.createJsonValue("\"test2\""))
                .add(parser.createJsonValue("\"test3\""));

        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
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
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.asList()).hasSize(3);
        assertThat(underTest.asList().get(0).isNumber()).isTrue();
        assertThat(underTest.asList().get(0).asNumber()).isEqualTo(1);
    }

    @Test
    void getFirst() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        underTest.add(parser.createJsonValue("\"test\"")).add(parser.createJsonValue("\"test2\""))
                .add(parser.createJsonValue("\"test3\""));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.getFirst().isString()).isTrue();
        assertThat(underTest.getFirst().asString()).isEqualTo("test");
    }

    @Test
    void iterator() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));

        Iterator<JsonValue> iterator = underTest.iterator();
        assertThat(iterator.hasNext()).isTrue();
        while (iterator.hasNext()) {
            assertThat(iterator.next().isNumber()).isTrue();
        }
    }

    @Test
    void remove() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));

        assertThat(underTest.remove(1).asNumber()).isEqualTo(2);
        assertThat(underTest.size()).isEqualTo(2);
        assertThat(underTest.get(0).asNumber()).isEqualTo(1);
        assertThat(underTest.get(1).asNumber()).isEqualTo(3);
    }

    @Test
    void replace() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2")).add(parser.createJsonValue("3"));
        JsonValue oldValue = underTest.replace(1, parser.createJsonValue("4"));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.get(0).asNumber()).isEqualTo(1);
        assertThat(underTest.get(1).asNumber()).isEqualTo(4);
        assertThat(underTest.get(2).asNumber()).isEqualTo(3);
        assertThat(oldValue.asNumber()).isEqualTo(2);
    }

    @Test
    void swap() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJsonParser parser = new DefaultJsonParser(objectMapper);
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        JsonArray swap = underTest.add(parser.createJsonValue("1")).add(parser.createJsonValue("2"))
                .add(parser.createJsonValue("3")).swap(1, parser.createJsonValue("4"));

        assertThat(underTest.size()).isEqualTo(3);
        assertThat(underTest.get(0).asNumber()).isEqualTo(1);
        assertThat(underTest.get(1).asNumber()).isEqualTo(4);
        assertThat(underTest.get(2).asNumber()).isEqualTo(3);
        assertThat(swap).isSameAs(underTest);
    }

    @Test
    void isJsonArray() {
        ObjectMapper objectMapper = new ObjectMapper();
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        assertThat(underTest.isJsonArray()).isTrue();

        assertThat(underTest.isJsonObject()).isFalse();
        assertThat(underTest.isNull()).isFalse();
        assertThat(underTest.isNumber()).isFalse();
        assertThat(underTest.isString()).isFalse();
    }

    @Test
    void asJsonArray() {
        ObjectMapper objectMapper = new ObjectMapper();
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);
        assertThat(underTest.asJsonArray()).isSameAs(underTest);
    }

    @Test
    void asAnythingElse() {
        ObjectMapper objectMapper = new ObjectMapper();
        DelegateJsonArray underTest = new DelegateJsonArray(objectMapper);

        assertThat(underTest.asBoolean()).isNull();
        assertThat(underTest.asJsonObject()).isNull();
        assertThat(underTest.asString()).isNull();
        assertThat(underTest.asNumber()).isNull();
    }

}
