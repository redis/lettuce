/*
 * Copyright 2026-present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link ComplexOutput} and {@link MapComplexData}.
 */
@Tag(UNIT_TEST)
class ComplexOutputUnitTests {

    @Test
    void resp3MapPreservesServerKeyOrder() {
        ComplexOutput<String, String, Map<Object, Object>> output = new ComplexOutput<>(StringCodec.UTF8,
                ComplexData::getDynamicMap);

        output.multiMap(4);
        output.set(ByteBuffer.wrap("zebra".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("alpha".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("2".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("middle".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("3".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("beta".getBytes(StandardCharsets.UTF_8)));
        output.set(ByteBuffer.wrap("4".getBytes(StandardCharsets.UTF_8)));

        Map<Object, Object> result = output.get();

        assertThat(result.keySet()).containsExactly("zebra", "alpha", "middle", "beta");
        assertThat(result.values()).containsExactly("1", "2", "3", "4");
    }

}
