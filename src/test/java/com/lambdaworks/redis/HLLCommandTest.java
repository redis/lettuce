// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HLLCommandTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void pfadd() throws Exception {

        assertThat(redis.pfadd(key, value, value)).isEqualTo(1);
        assertThat(redis.pfadd(key, value, value)).isEqualTo(0);

        assertThat(redis.pfadd(key, value)).isEqualTo(0);
    }

    @Test
    public void pfaddNullValues() throws Exception {
        try {
            redis.pfadd(key, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            redis.pfadd(key, value, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void pfmerge() throws Exception {
        redis.pfadd(key, value);
        redis.pfadd("key2", "value2");
        redis.pfadd("key3", "value3");

        assertThat(redis.pfmerge(key, "key2", "key3")).isEqualTo(1);
    }

    @Test
    public void pfcount() throws Exception {
        redis.pfadd(key, value);
        redis.pfadd("key2", "value2");
        assertThat(redis.pfcount(key)).isEqualTo(1);
        assertThat(redis.pfcount(key, "key2")).isEqualTo(2);

    }

}
