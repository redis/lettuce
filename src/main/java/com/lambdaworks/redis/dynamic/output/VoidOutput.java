package com.lambdaworks.redis.dynamic.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.CommandOutput;

/**
 * {@link Void} command output to consume data silently without actually processing it.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
class VoidOutput<K, V> extends CommandOutput<K, V, Void> {

    /**
     * Initialize a new instance that encodes and decodes keys and values using the supplied codec.
     *
     * @param codec Codec used to encode/decode keys and values, must not be {@literal null}.
     */
    public VoidOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        // no-op
    }

    @Override
    public void set(long integer) {
        // no-op
    }
}
