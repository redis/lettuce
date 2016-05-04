// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * Redis command arguments.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class CommandArgs<K, V> {

    protected final RedisCodec<K, V> codec;

    static final byte[] CRLF = "\r\n".getBytes(LettuceCharsets.ASCII);

    private final List<SingularArgument> singularArguments = new ArrayList<>(10);
    private Long firstInteger;
    private String firstString;
    private ByteBuffer firstEncodedKey;
    private K firstKey;

    /**
     *
     * @param codec Codec used to encode/decode keys and values, must not be {@literal null}.
     */
    public CommandArgs(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        this.codec = codec;
    }

    public int count() {
        return singularArguments.size();
    }

    public CommandArgs<K, V> addKey(K key) {

        if (firstKey == null) {
            firstKey = key;
        }

        singularArguments.add(new KeyArgument<>(key, codec));
        return this;
    }

    public CommandArgs<K, V> addKeys(Iterable<K> keys) {

        for (K key : keys) {
            addKey(key);
        }
        return this;
    }

    public CommandArgs<K, V> addKeys(K... keys) {

        for (K key : keys) {
            addKey(key);
        }
        return this;
    }

    public CommandArgs<K, V> addValue(V value) {

        singularArguments.add(new ValueArgument<>(value, codec));
        return this;
    }

    public CommandArgs<K, V> addValues(V... values) {
        for (V value : values) {
            addValue(value);
        }
        return this;
    }

    public CommandArgs<K, V> add(Map<K, V> map) {

        for (Map.Entry<K, V> entry : map.entrySet()) {
            addKey(entry.getKey()).addValue(entry.getValue());
        }

        return this;
    }

    public CommandArgs<K, V> add(String s) {

        if (firstString == null) {
            firstString = s;
        }

        singularArguments.add(new StringArgument(s));
        return this;
    }

    public CommandArgs<K, V> add(long n) {

        if (firstInteger == null) {
            firstInteger = n;
        }

        singularArguments.add(new IntegerArgument(n));
        return this;
    }

    public CommandArgs<K, V> add(double n) {

        singularArguments.add(new DoubleArgument(n));
        return this;
    }

    public CommandArgs<K, V> add(byte[] value) {

        singularArguments.add(new BytesArgument(value));
        return this;
    }

    public CommandArgs<K, V> add(CommandKeyword keyword) {
        return add(keyword.bytes);
    }

    public CommandArgs<K, V> add(CommandType type) {
        return add(type.bytes);
    }

    public CommandArgs<K, V> add(ProtocolKeyword keyword) {
        return add(keyword.getBytes());
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

    public Long getFirstInteger() {

        if (firstInteger == null) {
            return null;
        }

        return firstInteger;
    }

    public String getFirstString() {

        if (firstString == null) {
            return null;
        }

        return firstString;
    }

    public ByteBuffer getFirstEncodedKey() {

        if (firstKey == null) {
            return null;
        }

        if (firstEncodedKey == null) {
            firstEncodedKey = codec.encodeKey(firstKey);
        }

        return firstEncodedKey.duplicate();
    }

    public void encode(ByteBuf buf) {

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
        private final byte[] val;

        public BytesArgument(byte[] val) {
            this.val = val;
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
    }

    static class ByteBufferArgument {

        static void writeByteBuffer(ByteBuf target, ByteBuffer value) {

            target.writeByte('$');

            IntegerArgument.writeInteger(target, value.remaining());
            target.writeBytes(CRLF);

            target.writeBytes(value);
            target.writeBytes(CRLF);
        }
    }

    static class IntegerArgument extends SingularArgument {

        private final long val;

        public IntegerArgument(long val) {
            this.val = val;
        }

        @Override
        void encode(ByteBuf target) {
            StringArgument.writeString(target, Long.toString(val));
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

    static class DoubleArgument extends SingularArgument {

        private final double val;

        public DoubleArgument(double val) {
            this.val = val;
        }

        @Override
        void encode(ByteBuf target) {
            StringArgument.writeString(target, Double.toString(val));
        }
    }

    static class StringArgument extends SingularArgument {

        private final String val;

        public StringArgument(String val) {
            this.val = val;
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
    }

    static class KeyArgument<K, V> extends SingularArgument {

        final K val;
        final RedisCodec<K, V> codec;

        public KeyArgument(K val, RedisCodec<K, V> codec) {
            this.val = val;
            this.codec = codec;
        }

        @Override
        void encode(ByteBuf target) {

            if (codec == ExperimentalByteArrayCodec.INSTANCE) {
                ((ExperimentalByteArrayCodec) codec).encodeKey(target, (byte[]) val);
                return;
            }
            ByteBufferArgument.writeByteBuffer(target, codec.encodeKey(val));
        }
    }

    static class ValueArgument<K, V> extends SingularArgument {

        final V val;
        final RedisCodec<K, V> codec;

        public ValueArgument(V val, RedisCodec<K, V> codec) {
            this.val = val;
            this.codec = codec;
        }

        @Override
        void encode(ByteBuf target) {

            if (codec == ExperimentalByteArrayCodec.INSTANCE) {
                ((ExperimentalByteArrayCodec) codec).encodeValue(target, (byte[]) val);
                return;
            }

            ByteBufferArgument.writeByteBuffer(target, codec.encodeValue(val));
        }
    }

    /**
     * This codec writes directly {@code byte[]} to the target buffer.
     */
    public final static class ExperimentalByteArrayCodec extends ByteArrayCodec {

        public final static ExperimentalByteArrayCodec INSTANCE = new ExperimentalByteArrayCodec();

        private ExperimentalByteArrayCodec() {

        }

        public void encodeKey(ByteBuf target, byte[] key) {
            target.writeBytes(key);
        }

        public void encodeValue(ByteBuf target, byte[] value) {
            target.writeBytes(value);
        }
    }

}
