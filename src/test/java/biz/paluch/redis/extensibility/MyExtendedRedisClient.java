package biz.paluch.redis.extensibility;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.pubsub.PubSubCommandHandler;
import com.lambdaworks.redis.pubsub.RedisPubSubConnectionImpl;

import javax.enterprise.inject.Alternative;

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
    protected <K, V> RedisPubSubConnectionImpl<K, V> newRedisPubSubConnectionImpl(RedisChannelWriter<K, V> handler,
            RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        return new MyPubSubConnection<K, V>(handler, codec, timeout, unit);

    }
}
