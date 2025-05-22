/*
 * Copyright 2024-2025, Redis Ltd. and Contributors
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
package io.lettuce.core.output;

import io.lettuce.core.vector.QuantizationType;
import io.lettuce.core.vector.RawVector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RawVectorParser}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class RawVectorParserUnitTests {

    @Test
    void shouldParseQ8RawVector() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(4);
        complexData.store("int8");
        complexData.store(String.valueOf(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 })));
        complexData.store(3.14);
        complexData.store(0.5);

        // Act
        RawVector rawVector = RawVectorParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(rawVector).isNotNull();
        assertThat(rawVector.getType()).isEqualTo(QuantizationType.Q8);
        assertThat(rawVector.getVector()).isNotNull();
        assertThat(rawVector.getVector().remaining()).isEqualTo(42);
        assertThat(rawVector.beforeNormalization()).isEqualTo(3.14);
        assertThat(rawVector.getQuantizationRange()).isEqualTo(0.5);
    }

    @Test
    void shouldParseNoQuantizationRawVector() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(3);
        complexData.store("float32");
        complexData.store(String.valueOf(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 })));
        complexData.store(2.71);

        // Act
        RawVector rawVector = RawVectorParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(rawVector).isNotNull();
        assertThat(rawVector.getType()).isEqualTo(QuantizationType.NO_QUANTIZATION);
        assertThat(rawVector.getVector()).isNotNull();
        assertThat(rawVector.getVector().remaining()).isEqualTo(42);
        assertThat(rawVector.beforeNormalization()).isEqualTo(2.71);
        assertThat(rawVector.getQuantizationRange()).isNull();
    }

    @Test
    void shouldParseBinaryRawVector() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(3);
        complexData.store("binary");
        complexData.store(String.valueOf(ByteBuffer.wrap(new byte[] { 1, 0, 1, 0, 1 })));
        complexData.store(1.0);

        // Act
        RawVector rawVector = RawVectorParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(rawVector).isNotNull();
        assertThat(rawVector.getType()).isEqualTo(QuantizationType.BINARY);
        assertThat(rawVector.getVector()).isNotNull();
        assertThat(rawVector.getVector().remaining()).isEqualTo(42);
        assertThat(rawVector.beforeNormalization()).isEqualTo(1.0);
        assertThat(rawVector.getQuantizationRange()).isNull();
    }

    @Test
    void shouldParseRawVectorWithStringValues() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(4);
        complexData.store("int8");
        complexData.store("binary data");
        complexData.store("3.14");
        complexData.store("0.5");

        // Act
        RawVector rawVector = RawVectorParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(rawVector).isNotNull();
        assertThat(rawVector.getType()).isEqualTo(QuantizationType.Q8);
        assertThat(rawVector.getVector()).isNotNull();
        assertThat(rawVector.getVector().remaining()).isEqualTo("binary data".getBytes(StandardCharsets.UTF_8).length);
        assertThat(rawVector.beforeNormalization()).isEqualTo(3.14);
        assertThat(rawVector.getQuantizationRange()).isEqualTo(0.5);
    }

    @Test
    void shouldHandleUnknownQuantizationType() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(3);
        complexData.store("unknown");
        complexData.store(String.valueOf(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 })));
        complexData.store(1.0);

        // Act
        RawVector rawVector = RawVectorParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(rawVector).isNotNull();
        assertThat(rawVector.getType()).isNull();
        assertThat(rawVector.getVector()).isNotNull();
        assertThat(rawVector.beforeNormalization()).isEqualTo(1.0);
        assertThat(rawVector.getQuantizationRange()).isNull();
    }

    @Test
    void shouldThrowExceptionForNullInput() {
        // Act & Assert
        assertThatThrownBy(() -> RawVectorParser.INSTANCE.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vembOutput must not be null");
    }

    @Test
    void shouldThrowExceptionForEmptyInput() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(1);

        // Act & Assert
        assertThatThrownBy(() -> RawVectorParser.INSTANCE.parse(complexData)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data must not be null or empty");
    }

    @Test
    void shouldThrowExceptionForInsufficientElements() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(2);
        complexData.store("int8");
        complexData.store(String.valueOf(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 })));

        // Act & Assert
        assertThatThrownBy(() -> RawVectorParser.INSTANCE.parse(complexData)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data must contain at least 3 elements");
    }

    @Test
    void shouldThrowExceptionForNonListInput() {
        // Arrange
        MapComplexData complexData = new MapComplexData(2);
        complexData.storeObject("key");
        complexData.storeObject("value");

        // Act & Assert
        assertThatThrownBy(() -> RawVectorParser.INSTANCE.parse(complexData)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vembOutput must be a list");
    }

}
