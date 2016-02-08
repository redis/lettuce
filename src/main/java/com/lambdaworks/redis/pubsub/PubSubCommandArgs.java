package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * PubSub keys are not keys from the key-space.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.2
 */
class PubSubCommandArgs<K, V> extends CommandArgs<K, V> {

    /**
     * @param codec Codec used to encode/decode keys and values, must not be {@literal null}.
     */
    public PubSubCommandArgs(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public CommandArgs addKey(K key) {
        return write(codec.encodeKey(key));
    }
}
