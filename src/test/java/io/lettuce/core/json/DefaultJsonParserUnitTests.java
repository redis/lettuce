/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for {@link DefaultJsonParser}.
 */
class DefaultJsonParserUnitTests {

    @Test
    void loadJsonValue() {
        final String unprocessed = "{\"a\":1,\"b\":2}";

        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue jsonValue = parser.loadJsonValue(ByteBuffer.wrap(unprocessed.getBytes()));

        assertThat(jsonValue).isNotNull();
        assertThat(jsonValue).isInstanceOf(UnproccessedJsonValue.class);
        assertThat(((UnproccessedJsonValue) jsonValue).isDeserialized()).isFalse();
    }

    @Test
    void createJsonValue() {
        final String unprocessed = "\"someValue\"";

        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue jsonValue = parser.createJsonValue(ByteBuffer.wrap(unprocessed.getBytes()));

        assertThat(jsonValue).isNotNull();
        assertThat(jsonValue.isString()).isTrue();
        assertThat(jsonValue.asString()).isEqualTo("someValue");
    }

    @Test
    void createJsonObject() {
        final String unprocessed = "{\"a\":1,\"b\":2}";

        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue jsonValue = parser.createJsonObject();

        assertThat(jsonValue).isNotNull();
        assertThat(jsonValue.isJsonObject()).isTrue();
        assertThat(jsonValue.asJsonObject().size()).isZero();

        parser = new DefaultJsonParser();
        jsonValue = parser.createJsonValue(ByteBuffer.wrap(unprocessed.getBytes()));

        assertThat(jsonValue).isNotNull();
        assertThat(jsonValue.isJsonObject()).isTrue();
        assertThat(jsonValue.asJsonObject().get("a").asNumber()).isEqualTo(1);
        assertThat(jsonValue.asJsonObject().get("b").asNumber()).isEqualTo(2);

        jsonValue = parser.createJsonValue(unprocessed);

        assertThat(jsonValue).isNotNull();
        assertThat(jsonValue.isJsonObject()).isTrue();
        assertThat(jsonValue.asJsonObject().get("a").asNumber()).isEqualTo(1);
        assertThat(jsonValue.asJsonObject().get("b").asNumber()).isEqualTo(2);
    }

    @Test
    void createJsonArray() {
        DefaultJsonParser parser = new DefaultJsonParser();
        JsonValue jsonValue = parser.createJsonArray();

        assertThat(jsonValue).isNotNull();
        assertThat(jsonValue.isJsonArray()).isTrue();
        assertThat(jsonValue.asJsonArray().size()).isZero();

        final String unprocessed = "[1,2]";

        jsonValue = parser.createJsonValue(ByteBuffer.wrap(unprocessed.getBytes()));

        assertThat(jsonValue).isNotNull();
        assertThat(jsonValue.isJsonArray()).isTrue();
        assertThat(jsonValue.asJsonArray().get(0).asNumber()).isEqualTo(1);
        assertThat(jsonValue.asJsonArray().get(1).asNumber()).isEqualTo(2);
    }

    @Test
    void parsingIssues() {
        final String unprocessed = "{a\":1,\"b\":2}";

        DefaultJsonParser parser = new DefaultJsonParser();

        assertThatThrownBy(() -> parser.createJsonValue(unprocessed)).isInstanceOf(RedisJsonException.class);
        assertThatThrownBy(() -> parser.createJsonValue(ByteBuffer.wrap(unprocessed.getBytes())))
                .isInstanceOf(RedisJsonException.class);

    }

}
