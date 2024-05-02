package biz.paluch.redis.extensibility;

import java.time.Duration;

import javax.enterprise.inject.Alternative;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;
import io.lettuce.core.resource.ClientResources;

/**
 * Demo code for extending a RedisClient.
 *
 * @author Mark Paluch
 */
@Alternative
public class MyExtendedRedisClient extends RedisClient {

    public MyExtendedRedisClient(ClientResources clientResources, RedisURI redisURI) {
        super(clientResources, redisURI);
    }

    public MyExtendedRedisClient() {
    }

    @Override
    protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(PubSubEndpoint<K, V> endpoint,
            RedisChannelWriter channelWriter, RedisCodec<K, V> codec, Duration timeout) {
        return new MyPubSubConnection<>(endpoint, channelWriter, codec, timeout);
    }

}
