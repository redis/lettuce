/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.function.Supplier;

import io.lettuce.core.EmptyStatefulRedisConnection;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
class EmptyRedisClusterClient extends RedisClusterClient {

    public EmptyRedisClusterClient(RedisURI initialUri) {
        super(null, Collections.singleton(initialUri));
    }

    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, String nodeId, RedisChannelWriter clusterWriter,
            final Supplier<SocketAddress> socketAddressSupplier) {
        return EmptyStatefulRedisConnection.INSTANCE;
    }
}
