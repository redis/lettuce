package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.output.CommandOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.redis.RedisFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * State machine that handles Redis server responses using {@link RedisFrame} according to the
 * <a href="http://redis.io/topics/protocol">Unified Request Protocol (RESP)</a>.
 * 
 * @author Mark Paluch
 */
public class RedisFrameStateMachine<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisFrameStateMachine.class);
    private static final ByteBuf QUEUED = Unpooled.wrappedBuffer("QUEUED".getBytes());
    private Deque<State> stack = new ArrayDeque<>();

    static class State {
        enum Type {
            ROOT, MULTI
        }

        Type type;
        long expected = -1;
        long actual = 0;

        public State(Type type) {
            this.type = type;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [type=").append(type);
            sb.append(", expected=").append(expected);
            sb.append(", actual=").append(actual);
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Decodes a {@link RedisFrame}.
     * 
     * @param segment the segment
     * @param output the command output
     * @return {@literal true} if a command was fully decoded.
     */
    public boolean decode(RedisFrame segment, CommandOutput<K, V, ?> output) {

        if (stack.isEmpty()) {
            stack.add(new State(State.Type.ROOT));
        }

        if (output == null) {
            return stack.isEmpty();
        }

        State state = stack.peek();

        if (segment == RedisFrame.NullFrame.INSTANCE) {
            state.actual++;
            output.set(null);
            complete(output, stack.size());
        } else {
            switch (segment.type()) {

                case ARRAY:
                    RedisFrame.ArrayHeader arrayHeader = (RedisFrame.ArrayHeader) segment;

                    if (state.type == State.Type.ROOT) {
                        state.type = State.Type.MULTI;
                    } else {
                        state.actual++;
                        state = new State(State.Type.MULTI);
                        stack.push(state);
                    }

                    state.expected = arrayHeader.content();
                    output.multi((int) state.expected);
                    break;

                case ERROR:
                    RedisFrame.ByteBufFrame errorSegment = (RedisFrame.ByteBufFrame) segment;
                    state.actual++;
                    output.setError(errorSegment.content().nioBuffer());
                    complete(output, stack.size());
                    break;

                case SIMPLE_STRING:
                    RedisFrame.ByteBufFrame singleSegment = (RedisFrame.ByteBufFrame) segment;
                    state.actual++;

                    if (!QUEUED.equals(singleSegment.content())) {
                        output.set(singleSegment.content().nioBuffer());
                    }
                    complete(output, stack.size());
                    break;

                case BULK_STRING:
                    RedisFrame.ByteBufFrame bulkStringSegment = (RedisFrame.ByteBufFrame) segment;
                    state.actual++;

                    output.set(bulkStringSegment.content().nioBuffer());
                    complete(output, stack.size());
                    break;

                case INTEGER:
                    RedisFrame.IntegerFrame integerSegment = (RedisFrame.IntegerFrame) segment;
                    state.actual++;

                    output.set(integerSegment.content());
                    complete(output, stack.size());
                    break;
            }
        }

        while (state.type == State.Type.ROOT || (state.type == State.Type.MULTI && state.actual >= state.expected)) {
            stack.pop();
            complete(output, stack.size());

            if (stack.isEmpty()) {
                break;
            }

            state = stack.peek();
        }

        return stack.isEmpty();
    }

    private void complete(CommandOutput<K, V, ?> output, int size) {
        output.complete(size);
    }
}
