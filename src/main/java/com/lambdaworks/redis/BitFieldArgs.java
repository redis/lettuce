package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.LettuceCharsets;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

/**
 * Arguments and types for the {@code BITFIELD} command.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public class BitFieldArgs {

    private List<SubCommand> commands;

    /**
     * Creates a new {@link BitFieldArgs} instance.
     */
    public BitFieldArgs() {
        this(new ArrayList<>());
    }

    private BitFieldArgs(List<SubCommand> commands) {
        LettuceAssert.notNull(commands, "commands must not be null");
        this.commands = commands;
    }

    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        /**
         * Adds a new {@link Get} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@literal null}.
         * @param offset bitfield offset
         * @return a new {@link Get} subcommand for the given {@code bitFieldType} and {@code offset}.
         */
        public static BitFieldArgs get(BitFieldType bitFieldType, int offset) {
            return new BitFieldArgs().get(bitFieldType, offset);
        }

        /**
         * Adds a new {@link Set} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@literal null}.
         * @param offset bitfield offset
         * @param value the value
         * @return a new {@link Set} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
         */
        public static BitFieldArgs set(BitFieldType bitFieldType, int offset, long value) {
            return new BitFieldArgs().set(bitFieldType, offset, value);
        }

        /**
         * Adds a new {@link IncrBy} subcommand.
         *
         * @param bitFieldType the bit field type, must not be {@literal null}.
         * @param offset bitfield offset
         * @param value the value
         * @return a new {@link IncrBy} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
         */
        public static BitFieldArgs incrBy(BitFieldType bitFieldType, int offset, long value) {
            return new BitFieldArgs().incrBy(bitFieldType, offset, value);
        }

        /**
         * Adds a new {@link Overflow} subcommand.
         *
         * @param overflowType type of overflow, must not be {@literal null}.
         * @return a new {@link Overflow} subcommand for the given {@code overflowType}.
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
     * @param bits
     * @return
     */
    public static BitFieldType signed(int bits) {
        return new BitFieldType(true, bits);
    }

    /**
     * Creates a new unsigned {@link BitFieldType} for the given number of {@code bits}. Redis allows up to {@code 63} bits for
     * unsigned integers.
     *
     * @param bits
     * @return
     */
    public static BitFieldType unsigned(int bits) {
        return new BitFieldType(false, bits);
    }

    /**
     * Adds a new {@link SubCommand} to the {@code BITFIELD} execution.
     *
     * @param subCommand
     */
    private BitFieldArgs addSubCommand(SubCommand subCommand) {
        LettuceAssert.notNull(subCommand, "subCommand must not be null");
        commands.add(subCommand);
        return this;
    }

    /**
     * Adds a new {@link Get} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @return a new {@link Get} subcommand for the given {@code bitFieldType} and {@code offset}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs get() {
        return get(previousFieldType());
    }

    /**
     * Adds a new {@link Get} subcommand using offset {@code 0}.
     *
     * @param bitFieldType the bit field type, must not be {@literal null}.
     * @return a new {@link Get} subcommand for the given {@code bitFieldType} and {@code offset}.
     */
    public BitFieldArgs get(BitFieldType bitFieldType) {
        return get(bitFieldType, 0);
    }

    /**
     * Adds a new {@link Get} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@literal null}.
     * @param offset bitfield offset
     * @return a new {@link Get} subcommand for the given {@code bitFieldType} and {@code offset}.
     */
    public BitFieldArgs get(BitFieldType bitFieldType, int offset) {
        return addSubCommand(new Get(bitFieldType, offset));
    }

    /**
     * Adds a new {@link Get} subcommand using the field type of the previous command.
     *
     * @param offset bitfield offset
     * @return a new {@link Get} subcommand for the given {@code bitFieldType} and {@code offset}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs get(int offset) {
        return get(previousFieldType(), offset);
    }

    /**
     * Adds a new {@link Set} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @param value the value
     * @return a new {@link Set} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs set(long value) {
        return set(previousFieldType(), value);
    }

    /**
     * Adds a new {@link Set} subcommand using offset {@code 0}.
     *
     * @param bitFieldType the bit field type, must not be {@literal null}.
     * @param value the value
     * @return a new {@link Set} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs set(BitFieldType bitFieldType, long value) {
        return set(bitFieldType, 0, value);
    }

    /**
     * Adds a new {@link Set} subcommand using the field type of the previous command.
     *
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@link Set} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs set(int offset, long value) {
        return set(previousFieldType(), offset, value);
    }

    /**
     * Adds a new {@link Set} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@literal null}.
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@link Set} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs set(BitFieldType bitFieldType, int offset, long value) {
        return addSubCommand(new Set(bitFieldType, offset, value));
    }

    /**
     * Adds a new {@link IncrBy} subcommand using offset {@code 0} and the field type of the previous command.
     *
     * @param value the value
     * @return a new {@link IncrBy} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs incrBy(long value) {
        return incrBy(previousFieldType(), value);
    }

    /**
     * Adds a new {@link IncrBy} subcommand using offset {@code 0}.
     *
     * @param bitFieldType the bit field type, must not be {@literal null}.
     * @param value the value
     * @return a new {@link IncrBy} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs incrBy(BitFieldType bitFieldType, long value) {
        return incrBy(bitFieldType, 0, value);
    }

    /**
     * Adds a new {@link IncrBy} subcommand using the field type of the previous command.
     *
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@link IncrBy} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     * @throws IllegalStateException if no previous field type was found
     */
    public BitFieldArgs incrBy(int offset, long value) {
        return incrBy(previousFieldType(), offset, value);
    }

    /**
     * Adds a new {@link IncrBy} subcommand.
     *
     * @param bitFieldType the bit field type, must not be {@literal null}.
     * @param offset bitfield offset
     * @param value the value
     * @return a new {@link IncrBy} subcommand for the given {@code bitFieldType}, {@code offset} and {@code value}.
     */
    public BitFieldArgs incrBy(BitFieldType bitFieldType, int offset, long value) {
        return addSubCommand(new IncrBy(bitFieldType, offset, value));
    }

    /**
     * Adds a new {@link Overflow} subcommand.
     *
     * @param overflowType type of overflow, must not be {@literal null}.
     * @return a new {@link Overflow} subcommand for the given {@code overflowType}.
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
        private final long offset;
        private final long value;

        private Set(BitFieldType bitFieldType, int offset, long value) {

            LettuceAssert.notNull(bitFieldType, "bitFieldType must not be null");
            LettuceAssert.isTrue(offset > -1, "offset must be greater or equal to 0");

            this.offset = offset;
            this.bitFieldType = bitFieldType;
            this.value = value;
        }

        @Override
        <K, V> void build(CommandArgs<K, V> args) {
            args.add(CommandType.SET).add(bitFieldType.asString()).add(offset).add(value);
        }
    }

    /**
     * Representation for the {@code GET} subcommand for {@code BITFIELD}.
     */
    private static class Get extends SubCommand {

        private final BitFieldType bitFieldType;
        private final long offset;

        private Get(BitFieldType bitFieldType, int offset) {

            LettuceAssert.notNull(bitFieldType, "bitFieldType must not be null");
            LettuceAssert.isTrue(offset > -1, "offset must be greater or equal to 0");

            this.offset = offset;
            this.bitFieldType = bitFieldType;
        }

        @Override
        <K, V> void build(CommandArgs<K, V> args) {
            args.add(CommandType.GET).add(bitFieldType.asString()).add(offset);
        }
    }

    /**
     * Representation for the {@code INCRBY} subcommand for {@code BITFIELD}.
     */
    private static class IncrBy extends SubCommand {

        private final BitFieldType bitFieldType;
        private final long offset;
        private final long value;

        private IncrBy(BitFieldType bitFieldType, int offset, long value) {

            LettuceAssert.notNull(bitFieldType, "bitFieldType must not be null");
            LettuceAssert.isTrue(offset > -1, "offset must be greater or equal to 0");

            this.offset = offset;
            this.bitFieldType = bitFieldType;
            this.value = value;
        }

        @Override
        <K, V> void build(CommandArgs<K, V> args) {
            args.add(CommandType.INCRBY).add(bitFieldType.asString()).add(offset).add(value);
        }
    }

    /**
     * Representation for the {@code INCRBY} subcommand for {@code BITFIELD}.
     */
    private static class Overflow extends SubCommand {

        private final OverflowType overflowType;

        private Overflow(OverflowType overflowType) {

            LettuceAssert.notNull(overflowType, "overflowType must not be null");
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

    <K, V> void build(CommandArgs<K, V> args) {

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

        private OverflowType() {
            bytes = name().getBytes(LettuceCharsets.ASCII);
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

            LettuceAssert.isTrue(bits > 0, "bits must be greater 0");

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
         * @return {@literal true} if the bitfield type is signed.
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
    }
}
