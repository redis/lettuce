/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.DefaultJsonParser;
import io.lettuce.core.json.JsonType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonTypeListOutput}.
 */
@Tag(UNIT_TEST)
class JsonTypeListOutputUnitTests {

    @Test
    void set() {
        JsonTypeListOutput<String, String> sut = new JsonTypeListOutput<>(StringCodec.UTF8);
        sut.multi(7);
        sut.set(ByteBuffer.wrap("object".getBytes()));
        sut.set(ByteBuffer.wrap("array".getBytes()));
        sut.set(ByteBuffer.wrap("string".getBytes()));
        sut.set(ByteBuffer.wrap("integer".getBytes()));
        sut.set(ByteBuffer.wrap("number".getBytes()));
        sut.set(ByteBuffer.wrap("boolean".getBytes()));
        sut.set(ByteBuffer.wrap("null".getBytes()));

        assertThat(sut.get().isEmpty()).isFalse();
        assertThat(sut.get().size()).isEqualTo(7);
        assertThat(sut.get().get(0)).isEqualTo(JsonType.OBJECT);
        assertThat(sut.get().get(1)).isEqualTo(JsonType.ARRAY);
        assertThat(sut.get().get(2)).isEqualTo(JsonType.STRING);
        assertThat(sut.get().get(3)).isEqualTo(JsonType.INTEGER);
        assertThat(sut.get().get(4)).isEqualTo(JsonType.NUMBER);
        assertThat(sut.get().get(5)).isEqualTo(JsonType.BOOLEAN);
        assertThat(sut.get().get(6)).isEqualTo(JsonType.UNKNOWN);
    }

}
