package io.netty.handler.codec.redis;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;

/**
 * Redis message frame types.
 * 
 * @author Mark Paluch
 */
public abstract class RedisFrame {

    private final RedisMessageType messageType;

    public RedisFrame(RedisMessageType messageType) {
        this.messageType = messageType;
    }

    public RedisMessageType type() {
        return messageType;
    }

    /**
     * Array header denoting the number of expected elements within the following array.
     */
    public static class ArrayHeader extends RedisFrame {
        public final static ArrayHeader NULL = new ArrayHeader(-1);

        private final long content;

        public ArrayHeader(long content) {
            super(RedisMessageType.ARRAY);
            this.content = content;
        }

        public long content() {
            return content;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [content=").append(content);
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * 64bit integer data segment.
     */
    public static class IntegerFrame extends RedisFrame {

        private final long content;

        public IntegerFrame(long content) {
            super(RedisMessageType.INTEGER);
            this.content = content;
        }

        public long content() {
            return content;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [content=").append(content);
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * {@literal null} segment.
     */
    public static class NullFrame extends RedisFrame {

        public final static NullFrame INSTANCE = new NullFrame();

        private NullFrame() {
            super(RedisMessageType.BULK_STRING);
        }

        @Override
        public String toString() {
            return "(null) segment";
        }
    }

    /**
     * Reference counted data segment. Each {@link ByteBufFrame} must be {@link #release() released} after usage.
     */
    public static class ByteBufFrame extends RedisFrame implements ReferenceCounted {

        public static ByteBufFrame EMPTY_BULK_STRING = new ByteBufFrame(RedisMessageType.BULK_STRING,
                Unpooled.EMPTY_BUFFER);
        private final ByteBuf content;

        public ByteBufFrame(RedisMessageType messageType, ByteBuf content) {
            super(messageType);
            content.retain();
            this.content = content;
        }

        public ByteBuf content() {
            return content;
        }

        @Override
        public int refCnt() {
            return content.refCnt();
        }

        @Override
        public ReferenceCounted retain() {
            return content.retain();
        }

        @Override
        public ReferenceCounted retain(int increment) {
            return content.retain(increment);
        }

        @Override
        public ReferenceCounted touch() {
            return content.touch();
        }

        @Override
        public ReferenceCounted touch(Object hint) {
            content.touch();
            return this;
        }

        @Override
        public boolean release() {
            return content.release();
        }

        @Override
        public boolean release(int decrement) {
            return content.release(decrement);
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [content=...");
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * String segment for simple strings and error messages.
     */
    public static class StringFrame extends RedisFrame {

        private final String value;

        public StringFrame(RedisMessageType messageType, String value) {
            super(messageType);
            this.value = value;
        }
    }
}
