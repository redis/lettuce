package biz.paluch.redis.extensibility;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.pubsub.PubSubOutput;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnectionImpl;

/**
 * Demo code for extending a RedisPubSubConnectionImpl.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@SuppressWarnings("unchecked")
public class MyPubSubConnection<K, V> extends StatefulRedisPubSubConnectionImpl<K, V> {

    private Set<K> interceptedChannels = Sets.newHashSet();

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
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {

        if (cmd.getType() == CommandType.SUBSCRIBE) {
            interceptedChannels.addAll(cmd.getArgs().getKeys());
        }

        return super.dispatch(cmd);
    }

    public void channelRead(Object msg) {
        PubSubOutput<K, V, V> output = (PubSubOutput<K, V, V>) msg;
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
