// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.lambdaworks.redis.AbstractRedisClientTest;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class NumericCommandTest extends AbstractRedisClientTest {
    @Test
    public void decr() throws Exception {
        assertThat((long) redis.decr(key)).isEqualTo(-1);
        assertThat((long) redis.decr(key)).isEqualTo(-2);
    }

    @Test
    public void decrby() throws Exception {
        Assertions.assertThat(redis.decrby(key, 3)).isEqualTo(-3);
        Assertions.assertThat(redis.decrby(key, 3)).isEqualTo(-6);
    }

    @Test
    public void incr() throws Exception {
        assertThat((long) redis.incr(key)).isEqualTo(1);
        assertThat((long) redis.incr(key)).isEqualTo(2);
    }

    @Test
    public void incrby() throws Exception {
        Assertions.assertThat(redis.incrby(key, 3)).isEqualTo(3);
        Assertions.assertThat(redis.incrby(key, 3)).isEqualTo(6);
    }

    @Test
    public void incrbyfloat() throws Exception {

        Assertions.assertThat(redis.incrbyfloat(key, 3.0)).isEqualTo(3.0, offset(0.1));
        Assertions.assertThat(redis.incrbyfloat(key, 0.2)).isEqualTo(3.2, offset(0.1));
    }
}
