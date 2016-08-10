package biz.paluch.redis.extensibility;

import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Alternative;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.pubsub.PubSubEndpoint;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnectionImpl;
import com.lambdaworks.redis.resource.ClientResources;

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
            RedisChannelWriter channelWriter, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new MyPubSubConnection<>(endpoint, channelWriter, codec, timeout, unit);
    }
}
