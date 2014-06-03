// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import static org.junit.Assert.*;

public class NumericCommandTest extends AbstractCommandTest {
    @Test
    public void decr() throws Exception {
        assertEquals(-1, (long) redis.decr(key));
        assertEquals(-2, (long) redis.decr(key));
    }

    @Test
    public void decrby() throws Exception {
        assertEquals(-3, (long) redis.decrby(key, 3));
        assertEquals(-6, (long) redis.decrby(key, 3));
    }

    @Test
    public void incr() throws Exception {
        assertEquals(1, (long) redis.incr(key));
        assertEquals(2, (long) redis.incr(key));
    }

    @Test
    public void incrby() throws Exception {
        assertEquals(3, (long) redis.incrby(key, 3));
        assertEquals(6, (long) redis.incrby(key, 3));
    }

    @Test
    public void incrbyfloat() throws Exception {
        assertEquals(3.0, redis.incrbyfloat(key, 3.0), 0.1);
        assertEquals(3.2, redis.incrbyfloat(key, 0.2), 0.1);
    }
}
