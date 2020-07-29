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
