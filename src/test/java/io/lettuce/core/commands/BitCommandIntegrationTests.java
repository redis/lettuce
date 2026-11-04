/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.BitFieldArgs.offset;
import static io.lettuce.core.BitFieldArgs.signed;
import static io.lettuce.core.BitFieldArgs.typeWidthBasedOffset;
import static io.lettuce.core.BitFieldArgs.unsigned;
import static io.lettuce.core.BitFieldArgs.OverflowType.WRAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.BitRangeIntArrayArgs;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;
import io.netty.buffer.ByteBuf;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BitCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    private RedisCodec<String, String> oneByteStringCodec = new BitStringCodec() {

        @Override
        public void encodeValue(String oneByte, ByteBuf target) {
            byte value = 0;
            for (int i = 0; i < 8; i++) {
                if (oneByte.charAt(i) == '1') {
                    value |= (1 << i);
                }
            }
            target.writeByte(value);
        }

    };

    private RedisCommands<String, String> byteString;

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
        byteString = client.connect(oneByteStringCodec).sync();
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

        assertThat(offset(-1).toString()).isEqualTo(Integer.toUnsignedString(-1));
        assertThat(typeWidthBasedOffset(-1).toString()).isEqualTo("#" + Integer.toUnsignedString(-1));
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
    void bitfieldGetWithUnsignedOffset() {

        long unsignedIntMax = (1L << 32) - 1;
        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 1).get(signed(1), (int) unsignedIntMax);

        List<Long> values = redis.bitfield(key, bitFieldArgs);

        assertThat(values).containsExactly(0L, 0L);
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
    void bitfieldWithUnsignedOffsetSet() {

        long unsignedIntMax = (1L << 32) - 1;
        redis.bitfield(key, BitFieldArgs.Builder.set(signed(1), offset((int) unsignedIntMax), 1));
        assertThat(bitstring.getbit(key, unsignedIntMax)).isEqualTo(1);
        assertThat(bitstring.bitcount(key)).isEqualTo(1);
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
    void bitfieldWithUnsignedOffsetIncryBy() {

        long unsignedIntMax = (1L << 32) - 1;
        redis.bitfield(key, BitFieldArgs.Builder.incrBy(signed(1), offset((int) unsignedIntMax), 1));
        assertThat(bitstring.getbit(key, unsignedIntMax)).isEqualTo(1);
        assertThat(bitstring.bitcount(key)).isEqualTo(1);
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
    void bitopDiff() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test DIFF: X ∧ ¬(Y1 ∨ Y2 ∨ …)
        // Set up test data: foo has bits 0,1,2 set, bar has bit 1 set, baz has bit 2 set
        byteString.set("foo", "00000111");
        byteString.set("bar", "00000010");
        byteString.set("baz", "00000100");

        // DIFF should return bits in foo that are not in bar OR baz
        // foo: 00000111 (bits 0,1,2 set), bar: 00000010 (bit 1 set), baz: 00000100 (bit 2 set)
        // bar OR baz: 00000110
        // foo AND NOT(bar OR baz): 00000001
        assertThat(redis.bitopDiff(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00000001");
    }

    @Test
    void bitopDiff1() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test DIFF1: ¬X ∧ (Y1 ∨ Y2 ∨ …)
        // Set up test data: foo has bits 0,1 set, bar has bit 1,2 set, baz has bit 2,3 set
        byteString.set("foo", "00000011");
        byteString.set("bar", "00000110");
        byteString.set("baz", "00001100");

        // DIFF1 should return bits in (bar OR baz) that are not in foo
        // foo: 00000011 (bits 0,1 set), bar: 00000110 (bits 1,2 set), baz: 00001100 (bits 2,3 set)
        // bar OR baz: 00001110
        // NOT foo AND (bar OR baz): 00001100
        assertThat(redis.bitopDiff1(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00001100");
    }

    @Test
    void bitopAndor() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test ANDOR: X ∧ (Y1 ∨ Y2 ∨ …)
        // Set up test data: foo has bits 0,1,2 set, bar has bit 1 set, baz has bit 2,3 set
        byteString.set("foo", "00000111");
        byteString.set("bar", "00000010");
        byteString.set("baz", "00001100");

        // ANDOR should return bits in foo that are also in (bar OR baz)
        // foo: 00000111 (bits 0,1,2 set), bar: 00000010 (bit 1 set), baz: 00001100 (bits 2,3 set)
        // bar OR baz: 00001110
        // foo AND (bar OR baz): 00000110
        assertThat(redis.bitopAndor(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00000110");
    }

    @Test
    void bitopOne() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test ONE: members of exactly one of the given keys
        // Set up test data: foo has bits 0,1 set, bar has bit 1,2 set, baz has bit 2,3 set
        byteString.set("foo", "00000011");
        byteString.set("bar", "00000110");
        byteString.set("baz", "00001100");

        // ONE should return bits that appear in exactly one key
        // foo: 00000011 (bits 0,1 set), bar: 00000110 (bits 1,2 set), baz: 00001100 (bits 2,3 set)
        // bit 0: only in foo (1 key) ✓
        // bit 1: in foo and bar (2 keys) ✗
        // bit 2: in bar and baz (2 keys) ✗
        // bit 3: only in baz (1 key) ✓
        // Result: 00001001
        assertThat(redis.bitopOne(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00001001");
    }

    @Test
    void bitopOneWithTwoKeys() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test ONE with two keys (should be equivalent to XOR)
        byteString.set("foo", "00000011");
        byteString.set("bar", "00000110");

        // ONE with two keys should be same as XOR
        // foo: 00000011 (bits 0,1 set), bar: 00000110 (bits 1,2 set)
        // XOR result: 00000101 (bits 0,2 set)
        assertThat(redis.bitopOne(key, "foo", "bar")).isEqualTo(1);
        assertThat(bitstring.get(key)).isEqualTo("00000101");

        // Verify it's the same as XOR
        assertThat(redis.bitopXor("xor_result", "foo", "bar")).isEqualTo(1);
        assertThat(bitstring.get("xor_result")).isEqualTo("00000101");
    }

    @Test
    void bitopExtendedComplexPatterns() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test with more complex bit patterns using setbit for precise control
        // X = 11110000 (bits 4,5,6,7), Y1 = 10101010 (bits 1,3,5,7), Y2 = 01010101 (bits 0,2,4,6), Y3 = 11001100 (bits 2,3,6,7)

        // Set X = 11110000 (bits 4,5,6,7 set)
        byteString.set("X", "11110000");

        // Set Y1 = 10101010 (bits 1,3,5,7 set)
        byteString.set("Y1", "10101010");

        // Set Y2 = 01010101 (bits 0,2,4,6 set)
        byteString.set("Y2", "01010101");

        // Set Y3 = 11001100 (bits 2,3,6,7 set)
        byteString.set("Y3", "11001100");

        // Test DIFF: X ∧ ¬(Y1 ∨ Y2 ∨ Y3)
        // Y1 ∨ Y2 ∨ Y3 = all bits 0-7 are set = 11111111
        // X ∧ ¬(Y1 ∨ Y2 ∨ Y3) = 11110000 ∧ 00000000 = 00000000
        assertThat(redis.bitopDiff("diff_result", "X", "Y1", "Y2", "Y3")).isEqualTo(1);
        assertThat(bitstring.get("diff_result")).isEqualTo("00000000");

        // Test DIFF1: ¬X ∧ (Y1 ∨ Y2 ∨ Y3)
        // ¬X = 00001111, Y1 ∨ Y2 ∨ Y3 = 11111111
        // ¬X ∧ (Y1 ∨ Y2 ∨ Y3) = 00001111 ∧ 11111111 = 00001111
        assertThat(redis.bitopDiff1("diff1_result", "X", "Y1", "Y2", "Y3")).isEqualTo(1);
        assertThat(bitstring.get("diff1_result")).isEqualTo("00001111");

        // Test ANDOR: X ∧ (Y1 ∨ Y2 ∨ Y3)
        // Y1 ∨ Y2 ∨ Y3 = 11111111, X ∧ (Y1 ∨ Y2 ∨ Y3) = 11110000 ∧ 11111111 = 11110000
        assertThat(redis.bitopAndor("andor_result", "X", "Y1", "Y2", "Y3")).isEqualTo(1);
        assertThat(bitstring.get("andor_result")).isEqualTo("11110000");

        // Test ONE: bits set in exactly one bitmap
        // Bit 0: only in Y2 (1 key) ✓
        // Bit 1: only in Y1 (1 key) ✓
        // Bit 2: in Y2 and Y3 (2 keys) ✗
        // Bit 3: in Y1 and Y3 (2 keys) ✗
        // Bit 4: in X and Y2 (2 keys) ✗
        // Bit 5: in X and Y1 (2 keys) ✗
        // Bit 6: in X, Y2, Y3 (3 keys) ✗
        // Bit 7: in X, Y1, Y3 (3 keys) ✗
        // Expected result: 00000011 (bits 0,1 set)
        assertThat(redis.bitopOne("one_result", "X", "Y1", "Y2", "Y3")).isEqualTo(1);
        assertThat(bitstring.get("one_result")).isEqualTo("00000011");
    }

    @Test
    void bitopTwoOperandsComprehensive() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test with two operands: key1=10101010 (bits 1,3,5,7), key2=11001100 (bits 2,3,6,7)

        // Set key1 = 10101010 (bits 1,3,5,7 set)
        byteString.set("key1", "10101010");

        // Set key2 = 11001100 (bits 2,3,6,7 set)
        byteString.set("key2", "11001100");

        // Test DIFF: key1 ∧ ¬key2 = 10101010 ∧ ¬11001100 = 10101010 ∧ 00110011 = 00100010
        assertThat(redis.bitopDiff("diff2_result", "key1", "key2")).isEqualTo(1);
        assertThat(bitstring.get("diff2_result")).isEqualTo("00100010");

        // Test ONE with two operands (should be equivalent to XOR)
        // key1 XOR key2 = 10101010 XOR 11001100 = 01100110
        assertThat(redis.bitopOne("one2_result", "key1", "key2")).isEqualTo(1);
        assertThat(bitstring.get("one2_result")).isEqualTo("01100110");

        // Verify ONE equals XOR for two operands
        assertThat(redis.bitopXor("xor2_result", "key1", "key2")).isEqualTo(1);
        assertThat(bitstring.get("xor2_result")).isEqualTo("01100110");
        assertThat(bitstring.get("one2_result")).isEqualTo(bitstring.get("xor2_result"));
    }

    @Test
    void bitopEdgeCases() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test edge cases with empty keys and minimum operand requirements
        // Set nonempty = 11110000 (bits 4,5,6,7 set)
        byteString.set("nonempty", "11110000");

        // DIFF with empty key should return the first key
        assertThat(redis.bitopDiff("edge_result", "nonempty", "nonexistent")).isEqualTo(1);
        assertThat(bitstring.get("edge_result")).isEqualTo("11110000");

        // ONE with single key should return that key
        assertThat(redis.bitopOne("edge_one_result", "nonempty")).isEqualTo(1);
        assertThat(bitstring.get("edge_one_result")).isEqualTo("11110000");

        // ANDOR requires at least two keys - test with empty second key
        // nonempty ∧ (empty) = nonempty ∧ 00000000 = 00000000
        assertThat(redis.bitopAndor("edge_andor_result", "nonempty", "nonexistent")).isEqualTo(1);
        assertThat(bitstring.get("edge_andor_result")).isEqualTo("00000000");

        // DIFF1 requires at least two keys - test with empty second key
        // ¬nonempty ∧ (empty) = ¬11110000 ∧ 00000000 = 00001111 ∧ 00000000 = 00000000
        assertThat(redis.bitopDiff1("edge_diff1_result", "nonempty", "nonexistent")).isEqualTo(1);
        assertThat(bitstring.get("edge_diff1_result")).isEqualTo("00000000");
    }

    @Test
    void bitopMinimumOperandRequirements() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.1.240"));
        // Test minimum operand requirements for each BITOP operation
        byteString.set("test_key", "00000011");

        // Operations that work with single operand
        assertThat(redis.bitopOne("single_one", "test_key")).isEqualTo(1);
        assertThat(bitstring.get("single_one")).isEqualTo("00000011");

        // Note: DIFF1 requires at least two keys, so we test with an empty second key
        assertThat(redis.bitopDiff1("single_diff1", "test_key", "nonexistent")).isEqualTo(1);
        assertThat(bitstring.get("single_diff1")).isEqualTo("00000000");

        // Operations that require at least two operands
        redis.setbit("second_key", 1, 1);
        redis.setbit("second_key", 2, 1);

        assertThat(redis.bitopDiff("two_diff", "test_key", "second_key")).isEqualTo(1);
        assertThat(bitstring.get("two_diff")).isEqualTo("00000001");

        assertThat(redis.bitopAndor("two_andor", "test_key", "second_key")).isEqualTo(1);
        assertThat(bitstring.get("two_andor")).isEqualTo("00000010");
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

    private static final String F1 = "bria:f1";

    private static final String F2 = "bria:f2";

    private static final String F3 = "bria:f3";

    private static final String FULL = "bria:full";

    private static final String F4 = "bria:f4";

    private static final String F5 = "bria:f5";

    private static final String F6 = "bria:f6";

    private static final List<Long> F1_OFFSETS = Arrays.asList(0L, 7L, 8L, 15L, 16L, 23L, 24L, 31L);

    /**
     * F1: {@code 0x81818181}, set bits 0 7 8 15 16 23 24 31.
     */
    private void createF1() {
        redis.bitfield(F1, BitFieldArgs.Builder.set(unsigned(32), 0, 2172748161L));
    }

    /**
     * F2: {@code 0xff81}, set bits 0-7, 8 and 15.
     */
    private void createF2() {
        redis.bitfield(F2, BitFieldArgs.Builder.set(unsigned(16), 0, 65409));
    }

    /**
     * F3: {@code 0xff7e}, and {@code 0xffff} under {@link #FULL}.
     */
    private void createF3() {
        redis.bitfield(F3, BitFieldArgs.Builder.set(unsigned(16), 0, 65406));
        redis.bitfield(FULL, BitFieldArgs.Builder.set(unsigned(16), 0, 65535));
    }

    /**
     * F4: bytes 0, 7 and 8 full, byte 1 with bits 8 and 15, byte 9 with bit 73; 27 set bits.
     */
    private void createF4() {
        redis.bitfield(F4, BitFieldArgs.Builder.set(unsigned(8), 0, 255).set(unsigned(8), 56, 255).set(unsigned(8), 64, 255));
        redis.setbit(F4, 8, 1);
        redis.setbit(F4, 15, 1);
        redis.setbit(F4, 73, 1);
    }

    private static List<Long> range(long from, long to) {
        List<Long> result = new ArrayList<>();
        for (long i = from; i <= to; i++) {
            result.add(i);
        }
        return result;
    }

    private static List<Long> concat(List<Long> first, Long... rest) {
        List<Long> result = new ArrayList<>(first);
        result.addAll(Arrays.asList(rest));
        return result;
    }

    private List<List<Long>> pageInBits(String key, long count) {
        List<List<Long>> pages = new ArrayList<>();
        long start = 0;
        while (true) {
            List<Long> page = redis.bitrangeintarray(key, start, -1, BitRangeIntArrayArgs.Builder.bit().limit(count));
            pages.add(page);
            if (page.size() < count) {
                return pages;
            }
            start = page.get(page.size() - 1) + 1;
        }
    }

    private List<List<Long>> pageInBytes(String key, long count) {
        List<List<Long>> pages = new ArrayList<>();
        long start = 0;
        while (true) {
            List<Long> page = redis.bitrangeintarray(key, start, -1, BitRangeIntArrayArgs.Builder.limit(count));
            pages.add(page);
            if (page.size() < count) {
                return pages;
            }
            start = page.get(page.size() - 1) / 8 + 1;
        }
    }

    /**
     * Scenario C01: the whole value returns the set bit offsets in ascending order.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayWholeValue() {
        createF1();

        assertThat(redis.bitrangeintarray(F1, 0, -1)).isEqualTo(F1_OFFSETS);
    }

    /**
     * Scenario C02: byte ranges are inclusive, count negative indexes from the end and clamp to the value.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayByteRanges() {
        createF1();

        assertThat(redis.bitrangeintarray(F1, 1, 1)).containsExactly(8L, 15L);
        assertThat(redis.bitrangeintarray(F1, -1, -1)).containsExactly(24L, 31L);
        assertThat(redis.bitrangeintarray(F1, 2, 99)).containsExactly(16L, 23L, 24L, 31L);
        assertThat(redis.bitrangeintarray(F1, -99, -98)).containsExactly(0L, 7L);
        assertThat(redis.bitrangeintarray(F1, 3, 1)).isEmpty();
        assertThat(redis.bitrangeintarray(F1, 4, 9)).isEmpty();
        assertThat(redis.bitrangeintarray(F1, -1, -2)).isEmpty();
    }

    /**
     * Scenario C03: bit ranges are inclusive, count negative indexes from the end and clamp to the value.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayBitRanges() {
        createF1();

        assertThat(redis.bitrangeintarray(F1, 0, 0, BitRangeIntArrayArgs.Builder.bit())).containsExactly(0L);
        assertThat(redis.bitrangeintarray(F1, 7, 8, BitRangeIntArrayArgs.Builder.bit())).containsExactly(7L, 8L);
        assertThat(redis.bitrangeintarray(F1, 1, 6, BitRangeIntArrayArgs.Builder.bit())).isEmpty();
        assertThat(redis.bitrangeintarray(F1, -8, -1, BitRangeIntArrayArgs.Builder.bit())).containsExactly(24L, 31L);
        assertThat(redis.bitrangeintarray(F1, 30, 1000, BitRangeIntArrayArgs.Builder.bit())).containsExactly(31L);
        assertThat(redis.bitrangeintarray(F1, 32, 40, BitRangeIntArrayArgs.Builder.bit())).isEmpty();
    }

    /**
     * Scenario C04: the offsets are bit offsets in both units.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayOffsetsAreBitOffsetsInBothUnits() {
        createF1();

        assertThat(redis.bitrangeintarray(F1, 0, 0, BitRangeIntArrayArgs.Builder.byteUnit())).containsExactly(0L, 7L);
        assertThat(redis.bitrangeintarray(F1, 0, 7, BitRangeIntArrayArgs.Builder.bit())).containsExactly(0L, 7L);
    }

    /**
     * Scenario C05: a missing key returns an empty list, not {@code null}.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayMissingKey() {
        List<Long> result = redis.bitrangeintarray("bria:missing", 0, -1);

        assertThat(result).isNotNull().isEmpty();
    }

    /**
     * Scenario C06: an empty string returns an empty list.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayEmptyString() {
        redis.set("bria:empty", "");

        assertThat(redis.bitrangeintarray("bria:empty", 0, -1)).isNotNull().isEmpty();
    }

    /**
     * Scenario C07: a limit of 0 returns an empty list.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayLimitZero() {
        createF1();

        assertThat(redis.bitrangeintarray(F1, 0, -1, BitRangeIntArrayArgs.Builder.limit(0))).isNotNull().isEmpty();
    }

    /**
     * Scenario C08: a byte page completes the byte of its last offset and is returned whole; a bit page stops at the count.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayLimitCompletesByte() {
        createF2();

        assertThat(redis.bitrangeintarray(F2, 0, -1, BitRangeIntArrayArgs.Builder.limit(1))).isEqualTo(range(0, 7));
        assertThat(redis.bitrangeintarray(F2, 0, -1, BitRangeIntArrayArgs.Builder.limit(9)))
                .isEqualTo(concat(range(0, 7), 8L, 15L));
        assertThat(redis.bitrangeintarray(F2, 0, -1, BitRangeIntArrayArgs.Builder.bit().limit(1))).containsExactly(0L);
        assertThat(redis.bitrangeintarray(F2, 0, -1, BitRangeIntArrayArgs.Builder.bit().limit(9))).isEqualTo(range(0, 8));
    }

    /**
     * Scenario C09: {@code ZEROS} returns the clear bits inside the value only.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayZerosInsideValue() {
        createF3();

        assertThat(redis.bitrangeintarray(F3, 0, -1, BitRangeIntArrayArgs.Builder.zeros())).containsExactly(8L, 15L);
        assertThat(redis.bitrangeintarray(F3, 0, 99, BitRangeIntArrayArgs.Builder.zeros())).containsExactly(8L, 15L);
        assertThat(redis.bitrangeintarray(FULL, 0, -1, BitRangeIntArrayArgs.Builder.zeros())).isEmpty();
    }

    /**
     * Scenario C10: the number of offsets equals {@code BITCOUNT} over the same range.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayLengthEqualsBitcount() {
        createF1();

        assertThat(redis.bitrangeintarray(F1, 1, 2)).hasSize(4);
        assertThat(redis.bitcount(F1, 1, 2)).isEqualTo(4L);
    }

    /**
     * Scenario C11: paging in bit units with a limit of 9, the next start being the last offset plus 1.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayPagingInBits() {
        createF4();

        assertThat(pageInBits(F4, 9)).containsExactly(range(0, 8),
                concat(Collections.singletonList(15L), 56L, 57L, 58L, 59L, 60L, 61L, 62L, 63L), concat(range(64, 71), 73L),
                Collections.emptyList());
    }

    /**
     * Scenario C11a: paging in byte units with a limit of 9, the next start being the byte of the last offset plus 1.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayPagingInBytes() {
        createF4();

        List<List<Long>> pages = pageInBytes(F4, 9);

        assertThat(pages).containsExactly(concat(range(0, 7), 8L, 15L), range(56, 71), Collections.singletonList(73L));
        assertThat(pages.get(1)).hasSize(16);
    }

    /**
     * Scenario C11b: the joined pages of both units hold the same 27 offsets, each once, equal to the whole value.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayJoinedPagesEqualWholeValue() {
        createF4();

        List<Long> bitPages = new ArrayList<>();
        pageInBits(F4, 9).forEach(bitPages::addAll);
        List<Long> bytePages = new ArrayList<>();
        pageInBytes(F4, 9).forEach(bytePages::addAll);

        List<Long> wholeValue = redis.bitrangeintarray(F4, 0, -1);

        assertThat(wholeValue).hasSize(27).doesNotHaveDuplicates();
        assertThat(bitPages).isEqualTo(wholeValue);
        assertThat(bytePages).isEqualTo(wholeValue);
    }

    /**
     * Scenario C12: a native bitmap returns its set bit offsets.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayNativeBitmap() {
        try (StatefulRedisConnection<String, String> admin = client.connect()) {
            String roaring = admin.sync().configGet("bitmap-default-roaring").get("bitmap-default-roaring");
            try {
                admin.sync().configSet("bitmap-default-roaring", "yes");
                redis.setbit(F5, 0, 1);
                redis.setbit(F5, 8, 1);
                redis.setbit(F5, 65536, 1);

                assertThat(redis.type(F5)).isEqualTo("bitmap");
                assertThat(redis.bitrangeintarray(F5, 0, -1)).containsExactly(0L, 8L, 65536L);
            } finally {
                admin.sync().configSet("bitmap-default-roaring", roaring);
            }
        }
    }

    /**
     * Scenario C13: an offset above 2^32 is returned as a 64-bit value.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayOffsetAbove32Bits() {
        try (StatefulRedisConnection<String, String> admin = client.connect()) {
            String roaring = admin.sync().configGet("bitmap-default-roaring").get("bitmap-default-roaring");
            String bulkLen = admin.sync().configGet("proto-max-bulk-len").get("proto-max-bulk-len");
            try {
                admin.sync().configSet("bitmap-default-roaring", "yes");
                admin.sync().configSet("proto-max-bulk-len", "2gb");
                redis.setbit(F6, 4294967301L, 1);

                assertThat(redis.bitrangeintarray(F6, 0, -1)).containsExactly(4294967301L);
            } finally {
                admin.sync().configSet("proto-max-bulk-len", bulkLen);
                admin.sync().configSet("bitmap-default-roaring", roaring);
            }
        }
    }

    /**
     * Scenario D01: a key of another type raises a {@code WRONGTYPE} error.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayWrongType() {
        redis.rpush("bria:list", "a");

        assertThatThrownBy(() -> redis.bitrangeintarray("bria:list", 0, -1)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageStartingWith("WRONGTYPE");
    }

    /**
     * Scenario D02: an inverted range is sent as given and returns an empty list.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayInvertedRange() {
        createF1();

        assertThat(redis.bitrangeintarray(F1, 3, 1)).isNotNull().isEmpty();
    }

    /**
     * Scenario D03: on a server without the command, the unknown-command error reaches the caller.
     */
    @Test
    public void bitrangeintarrayUnknownCommand() {
        assumeFalse(RedisConditions.of(redis).hasCommand("BITRANGEINTARRAY"));

        assertThatThrownBy(() -> redis.bitrangeintarray(key, 0, -1)).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("unknown command");
    }

    /**
     * Scenario E01: the async API completes with the set bit offsets.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarrayAsync() throws Exception {
        createF1();

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisFuture<List<Long>> future = connection.async().bitrangeintarray(F1, 0, -1);

            assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo(F1_OFFSETS);
        }
    }

}
