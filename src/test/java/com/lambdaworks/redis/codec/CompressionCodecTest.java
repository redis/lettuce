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
        byte[] bytes = sut.encodeKey(value);
        assertThat(toString(bytes)).isEqualTo(value);

        String s = sut.decodeKey(ByteBuffer.wrap(bytes));
        assertThat(s).isEqualTo(value);
    }

    @Test
    public void gzipValueTest() throws Exception {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(new Utf8StringCodec(),
                CompressionCodec.CompressionType.GZIP);
        byte[] bytes = sut.encodeValue(key);
        assertThat(bytes).isEqualTo(keyGzipBytes);

        String s = sut.decodeValue(ByteBuffer.wrap(keyGzipBytes));
        assertThat(s).isEqualTo(key);
    }

    @Test
    public void deflateValueTest() throws Exception {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(new Utf8StringCodec(),
                CompressionCodec.CompressionType.DEFLATE);
        byte[] bytes = sut.encodeValue(key);
        assertThat(bytes).isEqualTo(keyDeflateBytes);

        String s = sut.decodeValue(ByteBuffer.wrap(keyDeflateBytes));
        assertThat(s).isEqualTo(key);
    }

    @Test(expected = IllegalStateException.class)
    public void wrongCompressionTypeOnDecode() throws Exception {
        RedisCodec<String, String> sut = CompressionCodec.valueCompressor(new Utf8StringCodec(),
                CompressionCodec.CompressionType.DEFLATE);

        sut.decodeValue(ByteBuffer.wrap(keyGzipBytes));
    }

    private String toString(byte[] bytes) throws IOException {
        return new String(bytes, "UTF-8");
    }
}
