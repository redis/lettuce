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
package io.lettuce.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/bitfield">BITFIELD</a> command.
 * <p>
 * {@link BitFieldArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @author Ian Pojman
 * @since 4.2
 */
public class BitFieldArgs implements CompositeArgument {

    private List<SubCommand> commands;

    /**
     * Creates a new {@link BitFieldArgs} instance.
     */
    public BitFieldArgs() {
        this(new ArrayList<>());
    }

    private BitFieldArgs(List<SubCommand> commands) {
        LettuceAssert.notNull(commands, "Commands must not be null");
        this.commands = commands;
    }

    /**
     * Builder entry points for {@link BitFieldArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        /**
         * Create a new {@code GET} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@code null}.
         * @param offset bitfield offset
         * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
         */
        public static BitFieldArgs get(BitFieldType bitFieldType, int offset) {
            return new BitFieldArgs().get(bitFieldType, offset);
        }

        /**
         * Create a new {@code GET} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@code null}.
         * @param offset bitfield offset, must not be {@code null}.
         * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
         * @since 4.3
         */
        public static BitFieldArgs get(BitFieldType bitFieldType, Offset offset) {
            return new BitFieldArgs().get(bitFieldType, offset);
        }

        /**
         * Create a new {@code SET} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@code null}.
         * @param offset bitfield offset
         * @param value the value
         * @return a new {@code SET} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
         */
        public static BitFieldArgs set(BitFieldType bitFieldType, int offset, long value) {
            return new BitFieldArgs().set(bitFieldType, offset, value);
        }

        /**
         * Create a new {@code SET} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@code null}.
         * @param offset bitfield offset, must not be {@code null}.
         * @param value the value
         * @return a new {@code SET} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
         * @since 4.3
         */
        public static BitFieldArgs set(BitFieldType bitFieldType, Offset offset, long value) {
            return new BitFieldArgs().set(bitFieldType, offset, value);
        }

        /**
         * Create a new {@code INCRBY} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@code null}.
         * @param offset bitfield offset
         * @param value the value
         * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value} .
         */
        public static BitFieldArgs incrBy(BitFieldType bitFieldType, int offset, long value) {
            return new BitFieldArgs().incrBy(bitFieldType, offset, value);
        }

        /**
         * Create a new {@code INCRBY} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@code null}.
         * @param offset bitfield offset, must not be {@code null}.
         * @param value the value
         * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value} .
         * @since 4.3
         */
        public static BitFieldArgs incrBy(BitFieldType bitFieldType, Offset offset, long value) {
            return new BitFieldArgs().incrBy(bitFieldType, offset, value);
        }

        /**
         * Adds a new {@code OVERFLOW} subcommand.
         *
         * @param overflowType type of overflow, must not be {@code null}.
         * @return a new {@code OVERFLOW} subcommand for the given {@code overflowType}.
         */
        public static BitFieldArgs overflow(OverflowType overflowType) {
            return new BitFieldArgs().overflow(overflowType);
        }

    }

    /**
     * Creates a new signed {@link BitFieldType} for the given number of {@code bits}.
     *
     * Redis allows up to {@code 64} bits for unsigned integers.
     *
     * @param bits number of bits to define the integer type width.
     * @return the {@link BitFieldType}.
     */
    public static BitFieldType signed(int bits) {
        return new BitFieldType(true, bits);
    }

    /**
     * Creates a new unsigned {@link BitFieldType} for the given number of {@code bits}. Redis allows up to {@code 63} bits for
     * unsigned integers.
     *
     * @param bits number of bits to define the integer type width.
     * @return the {@link BitFieldType}.
     */
    public static BitFieldType unsigned(int bits) {
        return new BitFieldType(false, bits);
    }

    /**
     * Creates a new {@link Offset} for the given {@code offset}.
     *
     * @param offset zero-based offset.
     * @return the {@link Offset}.
     * @since 4.3
     */
    public static Offset offset(int offset) {
        return new Offset(false, offset);
    }

    /**
     * Creates a new {@link Offset} for the given {@code offset} that is multiplied by the integer type width used in the sub
     * command.
     *
     * @param offset offset to be multiplied by the integer type width.
     * @return the {@link Offset}.
     * @since 4.3
     */
    public static Offset typeWidthBasedOffset(int offset) {
        return new Offset(true, offset);
    }

    /**
     * Adds a new {@link SubCommand} to the {@code BITFIELD} execution.
     *
     * @param subCommand must not be {@code null}.
     */
    private BitFieldArgs addSubCommand(SubCommand subCommand) {

        LettuceAssert.notNull(subCommand, "SubCommand must not be null");
        commands.add(subCommand);
        return this;
    }

    /**
     * Adds a new {@code GET} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs get() {
        return get(previousFieldType());
    }

    /**
     * Adds a new {@code GET} subcommand using offset {@code 0}.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
     */
    public BitFieldArgs get(BitFieldType bitFieldType) {
        return get(bitFieldType, 0);
    }

    /**
     * Adds a new {@code GET} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param offset bitfield offset
     * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
     */
    public BitFieldArgs get(BitFieldType bitFieldType, int offset) {
        return addSubCommand(new Get(bitFieldType, false, offset));
    }

    /**
     * Adds a new {@code GET} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param offset bitfield offset
     * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
     * @since 4.3
     */
    public BitFieldArgs get(BitFieldType bitFieldType, Offset offset) {

        LettuceAssert.notNull(offset, "BitFieldOffset must not be null");

        return addSubCommand(new Get(bitFieldType, offset.isMultiplyByTypeWidth(), offset.getOffset()));
    }

    /**
     * Adds a new {@code GET} subcommand using the field type of the previous command.
     *
     * @param offset bitfield offset
     * @return a new {@code GET} subcommand for the given {@code bitFieldType} and {@code offset}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs get(int offset) {
        return get(previousFieldType(), offset);
    }

    /**
     * Adds a new {@code SET} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @param value the value
     * @return a new {@code SET} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs set(long value) {
        return set(previousFieldType(), value);
    }

    /**
     * Adds a new {@code SET} subcommand using offset {@code 0}.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param value the value
     * @return a new {@code SET} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs set(BitFieldType bitFieldType, long value) {
        return set(bitFieldType, 0, value);
    }

    /**
     * Adds a new {@code SET} subcommand using the field type of the previous command.
     *
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@code SET} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs set(int offset, long value) {
        return set(previousFieldType(), offset, value);
    }

    /**
     * Adds a new {@code SET} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@code SET} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs set(BitFieldType bitFieldType, int offset, long value) {
        return addSubCommand(new Set(bitFieldType, false, offset, value));
    }

    /**
     * Adds a new {@code SET} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param offset bitfield offset, must not be {@code null}.
     * @param value the value
     * @return a new {@code SET} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @since 4.3
     */
    public BitFieldArgs set(BitFieldType bitFieldType, Offset offset, long value) {

        LettuceAssert.notNull(offset, "BitFieldOffset must not be null");

        return addSubCommand(new Set(bitFieldType, offset.isMultiplyByTypeWidth(), offset.getOffset(), value));
    }

    /**
     * Adds a new {@code INCRBY} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @param value the value
     * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs incrBy(long value) {
        return incrBy(previousFieldType(), value);
    }

    /**
     * Adds a new {@code INCRBY} subcommand using offset {@code 0}.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param value the value
     * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs incrBy(BitFieldType bitFieldType, long value) {
        return incrBy(bitFieldType, 0, value);
    }

    /**
     * Adds a new {@code INCRBY} subcommand using the field type of the previous command.
     *
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs incrBy(int offset, long value) {
        return incrBy(previousFieldType(), offset, value);
    }

    /**
     * Adds a new {@code INCRBY} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs incrBy(BitFieldType bitFieldType, int offset, long value) {
        return addSubCommand(new IncrBy(bitFieldType, false, offset, value));
    }

    /**
     * Adds a new {@code INCRBY} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@code null}.
     * @param offset bitfield offset, must not be {@code null}.
     * @param value the value
     * @return a new {@code INCRBY} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @since 4.3
     */
    public BitFieldArgs incrBy(BitFieldType bitFieldType, Offset offset, long value) {

        LettuceAssert.notNull(offset, "BitFieldOffset must not be null");

        return addSubCommand(new IncrBy(bitFieldType, offset.isMultiplyByTypeWidth(), offset.getOffset(), value));
    }

    /**
     * Adds a new {@code OVERFLOW} subcommand.
     *
     * @param overflowType type of overflow, must not be {@code null}.
     * @return a new {@code OVERFLOW} subcommand for the given {@code overflowType}.
     */
    public BitFieldArgs overflow(OverflowType overflowType) {
        return addSubCommand(new Overflow(overflowType));
    }

    private BitFieldType previousFieldType() {

        List<SubCommand> list = new ArrayList<>(commands);
        Collections.reverse(list);

        for (SubCommand command : list) {

            if (command instanceof Get) {
                return ((Get) command).bitFieldType;
            }

            if (command instanceof Set) {
                return ((Set) command).bitFieldType;
            }

            if (command instanceof IncrBy) {
                return ((IncrBy) command).bitFieldType;
            }
        }

        throw new IllegalStateException("No previous field type found");
    }

    /**
     * Representation for the {@code SET} subcommand for {@code BITFIELD}.
     */
    private static class Set extends SubCommand {

        private final BitFieldType bitFieldType;

        private final boolean bitOffset;

        private final long offset;

        private final long value;

        private Set(BitFieldType bitFieldType, boolean bitOffset, int offset, long value) {

            LettuceAssert.notNull(bitFieldType, "BitFieldType must not be null");
            LettuceAssert.isTrue(offset > -1, "Offset must be greater or equal to 0");

            this.bitFieldType = bitFieldType;
            this.bitOffset = bitOffset;
            this.offset = offset;
            this.value = value;
        }

        @Override
        <K, V> void build(CommandArgs<K, V> args) {

            args.add(CommandType.SET).add(bitFieldType.asString());

            if (bitOffset) {
                args.add("#" + offset);
            } else {
                args.add(offset);
            }

            args.add(value);
        }

    }

    /**
     * Representation for the {@code GET} subcommand for {@code BITFIELD}.
     */
    private static class Get extends SubCommand {

        private final BitFieldType bitFieldType;

        private final boolean bitOffset;

        private final int offset;

        private Get(BitFieldType bitFieldType, boolean bitOffset, int offset) {

            LettuceAssert.notNull(bitFieldType, "BitFieldType must not be null");
            LettuceAssert.isTrue(offset > -1, "Offset must be greater or equal to 0");

            this.bitFieldType = bitFieldType;
            this.bitOffset = bitOffset;
            this.offset = offset;
        }

        @Override
        <K, V> void build(CommandArgs<K, V> args) {

            args.add(CommandType.GET).add(bitFieldType.asString());

            if (bitOffset) {
                args.add("#" + offset);
            } else {
                args.add(offset);
            }
        }

    }

    /**
     * Representation for the {@code INCRBY} subcommand for {@code BITFIELD}.
     */
    private static class IncrBy extends SubCommand {

        private final BitFieldType bitFieldType;

        private final boolean bitOffset;

        private final long offset;

        private final long value;

        private IncrBy(BitFieldType bitFieldType, boolean offsetWidthMultiplier, int offset, long value) {

            LettuceAssert.notNull(bitFieldType, "BitFieldType must not be null");
            LettuceAssert.isTrue(offset > -1, "Offset must be greater or equal to 0");

            this.bitFieldType = bitFieldType;
            this.bitOffset = offsetWidthMultiplier;
            this.offset = offset;
            this.value = value;
        }

        @Override
        <K, V> void build(CommandArgs<K, V> args) {

            args.add(CommandType.INCRBY).add(bitFieldType.asString());

            if (bitOffset) {
                args.add("#" + offset);
            } else {
                args.add(offset);
            }

            args.add(value);

        }

    }

    /**
     * Representation for the {@code INCRBY} subcommand for {@code BITFIELD}.
     */
    private static class Overflow extends SubCommand {

        private final OverflowType overflowType;

        private Overflow(OverflowType overflowType) {

            LettuceAssert.notNull(overflowType, "OverflowType must not be null");
            this.overflowType = overflowType;
        }

        @Override
        <K, V> void build(CommandArgs<K, V> args) {
            args.add("OVERFLOW").add(overflowType);
        }

    }

    /**
     * Base class for bitfield subcommands.
     */
    private abstract static class SubCommand {

        abstract <K, V> void build(CommandArgs<K, V> args);

    }

    public <K, V> void build(CommandArgs<K, V> args) {

        for (SubCommand command : commands) {
            command.build(args);
        }
    }

    /**
     * Represents the overflow types for the {@code OVERFLOW} subcommand argument.
     */
    public enum OverflowType implements ProtocolKeyword {

        WRAP, SAT, FAIL;

        public final byte[] bytes;

        OverflowType() {
            bytes = name().getBytes(StandardCharsets.US_ASCII);
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

    }

    /**
     * Represents a bit field type with details about signed/unsigned and the number of bits.
     */
    public static class BitFieldType {

        private final boolean signed;

        private final int bits;

        private BitFieldType(boolean signed, int bits) {

            LettuceAssert.isTrue(bits > 0, "Bits must be greater 0");

            if (signed) {
                LettuceAssert.isTrue(bits < 65, "Signed integers support only up to 64 bits");
            } else {
                LettuceAssert.isTrue(bits < 64, "Unsigned integers support only up to 63 bits");
            }

            this.signed = signed;
            this.bits = bits;
        }

        /**
         *
         * @return {@code true} if the bitfield type is signed.
         */
        public boolean isSigned() {
            return signed;
        }

        /**
         *
         * @return number of bits.
         */
        public int getBits() {
            return bits;
        }

        private String asString() {
            return (signed ? "i" : "u") + bits;
        }

        @Override
        public String toString() {
            return asString();
        }

    }

    /**
     * Represents a bit field offset. See also <a href="http://redis.io/commands/bitfield#bits-and-positional-offsets">Bits and
     * positional offsets</a>
     *
     * @since 4.3
     */
    public static class Offset {

        private final boolean multiplyByTypeWidth;

        private final int offset;

        private Offset(boolean multiplyByTypeWidth, int offset) {

            this.multiplyByTypeWidth = multiplyByTypeWidth;
            this.offset = offset;
        }

        /**
         * @return {@code true} if the offset should be multiplied by integer width that is represented with a leading hash (
         *         {@code #}) when constructing the command
         */
        public boolean isMultiplyByTypeWidth() {
            return multiplyByTypeWidth;
        }

        /**
         *
         * @return the offset.
         */
        public int getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return (multiplyByTypeWidth ? "#" : "") + offset;
        }

    }

}
