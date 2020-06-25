/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.masterreplica;

import java.time.Duration;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
class StatefulRedisUpstreamReplicaConnectionImpl<K, V> extends StatefulRedisConnectionImpl<K, V>
        implements StatefulRedisMasterReplicaConnection<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     */
    StatefulRedisUpstreamReplicaConnectionImpl(UpstreamReplicaChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {
        super(writer, NoOpPushHandler.INSTANCE, codec, timeout);
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {
        getChannelWriter().setReadFrom(readFrom);
    }

    @Override
    public ReadFrom getReadFrom() {
        return getChannelWriter().getReadFrom();
    }

    @Override
    public UpstreamReplicaChannelWriter getChannelWriter() {
        return (UpstreamReplicaChannelWriter) super.getChannelWriter();
    }

}
