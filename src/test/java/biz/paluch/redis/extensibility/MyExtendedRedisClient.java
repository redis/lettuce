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
