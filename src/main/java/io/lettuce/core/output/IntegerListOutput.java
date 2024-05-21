package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link List} of 64-bit integer output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.3.2
 */
public class IntegerListOutput<K, V> extends CommandOutput<K, V, List<Long>> implements StreamingOutput<Long> {

    private boolean initialized;

    private Subscriber<Long> subscriber;

    public IntegerListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        subscriber.onNext(output, integer);
    }

    public void set(ByteBuffer bytes) {
        if (bytes != null) {
            super.set(bytes);
        }
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<Long> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Long> getSubscriber() {
        return subscriber;
    }

}
