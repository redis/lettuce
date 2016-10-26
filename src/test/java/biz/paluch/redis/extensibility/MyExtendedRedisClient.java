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
package biz.paluch.redis.extensibility;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnectionImpl;

import javax.enterprise.inject.Alternative;
import java.util.concurrent.TimeUnit;

/**
 * Demo code for extending a RedisClient.
 * 
 * @author Mark Paluch
 */
@Alternative
public class MyExtendedRedisClient extends RedisClient {
    public MyExtendedRedisClient() {
    }

    public MyExtendedRedisClient(String host) {
        super(host);
    }

    public MyExtendedRedisClient(String host, int port) {
        super(host, port);
    }

    public MyExtendedRedisClient(RedisURI redisURI) {
        super(redisURI);
    }

    @Override
    protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(
            PubSubCommandHandler<K, V> commandHandler, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new MyPubSubConnection<>(commandHandler, codec, timeout, unit);
    }
}
