package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * Glob Pattern output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Seonghwan Lee
 */
public class GlobPatternOutput<K, V> extends CommandOutput<K, V, List<K>> implements StreamingOutput<K> {

    private boolean initialized;

    private Subscriber<K> subscriber;

    public GlobPatternOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (bytes == null) {
            return;
        }

        subscriber.onNext(output, bytes == null ? null : codec.decodeKey(bytes));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<K> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<K> getSubscriber() {
        return subscriber;
    }

}
