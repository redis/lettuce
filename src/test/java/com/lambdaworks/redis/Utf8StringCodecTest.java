// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

public class Utf8StringCodecTest extends AbstractRedisClientTest {
    @Test
    public void decodeHugeBuffer() throws Exception {
        char[] huge = new char[8192];
        Arrays.fill(huge, 'A');
        String value = new String(huge);
        redis.set(key, value);
        assertThat(redis.get(key)).isEqualTo(value);
    }
}
