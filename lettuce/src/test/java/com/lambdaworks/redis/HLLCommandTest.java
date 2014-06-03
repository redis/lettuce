// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HLLCommandTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void pfadd() throws Exception {
        assertEquals(1, (long) redis.pfadd(key, value, value));
        assertEquals(0, (long) redis.pfadd(key, value));
    }

    @Test
    public void pfmerge() throws Exception {
        redis.pfadd(key, value);
        redis.pfadd("key2", "value2");
        redis.pfadd("key3", "value3");

        assertEquals(1, (long) redis.pfmerge(key, "key2", "key3"));
    }

    @Test
    public void pfcount() throws Exception {
        redis.pfadd(key, value);
        redis.pfadd("key2", "value2");
        assertEquals(1, (long) redis.pfcount(key));
        assertEquals(2, (long) redis.pfcount(key, "key2"));

    }

}
