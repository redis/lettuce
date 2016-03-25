package com.lambdaworks.redis.codec;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import com.google.common.io.ByteStreams;

/**
 * A compressing/decompressing {@link RedisCodec} that wraps a typed {@link RedisCodec codec} and compresses values using GZIP
 * or Deflate. See {@link com.lambdaworks.redis.codec.CompressionCodec.CompressionType} for supported compression types.
 * 
 * @author Mark Paluch
 */
public class CompressionCodec {

    /**
     * A {@link RedisCodec} that compresses values from a delegating {@link RedisCodec}.
     * 
     * @param delegate codec used for key-value encoding/decoding, must not be {@literal null}.
     * @param compressionType the compression type, must not be {@literal null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return Value-compressing codec.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K, V> RedisCodec<K, V> valueCompressor(RedisCodec<K, V> delegate, CompressionType compressionType) {
        checkArgument(delegate != null, "RedisCodec must not be null");
        checkArgument(compressionType != null, "CompressionType must not be null");
        return (RedisCodec) new CompressingValueCodecWrapper((RedisCodec) delegate, compressionType);
    }

    private static class CompressingValueCodecWrapper extends RedisCodec<Object, Object> {

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
        public byte[] encodeKey(Object key) {
            return delegate.encodeKey(key);
        }

        @Override
        public byte[] encodeValue(Object value) {
            try {
                return compress(delegate.encodeValue(value));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private byte[] compress(byte[] source) throws IOException {
            if (source == null || source.length == 0) {
                return source;
            }

            ByteArrayInputStream sourceStream = new ByteArrayInputStream(source);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(source.length / 2);
            OutputStream compressor = null;
            if (compressionType == CompressionType.GZIP) {
                compressor = new GZIPOutputStream(outputStream);
            }

            if (compressionType == CompressionType.DEFLATE) {
                compressor = new DeflaterOutputStream(outputStream);
            }

            try {
                ByteStreams.copy(sourceStream, compressor);
            } finally {
                compressor.close();
            }

            return outputStream.toByteArray();
        }

        private ByteBuffer decompress(ByteBuffer source) throws IOException {
            if (source.remaining() == 0) {
                return source;
            }

            ByteBufferInputStream sourceStream = new ByteBufferInputStream(source);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(source.remaining() * 2);
            InputStream decompressor = null;
            if (compressionType == CompressionType.GZIP) {
                decompressor = new GZIPInputStream(sourceStream);
            }

            if (compressionType == CompressionType.DEFLATE) {
                decompressor = new InflaterInputStream(sourceStream);
            }

            try {
                ByteStreams.copy(decompressor, outputStream);
            } finally {
                decompressor.close();
            }

            return ByteBuffer.wrap(outputStream.toByteArray());
        }

    }

    public enum CompressionType {
        GZIP, DEFLATE;
    }

}
