package com.lambdaworks.redis.codec;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.lambdaworks.redis.protocol.LettuceCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Mark Paluch
 */
public class StringCodecTest {

    String teststring = "hello üäü~∑†®†ª€∂‚¶¢ Wørld";
    String teststringPlain = "hello uufadsfasdfadssdfadfs";

    @Test
    public void encodeUtf8Buf() throws Exception {

        StringCodec codec = new StringCodec(LettuceCharsets.UTF8);

        ByteBuf buffer = Unpooled.buffer(1234);
        codec.encode(teststring, buffer);

        assertThat(buffer.toString(StandardCharsets.UTF_8)).isEqualTo(teststring);
    }

    @Test
    public void encodeAsciiBuf() throws Exception {

        StringCodec codec = new StringCodec(LettuceCharsets.ASCII);

        ByteBuf buffer = Unpooled.buffer(1234);
        codec.encode(teststringPlain, buffer);

        assertThat(buffer.toString(LettuceCharsets.ASCII)).isEqualTo(teststringPlain);
    }

    @Test
    public void encodeIso88591Buf() throws Exception {

        StringCodec codec = new StringCodec(StandardCharsets.ISO_8859_1);

        ByteBuf buffer = Unpooled.buffer(1234);
        codec.encodeValue(teststringPlain, buffer);

        assertThat(buffer.toString(StandardCharsets.ISO_8859_1)).isEqualTo(teststringPlain);
    }

    @Test
    public void encodeAndDecodeUtf8Buf() throws Exception {

        StringCodec codec = new StringCodec(LettuceCharsets.UTF8);

        ByteBuf buffer = Unpooled.buffer(1234);
        codec.encodeKey(teststring, buffer);

        assertThat(codec.decodeKey(buffer.nioBuffer())).isEqualTo(teststring);
    }

    @Test
    public void encodeAndDecodeUtf8() throws Exception {

        StringCodec codec = new StringCodec(LettuceCharsets.UTF8);
        ByteBuffer byteBuffer = codec.encodeKey(teststring);

        assertThat(codec.decodeKey(byteBuffer)).isEqualTo(teststring);
    }

    @Test
    public void encodeAndDecodeAsciiBuf() throws Exception {

        StringCodec codec = new StringCodec(LettuceCharsets.ASCII);

        ByteBuf buffer = Unpooled.buffer(1234);
        codec.encode(teststringPlain, buffer);

        assertThat(codec.decodeKey(buffer.nioBuffer())).isEqualTo(teststringPlain);
    }

    @Test
    public void encodeAndDecodeIso88591Buf() throws Exception {

        StringCodec codec = new StringCodec(StandardCharsets.ISO_8859_1);

        ByteBuf buffer = Unpooled.buffer(1234);
        codec.encode(teststringPlain, buffer);

        assertThat(codec.decodeKey(buffer.nioBuffer())).isEqualTo(teststringPlain);
    }

    @Test
    public void estimateSize() throws Exception {

        assertThat(new StringCodec(LettuceCharsets.UTF8).estimateSize(teststring)).isEqualTo((int) (teststring.length() * 1.1));
        assertThat(new StringCodec(LettuceCharsets.ASCII).estimateSize(teststring)).isEqualTo(teststring.length());
        assertThat(new StringCodec(StandardCharsets.ISO_8859_1).estimateSize(teststring)).isEqualTo(teststring.length());
    }
}