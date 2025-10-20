/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.multidb;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.protocol.PushHandler;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * A stateful connection to a MultiDb setup. This connection routes commands to the currently active endpoint and manages
 * connections to multiple Redis endpoints.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Ivo Gaydazhiev
 * @since 7.0
 */
public class StatefulMultiDbConnectionImpl<K, V> extends StatefulRedisConnectionImpl<K, V>
        implements StatefulMultiDbConnection<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param pushHandler the push handler
     * @param codec the codec used to encode/decode keys and values
     * @param timeout default timeout
     * @param parser JSON parser supplier
     */
    public StatefulMultiDbConnectionImpl(RedisChannelWriter writer, PushHandler pushHandler, RedisCodec<K, V> codec,
            Duration timeout, Supplier<JsonParser> parser) {
        super(writer, pushHandler, codec, timeout, parser);
    }

    /**
     * Get the MultiDb channel writer.
     *
     * @return the MultiDb channel writer
     */
    MultiDbChannelWriter getMultiDbChannelWriter() {
        return (MultiDbChannelWriter) super.getChannelWriter();
    }

    /**
     * Set the available endpoints for this connection. This will trigger cleanup of stale connections for endpoints that are no
     * longer available. This method is package-private and should only be called by {@link MultiDbClient}.
     *
     * @param endpoints the Redis endpoints, must not be {@code null}
     */
    void setEndpoints(RedisEndpoints endpoints) {
        LettuceAssert.notNull(endpoints, "RedisEndpoints must not be null");
        getMultiDbChannelWriter().setEndpoints(endpoints);
    }

    @Override
    public RedisEndpoints getEndpoints() {
        return getMultiDbChannelWriter().getEndpoints();
    }

}
