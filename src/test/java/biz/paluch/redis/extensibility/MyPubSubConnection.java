package biz.paluch.redis.extensibility;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;

/**
 * Demo code for extending a RedisPubSubConnectionImpl.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
class MyPubSubConnection<K, V> extends StatefulRedisPubSubConnectionImpl<K, V> {

    private AtomicInteger subscriptions = new AtomicInteger();

    /**
     * Initialize a new connection.
     *
     * @param endpoint
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     */
    public MyPubSubConnection(PubSubEndpoint<K, V> endpoint, RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {
        super(endpoint, writer, codec, timeout);
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {

        if (command.getType() == CommandType.SUBSCRIBE) {
            subscriptions.incrementAndGet();
        }

        return super.dispatch(command);
    }
}
