// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import static org.junit.Assert.*;

public class KeyValueTest {
    protected String key   = "key";
    protected String value = "value";

    @Test
    public void equals() throws Exception {
        KeyValue<String, String> kv = kv(key, value);
        assertTrue(kv.equals(kv(key, value)));
        assertFalse(kv.equals(null));
        assertFalse(kv.equals(kv("a", value)));
        assertFalse(kv.equals(kv(key, "b")));
    }

    @Test
    public void testToString() throws Exception {
        KeyValue<String, String> kv = kv(key, value);
        assertEquals(String.format("(%s, %s)", kv.key, kv.value), kv.toString());
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(kv(key, value).hashCode() != 0);
    }

    protected KeyValue<String, String> kv(String key, String value) {
        return new KeyValue<String, String>(key, value);
    }
}
