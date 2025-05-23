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
import io.lettuce.core.vector.VectorMetadata;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VectorMetadataParser}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class VectorMetadataParserUnitTests {

    @Test
    void shouldParseVectorMetadata() {
        // Arrange
        List<Object> vinfoOutput = Arrays.asList("quant-type", "int8", "vector-dim", 300, "size", 3000000, "max-level", 12,
                "vset-uid", 1, "ef-construction", 200, "hnsw-max-node-uid", 3000000, "hnsw-m", 16, "attributes-count", 0,
                "projection-input-dim", 0);

        ArrayComplexData complexData = new ArrayComplexData(vinfoOutput.size());
        for (Object item : vinfoOutput) {
            complexData.storeObject(item);
        }

        // Act
        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getType()).isEqualTo(QuantizationType.Q8);
        assertThat(metadata.getDimensionality()).isEqualTo(300);
        assertThat(metadata.getSize()).isEqualTo(3000000);
        assertThat(metadata.getvSetUid()).isEqualTo(1);
        assertThat(metadata.getMaxNodeUid()).isEqualTo(3000000);
        assertThat(metadata.getMaxNodes()).isEqualTo(16);
        assertThat(metadata.getMaxLevel()).isEqualTo(12);
        assertThat(metadata.getProjectionInputDim()).isEqualTo(0);
        assertThat(metadata.getAttributesCount()).isEqualTo(0);
    }

    @Test
    void shouldParseVectorMetadataWithNoQuantization() {
        // Arrange
        List<Object> vinfoOutput = Arrays.asList("quant-type", "float32", "vector-dim", 512, "size", 500);

        ArrayComplexData complexData = new ArrayComplexData(vinfoOutput.size());
        for (Object item : vinfoOutput) {
            complexData.storeObject(item);
        }

        // Act
        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getType()).isEqualTo(QuantizationType.NO_QUANTIZATION);
        assertThat(metadata.getDimensionality()).isEqualTo(512);
        assertThat(metadata.getSize()).isEqualTo(500);
    }

    @Test
    void shouldHandleUnknownFields() {
        // Arrange
        List<Object> vinfoOutput = Arrays.asList("quant-type", "int8", "vector-dim", 300, "unknown-field", "unknown-value",
                "size", 3000000);

        ArrayComplexData complexData = new ArrayComplexData(vinfoOutput.size());
        for (Object item : vinfoOutput) {
            complexData.storeObject(item);
        }

        // Act
        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getType()).isEqualTo(QuantizationType.Q8);
        assertThat(metadata.getDimensionality()).isEqualTo(300);
        assertThat(metadata.getSize()).isEqualTo(3000000);
    }

    @Test
    void shouldHandleUnknownQuantizationType() {
        // Arrange
        List<Object> vinfoOutput = Arrays.asList("quant-type", "unknown", "vector-dim", 300, "size", 3000000);

        ArrayComplexData complexData = new ArrayComplexData(vinfoOutput.size());
        for (Object item : vinfoOutput) {
            complexData.storeObject(item);
        }

        // Act
        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(complexData);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getType()).isNull();
        assertThat(metadata.getDimensionality()).isEqualTo(300);
        assertThat(metadata.getSize()).isEqualTo(3000000);
    }

    @Test
    void shouldHandleNullInput() {
        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(null);
        assertThat(metadata).isNull();
    }

    @Test
    void shouldHandleEmptyInput() {
        // Arrange
        ArrayComplexData complexData = new ArrayComplexData(1);

        // Act & Assert
        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(complexData);
        assertThat(metadata).isNotNull();
        assertThat(metadata.getType()).isNull();
        assertThat(metadata.getDimensionality()).isNull();
        assertThat(metadata.getSize()).isNull();
        assertThat(metadata.getMaxNodeUid()).isNull();
        assertThat(metadata.getvSetUid()).isNull();
        assertThat(metadata.getMaxNodes()).isNull();
        assertThat(metadata.getProjectionInputDim()).isNull();
        assertThat(metadata.getAttributesCount()).isNull();
        assertThat(metadata.getMaxLevel()).isNull();
    }

    @Test
    void shouldHandleOddNumberOfElements() {
        // Arrange
        List<Object> vinfoOutput = Arrays.asList("quant-type", "int8", "vector-dim", 300, "size" // Missing value for "size"
        );

        ArrayComplexData complexData = new ArrayComplexData(vinfoOutput.size());
        for (Object item : vinfoOutput) {
            complexData.storeObject(item);
        }

        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(complexData);
        assertThat(metadata).isNotNull();
        assertThat(metadata.getType()).isNull();
        assertThat(metadata.getDimensionality()).isNull();
        assertThat(metadata.getSize()).isNull();
        assertThat(metadata.getMaxNodeUid()).isNull();
        assertThat(metadata.getvSetUid()).isNull();
        assertThat(metadata.getMaxNodes()).isNull();
        assertThat(metadata.getProjectionInputDim()).isNull();
        assertThat(metadata.getAttributesCount()).isNull();
        assertThat(metadata.getMaxLevel()).isNull();
    }

    @Test
    void shouldHandleNonListInput() {
        // Arrange
        MapComplexData complexData = new MapComplexData(2);
        complexData.storeObject("key");
        complexData.storeObject("value");

        VectorMetadata metadata = VectorMetadataParser.INSTANCE.parse(complexData);
        assertThat(metadata).isNotNull();
        assertThat(metadata.getType()).isNull();
        assertThat(metadata.getDimensionality()).isNull();
        assertThat(metadata.getSize()).isNull();
        assertThat(metadata.getMaxNodeUid()).isNull();
        assertThat(metadata.getvSetUid()).isNull();
        assertThat(metadata.getMaxNodes()).isNull();
        assertThat(metadata.getProjectionInputDim()).isNull();
        assertThat(metadata.getAttributesCount()).isNull();
        assertThat(metadata.getMaxLevel()).isNull();
    }

}
