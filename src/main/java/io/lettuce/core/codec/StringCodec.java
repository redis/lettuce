/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.LettuceCharsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

/**
 * Optimized String codec. This {@link RedisCodec} encodes and decodes {@link String} keys and values using a specified
 * {@link Charset}. It accepts provided {@link ByteBuf buffers} so it does not need to allocate buffers during encoding.
 *
 * @author Mark Paluch
 * @since 4.3
 */
public class StringCodec implements RedisCodec<String, String>, ToByteBufEncoder<String, String> {

    public static final StringCodec UTF8 = new StringCodec(LettuceCharsets.UTF8);
    public static final StringCodec ASCII = new StringCodec(LettuceCharsets.ASCII);

    private static final byte[] EMPTY = new byte[0];

    private final Charset charset;
    private final boolean ascii;
    private final boolean utf8;

    /**
     * Creates a new {@link StringCodec} with the default {@link Charset#defaultCharset() charset}. The default is determined
     * from the {@code file.encoding} system property.
     */
    public StringCodec() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a new {@link StringCodec} for the given {@link Charset} that encodes and decodes keys and values.
     *
     * @param charset must not be {@literal null}.
     */
    public StringCodec(Charset charset) {

        LettuceAssert.notNull(charset, "Charset must not be null");

        this.charset = charset;

        if (charset.name().equals("UTF-8")) {
            utf8 = true;
            ascii = false;
        } else if (charset.name().contains("ASCII")) {
            utf8 = false;
            ascii = true;
        } else {
            ascii = false;
            utf8 = false;
        }
    }

    @Override
    public void encodeKey(String key, ByteBuf target) {
        encode(key, target);
    }

    public void encode(String str, ByteBuf target) {

        if (str == null) {
            return;
        }

        if (utf8) {
            ByteBufUtil.writeUtf8(target, str);
            return;
        }

        if (ascii) {
            ByteBufUtil.writeAscii(target, str);
            return;
        }

        CharsetEncoder encoder = CharsetUtil.encoder(charset);
        int length = (int) ((double) str.length() * encoder.maxBytesPerChar());
        target.ensureWritable(length);
        try {
            final ByteBuffer dstBuf = target.nioBuffer(0, length);
            final int pos = dstBuf.position();
            CoderResult cr = encoder.encode(CharBuffer.wrap(str), dstBuf, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            cr = encoder.flush(dstBuf);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            target.writerIndex(target.writerIndex() + dstBuf.position() - pos);
        } catch (CharacterCodingException x) {
            throw new IllegalStateException(x);
        }
    }

    @Override
    public int estimateSize(Object keyOrValue) {

        if (keyOrValue instanceof String) {
            CharsetEncoder encoder = CharsetUtil.encoder(charset);
            return (int) (encoder.averageBytesPerChar() * ((String) keyOrValue).length());
        }
        return 0;
    }

    @Override
    public void encodeValue(String value, ByteBuf target) {
        encode(value, target);
    }

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return Unpooled.wrappedBuffer(bytes).toString(charset);
    }

    @Override
    public String decodeValue(ByteBuffer bytes) {
        return Unpooled.wrappedBuffer(bytes).toString(charset);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return encodeAndAllocateBuffer(key);
    }

    @Override
    public ByteBuffer encodeValue(String value) {
        return encodeAndAllocateBuffer(value);
    }

    /**
     * Compatibility implementation.
     *
     * @param key
     * @return
     */
    private ByteBuffer encodeAndAllocateBuffer(String key) {
        if (key == null) {
            return ByteBuffer.wrap(EMPTY);
        }

        CharsetEncoder encoder = CharsetUtil.encoder(charset);
        ByteBuffer buffer = ByteBuffer.allocate((int) (encoder.maxBytesPerChar() * key.length()));

        ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
        byteBuf.clear();
        encode(key, byteBuf);
        buffer.limit(byteBuf.writerIndex());

        return buffer;
    }
}
