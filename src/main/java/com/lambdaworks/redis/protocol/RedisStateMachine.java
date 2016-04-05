// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import static com.lambdaworks.redis.protocol.LettuceCharsets.buffer;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.output.CommandOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * State machine that decodes redis server responses encoded according to the <a href="http://redis.io/topics/protocol">Unified
 * Request Protocol (RESP)</a>.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
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

    private final State[] stack;
    private int stackElements;

    // If DEBUG level logging has been enabled at startup.
    private final boolean debugEnabled;
    private final ToLongProcessor toLongProcessor = new ToLongProcessor();
    private final ByteBuf responseElementBuffer = PooledByteBufAllocator.DEFAULT.directBuffer(1024);

    /**
     * Initialize a new instance.
     */
    public RedisStateMachine() {
        stack = new State[32];
        debugEnabled = logger.isDebugEnabled();
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

        if (debugEnabled) {
            logger.debug("Decode " + command);
        }

        if (isEmpty(stack)) {
            add(stack, new State());
        }

        if (output == null) {
            return isEmpty(stack);
        }

        loop:

        while (!isEmpty(stack)) {
            State state = peek(stack);

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
                    addFirst(stack, new State());

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
            remove(stack);

            output.complete(size(stack));
        }

        if (debugEnabled) {
            logger.debug("Decoded " + command + ", empty stack: " + isEmpty(stack));
        }

        return isEmpty(stack);
    }

    /**
     * Reset the state machine.
     */
    public void reset() {
        Arrays.fill(stack, null);
        stackElements = 0;
    }

    /**
     * Close the state machine to free resources.
     */
    public void close(){
        responseElementBuffer.release();
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

        toLongProcessor.result = 0;
        toLongProcessor.first = true;

        buffer.forEachByte(start, end - start - 1, toLongProcessor);
        if (!toLongProcessor.negative) {
            toLongProcessor.result = -toLongProcessor.result;
        }
        buffer.readerIndex(end + 1);

        return toLongProcessor.result;
    }

    private ByteBuffer readLine(ByteBuf buffer) {

        ByteBuffer bytes = null;
        int end = findLineEnd(buffer);

        if (end > -1) {
            int start = buffer.readerIndex();
            responseElementBuffer.clear();
            int size = end - start - 1;

            if (responseElementBuffer.capacity() < size) {
                responseElementBuffer.capacity(size);
            }

            buffer.readBytes(responseElementBuffer, size);

            bytes = responseElementBuffer.internalNioBuffer(0, size);

            buffer.readerIndex(end + 1);
            buffer.markReaderIndex();
        }
        return bytes;
    }

    private ByteBuffer readBytes(ByteBuf buffer, int count) {

        ByteBuffer bytes = null;

        if (buffer.readableBytes() >= count) {
            responseElementBuffer.clear();

            int size = count - 2;

            if (responseElementBuffer.capacity() < size) {
                responseElementBuffer.capacity(size);
            }
            buffer.readBytes(responseElementBuffer, size);

            bytes = responseElementBuffer.internalNioBuffer(0, size);
            buffer.readerIndex(buffer.readerIndex() + 2);
        }
        return bytes;
    }

    /**
     * Remove the head element from the stack.
     * 
     * @param stack
     */
    private void remove(State[] stack) {
        stack[stackElements - 1] = null;
        stackElements--;
    }

    /**
     * Add the element to the stack to be the new head element.
     * 
     * @param stack
     * @param state
     */
    private void addFirst(State[] stack, State state) {
        stack[stackElements++] = state;
    }

    /**
     * Returns the head element without removing it.
     * 
     * @param stack
     * @return
     */
    private State peek(State[] stack) {
        return stack[stackElements - 1];
    }

    /**
     * Add a state as tail element. This method shifts the whole stack if the stack is not empty.
     * 
     * @param stack
     * @param state
     */
    private void add(State[] stack, State state) {

        if (stackElements != 0) {
            System.arraycopy(stack, 0, stack, 1, stackElements);
        }

        stack[0] = state;
        stackElements++;
    }

    /**
     * 
     * @param stack
     * @return number of stack elements.
     */
    private int size(State[] stack) {
        return stackElements;
    }

    /**
     * 
     * @param stack
     * @return true if the stack is empty.
     */
    private boolean isEmpty(State[] stack) {
        return stackElements == 0;
    }

    /**
     * Safely sets {@link CommandOutput#set(long)}. Completes a command exceptionally in case an exception occurs.
     * 
     * @param output
     * @param integer
     * @param command
     */
    protected void safeSet(CommandOutput<K, V, ?> output, long integer, RedisCommand<K, V, ?> command) {

        try {
            output.set(integer);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(ByteBuffer)}. Completes a command exceptionally in case an exception occurs.
     * 
     * @param output
     * @param bytes
     * @param command
     */
    protected void safeSet(CommandOutput<K, V, ?> output, ByteBuffer bytes, RedisCommand<K, V, ?> command) {

        try {
            output.set(bytes);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multi(int)}. Completes a command exceptionally in case an exception occurs.
     * 
     * @param output
     * @param count
     * @param command
     */
    protected void safeMulti(CommandOutput<K, V, ?> output, int count, RedisCommand<K, V, ?> command) {

        try {
            output.multi(count);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#setError(ByteBuffer)}. Completes a command exceptionally in case an exception occurs.
     * 
     * @param output
     * @param bytes
     * @param command
     */
    protected void safeSetError(CommandOutput<K, V, ?> output, ByteBuffer bytes, RedisCommand<K, V, ?> command) {

        try {
            output.setError(bytes);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    static class ToLongProcessor implements ByteBufProcessor {

        long result;
        boolean negative;
        boolean first;

        @Override
        public boolean process(byte value) throws Exception {

            if (first) {
                first = false;

                if (value == '-') {
                    negative = true;
                } else {
                    negative = false;
                    int digit = value - '0';
                    result = result * 10 - digit;
                }
                return true;
            }

            int digit = value - '0';
            result = result * 10 - digit;

            return true;
        }
    }

}
