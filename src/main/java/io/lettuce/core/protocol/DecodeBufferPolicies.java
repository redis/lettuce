package io.lettuce.core.protocol;

import io.netty.buffer.ByteBuf;

/**
 * Utility class to construct commonly used {@link DecodeBufferPolicy} objects.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public abstract class DecodeBufferPolicies {

    private static final DecodeBufferPolicy ALWAYS = new DecodeBufferPolicy() {

        @Override
        public void afterPartialDecode(ByteBuf buffer) {
            buffer.discardReadBytes();
        }

        @Override
        public void afterCommandDecoded(ByteBuf buffer) {
            buffer.discardReadBytes();
        }

        @Override
        public void afterDecoding(ByteBuf buffer) {
            buffer.discardReadBytes();
        }

    };

    private static final DecodeBufferPolicy ALWAYS_SOME = new DecodeBufferPolicy() {

        @Override
        public void afterPartialDecode(ByteBuf buffer) {
            buffer.discardSomeReadBytes();
        }

        @Override
        public void afterCommandDecoded(ByteBuf buffer) {
            buffer.discardSomeReadBytes();
        }

        @Override
        public void afterDecoding(ByteBuf buffer) {
            buffer.discardSomeReadBytes();
        }

    };

    private DecodeBufferPolicies() {

    }

    /**
     * Ratio-based discard policy that considers the capacity vs. usage of the aggregation buffer. This strategy optimizes for
     * CPU usage vs. memory usage by considering the usage ratio. Higher values lead to more memory usage.
     * <p>
     * The ratio is calculated with {@code bufferUsageRatio/(1+bufferUsageRatio)} which gives 50% for a value of {@code 1}, 66%
     * for {@code 2} and so on.
     *
     * @param bufferUsageRatio the buffer usage ratio. Must be between {@code 0} and {@code 2^31-1}, typically a value between 1
     *        and 10 representing 50% to 90%.
     * @return the new strategy object.
     */
    public static DecodeBufferPolicy ratio(float bufferUsageRatio) {
        return new RatioDecodeBufferPolicy(bufferUsageRatio);
    }

    /**
     * {@link DecodeBufferPolicy} that {@link ByteBuf#discardReadBytes() discards read bytes} after each decoding phase. This
     * strategy hast the most memory efficiency but also leads to more CPU pressure.
     *
     * @return the strategy object.
     */
    public static DecodeBufferPolicy always() {
        return ALWAYS;
    }

    /**
     * {@link DecodeBufferPolicy} that {@link ByteBuf#discardSomeReadBytes() discards some read bytes} after each decoding
     * phase. This strategy might discard some, all, or none of read bytes depending on its internal implementation to reduce
     * overall memory bandwidth consumption at the cost of potentially additional memory consumption.
     *
     * @return the strategy object.
     */
    public static DecodeBufferPolicy alwaysSome() {
        return ALWAYS_SOME;
    }

}
