package io.lettuce.core.codec;

import java.nio.ByteBuffer;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A {@link ComposedRedisCodec} combines two {@link RedisCodec cdecs} to encode/decode key and value to the command output.
 *
 * @author Dimitris Mandalidis
 * @since 5.2
 */
class ComposedRedisCodec<K, V> implements RedisCodec<K, V> {

    private final RedisCodec<K, ?> keyCodec;

    private final RedisCodec<?, V> valueCodec;

    ComposedRedisCodec(RedisCodec<K, ?> keyCodec, RedisCodec<?, V> valueCodec) {
        LettuceAssert.notNull(keyCodec, "Key codec must not be null");
        LettuceAssert.notNull(valueCodec, "Value codec must not be null");
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    @Override
    public K decodeKey(ByteBuffer bytes) {
        return keyCodec.decodeKey(bytes);
    }

    @Override
    public V decodeValue(ByteBuffer bytes) {
        return valueCodec.decodeValue(bytes);
    }

    @Override
    public ByteBuffer encodeKey(K key) {
        return keyCodec.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(V value) {
        return valueCodec.encodeValue(value);
    }

}
