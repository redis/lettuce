/*
 * Copyright 2011-2017 the original author or authors.
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
package biz.paluch.redis.extensibility;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.pubsub.PubSubEndpoint;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnectionImpl;

/**
 * Demo code for extending a RedisPubSubConnectionImpl.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
public class MyPubSubConnection<K, V> extends StatefulRedisPubSubConnectionImpl<K, V> {

    private AtomicInteger subscriptions = new AtomicInteger();

    /**
     * Initialize a new connection.
     *
     * @param endpoint
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public MyPubSubConnection(PubSubEndpoint<K, V> endpoint, RedisChannelWriter writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(endpoint, writer, codec, timeout, unit);
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {

        if (command.getType() == CommandType.SUBSCRIBE) {
            subscriptions.incrementAndGet();
        }

        return super.dispatch(command);
    }
}
