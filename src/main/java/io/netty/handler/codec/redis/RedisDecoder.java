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

import java.util.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

public class RedisDecoder extends ByteToMessageDecoder {

    private static final int CRLF_LENGTH = 2;
    private static final int NULL_RESPONSE = -1;

    private Deque<DecodeState> stages;

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
            while (next) {
                DecodeState current = stages.peek();
                switch (current.state) {
                    case DECODE_TYPE:
                        next = decodeType(current, in);
                        break;
                    case DECODE_INLINE:
                        next = decodeInline(current, in);
                        break;
                    case DECODE_LENGTH:
                        next = decodeLength(current, in);
                        break;
                    case DECODE_BULK_STRING:
                        next = decodeBulkStrings(current, in);
                        break;
                    case DECODE_ARRAY:
                        decodeArrays(current);
                        break;
                    case DECODE_COMPLETE:
                        aggregate(current, out);
                        break;
                    default:
                        throw new Error("Unknown state: " + current.state);
                }
            }
        } catch (Exception e) {
            resetDecoder();
            throw e;
        }
    }

    private void resetDecoder() {
        stages = new ArrayDeque<DecodeState>();
        stages.push(new DecodeState());
    }

    private boolean decodeType(DecodeState current, ByteBuf in) throws Exception {
        if (in.readableBytes() < 1) {
            return false;
        }
        current.type = RedisMessageType.valueOf(in.readByte());
        current.state = current.type.isInline() ? State.DECODE_INLINE : State.DECODE_LENGTH;
        return true;
    }

    private boolean decodeInline(DecodeState current, ByteBuf in) throws Exception {
        ByteBuf buf = readLine(in);
        if (buf == null) {
            return false;
        }
        current.content = buf;
        current.state = State.DECODE_COMPLETE;
        return true;
    }

    private boolean decodeLength(DecodeState current, ByteBuf in) throws Exception {
        ByteBuf buf = readLine(in);
        if (buf == null) {
            return false;
        }
        final int length = readLength(buf);
        current.length = length;
        if (current.type.isArray()) {
            setNextStateAndChildrenForArray(current);
        } else if (current.type.isBulkString()) {
            current.state = State.DECODE_BULK_STRING;
        } else {
            throw new Error("bad type: " + current.type);
        }
        return true;
    }

    private void setNextStateAndChildrenForArray(DecodeState current) throws Exception {
        if (current.length == NULL_RESPONSE) {
            // $-1\r\n <here>
            current.children = null;
            current.state = State.DECODE_COMPLETE;
        } else if (current.length == 0) {
            // $0\r\n <here>
            current.children = Collections.emptyList();
            current.state = State.DECODE_COMPLETE;
        } else if (current.length > 0) {
            // ${length}\r\n <here> {children...}\r\n
            current.children = new ArrayList<RedisMessage>();
            current.state = State.DECODE_ARRAY;
        } else {
            throw new IllegalArgumentException("bad length: " + current.length);
        }
    }

    private boolean decodeBulkStrings(DecodeState current, ByteBuf in) throws Exception {
        if (current.length == NULL_RESPONSE) {
            // $-1\r\n
            current.content = null;
            current.state = State.DECODE_COMPLETE;
        } else if (current.length == 0) {
            // $0\r\n <here> \r\n
            if (in.readableBytes() < CRLF_LENGTH) {
                return false;
            }
            in.skipBytes(CRLF_LENGTH);
            current.content = Unpooled.EMPTY_BUFFER;
            current.state = State.DECODE_COMPLETE;
        } else if (current.length > 0) {
            // ${length}\r\n <here> {data...}\r\n
            if (in.readableBytes() < current.length + CRLF_LENGTH) {
                return false;
            }
            current.content = in.readSlice(current.length);
            in.skipBytes(CRLF_LENGTH);
            current.state = State.DECODE_COMPLETE;
        } else {
            throw new IllegalArgumentException("bad length: " + current.length);
        }
        return true;
    }

    private void decodeArrays(DecodeState current) throws Exception {
        if (current.children.size() == current.length) {
            current.state = State.DECODE_COMPLETE;
        } else if (current.children.size() < current.length) {
            stages.push(new DecodeState()); // for child
        } else {
            throw new IllegalArgumentException(
                    "children.size: " + current.children.size() + ", current.length: " + current.length);
        }
    }

    private void aggregate(DecodeState current, List<Object> out) throws Exception {
        RedisMessage msg = newRedisMessage(current);
        if (stages.size() == 1) {
            out.add(msg);
            stages.pop();
            stages.push(new DecodeState()); // for next message
        } else if (stages.size() > 1) {
            stages.pop();
            DecodeState parent = stages.peek();
            parent.children.add(msg);
        } else {
            throw new Error("bad size: " + stages.size());
        }
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

    private static int readLength(ByteBuf in) {
        byte[] data = new byte[in.readableBytes()];
        in.readBytes(data);
        return Integer.parseInt(new String(data, CharsetUtil.UTF_8));
    }

    private static RedisMessage newRedisMessage(DecodeState state) {
        if (state.type.isArray()) {
            return new RedisMessage(state.type, state.children);
        } else {
            return new RedisMessage(state.type, state.content);
        }
    }

    private static class DecodeState {
        State state;
        RedisMessageType type;
        int length;
        ByteBuf content;
        List<RedisMessage> children;

        DecodeState() {
            state = State.DECODE_TYPE;
        }
    }

}
