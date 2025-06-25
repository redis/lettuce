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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
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

}
