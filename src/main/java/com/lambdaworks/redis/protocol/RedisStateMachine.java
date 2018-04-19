/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.protocol;

import static com.lambdaworks.redis.protocol.LettuceCharsets.buffer;
import static com.lambdaworks.redis.protocol.RedisStateMachine.State.Type.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.output.CommandOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.Version;
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
 * @author Helly Guo
 */
public class RedisStateMachine<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisStateMachine.class);
    private static final ByteBuffer QUEUED = buffer("QUEUED");

    private static final boolean USE_NETTY40_BYTEBUF_COMPATIBILITY;
    private static final Class<?> LONG_PROCESSOR_CLASS;
    private static final Class<?> INDEX_OF_LINEBREAK_PROCESSOR_CLASS;

    static {

        Version nettyBufferVersion = Version.identify().get("netty-buffer");

        USE_NETTY40_BYTEBUF_COMPATIBILITY = nettyBufferVersion != null
                && nettyBufferVersion.artifactVersion().startsWith("4.0");
        if (!USE_NETTY40_BYTEBUF_COMPATIBILITY) {
            try {
                LONG_PROCESSOR_CLASS = Class.forName("com.lambdaworks.redis.protocol.RedisStateMachine$Netty41LongProcessor");
            } catch (ClassNotFoundException e) {
                throw new RedisException("Cannot load Netty41LongProcessor class", e);
            }
            try {
                INDEX_OF_LINEBREAK_PROCESSOR_CLASS = Class
                        .forName("com.lambdaworks.redis.protocol.RedisStateMachine$Netty41IndexOfLineBreakProcessor");
            } catch (ClassNotFoundException e) {
                throw new RedisException("Cannot load Netty41IndexOfLineBreakProcessor class", e);
            }
        } else {
            LONG_PROCESSOR_CLASS = null;
            INDEX_OF_LINEBREAK_PROCESSOR_CLASS = null;
        }
    }

    static class State {
        enum Type {
            SINGLE, ERROR, INTEGER, BULK, MULTI, BYTES
        }

        Type type = null;
        int count = -1;
    }

    private final State[] stack = new State[32];
    private final boolean debugEnabled = logger.isDebugEnabled();
    private final LongProcessor longProcessor;
    private final IndexOfLineBreakProcessor indexOfLineBreakProcessor;
    private final ByteBuf responseElementBuffer = PooledByteBufAllocator.DEFAULT.directBuffer(1024);
    private final AtomicBoolean closed = new AtomicBoolean();

    private int stackElements;

    /**
     * Initialize a new instance.
     */
    public RedisStateMachine() {

        LongProcessor longProcessor;
        IndexOfLineBreakProcessor indexOfLineBreakProcessor;

        if (!USE_NETTY40_BYTEBUF_COMPATIBILITY) {
            try {
                longProcessor = (LongProcessor) LONG_PROCESSOR_CLASS.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RedisException("Cannot create Netty41LongProcessor instance", e);
            }

            try {
                indexOfLineBreakProcessor = (IndexOfLineBreakProcessor) INDEX_OF_LINEBREAK_PROCESSOR_CLASS.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RedisException("Cannot create Netty41IndexOfLineBreakProcessor instance", e);
            }
        } else {
            longProcessor = new LongProcessor();
            indexOfLineBreakProcessor = new IndexOfLineBreakProcessor();
        }

        this.longProcessor = longProcessor;
        this.indexOfLineBreakProcessor = indexOfLineBreakProcessor;
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
     * @return true if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, RedisCommand<K, V, ?> command, CommandOutput<K, V, ?> output) {

        int length, end;
        ByteBuffer bytes;

        if (debugEnabled) {
            logger.debug("Decode {}", command);
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
                        safeSetSingle(output, bytes, command);
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
            logger.debug("Decoded {}, empty stack: {}", command, isEmpty(stack));
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
    public void close() {
        if (closed.compareAndSet(false, true)) {
            responseElementBuffer.release();
        }
    }

    private int findLineEnd(ByteBuf buffer) {

        int index = (int) indexOfLineBreakProcessor.getValue(buffer, buffer.readerIndex(), buffer.writerIndex());
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
        return longProcessor.getValue(buffer, start, end);
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
     * @param stack
     * @return number of stack elements.
     */
    private int size(State[] stack) {
        return stackElements;
    }

    /**
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
     * Safely sets {@link CommandOutput#set(ByteBuffer)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param bytes
     * @param command
     */
    protected void safeSetSingle(CommandOutput<K, V, ?> output, ByteBuffer bytes, RedisCommand<K, V, ?> command) {

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

    /**
     * Compatibility code that works also on Netty 4.0.
     */
    static class LongProcessor {

        public long getValue(ByteBuf buffer, int start, int end) {

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
            buffer.markReaderIndex();
            return value;
        }
    }

    /**
     * Processor for Netty 4.1. Note {@link ByteBufProcessor} is deprecated but ByteProcessor does not exist in Netty 4.0. So we
     * need to stick to that as long as we support Netty 4.0.
     */
    @SuppressWarnings("unused")
    static class Netty41LongProcessor extends LongProcessor implements ByteBufProcessor {

        long result;
        boolean negative;
        boolean first;

        @Override
        public long getValue(ByteBuf buffer, int start, int end) {

            this.result = 0;
            this.first = true;

            buffer.forEachByte(start, end - start - 1, this);

            if (!this.negative) {
                this.result = -this.result;
            }
            buffer.readerIndex(end + 1);

            return this.result;
        }

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

    /**
     * Compatibility code that works also on Netty 4.0.
     */
    static class IndexOfLineBreakProcessor {

        public long getValue(ByteBuf buffer, int start, int end) {
            return buffer.indexOf(start, buffer.writerIndex(), (byte) '\n');
        }
    }

    /**
     * Processor for Netty 4.1. Note {@link ByteBufProcessor} is deprecated but ByteProcessor does not exist in Netty 4.0. So we
     * need to stick to that as long as we support Netty 4.0.
     */
    static class Netty41IndexOfLineBreakProcessor extends IndexOfLineBreakProcessor implements ByteBufProcessor {

        @Override
        public long getValue(ByteBuf buffer, int start, int end) {
            return buffer.forEachByte(start, end - start, this);
        }

        @Override
        public boolean process(byte value) throws Exception {
            return value != (byte) '\n';
        }
    }
}
