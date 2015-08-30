// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.codec;

import static java.nio.charset.CoderResult.OVERFLOW;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import com.lambdaworks.redis.protocol.LettuceCharsets;

/**
 * A {@link RedisCodec} that handles UTF-8 encoded keys and values.
 * 
 * @author Will Glozer
 */
public class Utf8StringCodec implements RedisCodec<String, String> {

    private final static byte[] EMPTY = new byte[0];

    private Charset charset;
    private CharsetDecoder decoder;
    private CharBuffer chars;


    /**
     * Initialize a new instance that encodes and decodes strings using the UTF-8 charset;
     */
    public Utf8StringCodec() {
        charset = LettuceCharsets.UTF8;
        decoder = charset.newDecoder();
        chars = CharBuffer.allocate(1024);
    }

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return decode(bytes);
    }

    @Override
    public String decodeValue(ByteBuffer bytes) {
        return decode(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return encode(key);
    }

    @Override
    public ByteBuffer encodeValue(String value) {
        return encode(value);
    }

    private synchronized String decode(ByteBuffer bytes) {
        chars.clear();
        bytes.mark();

        decoder.reset();
        while (decoder.decode(bytes, chars, true) == OVERFLOW || decoder.flush(chars) == OVERFLOW) {
            chars = CharBuffer.allocate(chars.capacity() * 2);
            bytes.reset();
        }

        return chars.flip().toString();
    }

    private ByteBuffer encode(String string) {
        if (string == null) {
            return ByteBuffer.wrap(EMPTY);
        }

        return charset.encode(string);
    }
}
