package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.Value;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of {@link io.lettuce.core.Value} output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
public class StringValueListOutput<K, V> extends CommandOutput<K, V, List<Value<String>>>
        implements StreamingOutput<Value<String>> {

    private boolean initialized;

    private Subscriber<Value<String>> subscriber;

    public StringValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(output, bytes == null ? Value.empty() : Value.fromNullable(decodeUtf8(bytes)));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<Value<String>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Value<String>> getSubscriber() {
        return subscriber;
    }

}
