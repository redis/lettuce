package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lambdaworks.redis.KeyValue;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link List} of values output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Mark Paluch
 */
public class KeyValueListOutput<K, V> extends CommandOutput<K, V, List<KeyValue<K, V>>>
        implements StreamingOutput<KeyValue<K, V>> {

    private Subscriber<KeyValue<K, V>> subscriber;
    private Iterable<K> keys;
    private Iterator<K> keyIterator;

    public KeyValueListOutput(RedisCodec<K, V> codec, Iterable<K> keys) {
        super(codec, new ArrayList<>());
        setSubscriber(ListSubscriber.of(output));
        this.keys = keys;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if(keyIterator == null) {
            keyIterator = keys.iterator();
        }

        subscriber.onNext(KeyValue.fromNullable(keyIterator.next(), bytes == null ? null : codec.decodeValue(bytes)));
    }

    @Override
    public void setSubscriber(Subscriber<KeyValue<K, V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<KeyValue<K, V>> getSubscriber() {
        return subscriber;
    }
}
