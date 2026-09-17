/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.output.ComplexData;

/**
 * Unit tests for {@link CMSInfoValueParser}.
 */
@Tag(UNIT_TEST)
class CMSInfoValueParserUnitTests {

    private final CMSInfoValueParser parser = CMSInfoValueParser.INSTANCE;

    private static ComplexData buildMapData(Object... pairs) {
        return new ComplexData() {

            @Override
            public void storeObject(Object value) {
                // not needed
            }

            @Override
            public Map<Object, Object> getDynamicMap() {
                Map<Object, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < pairs.length - 1; i += 2) {
                    map.put(pairs[i], pairs[i + 1]);
                }
                return map;
            }

        };
    }

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parseNullThrows() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CMS.INFO");
    }

    @Test
    void parseAllFieldsIncludingCellSize() {
        ComplexData data = buildMapData(buf("width"), 1000L, buf("depth"), 5L, buf("count"), 203L, buf("cell_size"), 1L);

        CMSInfoValue value = parser.parse(data);

        assertThat(value.getWidth()).isEqualTo(1000L);
        assertThat(value.getDepth()).isEqualTo(5L);
        assertThat(value.getCount()).isEqualTo(203L);
        assertThat(value.getCellSize()).isEqualTo(1L);
        assertThat(value.getRawInfo()).containsEntry("cell_size", 1L);
    }

    @Test
    void parseCellSizeWithSpaceSeparatedKey() {
        ComplexData data = buildMapData(buf("width"), 1L, buf("depth"), 2L, buf("count"), 0L, buf("cell size"), 2L);

        assertThat(parser.parse(data).getCellSize()).isEqualTo(2L);
    }

    @Test
    void cellSizeIsNullOnOlderServers() {
        ComplexData data = buildMapData(buf("width"), 2000L, buf("depth"), 5L, buf("count"), 5L);

        CMSInfoValue value = parser.parse(data);

        assertThat(value.getWidth()).isEqualTo(2000L);
        assertThat(value.getCellSize()).isNull();
    }

}
