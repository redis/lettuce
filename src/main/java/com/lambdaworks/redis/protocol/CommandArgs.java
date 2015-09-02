// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Redis command arguments.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class CommandArgs<K, V> {
    private static final byte[] CRLF = "\r\n".getBytes(LettuceCharsets.ASCII);

    private final RedisCodec<K, V> codec;
    private ByteBuffer buffer;
    private ByteBuffer firstEncodedKey;
    private int count;

    private Long firstInteger;
    private String firstString;

    /**
     *
     * @param codec Codec used to encode/decode keys and values, must not be {@literal null}.
     */
    public CommandArgs(RedisCodec<K, V> codec) {
        checkArgument(codec != null, "RedisCodec must not be null");
        this.codec = codec;
        this.buffer = ByteBuffer.allocate(32);
    }

    ByteBuffer buffer() {
        buffer.flip();
        return buffer;
    }

    public int count() {
        return count;
    }

    public CommandArgs<K, V> addKey(K key) {

        if (firstEncodedKey == null) {
            firstEncodedKey = codec.encodeKey(key);
            return write(firstEncodedKey.duplicate());
        }

        return write(codec.encodeKey(key));
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
        return write(codec.encodeValue(value));
    }

    public CommandArgs<K, V> addValues(V... values) {
        for (V value : values) {
            addValue(value);
        }
        return this;
    }

    public CommandArgs<K, V> add(Map<K, V> map) {
        if (map.size() > 2) {
            realloc(buffer.capacity() + 16 * map.size());
        }

        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (firstEncodedKey == null) {
                firstEncodedKey = codec.encodeKey(entry.getKey());
                write(firstEncodedKey.duplicate());

            } else {
                write(codec.encodeKey(entry.getKey()));
            }

            write(codec.encodeValue(entry.getValue()));
        }

        return this;
    }

    public CommandArgs<K, V> add(String s) {
        if (firstString == null) {
            firstString = s;
        }
        return write(s);
    }

    public CommandArgs<K, V> add(long n) {
        if (firstInteger == null) {
            firstInteger = n;
        }
        return write(Long.toString(n));
    }

    public CommandArgs<K, V> add(double n) {
        return write(Double.toString(n));
    }

    public CommandArgs<K, V> add(byte[] value) {
        return write(value);
    }

    public CommandArgs<K, V> add(CommandKeyword keyword) {
        return write(keyword.bytes);
    }

    public CommandArgs<K, V> add(CommandType type) {
        return write(type.bytes);
    }

    public CommandArgs<K, V> add(ProtocolKeyword keyword) {
        return write(keyword.getBytes());
    }

    private CommandArgs<K, V> write(ByteBuffer arg) {
        buffer.mark();

        if (buffer.remaining() < arg.remaining()) {
            int estimate = buffer.remaining() + arg.remaining() + 10;
            realloc(max(buffer.capacity() * 2, estimate));
        }

        while (true) {
            try {
                ByteBuffer toWrite = arg.duplicate();
                buffer.put((byte) '$');
                write(toWrite.remaining());
                buffer.put(CRLF);
                buffer.put(toWrite);
                buffer.put(CRLF);
                break;
            } catch (BufferOverflowException e) {
                buffer.reset();
                realloc(buffer.capacity() * 2);
            }
        }

        count++;
        return this;
    }

    private CommandArgs<K, V> write(byte[] arg) {
        buffer.mark();

        if (buffer.remaining() < arg.length) {
            int estimate = buffer.remaining() + arg.length + 10;
            realloc(max(buffer.capacity() * 2, estimate));
        }

        while (true) {
            try {
                buffer.put((byte) '$');
                write(arg.length);
                buffer.put(CRLF);
                buffer.put(arg);
                buffer.put(CRLF);
                break;
            } catch (BufferOverflowException e) {
                buffer.reset();
                realloc(buffer.capacity() * 2);
            }
        }

        count++;
        return this;
    }

    private CommandArgs<K, V> write(String arg) {
        int length = arg.length();

        buffer.mark();

        if (buffer.remaining() < length) {
            int estimate = buffer.remaining() + length + 10;
            realloc(max(buffer.capacity() * 2, estimate));
        }

        while (true) {
            try {
                buffer.put((byte) '$');
                write(length);
                buffer.put(CRLF);
                for (int i = 0; i < length; i++) {
                    buffer.put((byte) arg.charAt(i));
                }
                buffer.put(CRLF);
                break;
            } catch (BufferOverflowException e) {
                buffer.reset();
                realloc(buffer.capacity() * 2);
            }
        }

        count++;
        return this;
    }

    private void write(long value) {
        if (value < 10) {
            buffer.put((byte) ('0' + value));
            return;
        }

        StringBuilder sb = new StringBuilder(8);
        while (value > 0) {
            long digit = value % 10;
            sb.append((char) ('0' + digit));
            value /= 10;
        }

        for (int i = sb.length() - 1; i >= 0; i--) {
            buffer.put((byte) sb.charAt(i));
        }
    }

    private void realloc(int size) {
        ByteBuffer newBuffer = ByteBuffer.allocate(size);
        this.buffer.flip();
        newBuffer.put(this.buffer);
        newBuffer.mark();
        this.buffer = newBuffer;
    }

    public ByteBuffer getFirstEncodedKey() {
        if (firstEncodedKey != null) {
            return firstEncodedKey.duplicate();
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [buffer=").append(new String(buffer.array()));
        sb.append(']');
        return sb.toString();
    }

    public Long getFirstInteger() {
        return firstInteger;
    }

    public String getFirstString() {
        return firstString;
    }
}
