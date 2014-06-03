// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Utf8StringCodecTest extends AbstractCommandTest {
    @Test
    public void decodeHugeBuffer() throws Exception {
        char[] huge = new char[8192];
        Arrays.fill(huge, 'A');
        String value = new String(huge);
        redis.set(key, value);
        assertEquals(value, redis.get(key));
    }
}


