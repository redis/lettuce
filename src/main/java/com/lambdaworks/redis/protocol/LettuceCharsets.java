// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * {@link Charset}-related utilities.
 * 
 * @author Will Glozer
 */
public class LettuceCharsets {

    /**
     * US-ASCII charset.
     */
    public static final Charset ASCII = Charset.forName("US-ASCII");

    /**
     * UTF-8 charset.
     */
    public static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Utility constructor.
     */
    private LettuceCharsets() {

    }

    /**
     * Create a ByteBuffer from a string using ASCII encoding.
     * 
     * @param s the string
     * @return ByteBuffer
     */
    public static ByteBuffer buffer(String s) {
        return ByteBuffer.wrap(s.getBytes(ASCII));
    }

}
