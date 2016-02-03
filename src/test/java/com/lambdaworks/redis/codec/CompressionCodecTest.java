package com.lambdaworks.redis.codec;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class CompressionCodecTest {

    private String key = "key";
    private byte[] keyGzipBytes = new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0, -53, 78, -83, 4, 0, -87, -85, -112, -118, 3,
            0, 0, 0 };
    private byte[] keyDeflateBytes = new byte[] { 120, -100, -53, 78, -83, 4, 0, 2, -121, 1, 74 };
    private String value = "value";

    @Test
    public void keyPassthroughTest() throws Exception {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(new Utf8StringCodec(),
                CompressionCodec.CompressionType.GZIP);
        ByteBuffer byteBuffer = sut.encodeKey(value);
        assertThat(toString(byteBuffer.duplicate())).isEqualTo(value);

        String s = sut.decodeKey(byteBuffer);
        assertThat(s).isEqualTo(value);
    }

    @Test
    public void gzipValueTest() throws Exception {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(new Utf8StringCodec(),
                CompressionCodec.CompressionType.GZIP);
        ByteBuffer byteBuffer = sut.encodeValue(key);
        assertThat(toBytes(byteBuffer.duplicate())).isEqualTo(keyGzipBytes);

        String s = sut.decodeValue(ByteBuffer.wrap(keyGzipBytes));
        assertThat(s).isEqualTo(key);
    }

    @Test
    public void deflateValueTest() throws Exception {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(new Utf8StringCodec(),
                CompressionCodec.CompressionType.DEFLATE);
        ByteBuffer byteBuffer = sut.encodeValue(key);
        assertThat(toBytes(byteBuffer.duplicate())).isEqualTo(keyDeflateBytes);

        String s = sut.decodeValue(ByteBuffer.wrap(keyDeflateBytes));
        assertThat(s).isEqualTo(key);
    }

    @Test(expected = IllegalStateException.class)
    public void wrongCompressionTypeOnDecode() throws Exception {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(new Utf8StringCodec(),
                CompressionCodec.CompressionType.DEFLATE);

        sut.decodeValue(ByteBuffer.wrap(keyGzipBytes));
    }

    private String toString(ByteBuffer buffer) throws IOException {
        byte[] bytes = toBytes(buffer);
        return new String(bytes, "UTF-8");
    }

    private byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return bytes;
    }
}