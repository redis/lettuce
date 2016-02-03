package biz.paluch.redis.extensibility;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnectionImpl;

import javax.enterprise.inject.Alternative;

/**
 * Demo code for extending a RedisClient.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
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
            PubSubCommandHandler<K, V> handler, RedisCodec<K, V> codec) {
        return new MyPubSubConnection<>(handler, codec, timeout, unit);
    }
}
