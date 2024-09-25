/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lettuce.core.json;

import io.lettuce.core.codec.StringCodec;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for {@link UnproccessedJsonValue}.
 */
class UnproccessedJsonValueUnitTests {

    @Test
    void asString() {
        final String unprocessed = "{\"a\":1,\"b\":2}";
        final String modified = "{\"a\":1}";

        DefaultJsonParser parser = DefaultJsonParser.INSTANCE;
        ByteBuffer buffer = ByteBuffer.wrap(unprocessed.getBytes());
        UnproccessedJsonValue underTest = new UnproccessedJsonValue(buffer, parser);

        String value = StringCodec.UTF8.decodeValue(buffer);
        assertThat(underTest.toString()).isEqualTo(value);
        assertThat(underTest.asByteBuffer()).isEqualTo(ByteBuffer.wrap(unprocessed.getBytes()));

        assertThat(underTest.isJsonObject()).isTrue();
        assertThat(underTest.asJsonObject().remove("b")).isNotNull();

        assertThat(underTest.toString()).isEqualTo(modified);
        assertThat(underTest.asByteBuffer()).isEqualTo(ByteBuffer.wrap(modified.getBytes()));
    }

    @Test
    void asTextual() {
        DefaultJsonParser parser = DefaultJsonParser.INSTANCE;
        ByteBuffer buffer = ByteBuffer.wrap("\"textual\"".getBytes());
        UnproccessedJsonValue underTest = new UnproccessedJsonValue(buffer, parser);

        assertThat(underTest.isString()).isTrue();
        assertThat(underTest.asString()).isEqualTo("textual");

        Assertions.assertThat(underTest.isBoolean()).isFalse();
        Assertions.assertThat(underTest.isNull()).isFalse();
        Assertions.assertThat(underTest.isNumber()).isFalse();
        Assertions.assertThat(underTest.isJsonObject()).isFalse();
        Assertions.assertThat(underTest.isJsonArray()).isFalse();
    }

    @Test
    void asNull() {

        DefaultJsonParser parser = DefaultJsonParser.INSTANCE;
        ByteBuffer buffer = ByteBuffer.wrap("null".getBytes());
        UnproccessedJsonValue underTest = new UnproccessedJsonValue(buffer, parser);

        assertThat(underTest.isNull()).isTrue();

        Assertions.assertThat(underTest.isBoolean()).isFalse();
        Assertions.assertThat(underTest.isString()).isFalse();
        Assertions.assertThat(underTest.isNumber()).isFalse();
        Assertions.assertThat(underTest.isJsonObject()).isFalse();
        Assertions.assertThat(underTest.isJsonArray()).isFalse();
    }

    @Test
    void asNumber() {

        DefaultJsonParser parser = DefaultJsonParser.INSTANCE;
        ByteBuffer buffer = ByteBuffer.wrap("1".getBytes());
        UnproccessedJsonValue underTest = new UnproccessedJsonValue(buffer, parser);

        assertThat(underTest.isNumber()).isTrue();
        assertThat(underTest.asNumber()).isEqualTo(1);

        Assertions.assertThat(underTest.isBoolean()).isFalse();
        Assertions.assertThat(underTest.isString()).isFalse();
        Assertions.assertThat(underTest.isNull()).isFalse();
        Assertions.assertThat(underTest.isJsonObject()).isFalse();
        Assertions.assertThat(underTest.isJsonArray()).isFalse();
    }

    @Test
    void asBoolean() {

        DefaultJsonParser parser = DefaultJsonParser.INSTANCE;
        ByteBuffer buffer = ByteBuffer.wrap("true".getBytes());
        UnproccessedJsonValue underTest = new UnproccessedJsonValue(buffer, parser);

        assertThat(underTest.isBoolean()).isTrue();
        assertThat(underTest.asBoolean()).isEqualTo(true);

        Assertions.assertThat(underTest.isNumber()).isFalse();
        Assertions.assertThat(underTest.isString()).isFalse();
        Assertions.assertThat(underTest.isNull()).isFalse();
        Assertions.assertThat(underTest.isJsonObject()).isFalse();
        Assertions.assertThat(underTest.isJsonArray()).isFalse();
    }

    @Test
    void asArray() {

        DefaultJsonParser parser = DefaultJsonParser.INSTANCE;
        ByteBuffer buffer = ByteBuffer.wrap("[1,2,3,4]".getBytes());
        UnproccessedJsonValue underTest = new UnproccessedJsonValue(buffer, parser);

        assertThat(underTest.isJsonArray()).isTrue();
        assertThat(underTest.asJsonArray().toString()).isEqualTo("[1,2,3,4]");

        Assertions.assertThat(underTest.isNumber()).isFalse();
        Assertions.assertThat(underTest.isString()).isFalse();
        Assertions.assertThat(underTest.isNull()).isFalse();
        Assertions.assertThat(underTest.isJsonObject()).isFalse();
        Assertions.assertThat(underTest.isBoolean()).isFalse();
    }

}
