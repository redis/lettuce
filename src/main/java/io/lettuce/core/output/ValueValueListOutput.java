package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.Value;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of {@link Value} wrapped values output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Mark Paluch
 */
public class ValueValueListOutput<K, V> extends CommandOutput<K, V, List<Value<V>>> implements StreamingOutput<Value<V>> {

    private boolean initialized;

    private Subscriber<Value<V>> subscriber;

    public ValueValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(output, Value.fromNullable(bytes == null ? null : codec.decodeValue(bytes)));
    }

    @Override
    public void set(long integer) {
        subscriber.onNext(output, Value.fromNullable((V) Long.valueOf(integer)));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
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
