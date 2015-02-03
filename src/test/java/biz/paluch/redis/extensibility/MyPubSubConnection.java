package biz.paluch.redis.extensibility;

import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Alternative;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.pubsub.PubSubOutput;
import com.lambdaworks.redis.pubsub.RedisPubSubConnectionImpl;

/**
 * Demo code for extending a RedisPubSubConnectionImpl.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@SuppressWarnings("unchecked")
public class MyPubSubConnection<K, V> extends RedisPubSubConnectionImpl<K, V> {
    /**
     * Initialize a new connection.
     * 
     * @param writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a responses.
     * @param unit Unit of time for the timeout.
     */
    public MyPubSubConnection(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        super(writer, codec, timeout, unit);
    }

    @Override
    public void psubscribe(K... patterns) {
        super.psubscribe(patterns);
        channels.size();
    }

    public void channelRead(Object msg) {
        PubSubOutput<K, V> output = (PubSubOutput<K, V>) msg;
        // update internal state
        switch (output.type()) {
            case psubscribe:
                patterns.add(output.pattern());
                break;
            case punsubscribe:
                patterns.remove(output.pattern());
                break;
            case subscribe:
                channels.add(output.channel());
                break;
            case unsubscribe:
                channels.remove(output.channel());
                break;
            default:
                break;
        }
        super.channelRead(msg);
    }

}
