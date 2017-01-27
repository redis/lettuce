/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;
import java.util.function.Supplier;

import com.lambdaworks.redis.EmptyStatefulRedisConnection;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
public class EmptyRedisClusterClient extends RedisClusterClient {

    public EmptyRedisClusterClient(RedisURI initialUri) {
        super(initialUri);
    }

    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, String nodeId, RedisChannelWriter<K, V> clusterWriter,
            final Supplier<SocketAddress> socketAddressSupplier) {
        return EmptyStatefulRedisConnection.INSTANCE;
    }
}
