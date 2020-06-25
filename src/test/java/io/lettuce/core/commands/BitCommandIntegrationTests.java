/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.commands;

import static io.lettuce.core.BitFieldArgs.offset;
import static io.lettuce.core.BitFieldArgs.signed;
import static io.lettuce.core.BitFieldArgs.typeWidthBasedOffset;
import static io.lettuce.core.BitFieldArgs.unsigned;
import static io.lettuce.core.BitFieldArgs.OverflowType.WRAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BitCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    protected RedisCommands<String, String> bitstring;

    @Inject
    protected BitCommandIntegrationTests(RedisClient client, RedisCommands<String, String> redis) {
        this.client = client;
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
        this.bitstring = client.connect(new BitStringCodec()).sync();
    }

    @AfterEach
    void tearDown() {
        this.bitstring.getStatefulConnection().close();
    }

    @Test
    void bitcount() {
        assertThat((long) redis.bitcount(key)).isEqualTo(0);

        redis.setbit(key, 0, 1);
        redis.setbit(key, 1, 1);
        redis.setbit(key, 2, 1);

        assertThat((long) redis.bitcount(key)).isEqualTo(3);
        assertThat(redis.bitcount(key, 3, -1)).isEqualTo(0);
    }

    @Test
    void bitfieldType() {
        assertThat(signed(64).getBits()).isEqualTo(64);
        assertThat(signed(64).isSigned()).isTrue();
        assertThat(unsigned(63).getBits()).isEqualTo(63);
        assertThat(unsigned(63).isSigned()).isFalse();
    }

    @Test
    void bitfieldTypeSigned65() {
        assertThatThrownBy(() -> signed(65)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bitfieldTypeUnsigned64() {
        assertThatThrownBy(() -> unsigned(64)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bitfieldBuilderEmptyPreviousType() {
        assertThatThrownBy(() -> new BitFieldArgs().overflow(WRAP).get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bitfieldArgsTest() {

        assertThat(signed(5).toString()).isEqualTo("i5");
        assertThat(unsigned(5).toString()).isEqualTo("u5");

        assertThat(offset(5).toString()).isEqualTo("5");
        assertThat(typeWidthBasedOffset(5).toString()).isEqualTo("#5");
    }

    @Test
    @EnabledOnCommand("BITFIELD")
    void bitfield() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 1).set(5, 1).incrBy(2, 3).get().get(2);

        List<Long> values = redis.bitfield(key, bitFieldArgs);

        assertThat(values).containsExactly(0L, 32L, 3L, 0L, 3L);
        assertThat(bitstring.get(key)).isEqualTo("0000000000010011");
    }

    @Test
    @EnabledOnCommand("BITFIELD")
    void bitfieldGetWithOffset() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 1).get(signed(2), typeWidthBasedOffset(1));

        List<Long> values = redis.bitfield(key, bitFieldArgs);

        assertThat(values).containsExactly(0L, 0L);
        assertThat(bitstring.get(key)).isEqualTo("10000000");
    }

    @Test
    @EnabledOnCommand("BITFIELD")
    void bitfieldSet() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 5).set(5);

        List<Long> values = redis.bitfield(key, bitFieldArgs);

        assertThat(values).containsExactly(0L, 5L);
        assertThat(bitstring.get(key)).isEqualTo("10100000");
    }

    @Test
    @EnabledOnCommand("BITFIELD")
    void bitfieldWithOffsetSet() {

        redis.bitfield(key, BitFieldArgs.Builder.set(signed(8), typeWidthBasedOffset(2), 5));
        assertThat(bitstring.get(key)).isEqualTo("000000000000000010100000");

        redis.del(key);
        redis.bitfield(key, BitFieldArgs.Builder.set(signed(8), offset(2), 5));
        assertThat(bitstring.get(key)).isEqualTo("1000000000000010");
    }

    @Test
    @EnabledOnCommand("BITFIELD")
    void bitfieldIncrBy() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 5).incrBy(1);

        List<Long> values = redis.bitfield(key, bitFieldArgs);

        assertThat(values).containsExactly(0L, 6L);
        assertThat(bitstring.get(key)).isEqualTo("01100000");
    }

    @Test
    @EnabledOnCommand("BITFIELD")
    void bitfieldWithOffsetIncrBy() {

        redis.bitfield(key, BitFieldArgs.Builder.incrBy(signed(8), typeWidthBasedOffset(2), 1));
        assertThat(bitstring.get(key)).isEqualTo("000000000000000010000000");

        redis.del(key);
        redis.bitfield(key, BitFieldArgs.Builder.incrBy(signed(8), offset(2), 1));
        assertThat(bitstring.get(key)).isEqualTo("0000000000000010");
    }

    @Test
    @EnabledOnCommand("BITFIELD")
    void bitfieldOverflow() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.overflow(WRAP).set(signed(8), 9, Integer.MAX_VALUE).get(signed(8));

        List<Long> values = redis.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 0L);
        assertThat(bitstring.get(key)).isEqualTo("000000001111111000000001");
    }

    @Test
    void bitpos() {
        assertThat((long) redis.bitcount(key)).isEqualTo(0);
        redis.setbit(key, 0, 0);
        redis.setbit(key, 1, 1);

        assertThat(bitstring.get(key)).isEqualTo("00000010");
        assertThat((long) redis.bitpos(key, true)).isEqualTo(1);
    }

    @Test
    void bitposOffset() {
        assertThat((long) redis.bitcount(key)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        redis.setbit(key, 1, 1);
        redis.setbit(key, 2, 0);
        redis.setbit(key, 3, 0);
        redis.setbit(key, 4, 0);
        redis.setbit(key, 5, 1);
        redis.setbit(key, 16, 1);

        assertThat((long) bitstring.getbit(key, 1)).isEqualTo(1);
        assertThat((long) bitstring.getbit(key, 4)).isEqualTo(0);
        assertThat((long) bitstring.getbit(key, 5)).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("001000110000000000000001");
        assertThat((long) redis.bitpos(key, true, 1)).isEqualTo(16);
        assertThat((long) redis.bitpos(key, false, 0, 0)).isEqualTo(2);
    }

    @Test
    void bitopAnd() {
        redis.setbit("foo", 0, 1);
        redis.setbit("bar", 1, 1);
        redis.setbit("baz", 2, 1);
        assertThat(redis.bitopAnd(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat((long) redis.bitcount(key)).isEqualTo(0);
        assertThat(bitstring.get(key)).isEqualTo("00000000");
    }

    @Test
    void bitopNot() {
        redis.setbit("foo", 0, 1);
        redis.setbit("foo", 2, 1);

        assertThat(redis.bitopNot(key, "foo")).isEqualTo(1);
        assertThat((long) redis.bitcount(key)).isEqualTo(6);
        assertThat(bitstring.get(key)).isEqualTo("11111010");
    }

    @Test
    void bitopOr() {
        redis.setbit("foo", 0, 1);
        redis.setbit("bar", 1, 1);
        redis.setbit("baz", 2, 1);
        assertThat(redis.bitopOr(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00000111");
    }

    @Test
    void bitopXor() {
        redis.setbit("foo", 0, 1);
        redis.setbit("bar", 0, 1);
        redis.setbit("baz", 2, 1);
        assertThat(redis.bitopXor(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00000100");
    }

    @Test
    void getbit() {
        assertThat(redis.getbit(key, 0)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        assertThat(redis.getbit(key, 0)).isEqualTo(1);
    }

    @Test
    void setbit() {

        assertThat(redis.setbit(key, 0, 1)).isEqualTo(0);
        assertThat(redis.setbit(key, 0, 0)).isEqualTo(1);
    }

}
