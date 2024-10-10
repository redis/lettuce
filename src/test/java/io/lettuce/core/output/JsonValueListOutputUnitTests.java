/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.DefaultJsonParser;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JsonValueListOutput}.
 */
class JsonValueListOutputUnitTests {

    @Test
    void set() {
        JsonValueListOutput<String, String> sut = new JsonValueListOutput<>(StringCodec.UTF8, new DefaultJsonParser());
        sut.multi(2);
        sut.set(ByteBuffer.wrap("[1,2,3]".getBytes()));
        sut.set(ByteBuffer.wrap("world".getBytes()));

        assertThat(sut.get().isEmpty()).isFalse();
        assertThat(sut.get().size()).isEqualTo(2);
        assertThat(sut.get().get(0).toString()).isEqualTo("[1,2,3]");
        assertThat(sut.get().get(1).toString()).isEqualTo("world");
    }

}
