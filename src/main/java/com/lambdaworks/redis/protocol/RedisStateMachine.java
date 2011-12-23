// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.RedisException;
import org.jboss.netty.buffer.ChannelBuffer;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import static com.lambdaworks.redis.protocol.Charsets.buffer;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.*;

/**
 * State machine that decodes redis server responses encoded according to the
 * <a href="http://redis.io/topics/protocol">Unified Request Protocol</a>.
 *
 * @author Will Glozer
 */
public class RedisStateMachine<K, V> {
    private static final ByteBuffer QUEUED = buffer("QUEUED");

    static class State {
        enum Type { SINGLE, ERROR, INTEGER, BULK, MULTI, BYTES }
        Type type  = null;
        int  count = -1;
    }

    private LinkedList<State> stack;

    /**
     * Initialize a new instance.
     */
    public RedisStateMachine() {
        stack = new LinkedList<State>();
    }

    /**
     * Attempt to decode a redis response and return a flag indicating whether a complete
     * response was read.
     *
     * @param buffer    Buffer containing data from the server.
     * @param output    Current command output.
     *
     * @return true if a complete response was read.
     */
    public boolean decode(ChannelBuffer buffer, CommandOutput<K, V, ?> output) {
        int length, end;
        ByteBuffer bytes;

        if (stack.isEmpty()) {
            stack.add(new State());
        }

        loop:

        while (!stack.isEmpty()) {
            State state = stack.peek();

            if (state.type == null) {
                if (!buffer.readable()) break;
                state.type = readReplyType(buffer);
                buffer.markReaderIndex();
            }

            switch (state.type) {
                case SINGLE:
                    if ((bytes = readLine(buffer)) == null) break loop;
                    if (!QUEUED.equals(bytes)) {
                        output.set(bytes);
                    }
                    break;
                case ERROR:
                    if ((bytes = readLine(buffer)) == null) break loop;
                    output.setError(bytes);
                    break;
                case INTEGER:
                    if ((end = findLineEnd(buffer)) == -1) break loop;
                    output.set(readLong(buffer, buffer.readerIndex(), end));
                    break;
                case BULK:
                    if ((end = findLineEnd(buffer)) == -1) break loop;
                    length = (int) readLong(buffer, buffer.readerIndex(), end);
                    if (length == -1) {
                        output.set(null);
                    } else {
                        state.type = BYTES;
                        state.count = length + 2;
                        buffer.markReaderIndex();
                        continue loop;
                    }
                    break;
                case MULTI:
                    if (state.count == -1) {
                        if ((end = findLineEnd(buffer)) == -1) break loop;
                        length = (int) readLong(buffer, buffer.readerIndex(), end);
                        state.count = length;
                        buffer.markReaderIndex();
                    }

                    if (state.count == -1) {
                        output.set(null);
                        break;
                    } else if (state.count == 0) {
                        break;
                    } else {
                        state.count--;
                        stack.addFirst(new State());
                    }
                    continue loop;
                case BYTES:
                    if ((bytes = readBytes(buffer, state.count)) == null) break loop;
                    output.set(bytes);
            }

            buffer.markReaderIndex();
            stack.remove();
            output.complete(stack.size());
        }

        return stack.isEmpty();
    }

    private int findLineEnd(ChannelBuffer buffer) {
        int start = buffer.readerIndex();
        int index = buffer.indexOf(start, buffer.writerIndex(), (byte) '\n');
        return (index > 0 && buffer.getByte(index - 1) == '\r') ? index : -1;
    }

    private State.Type readReplyType(ChannelBuffer buffer) {
        switch (buffer.readByte()) {
            case '+': return SINGLE;
            case '-': return ERROR;
            case ':': return INTEGER;
            case '$': return BULK;
            case '*': return MULTI;
            default:  throw new RedisException("Invalid first byte");
        }
    }

    private long readLong(ChannelBuffer buffer, int start, int end) {
        long value = 0;

        boolean negative = buffer.getByte(start) == '-';
        int offset = negative ? start + 1 : start;
        while (offset < end - 1) {
            int digit = buffer.getByte(offset++) - '0';
            value = value * 10 - digit;
        }
        if (!negative) value = -value;
        buffer.readerIndex(end + 1);

        return value;
    }

    private ByteBuffer readLine(ChannelBuffer buffer) {
        ByteBuffer bytes = null;
        int end = findLineEnd(buffer);
        if (end > -1) {
            int start = buffer.readerIndex();
            bytes = buffer.toByteBuffer(start, end - start - 1);
            buffer.readerIndex(end + 1);
        }
        return bytes;
    }

    private ByteBuffer readBytes(ChannelBuffer buffer, int count) {
        ByteBuffer bytes = null;
        if (buffer.readableBytes() >= count) {
            bytes = buffer.toByteBuffer(buffer.readerIndex(), count - 2);
            buffer.readerIndex(buffer.readerIndex() + count);
        }
        return bytes;
    }
}
