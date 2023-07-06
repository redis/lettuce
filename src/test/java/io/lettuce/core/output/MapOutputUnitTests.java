/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link MapOutput}.
 *
 * @author Mark Paluch
 */
class MapOutputUnitTests {

    @Test
    void shouldAcceptValue() {

        MapOutput<String, String> sut = new MapOutput<>(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(ByteBuffer.wrap("world".getBytes()));

        assertThat(sut.get()).containsEntry("hello", "world");
    }

    @Test
    void shouldAcceptBoolean() {

        MapOutput<String, Object> sut = new MapOutput(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(true);

        assertThat(sut.get()).containsEntry("hello", true);
    }

    @Test
    void shouldAcceptDouble() {

        MapOutput<String, Object> sut = new MapOutput(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(1.2);

        assertThat(sut.get()).containsEntry("hello", 1.2);
    }

    @Test
    void shouldAcceptInteger() {

        MapOutput<String, Object> sut = new MapOutput(StringCodec.UTF8);
        sut.multi(2);
        sut.set(ByteBuffer.wrap("hello".getBytes()));
        sut.set(1L);

        assertThat(sut.get()).containsEntry("hello", 1L);
    }

}
