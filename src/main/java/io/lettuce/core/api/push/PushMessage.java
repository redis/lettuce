/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.api.push;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;

/**
 * Interface representing a push message received from Redis. Push messages are messages received through of Pub/Sub or
 * client-side caching registrations.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface PushMessage {

    /**
     * @return the push message type.
     */
    String getType();

    /**
     * Returns the notification message contents. The content contains all response value beginning with {@link #getType()}
     * using their appropriate Java representation. String data (simple and bulk) are represented as {@link java.nio.ByteBuffer}
     * and can be decoded through {@link io.lettuce.core.codec.StringCodec#decodeValue(ByteBuffer)}. {@link ByteBuffer} objects
     * are read-only buffers that share the content without sharing the read-position.
     *
     * @return the notification message containing all response values including {@link #getType()}.
     */
    List<Object> getContent();

    /**
     * Returns the notification message contents by applying a {@code decodeFunction} on {@link ByteBuffer} elements. The
     * content contains all response value beginning with {@link #getType()} using their appropriate Java representation. String
     * data (simple and bulk) are mapped using {@link Function decodeFunction}. Please note that buffer read positions are
     * tracked by this method so decode functions are not required to reset the position.
     *
     * @return the notification message containing all response values including {@link #getType()}.
     */
    List<Object> getContent(Function<ByteBuffer, Object> decodeFunction);

}
