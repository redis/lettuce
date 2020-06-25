/*
 * Copyright 2011-2020 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A compressing/decompressing {@link RedisCodec} that wraps a typed {@link RedisCodec codec} and compresses values using GZIP
 * or Deflate. See {@link io.lettuce.core.codec.CompressionCodec.CompressionType} for supported compression types.
 *
 * @author Mark Paluch
 */
public abstract class CompressionCodec {

    private CompressionCodec() {
    }

    /**
     * A {@link RedisCodec} that compresses values from a delegating {@link RedisCodec}.
     *
     * @param delegate codec used for key-value encoding/decoding, must not be {@code null}.
     * @param compressionType the compression type, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return Value-compressing codec.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K, V> RedisCodec<K, V> valueCompressor(RedisCodec<K, V> delegate, CompressionType compressionType) {
        LettuceAssert.notNull(delegate, "RedisCodec must not be null");
        LettuceAssert.notNull(compressionType, "CompressionType must not be null");
        return (RedisCodec) new CompressingValueCodecWrapper((RedisCodec) delegate, compressionType);
    }

    private static class CompressingValueCodecWrapper implements RedisCodec<Object, Object> {

        private RedisCodec<Object, Object> delegate;

        private CompressionType compressionType;

        public CompressingValueCodecWrapper(RedisCodec<Object, Object> delegate, CompressionType compressionType) {
            this.delegate = delegate;
            this.compressionType = compressionType;
        }

        @Override
        public Object decodeKey(ByteBuffer bytes) {
            return delegate.decodeKey(bytes);
        }

        @Override
        public Object decodeValue(ByteBuffer bytes) {
            try {
                return delegate.decodeValue(decompress(bytes));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public ByteBuffer encodeKey(Object key) {
            return delegate.encodeKey(key);
        }

        @Override
        public ByteBuffer encodeValue(Object value) {
            try {
                return compress(delegate.encodeValue(value));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private ByteBuffer compress(ByteBuffer source) throws IOException {
            if (source.remaining() == 0) {
                return source;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(source.remaining() / 2);
            OutputStream compressor = null;

            try {
                try (ByteBufferInputStream sourceStream = new ByteBufferInputStream(source)) {
                    if (compressionType == CompressionType.GZIP) {
                        compressor = new GZIPOutputStream(outputStream);
                    }

                    if (compressionType == CompressionType.DEFLATE) {
                        compressor = new DeflaterOutputStream(outputStream);
                    }
                    copy(sourceStream, compressor);
                } finally {

                    if (compressor != null) {
                        compressor.close();
                    }
                }

                return ByteBuffer.wrap(outputStream.toByteArray());
            } finally {
                outputStream.close();
            }
        }

        private ByteBuffer decompress(ByteBuffer source) throws IOException {
            if (source.remaining() == 0) {
                return source;
            }

            InputStream decompressor = null;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(source.remaining() * 2);

            try {
                try (ByteBufferInputStream sourceStream = new ByteBufferInputStream(source);) {

                    if (compressionType == CompressionType.GZIP) {
                        decompressor = new GZIPInputStream(sourceStream);
                    }

                    if (compressionType == CompressionType.DEFLATE) {
                        decompressor = new InflaterInputStream(sourceStream);
                    }

                    copy(decompressor, outputStream);
                } finally {
                    if (decompressor != null) {
                        decompressor.close();
                    }
                }

                return ByteBuffer.wrap(outputStream.toByteArray());
            } finally {
                outputStream.close();
            }
        }

    }

    /**
     * Copies all bytes from the input stream to the output stream. Does not close or flush either stream.
     *
     * @param from the input stream to read from
     * @param to the output stream to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    private static long copy(InputStream from, OutputStream to) throws IOException {
        LettuceAssert.notNull(from, "From must not be null");
        LettuceAssert.notNull(to, "From must not be null");
        byte[] buf = new byte[4096];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public enum CompressionType {
        GZIP, DEFLATE;
    }

}
