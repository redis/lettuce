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
package io.lettuce.core.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.ToByteBufEncoder;
import io.lettuce.core.internal.LettuceAssert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * Redis command arguments. {@link CommandArgs} is a container for multiple singular arguments. Key and Value arguments are
 * encoded using the {@link RedisCodec} to their byte representation. {@link CommandArgs} provides a fluent style of adding
 * multiple arguments. A {@link CommandArgs} instance can be reused across multiple commands and invocations.
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * new CommandArgs&lt;&gt;(codec).addKey(key).addValue(value).add(CommandKeyword.FORCE);
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class CommandArgs<K, V> {

    static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);

    protected final RedisCodec<K, V> codec;

    final List<SingularArgument> singularArguments = new ArrayList<>(10);

    /**
     * @param codec Codec used to encode/decode keys and values, must not be {@code null}.
     */
    public CommandArgs(RedisCodec<K, V> codec) {

        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        this.codec = codec;
    }

    /**
     *
     * @return the number of arguments.
     */
    public int count() {
        return singularArguments.size();
    }

    /**
     * Adds a key argument.
     *
     * @param key the key
     * @return the command args.
     */
    public CommandArgs<K, V> addKey(K key) {

        singularArguments.add(KeyArgument.of(key, codec));
        return this;
    }

    /**
     * Add multiple key arguments.
     *
     * @param keys must not be {@code null}.
     * @return the command args.
     */
    public CommandArgs<K, V> addKeys(Iterable<K> keys) {

        LettuceAssert.notNull(keys, "Keys must not be null");

        for (K key : keys) {
            addKey(key);
        }
        return this;
    }

    /**
     * Add multiple key arguments.
     *
     * @param keys must not be {@code null}.
     * @return the command args.
     */
    @SafeVarargs
    public final CommandArgs<K, V> addKeys(K... keys) {

        LettuceAssert.notNull(keys, "Keys must not be null");

        for (K key : keys) {
            addKey(key);
        }
        return this;
    }

    /**
     * Add a value argument.
     *
     * @param value the value
     * @return the command args.
     */
    public CommandArgs<K, V> addValue(V value) {

        singularArguments.add(ValueArgument.of(value, codec));
        return this;
    }

    /**
     * Add multiple value arguments.
     *
     * @param values must not be {@code null}.
     * @return the command args.
     */
    public CommandArgs<K, V> addValues(Iterable<V> values) {

        LettuceAssert.notNull(values, "Values must not be null");

        for (V value : values) {
            addValue(value);
        }
        return this;
    }

    /**
     * Add multiple value arguments.
     *
     * @param values must not be {@code null}.
     * @return the command args.
     */
    @SafeVarargs
    public final CommandArgs<K, V> addValues(V... values) {

        LettuceAssert.notNull(values, "Values must not be null");

        for (V value : values) {
            addValue(value);
        }
        return this;
    }

    /**
     * Add a map (hash) argument.
     *
     * @param map the map, must not be {@code null}.
     * @return the command args.
     */
    public CommandArgs<K, V> add(Map<K, V> map) {

        LettuceAssert.notNull(map, "Map must not be null");

        for (Map.Entry<K, V> entry : map.entrySet()) {
            addKey(entry.getKey()).addValue(entry.getValue());
        }

        return this;
    }

    /**
     * Add a string argument. The argument is represented as bulk string.
     *
     * @param s the string.
     * @return the command args.
     */
    public CommandArgs<K, V> add(String s) {

        singularArguments.add(StringArgument.of(s));
        return this;
    }

    /**
     * Add a string as char-array. The argument is represented as bulk string.
     *
     * @param cs the string.
     * @return the command args.
     */
    public CommandArgs<K, V> add(char[] cs) {

        singularArguments.add(CharArrayArgument.of(cs));
        return this;
    }

    /**
     * Add an 64-bit integer (long) argument.
     *
     * @param n the argument.
     * @return the command args.
     */
    public CommandArgs<K, V> add(long n) {

        singularArguments.add(IntegerArgument.of(n));
        return this;
    }

    /**
     * Add a double argument.
     *
     * @param n the double argument.
     * @return the command args.
     */
    public CommandArgs<K, V> add(double n) {

        singularArguments.add(DoubleArgument.of(n));
        return this;
    }

    /**
     * Add a byte-array argument. The argument is represented as bulk string.
     *
     * @param value the byte-array.
     * @return the command args.
     */
    public CommandArgs<K, V> add(byte[] value) {

        singularArguments.add(BytesArgument.of(value));
        return this;
    }

    /**
     * Add a {@link CommandKeyword} argument. The argument is represented as bulk string.
     *
     * @param keyword must not be {@code null}.
     * @return the command args.
     */
    public CommandArgs<K, V> add(CommandKeyword keyword) {

        LettuceAssert.notNull(keyword, "CommandKeyword must not be null");
        singularArguments.add(ProtocolKeywordArgument.of(keyword));
        return this;
    }

    /**
     * Add a {@link CommandType} argument. The argument is represented as bulk string.
     *
     * @param type must not be {@code null}.
     * @return the command args.
     */
    public CommandArgs<K, V> add(CommandType type) {

        LettuceAssert.notNull(type, "CommandType must not be null");
        singularArguments.add(ProtocolKeywordArgument.of(type));
        return this;
    }

    /**
     * Add a {@link ProtocolKeyword} argument. The argument is represented as bulk string.
     *
     * @param keyword the keyword, must not be {@code null}
     * @return the command args.
     */
    public CommandArgs<K, V> add(ProtocolKeyword keyword) {

        LettuceAssert.notNull(keyword, "CommandKeyword must not be null");
        singularArguments.add(ProtocolKeywordArgument.of(keyword));
        return this;
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());

        ByteBuf buffer = UnpooledByteBufAllocator.DEFAULT.buffer(singularArguments.size() * 10);
        encode(buffer);
        buffer.resetReaderIndex();

        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        sb.append(" [buffer=").append(new String(bytes));
        sb.append(']');
        buffer.release();

        return sb.toString();
    }

    /**
     * Returns a command string representation of {@link CommandArgs} with annotated key and value parameters.
     *
     * {@code args.addKey("mykey").add(2.0)} will return {@code key<mykey> 2.0}.
     *
     * @return the command string representation.
     */
    public String toCommandString() {
        return LettuceStrings.collectionToDelimitedString(singularArguments, " ", "", "");
    }

    /**
     * Returns the first integer argument.
     *
     * @return the first integer argument or {@code null}.
     */
    @Deprecated
    public Long getFirstInteger() {
        return CommandArgsAccessor.getFirstInteger(this);
    }

    /**
     * Returns the first string argument.
     *
     * @return the first string argument or {@code null}.
     */
    @Deprecated
    public String getFirstString() {
        return CommandArgsAccessor.getFirstString(this);
    }

    /**
     * Returns the first key argument in its byte-encoded representation.
     *
     * @return the first key argument in its byte-encoded representation or {@code null}.
     */
    public ByteBuffer getFirstEncodedKey() {
        return CommandArgsAccessor.encodeFirstKey(this);
    }

    /**
     * Encode the {@link CommandArgs} and write the arguments to the {@link ByteBuf}.
     *
     * @param buf the target buffer.
     */
    public void encode(ByteBuf buf) {

        buf.touch("CommandArgs.encode(…)");
        for (SingularArgument singularArgument : singularArguments) {
            singularArgument.encode(buf);
        }
    }

    /**
     * Single argument wrapper that can be encoded.
     */
    static abstract class SingularArgument {

        /**
         * Encode the argument and write it to the {@code buffer}.
         *
         * @param buffer
         */
        abstract void encode(ByteBuf buffer);

    }

    static class BytesArgument extends SingularArgument {

        final byte[] val;

        private BytesArgument(byte[] val) {
            this.val = val;
        }

        static BytesArgument of(byte[] val) {
            return new BytesArgument(val);
        }

        @Override
        void encode(ByteBuf buffer) {
            writeBytes(buffer, val);
        }

        static void writeBytes(ByteBuf buffer, byte[] value) {

            buffer.writeByte('$');

            IntegerArgument.writeInteger(buffer, value.length);
            buffer.writeBytes(CRLF);

            buffer.writeBytes(value);
            buffer.writeBytes(CRLF);
        }

        @Override
        public String toString() {
            return Base64.getEncoder().encodeToString(val);
        }

    }

    static class ProtocolKeywordArgument extends BytesArgument {

        private final ProtocolKeyword protocolKeyword;

        private ProtocolKeywordArgument(ProtocolKeyword protocolKeyword) {
            super(protocolKeyword.getBytes());
            this.protocolKeyword = protocolKeyword;
        }

        static BytesArgument of(ProtocolKeyword protocolKeyword) {

            if (protocolKeyword instanceof CommandType) {
                return CommandTypeCache.cache[((Enum) protocolKeyword).ordinal()];
            }

            if (protocolKeyword instanceof CommandKeyword) {
                return CommandKeywordCache.cache[((Enum) protocolKeyword).ordinal()];
            }

            return ProtocolKeywordArgument.of(protocolKeyword.getBytes());
        }

        @Override
        public String toString() {
            return protocolKeyword.name();
        }

    }

    static class CommandTypeCache {

        static final ProtocolKeywordArgument cache[];

        static {

            CommandType[] values = CommandType.values();
            cache = new ProtocolKeywordArgument[values.length];
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new ProtocolKeywordArgument(values[i]);
            }
        }

    }

    static class CommandKeywordCache {

        static final ProtocolKeywordArgument cache[];

        static {

            CommandKeyword[] values = CommandKeyword.values();
            cache = new ProtocolKeywordArgument[values.length];
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new ProtocolKeywordArgument(values[i]);
            }
        }

    }

    static class ByteBufferArgument {

        static void writeByteBuffer(ByteBuf target, ByteBuffer value) {

            target.writeByte('$');

            IntegerArgument.writeInteger(target, value.remaining());
            target.writeBytes(CRLF);

            target.writeBytes(value);
            target.writeBytes(CRLF);
        }

        static void writeByteBuf(ByteBuf target, ByteBuf value) {

            target.writeByte('$');

            IntegerArgument.writeInteger(target, value.readableBytes());
            target.writeBytes(CRLF);

            target.writeBytes(value);
            target.writeBytes(CRLF);
        }

    }

    static class IntegerArgument extends SingularArgument {

        final long val;

        private IntegerArgument(long val) {
            this.val = val;
        }

        static IntegerArgument of(long val) {

            if (val >= 0 && val < IntegerCache.cache.length) {
                return IntegerCache.cache[(int) val];
            }

            if (val < 0 && -val < IntegerCache.cache.length) {
                return IntegerCache.negativeCache[(int) -val];
            }

            return new IntegerArgument(val);
        }

        @Override
        void encode(ByteBuf target) {
            StringArgument.writeString(target, Long.toString(val));
        }

        @Override
        public String toString() {
            return "" + val;
        }

        static void writeInteger(ByteBuf target, long value) {

            if (value < 10) {
                target.writeByte((byte) ('0' + value));
                return;
            }

            String asString = Long.toString(value);

            for (int i = 0; i < asString.length(); i++) {
                target.writeByte((byte) asString.charAt(i));
            }
        }

    }

    static class IntegerCache {

        static final IntegerArgument cache[];

        static final IntegerArgument negativeCache[];

        static {
            int high = Integer.getInteger("io.lettuce.core.CommandArgs.IntegerCache", 128);
            cache = new IntegerArgument[high];
            negativeCache = new IntegerArgument[high];
            for (int i = 0; i < high; i++) {
                cache[i] = new IntegerArgument(i);
                negativeCache[i] = new IntegerArgument(-i);
            }
        }

    }

    static class DoubleArgument extends SingularArgument {

        final double val;

        private DoubleArgument(double val) {
            this.val = val;
        }

        static DoubleArgument of(double val) {
            return new DoubleArgument(val);
        }

        @Override
        void encode(ByteBuf target) {
            StringArgument.writeString(target, Double.toString(val));
        }

        @Override
        public String toString() {
            return "" + val;
        }

    }

    static class StringArgument extends SingularArgument {

        final String val;

        private StringArgument(String val) {
            this.val = val;
        }

        static StringArgument of(String val) {
            return new StringArgument(val);
        }

        @Override
        void encode(ByteBuf target) {
            writeString(target, val);
        }

        static void writeString(ByteBuf target, String value) {

            target.writeByte('$');

            IntegerArgument.writeInteger(target, value.length());
            target.writeBytes(CRLF);

            for (int i = 0; i < value.length(); i++) {
                target.writeByte((byte) value.charAt(i));
            }
            target.writeBytes(CRLF);
        }

        @Override
        public String toString() {
            return val;
        }

    }

    static class CharArrayArgument extends SingularArgument {

        final char[] val;

        private CharArrayArgument(char[] val) {
            this.val = val;
        }

        static CharArrayArgument of(char[] val) {
            return new CharArrayArgument(val);
        }

        @Override
        void encode(ByteBuf target) {
            writeString(target, val);
        }

        static void writeString(ByteBuf target, char[] value) {

            target.writeByte('$');

            IntegerArgument.writeInteger(target, value.length);
            target.writeBytes(CRLF);

            for (int i = 0; i < value.length; i++) {
                target.writeByte((byte) value[i]);
            }
            target.writeBytes(CRLF);
        }

        @Override
        public String toString() {
            return new String(val);
        }

    }

    static class KeyArgument<K, V> extends SingularArgument {

        final K key;

        final RedisCodec<K, V> codec;

        private KeyArgument(K key, RedisCodec<K, V> codec) {
            this.key = key;
            this.codec = codec;
        }

        static <K, V> KeyArgument<K, V> of(K key, RedisCodec<K, V> codec) {
            return new KeyArgument<>(key, codec);
        }

        @SuppressWarnings("unchecked")
        @Override
        void encode(ByteBuf target) {

            if (codec instanceof ToByteBufEncoder) {

                ToByteBufEncoder<K, V> toByteBufEncoder = (ToByteBufEncoder<K, V>) codec;
                ByteBuf temporaryBuffer = target.alloc().buffer(toByteBufEncoder.estimateSize(key) + 6);

                try {

                    toByteBufEncoder.encodeKey(key, temporaryBuffer);
                    ByteBufferArgument.writeByteBuf(target, temporaryBuffer);
                } finally {
                    temporaryBuffer.release();
                }

                return;
            }

            ByteBufferArgument.writeByteBuffer(target, codec.encodeKey(key));
        }

        @Override
        public String toString() {
            return String.format("key<%s>", new StringCodec().decodeKey(codec.encodeKey(key)));
        }

    }

    static class ValueArgument<K, V> extends SingularArgument {

        final V val;

        final RedisCodec<K, V> codec;

        private ValueArgument(V val, RedisCodec<K, V> codec) {
            this.val = val;
            this.codec = codec;
        }

        static <K, V> ValueArgument<K, V> of(V val, RedisCodec<K, V> codec) {
            return new ValueArgument<>(val, codec);
        }

        @SuppressWarnings("unchecked")
        @Override
        void encode(ByteBuf target) {

            if (codec instanceof ToByteBufEncoder) {

                ToByteBufEncoder<K, V> toByteBufEncoder = (ToByteBufEncoder<K, V>) codec;
                ByteBuf temporaryBuffer = target.alloc().buffer(toByteBufEncoder.estimateSize(val) + 6);

                try {
                    toByteBufEncoder.encodeValue(val, temporaryBuffer);
                    ByteBufferArgument.writeByteBuf(target, temporaryBuffer);
                } finally {
                    temporaryBuffer.release();
                }

                return;
            }

            ByteBufferArgument.writeByteBuffer(target, codec.encodeValue(val));
        }

        @Override
        public String toString() {
            return String.format("value<%s>", new StringCodec().decodeValue(codec.encodeValue(val)));
        }

    }

}
