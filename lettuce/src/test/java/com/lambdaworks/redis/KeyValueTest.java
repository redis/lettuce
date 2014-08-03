// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class KeyValueTest {
    protected String key = "key";
    protected String value = "value";

    @Test
    public void equals() throws Exception {
        KeyValue<String, String> kv = kv(key, value);
        assertThat(kv.equals(kv(key, value))).isTrue();
        assertThat(kv.equals(null)).isFalse();
        assertThat(kv.equals(kv("a", value))).isFalse();
        assertThat(kv.equals(kv(key, "b"))).isFalse();
    }

    @Test
    public void testToString() throws Exception {
        KeyValue<String, String> kv = kv(key, value);
        assertThat(kv.toString()).isEqualTo(String.format("(%s, %s)", kv.key, kv.value));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(kv(key, value).hashCode() != 0).isTrue();
    }

    protected KeyValue<String, String> kv(String key, String value) {
        return new KeyValue<String, String>(key, value);
    }
}
