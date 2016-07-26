
package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link List} of {@link Value} wrapped values output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Mark Paluch
 */
public class ValueValueListOutput<K, V> extends CommandOutput<K, V, List<Value<V>>> implements StreamingOutput<Value<V>> {

    private Subscriber<Value<V>> subscriber;

    public ValueValueListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(Value.fromNullable(bytes == null ? null : codec.decodeValue(bytes)));
    }

    @Override
    public void set(long integer) {
        subscriber.onNext(Value.fromNullable((V) Long.valueOf(integer)));
    }

    @Override
    public void setSubscriber(Subscriber<Value<V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Value<V>> getSubscriber() {
        return subscriber;
    }
}
