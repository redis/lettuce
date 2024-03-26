package io.lettuce.core.protocol;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.buffer.ByteBuf;

/**
 * Ratio-based discard policy that considers the capacity vs. usage of the aggregation buffer. This strategy optimizes for CPU
 * usage vs. memory usage by considering the usage ratio. Higher values lead to more memory usage.
 * <p>
 * The ratio is calculated with {@code bufferUsageRatio/(1+bufferUsageRatio)} which gives 50% for a value of {@code 1}, 66% for
 * {@code 2} and so on.
 *
 * @author Shaphan
 * @author Mark Paluch
 * @since 6.0
 */
class RatioDecodeBufferPolicy implements DecodeBufferPolicy {

    private final float discardReadBytesRatio;

    /**
     * Create a new {@link RatioDecodeBufferPolicy} using {@code bufferUsageRatio}.
     *
     * @param bufferUsageRatio the buffer usage ratio. Must be between {@code 0} and {@code 2^31-1}, typically a value between 1
     *        and 10 representing 50% to 90%.
     *
     */
    public RatioDecodeBufferPolicy(float bufferUsageRatio) {

        LettuceAssert.isTrue(bufferUsageRatio > 0 && bufferUsageRatio < Integer.MAX_VALUE,
                "BufferUsageRatio must be greater than 0");

        this.discardReadBytesRatio = bufferUsageRatio / (bufferUsageRatio + 1);
    }

    @Override
    public void afterPartialDecode(ByteBuf buffer) {
        discardReadBytesIfNecessary(buffer);
    }

    @Override
    public void afterDecoding(ByteBuf buffer) {
        discardReadBytesIfNecessary(buffer);
    }

    @Override
    public void afterCommandDecoded(ByteBuf buffer) {
        discardReadBytesIfNecessary(buffer);
    }

    private void discardReadBytesIfNecessary(ByteBuf buffer) {

        float usedRatio = (float) buffer.readerIndex() / buffer.capacity();

        if (usedRatio >= discardReadBytesRatio) {
            buffer.discardReadBytes();
        }
    }

}
