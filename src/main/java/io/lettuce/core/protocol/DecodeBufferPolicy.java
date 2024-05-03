package io.lettuce.core.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Strategy defining the approach to discard bytes from the response aggregation {@link ByteBuf buffer} in
 * {@link CommandHandler#decode(ChannelHandlerContext ctx, ByteBuf buffer)} to reclaim memory after various response decoding
 * phases.
 *
 * @since 6.0
 * @author Shaphan
 * @author Mark Paluch
 */
public interface DecodeBufferPolicy {

    /**
     * Callback that is invoked after partially decoding a command.
     *
     * @param buffer aggregation buffer
     */
    default void afterPartialDecode(ByteBuf buffer) {

    }

    /**
     * Callback that is invoked after a command has been fully decoded.
     *
     * @param buffer aggregation buffer
     */
    default void afterCommandDecoded(ByteBuf buffer) {

    }

    /**
     * Callback that is invoked after leaving the decode loop.
     *
     * @param buffer aggregation buffer
     */
    default void afterDecoding(ByteBuf buffer) {
        buffer.discardSomeReadBytes();
    }

}
