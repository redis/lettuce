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

import static io.lettuce.core.protocol.RedisStateMachine.State.Type.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.lettuce.core.internal.LettuceStrings;
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

    private final static State.Type[] TYPE_BY_BYTE_MARKER = new State.Type[Byte.MAX_VALUE + 1];

    static {
        for (State.Type type : values()) {

            if (type == BYTES || type == VERBATIM_STRING) {
                continue;
            }

            if (TYPE_BY_BYTE_MARKER[type.marker] != null) {
                throw new IllegalStateException("Cannot overwrite message marker assignment for '"
                        + new String(new byte[] { type.marker }) + "' with " + type);
            }

            TYPE_BY_BYTE_MARKER[type.marker] = type;
        }
    }

    static class State {

        /**
         * Callback interface to handle a {@link State}.
         */
        @FunctionalInterface
        interface StateHandler {

            Result handle(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
                    Consumer<Exception> errorHandler);

        }

        enum Type implements StateHandler {

            /**
             * First byte: {@code +}.
             */
            SINGLE('+', RedisStateMachine::handleSingle),

            /**
             * First byte: {@code -}.
             */
            ERROR('-', RedisStateMachine::handleError),

            /**
             * First byte: {@code :}.
             */
            INTEGER(':', RedisStateMachine::handleInteger),

            /**
             * First byte: {@code ,}.
             *
             * @since 6.0/RESP3
             */
            FLOAT(',', RedisStateMachine::handleFloat),

            /**
             * First byte: {@code #}.
             *
             * @since 6.0/RESP3
             */
            BOOLEAN('#', RedisStateMachine::handleBoolean),

            /**
             * First byte: {@code !}.
             *
             * @since 6.0/RESP3
             */
            BULK_ERROR('!', RedisStateMachine::handleBulkError),

            /**
             * First byte: {@code =}.
             *
             * @since 6.0/RESP3
             */
            VERBATIM('=', RedisStateMachine::handleBulkAndVerbatim), VERBATIM_STRING('=', RedisStateMachine::handleVerbatim),

            /**
             * First byte: {@code (}.
             *
             * @since 6.0/RESP3
             */
            BIG_NUMBER('(', RedisStateMachine::handleBigNumber),

            /**
             * First byte: {@code %}.
             *
             * @see #HELLO_V3
             * @since 6.0/RESP3
             */
            MAP('%', RedisStateMachine::handleMap),

            /**
             * First byte: {@code ~}.
             *
             * @see #MULTI
             * @since 6.0/RESP3
             */
            SET('~', RedisStateMachine::handleSet),

            /**
             * First byte: {@code |}.
             *
             * @since 6.0/RESP3
             */
            ATTRIBUTE('|', RedisStateMachine::handleAttribute),

            /**
             * First byte: {@code >}.
             *
             * @see #MULTI
             * @since 6.0/RESP3
             */
            PUSH('>', RedisStateMachine::handlePushAndMulti),

            /**
             * First byte: {@code @}.
             *
             * @see #MAP
             * @since 6.0/RESP3
             */
            HELLO_V3('@', RedisStateMachine::handleHelloV3),

            /**
             * First byte: {@code _}.
             *
             * @since 6.0/RESP3
             */
            NULL('_', RedisStateMachine::handleNull),

            /**
             * First byte: {@code $}.
             */
            BULK('$', RedisStateMachine::handleBulkAndVerbatim),

            /**
             * First byte: {@code *}.
             *
             * @see #SET
             * @see #MAP
             */
            MULTI('*', RedisStateMachine::handlePushAndMulti), BYTES('*', RedisStateMachine::handleBytes);

            final byte marker;

            private final StateHandler behavior;

            Type(char marker, StateHandler behavior) {
                this.marker = (byte) marker;
                this.behavior = behavior;
            }

            @Override
            public Result handle(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
                    Consumer<Exception> errorHandler) {
                return behavior.handle(rsm, state, buffer, output, errorHandler);
            }
        }

        enum Result {
            NORMAL_END, BREAK_LOOP, CONTINUE_LOOP
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
        this.responseElementBuffer = alloc.buffer(1024);
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
        return decode(buffer, output, ex -> {
        });
    }

    /**
     * Attempt to decode a redis response and return a flag indicating whether a complete response was read.
     *
     * @param buffer Buffer containing data from the server.
     * @param output Current command output.
     * @param errorHandler the error handler
     * @return true if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {

        buffer.touch("RedisStateMachine.decode(…)");

        if (isEmpty(stack)) {
            add(stack, new State());
        }

        if (output == null) {
            return isEmpty(stack);
        }

        boolean resp3Indicator = doDecode(buffer, output, errorHandler);

        if (debugEnabled) {
            logger.debug("Decode done, empty stack: {}", isEmpty(stack));
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

    private boolean doDecode(ByteBuf buffer, CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {

        boolean resp3Indicator = false;

        State.Result result;

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

            result = state.type.handle(this, state, buffer, output, errorHandler);
            if (State.Result.BREAK_LOOP.equals(result)) {
                break;
            } else if (State.Result.CONTINUE_LOOP.equals(result)) {
                continue;
            }
            buffer.markReaderIndex();
            remove(stack);

            output.complete(size(stack));
        }

        return resp3Indicator;
    }

    static State.Result handleSingle(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        ByteBuffer bytes;

        if ((bytes = rsm.readLine(buffer)) == null) {
            return State.Result.BREAK_LOOP;
        }

        if (!QUEUED.equals(bytes)) {
            rsm.safeSetSingle(output, bytes, errorHandler);
        }
        return State.Result.NORMAL_END;
    }

    static State.Result handleBigNumber(RedisStateMachine rsm, State state, ByteBuf buffer,
            CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {
        ByteBuffer bytes;

        if ((bytes = rsm.readLine(buffer)) == null) {
            return State.Result.BREAK_LOOP;
        }

        rsm.safeSetBigNumber(output, bytes, errorHandler);
        return State.Result.NORMAL_END;
    }

    static State.Result handleError(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        ByteBuffer bytes;

        if ((bytes = rsm.readLine(buffer)) == null) {
            return State.Result.BREAK_LOOP;
        }
        rsm.safeSetError(output, bytes, errorHandler);

        return State.Result.NORMAL_END;
    }

    static State.Result handleNull(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        if (rsm.readLine(buffer) == null) {
            return State.Result.BREAK_LOOP;
        }
        rsm.safeSet(output, null, errorHandler);
        return State.Result.NORMAL_END;
    }

    static State.Result handleInteger(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        int end;

        if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
            return State.Result.BREAK_LOOP;
        }
        long integer = rsm.readLong(buffer, buffer.readerIndex(), end);
        rsm.safeSet(output, integer, errorHandler);
        return State.Result.NORMAL_END;
    }

    static State.Result handleBoolean(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        if (rsm.findLineEnd(buffer) == NOT_FOUND) {
            return State.Result.BREAK_LOOP;
        }
        boolean value = rsm.readBoolean(buffer);
        rsm.safeSet(output, value, errorHandler);
        return State.Result.NORMAL_END;
    }

    static State.Result handleFloat(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        int end;

        if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
            return State.Result.BREAK_LOOP;
        }
        double f = rsm.readFloat(buffer, buffer.readerIndex(), end);
        rsm.safeSet(output, f, errorHandler);
        return State.Result.NORMAL_END;
    }

    static State.Result handleBulkAndVerbatim(RedisStateMachine rsm, State state, ByteBuf buffer,
            CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {
        int length;
        int end;

        if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
            return State.Result.BREAK_LOOP;
        }
        length = (int) rsm.readLong(buffer, buffer.readerIndex(), end);
        if (length == NOT_FOUND) {
            rsm.safeSet(output, null, errorHandler);
        } else {
            state.type = state.type == VERBATIM ? VERBATIM_STRING : BYTES;
            state.count = length + TERMINATOR_LENGTH;
            buffer.markReaderIndex();
            return State.Result.CONTINUE_LOOP;
        }
        return State.Result.NORMAL_END;
    }

    static State.Result handleBulkError(RedisStateMachine rsm, State state, ByteBuf buffer,
            CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {
        int length;
        int end;

        if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
            return State.Result.BREAK_LOOP;
        }
        length = (int) rsm.readLong(buffer, buffer.readerIndex(), end);
        if (length == NOT_FOUND) {
            rsm.safeSetError(output, null, errorHandler);
        } else {
            state.type = BYTES;
            state.count = length + TERMINATOR_LENGTH;
            buffer.markReaderIndex();
            return State.Result.CONTINUE_LOOP;
        }
        return State.Result.NORMAL_END;
    }

    static State.Result handleHelloV3(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        int end;

        if (state.count == NOT_FOUND) {
            if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
                return State.Result.BREAK_LOOP;
            }
            readAndMarkReadIdx(rsm, state, buffer, end);
        }

        return returnDependStateCount(rsm, state);
    }

    static State.Result handlePushAndMulti(RedisStateMachine rsm, State state, ByteBuf buffer,
            CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {
        int end;

        if (state.count == NOT_FOUND) {
            if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
                return State.Result.BREAK_LOOP;
            }
            readAndMarkReadIdx(rsm, state, buffer, end);

            rsm.safeMultiArray(output, state.count, errorHandler);
        }

        return returnDependStateCount(rsm, state);
    }

    static State.Result handleMap(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        int length;
        int end;

        if (state.count == NOT_FOUND) {
            if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
                return State.Result.BREAK_LOOP;
            }
            length = readAndMarkReadIdx(rsm, state, buffer, end);

            rsm.safeMultiMap(output, state.count, errorHandler);
            state.count = length * 2;
        }

        return returnDependStateCount(rsm, state);
    }

    static State.Result handleSet(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        int end;

        if (state.count == NOT_FOUND) {
            if ((end = rsm.findLineEnd(buffer)) == NOT_FOUND) {
                return State.Result.BREAK_LOOP;
            }
            readAndMarkReadIdx(rsm, state, buffer, end);

            rsm.safeMultiSet(output, state.count, errorHandler);
        }

        return returnDependStateCount(rsm, state);
    }

    static int readAndMarkReadIdx(RedisStateMachine rsm, State state, ByteBuf buffer, int end) {
        int length = (int) rsm.readLong(buffer, buffer.readerIndex(), end);
        state.count = length;
        buffer.markReaderIndex();
        return length;
    }

    static State.Result returnDependStateCount(RedisStateMachine rsm, State state) {
        if (state.count <= 0) {
            return State.Result.NORMAL_END;
        }

        state.count--;
        rsm.addFirst(rsm.stack, new State());

        return State.Result.CONTINUE_LOOP;
    }

    static State.Result handleVerbatim(RedisStateMachine rsm, State state, ByteBuf buffer,
            CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {
        ByteBuffer bytes;

        if ((bytes = rsm.readBytes(buffer, state.count)) == null) {
            return State.Result.BREAK_LOOP;
        }
        // skip txt: and mkd:
        bytes.position(bytes.position() + 4);
        rsm.safeSet(output, bytes, errorHandler);
        return State.Result.NORMAL_END;
    }

    static State.Result handleBytes(RedisStateMachine rsm, State state, ByteBuf buffer, CommandOutput<?, ?, ?> output,
            Consumer<Exception> errorHandler) {
        ByteBuffer bytes;

        if ((bytes = rsm.readBytes(buffer, state.count)) == null) {
            return State.Result.BREAK_LOOP;
        }
        rsm.safeSet(output, bytes, errorHandler);
        return State.Result.NORMAL_END;
    }

    private static State.Result handleAttribute(RedisStateMachine rsm, State state, ByteBuf buffer,
            CommandOutput<?, ?, ?> output, Consumer<Exception> errorHandler) {
        throw new RedisProtocolException("Not implemented");
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
        byte b = buffer.readByte();
        State.Type type = TYPE_BY_BYTE_MARKER[b];

        if (type == null) {
            throw new RedisProtocolException("Invalid first byte: " + b + " (" + new String(new byte[] { b }) + ")"
                    + " at buffer index " + buffer.readerIndex() + " decoding using " + getProtocolVersion());
        }

        return type;
    }

    private long readLong(ByteBuf buffer, int start, int end) {
        return longProcessor.getValue(buffer, start, end);
    }

    private double readFloat(ByteBuf buffer, int start, int end) {

        int valueLength = end - start;
        String value = buffer.toString(start, valueLength, StandardCharsets.US_ASCII);

        buffer.skipBytes(valueLength + TERMINATOR_LENGTH);

        return LettuceStrings.toDouble(value);
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
     * Safely sets {@link CommandOutput#set(boolean)}. Completes a errorHandler exceptionally in case an exception occurs.
     *
     * @param output
     * @param value
     * @param errorHandler
     */
    protected void safeSet(CommandOutput<?, ?, ?> output, boolean value, Consumer<Exception> errorHandler) {

        try {
            output.set(value);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(long)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param number
     * @param errorHandler
     */
    protected void safeSet(CommandOutput<?, ?, ?> output, long number, Consumer<Exception> errorHandler) {

        try {
            output.set(number);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(double)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param number
     * @param errorHandler
     */
    protected void safeSet(CommandOutput<?, ?, ?> output, double number, Consumer<Exception> errorHandler) {

        try {
            output.set(number);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(ByteBuffer)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param bytes
     * @param errorHandler
     */
    protected void safeSet(CommandOutput<?, ?, ?> output, ByteBuffer bytes, Consumer<Exception> errorHandler) {

        try {
            output.set(bytes);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(ByteBuffer)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param bytes
     * @param errorHandler
     */
    protected void safeSetSingle(CommandOutput<?, ?, ?> output, ByteBuffer bytes, Consumer<Exception> errorHandler) {

        try {
            output.set(bytes);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#set(ByteBuffer)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param bytes
     * @param errorHandler
     */
    protected void safeSetBigNumber(CommandOutput<?, ?, ?> output, ByteBuffer bytes, Consumer<Exception> errorHandler) {

        try {
            output.setBigNumber(bytes);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiArray(int)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param count
     * @param errorHandler
     */
    protected void safeMultiArray(CommandOutput<?, ?, ?> output, int count, Consumer<Exception> errorHandler) {

        try {
            output.multiArray(count);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiPush(int)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param count
     * @param errorHandler
     */
    protected void safeMultiPush(CommandOutput<?, ?, ?> output, int count, Consumer<Exception> errorHandler) {

        try {
            output.multiPush(count);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiSet(int)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param count
     * @param errorHandler
     */
    protected void safeMultiSet(CommandOutput<?, ?, ?> output, int count, Consumer<Exception> errorHandler) {

        try {
            output.multiSet(count);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#multiMap(int)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param count
     * @param errorHandler
     */
    protected void safeMultiMap(CommandOutput<?, ?, ?> output, int count, Consumer<Exception> errorHandler) {

        try {
            output.multiMap(count);
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    /**
     * Safely sets {@link CommandOutput#setError(ByteBuffer)}. Notifies the {@code errorHandler} if an exception occurs.
     *
     * @param output
     * @param bytes
     * @param errorHandler
     */
    protected void safeSetError(CommandOutput<?, ?, ?> output, ByteBuffer bytes, Consumer<Exception> errorHandler) {

        try {
            output.setError(bytes);
        } catch (Exception e) {
            errorHandler.accept(e);
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
