/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.netty.handler.codec.redis;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;

/**
 * @author Jongyeol Choi
 * @author Mark Paluch
 */
public class RedisDecoder extends ByteToMessageDecoder {

    private static final int CRLF_LENGTH = 2;
    private static final int NULL_RESPONSE = -1;

    private final DecodeState state = new DecodeState();
    private final ToLongProcessor toLong = new ToLongProcessor();

    private enum State {
        DECODE_TYPE, DECODE_INLINE, // SIMPLE_STRING, ERROR, INTEGER
        DECODE_LENGTH, // BULK_STRING, ARRAY
        DECODE_BULK_STRING, DECODE_ARRAY, DECODE_COMPLETE,
    }

    public RedisDecoder() {
        resetDecoder();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            boolean next = true;
            do {
                switch (state.state) {
                    case DECODE_TYPE:
                        next = decodeType(in);
                        break;
                    case DECODE_INLINE:
                        next = decodeInline(in, out);
                        break;
                    case DECODE_LENGTH:
                        next = decodeLength(in, out);
                        break;
                    case DECODE_BULK_STRING:
                        next = decodeBulkStrings(in, out);
                        break;
                    case DECODE_ARRAY:
                        decodeArrays();
                        break;
                    case DECODE_COMPLETE:
                        aggregate();
                        break;
                    default:
                        throw new Error("Unknown state: " + state.state);
                }
            } while (next);
        } catch (Exception e) {
            resetDecoder();
            throw e;
        }
    }

    private void resetDecoder() {
        state.reset();
    }

    private boolean decodeType(ByteBuf in) throws Exception {
        if (in.readableBytes() < 1) {
            return false;
        }
        state.type = RedisMessageType.valueOf(in.readByte());
        state.state = state.type.isInline() ? State.DECODE_INLINE : State.DECODE_LENGTH;
        return true;
    }

    private boolean decodeInline(ByteBuf in, List<Object> out) throws Exception {
        ByteBuf buf = readLine(in);
        if (buf == null) {
            return false;
        }

        switch (state.type) {
            case INTEGER:
                toLong.reset();
                out.add(new RedisFrame.IntegerFrame(readLength(buf, toLong)));
                state.segments++;
                state.state = State.DECODE_COMPLETE;
                break;
            case ERROR:
            case SIMPLE_STRING:
                out.add(new RedisFrame.ByteBufFrame(state.type, buf)); // TODO: Use StringSegment?
                state.segments++;
                state.state = State.DECODE_COMPLETE;
                break;
            default:
                throw new Error("Cannot decodeInline for type: " + state.type);
        }

        return true;
    }

    private boolean decodeLength(ByteBuf in, List<Object> out) throws Exception {
        ByteBuf buf = readLine(in);
        if (buf == null) {
            return false;
        }
        toLong.reset();
        final long length = readLength(buf, toLong);
        state.length = length;
        if (state.type.isArray()) {
            setNextStateAndChildrenForArray(state, out);
        } else if (state.type.isBulkString()) {
            state.state = State.DECODE_BULK_STRING;
        } else {
            throw new Error("bad type: " + state.type);
        }
        return true;
    }

    private void setNextStateAndChildrenForArray(DecodeState current, List<Object> out) throws Exception {
        if (current.length == NULL_RESPONSE) {
            // *-1\r\n <here>
            out.add(RedisFrame.ArrayHeader.NULL);
            current.segments++;
            current.state = State.DECODE_COMPLETE;
        } else if (current.length == 0) {
            // *0\r\n <here>
            out.add(new RedisFrame.ArrayHeader(0));
            current.segments++;
            current.state = State.DECODE_COMPLETE;
        } else if (current.length > 0) {
            // *{length}\r\n <here> {children...}\r\n
            out.add(new RedisFrame.ArrayHeader(current.length));
            current.segments++;
            current.state = State.DECODE_ARRAY;
        } else {
            throw new IllegalArgumentException("bad length: " + current.length);
        }
    }

    private boolean decodeBulkStrings(ByteBuf in, List<Object> out) throws Exception {
        if (state.length == NULL_RESPONSE) {
            // $-1\r\n
            out.add(RedisFrame.NullFrame.INSTANCE);
            state.segments++;
            state.state = State.DECODE_COMPLETE;
        } else if (state.length == 0) {
            // $0\r\n <here> \r\n
            if (in.readableBytes() < CRLF_LENGTH) {
                return false;
            }
            in.skipBytes(CRLF_LENGTH);
            out.add(RedisFrame.ByteBufFrame.EMPTY_BULK_STRING);
            state.segments++;
            state.state = State.DECODE_COMPLETE;
        } else if (state.length > 0) {
            // ${length}\r\n <here> {data...}\r\n
            if (in.readableBytes() < state.length + CRLF_LENGTH) {
                return false;
            }

            assert state.length <= Integer.MAX_VALUE;
            ByteBuf byteBuf = in.readSlice((int) state.length);
            out.add(new RedisFrame.ByteBufFrame(RedisMessageType.BULK_STRING, byteBuf));
            in.skipBytes(CRLF_LENGTH);
            state.segments++;
            state.state = State.DECODE_COMPLETE;
        } else {
            throw new IllegalArgumentException("bad length: " + state.length);
        }
        return true;
    }

    private void decodeArrays() throws Exception {
        if (state.segments == state.length) {
            state.state = State.DECODE_COMPLETE;
        } else if (state.segments < state.length) {
            this.state.reset();
        } else {
            throw new IllegalArgumentException("children.size: " + state.segments + ", current.length: " + state.length);
        }
    }

    private void aggregate() throws Exception {
        state.reset();
    }

    private static ByteBuf readLine(ByteBuf in) {
        final int lfIndex = in.forEachByte(ByteProcessor.FIND_LF);
        if (lfIndex < 0) {
            return null;
        }
        final int length = lfIndex - in.readerIndex() - 1; // `-1` is for CR
        return readBytes(in, length);
    }

    private static ByteBuf readBytes(ByteBuf in, int length) {
        ByteBuf buf = in.readSlice(length);
        in.skipBytes(CRLF_LENGTH); // skip CRLF
        return buf;
    }

    private static long readLength(ByteBuf in, ToLongProcessor toLong) {
        in.forEachByte(toLong);
        return toLong.content();
    }

    private static class DecodeState {
        State state;
        RedisMessageType type;
        long length;
        long segments;

        DecodeState() {
            reset();
        }

        public void reset() {
            state = null;
            state = State.DECODE_TYPE;
            length = 0;
            segments = 0;
        }
    }

    static class ToLongProcessor implements ByteProcessor {

        private long result;
        private boolean negative;
        private boolean first;

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

        public long content() {
            if (!negative) {
                return -result;
            }
            return result;
        }

        public void reset() {
            first = true;
            result = 0;
        }
    }

}
