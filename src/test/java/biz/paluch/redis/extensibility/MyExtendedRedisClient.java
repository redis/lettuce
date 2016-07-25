package biz.paluch.redis.extensibility;

import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Alternative;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
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
    protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(
            PubSubCommandHandler<K, V> commandHandler, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new MyPubSubConnection<>(commandHandler, codec, timeout, unit);
    }
}
