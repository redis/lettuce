// Copyright (C) 2012 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisConnection;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.codec.Utf8StringCodec;

public class BitCommandTest extends AbstractRedisClientTest {
    protected RedisConnection<String, String> bitstring;

    @Before
    public final void openBitStringConnection() throws Exception {
        bitstring = client.connect(new BitStringCodec());
    }

    @After
    public final void closeBitStringConnection() throws Exception {
        bitstring.close();
    }

    @Test
    public void bitcount() throws Exception {
        assertThat((long) redis.bitcount(key)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        redis.setbit(key, 1, 1);
        redis.setbit(key, 2, 1);
        assertThat((long) redis.bitcount(key)).isEqualTo(3);
        // assertThat(1).isEqualTo(2, (long) redis.bitcount(key, offset(3)));
        Assertions.assertThat(redis.bitcount(key, 3, -1)).isEqualTo(0);
    }

    @Test
    public void bitpos() throws Exception {
        assertThat((long) redis.bitcount(key)).isEqualTo(0);
        redis.setbit(key, 0, 0);
        redis.setbit(key, 1, 1);

        assertThat(bitstring.get(key)).isEqualTo("00000010");
        assertThat((long) redis.bitpos(key, true)).isEqualTo(1);
    }

    @Test
    public void bitposOffset() throws Exception {
        assertThat((long) redis.bitcount(key)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        redis.setbit(key, 1, 1);
        redis.setbit(key, 2, 0);
        redis.setbit(key, 3, 0);
        redis.setbit(key, 4, 0);
        redis.setbit(key, 5, 1);

        assertThat((long) bitstring.getbit(key, 1)).isEqualTo(1);
        assertThat((long) bitstring.getbit(key, 4)).isEqualTo(0);
        assertThat((long) bitstring.getbit(key, 5)).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00100011");
        assertThat((long) redis.bitpos(key, false, 0, 0)).isEqualTo(2);
    }

    @Test
    public void bitopAnd() throws Exception {
        redis.setbit("foo", 0, 1);
        redis.setbit("bar", 1, 1);
        redis.setbit("baz", 2, 1);
        Assertions.assertThat(redis.bitopAnd(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat((long) redis.bitcount(key)).isEqualTo(0);
        assertThat(bitstring.get(key)).isEqualTo("00000000");
    }

    @Test
    public void bitopNot() throws Exception {
        redis.setbit("foo", 0, 1);
        redis.setbit("foo", 2, 1);

        Assertions.assertThat(redis.bitopNot(key, "foo")).isEqualTo(1);
        assertThat((long) redis.bitcount(key)).isEqualTo(6);
        assertThat(bitstring.get(key)).isEqualTo("11111010");
    }

    @Test
    public void bitopOr() throws Exception {
        redis.setbit("foo", 0, 1);
        redis.setbit("bar", 1, 1);
        redis.setbit("baz", 2, 1);
        Assertions.assertThat(redis.bitopOr(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00000111");
    }

    @Test
    public void bitopXor() throws Exception {
        redis.setbit("foo", 0, 1);
        redis.setbit("bar", 0, 1);
        redis.setbit("baz", 2, 1);
        Assertions.assertThat(redis.bitopXor(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00000100");
    }

    @Test
    public void getbit() throws Exception {
        Assertions.assertThat(redis.getbit(key, 0)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        Assertions.assertThat(redis.getbit(key, 0)).isEqualTo(1);
    }

    @Test
    public void setbit() throws Exception {

        Assertions.assertThat(redis.setbit(key, 0, 1)).isEqualTo(0);
        Assertions.assertThat(redis.setbit(key, 0, 0)).isEqualTo(1);
    }

    static class BitStringCodec extends Utf8StringCodec {
        @Override
        public String decodeValue(ByteBuffer bytes) {
            StringBuilder bits = new StringBuilder(bytes.remaining() * 8);
            while (bytes.remaining() > 0) {
                byte b = bytes.get();
                for (int i = 0; i < 8; i++) {
                    bits.append(Integer.valueOf(b >>> i & 1));
                }
            }
            return bits.toString();
        }
    }
}
