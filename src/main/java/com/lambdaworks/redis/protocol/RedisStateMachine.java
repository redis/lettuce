// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import static com.lambdaworks.redis.protocol.LettuceCharsets.buffer;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.BULK;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.BYTES;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.ERROR;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.INTEGER;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.MULTI;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.SINGLE;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;

import com.lambdaworks.redis.RedisException;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * State machine that decodes redis server responses encoded according to the <a href="http://redis.io/topics/protocol">Unified
 * Request Protocol</a>.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class RedisStateMachine<K, V> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisStateMachine.class);
    private static final ByteBuffer QUEUED = buffer("QUEUED");

    static class State {
        enum Type {
            SINGLE, ERROR, INTEGER, BULK, MULTI, BYTES
        }

        Type type = null;
        int count = -1;
    }

    private final Deque<State> stack;

    /**
     * Initialize a new instance.
     */
    public RedisStateMachine() {
        stack = new LinkedList<State>();
    }

    /**
     * Decode a command using the input buffer.
     * 
     * @param buffer Buffer containing data from the server.
     * @param output Current command output.
     * @return true if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, CommandOutput<K, V, ?> output) {
        return decode(buffer, null, output);
    }

    /**
     * Attempt to decode a redis response and return a flag indicating whether a complete response was read.
     * 
     * @param buffer Buffer containing data from the server.
     * @param command the command itself
     * @param output Current command output.
     * 
     * @return true if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, RedisCommand<K, V, ?> command, CommandOutput<K, V, ?> output) {
        int length, end;
        ByteBuffer bytes;

        if (logger.isDebugEnabled()) {
            logger.debug("Decode " + command);
        }

        if (stack.isEmpty()) {
            stack.add(new State());
        }

        if (output == null) {
            return stack.isEmpty();
        }

        loop:

        while (!stack.isEmpty()) {
            State state = stack.peek();

            if (state.type == null) {
                if (!buffer.isReadable()) {
                    break;
                }
                state.type = readReplyType(buffer);
                buffer.markReaderIndex();
            }

            switch (state.type) {
                case SINGLE:
                    if ((bytes = readLine(buffer)) == null) {
                        break loop;
                    }
                    if (!QUEUED.equals(bytes)) {
                        safeSet(output, bytes, command);
                    }
                    break;
                case ERROR:
                    if ((bytes = readLine(buffer)) == null) {
                        break loop;
                    }
                    safeSetError(output, bytes, command);
                    break;
                case INTEGER:
                    if ((end = findLineEnd(buffer)) == -1) {
                        break loop;
                    }
                    long integer = readLong(buffer, buffer.readerIndex(), end);
                    safeSet(output, integer, command);
                    break;
                case BULK:
                    if ((end = findLineEnd(buffer)) == -1) {
                        break loop;
                    }
                    length = (int) readLong(buffer, buffer.readerIndex(), end);
                    if (length == -1) {
                        safeSet(output, null, command);
                    } else {
                        state.type = BYTES;
                        state.count = length + 2;
                        buffer.markReaderIndex();
                        continue loop;
                    }
                    break;
                case MULTI:
                    if (state.count == -1) {
                        if ((end = findLineEnd(buffer)) == -1) {
                            break loop;
                        }
                        length = (int) readLong(buffer, buffer.readerIndex(), end);
                        state.count = length;
                        buffer.markReaderIndex();
                        safeMulti(output, state.count, command);
                    }

                    if (state.count <= 0) {
                        break;
                    }

                    state.count--;
                    stack.addFirst(new State());

                    continue loop;
                case BYTES:
                    if ((bytes = readBytes(buffer, state.count)) == null) {
                        break loop;
                    }
                    safeSet(output, bytes, command);
                    break;
                default:
                    throw new IllegalStateException("State " + state.type + " not supported");
            }

            buffer.markReaderIndex();
            stack.remove();

            output.complete(stack.size());

        }

        if (logger.isDebugEnabled()) {
            logger.debug("Decoded " + command + ", empty stack: " + stack.isEmpty());
        }

        return stack.isEmpty();
    }

    protected void safeSet(CommandOutput<K, V, ?> output, long integer, RedisCommand<K, V, ?> command) {

        try {
            output.set(integer);
        } catch (Exception e) {
            command.setException(e);
            command.cancel(true);
        }
    }

    protected void safeSet(CommandOutput<K, V, ?> output, ByteBuffer bytes, RedisCommand<K, V, ?> command) {
        try {
            output.set(bytes);
        } catch (Exception e) {
            command.setException(e);
            command.cancel(true);
        }
    }

    protected void safeMulti(CommandOutput<K, V, ?> output, int count, RedisCommand<K, V, ?> command) {
        try {
            output.multi(count);
        } catch (Exception e) {
            command.setException(e);
            command.cancel(true);
        }
    }

    protected void safeSetError(CommandOutput<K, V, ?> output, ByteBuffer bytes, RedisCommand<K, V, ?> command) {
        try {
            output.setError(bytes);
        } catch (Exception e) {
            command.setException(e);
            command.cancel(true);
        }
    }

    private int findLineEnd(ByteBuf buffer) {
        int start = buffer.readerIndex();
        int index = buffer.indexOf(start, buffer.writerIndex(), (byte) '\n');
        return (index > 0 && buffer.getByte(index - 1) == '\r') ? index : -1;
    }

    private State.Type readReplyType(ByteBuf buffer) {
        byte b = buffer.readByte();
        switch (b) {
            case '+':
                return SINGLE;
            case '-':
                return ERROR;
            case ':':
                return INTEGER;
            case '$':
                return BULK;
            case '*':
                return MULTI;
            default:
                throw new RedisException("Invalid first byte: " + Byte.toString(b));
        }
    }

    private long readLong(ByteBuf buffer, int start, int end) {
        long value = 0;

        boolean negative = buffer.getByte(start) == '-';
        int offset = negative ? start + 1 : start;
        while (offset < end - 1) {
            int digit = buffer.getByte(offset++) - '0';
            value = value * 10 - digit;
        }
        if (!negative) {
            value = -value;
        }
        buffer.readerIndex(end + 1);

        return value;
    }

    private ByteBuffer readLine(ByteBuf buffer) {
        ByteBuffer bytes = null;
        int end = findLineEnd(buffer);
        if (end > -1) {
            int start = buffer.readerIndex();
            bytes = buffer.nioBuffer(start, end - start - 1);
            buffer.readerIndex(end + 1);
            buffer.markReaderIndex();
        }
        return bytes;
    }

    private ByteBuffer readBytes(ByteBuf buffer, int count) {
        ByteBuffer bytes = null;
        if (buffer.readableBytes() >= count) {
            bytes = buffer.nioBuffer(buffer.readerIndex(), count - 2);
            buffer.readerIndex(buffer.readerIndex() + count);
        }
        return bytes;
    }

    public void reset() {
        stack.clear();
    }
}
