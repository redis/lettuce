/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core.codec;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CompressionCodec}.
 *
 * @author Mark Paluch
 */
class CompressionCodecUnitTests {

    private String key = "key";
    private byte[] keyGzipBytes = new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0, -53, 78, -83, 4, 0, -87, -85, -112, -118, 3, 0,
            0, 0 };
    private byte[] keyDeflateBytes = new byte[] { 120, -100, -53, 78, -83, 4, 0, 2, -121, 1, 74 };
    private String value = "value";

    @Test
    void keyPassthroughTest() {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(StringCodec.UTF8,
                CompressionCodec.CompressionType.GZIP);
        ByteBuffer byteBuffer = sut.encodeKey(value);
        assertThat(toString(byteBuffer.duplicate())).isEqualTo(value);

        String s = sut.decodeKey(byteBuffer);
        assertThat(s).isEqualTo(value);
    }

    @Test
    void gzipValueTest() {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(StringCodec.UTF8,
                CompressionCodec.CompressionType.GZIP);
        ByteBuffer byteBuffer = sut.encodeValue(key);

        assertThat(sut.decodeValue(byteBuffer)).isEqualTo(key);
        assertThat(sut.decodeValue(ByteBuffer.wrap(keyGzipBytes))).isEqualTo(key);
    }

    @Test
    void deflateValueTest() {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(StringCodec.UTF8,
                CompressionCodec.CompressionType.DEFLATE);
        ByteBuffer byteBuffer = sut.encodeValue(key);

        assertThat(sut.decodeValue(byteBuffer)).isEqualTo(key);
        assertThat(sut.decodeValue(ByteBuffer.wrap(keyDeflateBytes))).isEqualTo(key);
    }

    @Test
    void wrongCompressionTypeOnDecode() {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(StringCodec.UTF8,
                CompressionCodec.CompressionType.DEFLATE);

        assertThatThrownBy(() -> sut.decodeValue(ByteBuffer.wrap(keyGzipBytes)))
                .isInstanceOf(IllegalStateException.class);
    }

    private String toString(ByteBuffer buffer) {
        byte[] bytes = toBytes(buffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
