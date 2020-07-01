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

import static io.lettuce.core.protocol.LettuceCharsets.buffer;
import static io.lettuce.core.protocol.RedisStateMachine.State.Type.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import io.lettuce.core.RedisException;
import io.lettuce.core.output.CommandOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * State machine that decodes redis server responses encoded according to the <a href="http://redis.io/topics/protocol">Unified
 * Request Protocol (RESP)</a>.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Helly Guo
 */
public class RedisStateMachine {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisStateMachine.class);

    private static final ByteBuffer QUEUED = buffer("QUEUED");

    static class State {

        /**
         * Callback interface to handle a {@link State}.
         */
        @FunctionalInterface
        interface StateHandler {

            Result handle(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
                    CommandOutput<?, ?, ?> output);
        }

        enum Type implements StateHandler {

            SINGLE(RedisStateMachine::handleSingle), //
            ERROR(RedisStateMachine::handleError), //
            INTEGER(RedisStateMachine::handleInteger), //
            BULK(RedisStateMachine::handleBulk), //
            MULTI(RedisStateMachine::handleMulti), //
            BYTES(RedisStateMachine::handleBytes);

            private final StateHandler behavior;

            Type(StateHandler behavior) {
                this.behavior = behavior;
            }

            public Result handle(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
                    CommandOutput<?, ?, ?> output) {
                return behavior.handle(rsm, state, buffer, command, output);
            }
        }

        enum Result {
            NORMAL_END, BREAK_LOOP, CONTINUE_LOOP
        }

        Type type = null;

        int count = -1;
    }

    private final State[] stack = new State[32];

    private final boolean debugEnabled = logger.isDebugEnabled();

    private final LongProcessor longProcessor = new LongProcessor();

    private final ByteBuf responseElementBuffer = PooledByteBufAllocator.DEFAULT.buffer(1024);

    private final AtomicBoolean closed = new AtomicBoolean();

    private int stackElements;

    /**
     * Initialize a new instance.
     */
    public RedisStateMachine() {
    }

    /**
     * Decode a command using the input buffer.
     *
     * @param buffer Buffer containing data from the server.
     * @param output Current command output.
     * @return {@code true} if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, CommandOutput<?, ?, ?> output) {
        return decode(buffer, null, output);
    }

    /**
     * Attempt to decode a redis response and return a flag indicating whether a complete response was read.
     *
     * @param buffer Buffer containing data from the server.
     * @param command the command itself.
     * @param output Current command output.
     * @return {@code true} if a complete response was read.
     */
    public boolean decode(ByteBuf buffer, RedisCommand<?, ?, ?> command, CommandOutput<?, ?, ?> output) {

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

        doDecode(buffer, command, output);

        if (debugEnabled) {
            logger.debug("Decoded {}, empty stack: {}", command, isEmpty(stack));
        }

        return isEmpty(stack);
    }

    private void doDecode(ByteBuf buffer, RedisCommand<?, ?, ?> command, CommandOutput<?, ?, ?> output) {

        State.Result result;

        while (!isEmpty(stack)) {
            State state = peek(stack);

            if (state.type == null) {
                if (!buffer.isReadable()) {
                    break;
                }
                state.type = readReplyType(buffer);
                buffer.markReaderIndex();
            }

            result = state.type.handle(this, state, buffer, command, output);
            if (State.Result.BREAK_LOOP.equals(result)) {
                break;
            } else if (State.Result.CONTINUE_LOOP.equals(result)) {
                continue;
            }

            buffer.markReaderIndex();
            remove(stack);

            output.complete(size(stack));
        }
    }

    private static State.Result handleSingle(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
            CommandOutput<?, ?, ?> output) {

        ByteBuffer bytes;
        if ((bytes = rsm.readLine(buffer)) == null) {
            return State.Result.BREAK_LOOP;
        }

        if (!QUEUED.equals(bytes)) {
            rsm.safeSetSingle(output, bytes, command);
        }
        return State.Result.NORMAL_END;
    }

    private static State.Result handleError(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
            CommandOutput<?, ?, ?> output) {

        ByteBuffer bytes;
        if ((bytes = rsm.readLine(buffer)) == null) {
            return State.Result.BREAK_LOOP;
        }

        rsm.safeSetError(output, bytes, command);
        return State.Result.NORMAL_END;
    }

    private static State.Result handleInteger(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
            CommandOutput<?, ?, ?> output) {

        int end;
        if ((end = rsm.findLineEnd(buffer)) == -1) {
            return State.Result.BREAK_LOOP;
        }

        long integer = rsm.readLong(buffer, buffer.readerIndex(), end);
        rsm.safeSet(output, integer, command);
        return State.Result.NORMAL_END;
    }

    private static State.Result handleBulk(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
            CommandOutput<?, ?, ?> output) {

        int length;
        int end;
        if ((end = rsm.findLineEnd(buffer)) == -1) {
            return State.Result.BREAK_LOOP;
        }

        length = (int) rsm.readLong(buffer, buffer.readerIndex(), end);
        if (length == -1) {
            rsm.safeSet(output, null, command);
        } else {
            state.type = BYTES;
            state.count = length + 2;
            buffer.markReaderIndex();
            return State.Result.CONTINUE_LOOP;
        }

        return State.Result.NORMAL_END;
    }

    private static State.Result handleMulti(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
            CommandOutput<?, ?, ?> output) {

        int length;
        int end;
        if (state.count == -1) {
            if ((end = rsm.findLineEnd(buffer)) == -1) {
                return State.Result.BREAK_LOOP;
            }
            length = (int) rsm.readLong(buffer, buffer.readerIndex(), end);
            state.count = length;
            buffer.markReaderIndex();
            rsm.safeMulti(output, state.count, command);
        }

        if (state.count <= 0) {
            return State.Result.NORMAL_END;
        }

        state.count--;
        rsm.addFirst(rsm.stack, new State());

        return State.Result.CONTINUE_LOOP;
    }

    private static State.Result handleBytes(RedisStateMachine rsm, State state, ByteBuf buffer, RedisCommand<?, ?, ?> command,
            CommandOutput<?, ?, ?> output) {

        ByteBuffer bytes;
        if ((bytes = rsm.readBytes(buffer, state.count)) == null) {
            return State.Result.BREAK_LOOP;
        }

        rsm.safeSet(output, bytes, command);
        return State.Result.NORMAL_END;
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
                throw new RedisException("Invalid first byte: " + b);
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
     * @return {@code true} if the stack is empty.
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
    protected void safeSet(CommandOutput<?, ?, ?> output, long integer, RedisCommand<?, ?, ?> command) {

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
     * Safely sets {@link CommandOutput#multi(int)}. Completes a command exceptionally in case an exception occurs.
     *
     * @param output
     * @param count
     * @param command
     */
    protected void safeMulti(CommandOutput<?, ?, ?> output, int count, RedisCommand<?, ?, ?> command) {

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
    protected void safeSetError(CommandOutput<?, ?, ?> output, ByteBuffer bytes, RedisCommand<?, ?, ?> command) {

        try {
            output.setError(bytes);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    @SuppressWarnings("unused")
    static class LongProcessor implements ByteProcessor {

        long result;

        boolean negative;

        boolean first;

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
