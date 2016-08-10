package biz.paluch.redis.extensibility;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.pubsub.PubSubEndpoint;
import com.lambdaworks.redis.pubsub.PubSubOutput;
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
    public MyPubSubConnection(PubSubEndpoint<K, V> endpoint,
                              RedisChannelWriter writer, RedisCodec<K, V> codec, long timeout,
                              TimeUnit unit) {
        super(endpoint, writer, codec, timeout, unit);
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {

        if (cmd.getType() == CommandType.SUBSCRIBE) {
            subscriptions.incrementAndGet();
        }

        return super.dispatch(cmd);
    }

}
