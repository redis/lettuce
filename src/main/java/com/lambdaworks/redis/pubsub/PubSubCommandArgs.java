package com.lambdaworks.redis.pubsub;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
 *
 * Command args for Pub/Sub connections. This implementation hides the first key as PubSub keys are not keys from the key-space.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
class PubSubCommandArgs<K, V> extends CommandArgs<K, V> {

    /**
     * @param codec Codec used to encode/decode keys and values, must not be {@literal null}.
     */
    public PubSubCommandArgs(RedisCodec<K, V> codec) {
        super(codec);
    }

    /**
     *
     * @return always {@literal null}.
     */
    @Override
    public ByteBuffer getFirstEncodedKey() {
        return null;
    }
}
