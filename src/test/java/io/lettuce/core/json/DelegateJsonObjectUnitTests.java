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
 * Unit tests for {@link DelegateJsonObject}.
 */
@Tag(UNIT_TEST)
class DelegateJsonObjectUnitTests {

    @Test
    void put() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonObject underTest = new DelegateJsonObject();

        underTest.put("test", parser.createJsonValue("\"test\"")).put("test2", parser.createJsonValue("1")).put("test2",
                parser.createJsonValue("true"));

        assertThat(underTest.size()).isEqualTo(2);
        assertThat(underTest.get("test").asString()).isEqualTo("test");
        assertThat(underTest.get("test2").asBoolean()).isTrue();
    }

    @Test
    void remove() {
        DefaultJsonParser parser = new DefaultJsonParser();
        DelegateJsonObject underTest = new DelegateJsonObject();

        underTest.put("test", parser.createJsonValue("\"test\"")).put("test2", parser.createJsonValue("1")).remove("test");

        assertThat(underTest.size()).isEqualTo(1);
        assertThat(underTest.get("test")).isNull();
        assertThat(underTest.get("test2").asNumber()).isEqualTo(1);
    }

    @Test
    void isAnythingElse() {
        DelegateJsonObject underTest = new DelegateJsonObject();

        assertThat(underTest.isJsonObject()).isTrue();

        assertThat(underTest.isNull()).isFalse();
        assertThat(underTest.isBoolean()).isFalse();
        assertThat(underTest.isNumber()).isFalse();
        assertThat(underTest.isString()).isFalse();
        assertThat(underTest.isJsonArray()).isFalse();
    }

    @Test
    void asAnythingElse() {
        DelegateJsonObject underTest = new DelegateJsonObject();

        assertThat(underTest.asJsonObject()).isNotNull();

        assertThat(underTest.asBoolean()).isNull();
        assertThat(underTest.asJsonArray()).isNull();
        assertThat(underTest.asString()).isNull();
        assertThat(underTest.asNumber()).isNull();
    }

}
