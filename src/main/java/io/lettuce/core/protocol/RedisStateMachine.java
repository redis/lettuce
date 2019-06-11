/*
 * Copyright 2011-2019 the original author or authors.
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
package io.lettuce.core.protocol;

import static io.lettuce.core.protocol.RedisStateMachine.State.Type.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import io.lettuce.core.output.CommandOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * State machine that decodes redis server responses encoded according to the <a href="http://redis.io/topics/protocol">Unified
 * Request Protocol (RESP)</a>. Supports RESP2 and RESP3. Initialized with protocol discovery.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Helly Guo
 */
public class RedisStateMachine {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisStateMachine.class);
    private static final ByteBuffer QUEUED = StandardCharsets.US_ASCII.encode("QUEUED");
    private static final int TERMINATOR_LENGTH = 2;
    private static final int NOT_FOUND = -1;

    static class State {
        enum Type {

            /**
             * First byte: {@code +}.
             */
            SINGLE,

            /**
             * First byte: {@code +}.
             */
            ERROR,

            /**
             * First byte: {@code :}.
             */
            INTEGER,

            /**
             * First byte: {@code ,}.
             *
             * @since 6.0/RESP3
             */
            FLOAT,

            /**
             * First byte: {@code #}.
             *
             * @since 6.0/RESP3
             */
            BOOLEAN,

            /**
             * First byte: {@code !}.
             *
             * @since 6.0/RESP3
             */
            BULK_ERROR,

            /**
             * First byte: {@code =}.
             *
             * @since 6.0/RESP3
             */
            VERBATIM,

            /**
             * First byte: {@code (}.
             *
             * @since 6.0/RESP3
             */
            BIG_NUMBER,

            /**
             * First byte: {@code %}.
             *
             * @see #HELLO_V3
             * @since 6.0/RESP3
             */
            MAP,

            /**
             * First byte: {@code ~}.
             *
             * @see #MULTI
             * @since 6.0/RESP3
             */
            SET,

            /**
             * First byte: {@code |}.
             *
             * @since 6.0/RESP3
             */
            ATTRIBUTE,

            /**
             * First byte: {@code >}.
             *
             * @see #MULTI
             * @since 6.0/RESP3
             */
            PUSH,

            /**
             * First byte: {@code @}.
             *
             * @see #MAP
             * @since 6.0/RESP3
             */
            HELLO_V3,

            /**
             * First byte: {@code _}.
             *
             * @since 6.0/RESP3
             */
            NULL,

            /**
             * First byte: {@code $}.
             */
            BULK,

            /**
             * First byte: {@code *}.
             *
             * @see #SET
             * @see #MAP
             */
            MULTI, BYTES
        }

        Type type = null;
        int count = NOT_FOUND;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [type=").append(type);
            sb.append(", count=").append(count);
            sb.append(']');
            return sb.toString();
        }
    }

    private final State[] stack = new State[32];
    private final boolean debugEnabled = logger.isDebugEnabled();
    private final ByteBuf responseElementBuffer;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Resp2LongProcessor longProcessor = new Resp2LongProcessor();

    private ProtocolVersion protocolVersion = null;
    private int stackElements;

    /**
     * Initialize a new instance.
     */
    public RedisStateMachine(ByteBufAllocator alloc) {
        this.responseElementBuffer = alloc.directBuffer(1024);
    }

    public boolean isDiscoverProtocol() {
        return this.protocolVersion == null;
    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /**
     * Decode a command using the input buffer.
     *
     * @param buffer Buffer containing data from the server.
     * @param output Current command output.
     * @return true if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, CommandOutput<?, ?, ?> output) {
        return decode(buffer, null, output);
    }

    /**
     * Attempt to decode a redis response and return a flag indicating whether a complete response was read.
     *
     * @param buffer Buffer containing data from the server.
     * @param command the command itself TODO: Change to Consumer<Throwable>
     * @param output Current command output.
     * @return true if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, RedisCommand<?, ?, ?> command, CommandOutput<?, ?, ?> output) {

        int length, end;
        ByteBuffer bytes;

        buffer.touch("RedisStateMachine.decode(…)");
        if (debugEnabled) {
            logger.debug("Decode {}", command);
        }

        if (isEmpty(stack)) {
            add(stack, new State());
        }

        if (output == null) {
            return isEmpty(stack);
        }

        boolean resp3Indicator = false;

        loop:

        while (!isEmpty(stack)) {
            State state = peek(stack);

            if (state.type == null) {
                if (!buffer.isReadable()) {
                    break;
                }
                state.type = readReplyType(buffer);

                if (state.type == HELLO_V3 || state.type == MAP) {
                    resp3Indicator = true;
                }

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

                case BIG_NUMBER:
                    if ((bytes = readLine(buffer)) == null) {
                        break loop;
                    }

                    safeSetBigNumber(output, bytes, command);
                    break;
                case ERROR:
                    if ((bytes = readLine(buffer)) == null) {
                        break loop;
                    }
                    safeSetError(output, bytes, command);
                    break;
                case NULL:
                    if ((bytes = readLine(buffer)) == null) {
                        break loop;
                    }
                    safeSet(output, null, command);
                    break;
                case INTEGER:
                    if ((end = findLineEnd(buffer)) == NOT_FOUND) {
                        break loop;
                    }
                    long integer = readLong(buffer, buffer.readerIndex(), end);
                    safeSet(output, integer, command);
                    break;
                case BOOLEAN:
                    if ((end = findLineEnd(buffer)) == NOT_FOUND) {
                        break loop;
                    }
                    boolean value = readBoolean(buffer);
                    safeSet(output, value, command);
                    break;
                case FLOAT:
                    if ((end = findLineEnd(buffer)) == NOT_FOUND) {
                        break loop;
                    }
                    double f = readFloat(buffer, buffer.readerIndex(), end);
                    safeSet(output, f, command);
                    break;
                case BULK:
                case VERBATIM:
                    if ((end = findLineEnd(buffer)) == NOT_FOUND) {
                        break loop;
                    }
                    length = (int) readLong(buffer, buffer.readerIndex(), end);
                    if (length == NOT_FOUND) {
                        safeSet(output, null, command);
                    } else {
                        state.type = BYTES;
                        state.count = length + TERMINATOR_LENGTH;
                        buffer.markReaderIndex();
                        continue loop;
                    }
                    break;
                case BULK_ERROR:
                    if ((end = findLineEnd(buffer)) == NOT_FOUND) {
                        break loop;
                    }
                    length = (int) readLong(buffer, buffer.readerIndex(), end);
                    if (length == NOT_FOUND) {
                        safeSetError(output, null, command);
                    } else {
                        state.type = BYTES;
                        state.count = length + TERMINATOR_LENGTH;
                        buffer.markReaderIndex();
                        continue loop;
                    }
                    break;
                case HELLO_V3:
                case PUSH:
                case MULTI:
                case SET:
                case MAP:

                    if (state.count == NOT_FOUND) {
                        if ((end = findLineEnd(buffer)) == NOT_FOUND) {
                            break loop;
                        }
                        length = (int) readLong(buffer, buffer.readerIndex(), end);
                        state.count = length;
                        buffer.markReaderIndex();

                        switch (state.type) {
                            case MULTI:
                            case PUSH:
                                safeMultiArray(output, state.count, command);
                                break;
                            case MAP:
                                safeMultiMap(output, state.count, command);
                                state.count = length * 2;
                                break;
                            case SET:
                                safeMultiSet(output, state.count, command);
                                break;
                        }
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
                case ATTRIBUTE:
                    throw new RedisProtocolException("Not implemented");
                default:
                    throw new RedisProtocolException("State " + state.type + " not supported");
            }

            buffer.markReaderIndex();
            remove(stack);

            output.complete(size(stack));
        }

        if (debugEnabled) {
            logger.debug("Decoded {}, empty stack: {}", command, isEmpty(stack));
        }

        if (isDiscoverProtocol()) {
            if (resp3Indicator) {
                setProtocolVersion(ProtocolVersion.RESP3);
            } else {
                setProtocolVersion(ProtocolVersion.RESP2);
            }
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

        int index = buffer.forEachByte(ByteProcessor.FIND_LF);
        return (index > 0 && buffer.getByte(index - 1) == '\r') ? index - 1 : NOT_FOUND;
    }

    private State.Type readReplyType(ByteBuf buffer) {
        return getType(buffer.readerIndex(), buffer.readByte());
    }

    private State.Type getType(int index, byte b) {

        switch (b) {
            case '+':
                return SINGLE;
            case '-':
                return ERROR;
            case ':':
                return INTEGER;
            case ',':
                return FLOAT;
            case '#':
                return BOOLEAN;
            case '=':
                return VERBATIM;
            case '(':
                return BIG_NUMBER;
            case '%':
                return MAP;
            case '~':
                return SET;
            case '|':
                return ATTRIBUTE;
            case '@':
                return HELLO_V3;
            case '$':
                return BULK;
            case '*':
                return MULTI;
            case '>':
                return PUSH;
            case '_':
                return NULL;
            default:
                throw new RedisProtocolException("Invalid first byte: " + b + " (" + new String(new byte[] { b }) + ")"
                        + " at buffer index " + index + " decoding using " + getProtocolVersion());
        }
    }

    private long readLong(ByteBuf buffer, int start, int end) {
        return longProcessor.getValue(buffer, start, end);
    }

    private double readFloat(ByteBuf buffer, int start, int end) {

        int valueLength = end - start;
        String value = buffer.toString(start, valueLength, StandardCharsets.US_ASCII);

        buffer.skipBytes(valueLength + TERMINATOR_LENGTH);

        return Double.parseDouble(value);
    }

    private boolean readBoolean(ByteBuf buffer) {

        byte b = buffer.readByte();
        buffer.skipBytes(TERMINATOR_LENGTH);

        switch (b) {
            case 't':
                return true;
            case 'f':
                return false;
        }

        throw new RedisProtocolException("Unexpected BOOLEAN value: " + b);
    }

    private ByteBuffer readLine(ByteBuf buffer) {

        ByteBuffer bytes = null;
        int end = findLineEnd(buffer);

        if (end > NOT_FOUND) {
            bytes = readBytes0(buffer, end - buffer.readerIndex());

            buffer.skipBytes(TERMINATOR_LENGTH);
            buffer.markReaderIndex();
        }

        return bytes;
    }

    private ByteBuffer readBytes(ByteBuf buffer, int count) {

        if (buffer.readableBytes() >= count) {

            ByteBuffer byteBuffer = readBytes0(buffer, count - TERMINATOR_LENGTH);

            buffer.skipBytes(TERMINATOR_LENGTH);
            buffer.markReaderIndex();

            return byteBuffer;
        }

        return null;
    }

    private ByteBuffer readBytes0(ByteBuf buffer, int count) {

        ByteBuffer bytes;
        responseElementBuffer.clear();

        if (responseElementBuffer.capacity() < count) {
            responseElementBuffer.capacity(count);
        }

        buffer.readBytes(responseElementBuffer, count);
        bytes = responseElementBuffer.internalNioBuffer(0, count);

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
     * Safely sets {@link CommandOutput#set(boolean)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param value
     * @param command
     */
    protected void safeSet(CommandOutput<?, ?, ?> output, boolean value, RedisCommand<?, ?, ?> command) {

        try {
            output.set(value);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(long)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param number
     * @param command
     */
    protected void safeSet(CommandOutput<?, ?, ?> output, long number, RedisCommand<?, ?, ?> command) {

        try {
            output.set(number);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(double)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param number
     * @param command
     */
    protected void safeSet(CommandOutput<?, ?, ?> output, double number, RedisCommand<?, ?, ?> command) {

        try {
            output.set(number);
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
    protected void safeSet(CommandOutput<?, ?, ?> output, ByteBuffer bytes, RedisCommand<?, ?, ?> command) {

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
    protected void safeSetSingle(CommandOutput<?, ?, ?> output, ByteBuffer bytes, RedisCommand<?, ?, ?> command) {

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
    protected void safeSetBigNumber(CommandOutput<?, ?, ?> output, ByteBuffer bytes, RedisCommand<?, ?, ?> command) {

        try {
            output.setBigNumber(bytes);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiArray(int)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param count
     * @param command
     */
    protected void safeMultiArray(CommandOutput<?, ?, ?> output, int count, RedisCommand<?, ?, ?> command) {

        try {
            output.multiArray(count);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiPush(int)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param count
     * @param command
     */
    protected void safeMultiPush(CommandOutput<?, ?, ?> output, int count, RedisCommand<?, ?, ?> command) {

        try {
            output.multiPush(count);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiSet(int)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param count
     * @param command
     */
    protected void safeMultiSet(CommandOutput<?, ?, ?> output, int count, RedisCommand<?, ?, ?> command) {

        try {
            output.multiSet(count);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiMap(int)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param count
     * @param command
     */
    protected void safeMultiMap(CommandOutput<?, ?, ?> output, int count, RedisCommand<?, ?, ?> command) {

        try {
            output.multiMap(count);
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
    protected void safeSetError(CommandOutput<?, ?, ?> output, ByteBuffer bytes, RedisCommand<?, ?, ?> command) {

        try {
            output.setError(bytes);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    @SuppressWarnings("unused")
    static class Resp2LongProcessor implements ByteProcessor {

        long result;
        boolean negative;
        boolean first;

        public long getValue(ByteBuf buffer, int start, int end) {

            this.result = 0;
            this.first = true;

            int length = end - start;
            buffer.forEachByte(start, length, this);

            if (!this.negative) {
                this.result = -this.result;
            }

            buffer.skipBytes(length + TERMINATOR_LENGTH);

            return this.result;
        }

        @Override
        public boolean process(byte value) {

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
