package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.lettuce.core.KeyValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of {@link KeyValue} output. Can be either used to decode key-value tuples (e.g. {@code HGETALL}) of for a pure
 * value response where keys are supplied as input (for e.g. {@code HMGET}).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Mark Paluch
 */
public class KeyValueListOutput<K, V> extends CommandOutput<K, V, List<KeyValue<K, V>>>
        implements StreamingOutput<KeyValue<K, V>> {

    private boolean initialized;

    private Subscriber<KeyValue<K, V>> subscriber;

    private final Iterable<K> keys;

    private Iterator<K> keyIterator;

    private K key;

    private boolean hasKey;

    public KeyValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
        this.keys = null;
    }

    public KeyValueListOutput(RedisCodec<K, V> codec, Iterable<K> keys) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
        this.keys = keys;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (keys == null) {
            if (!hasKey) {
                key = codec.decodeKey(bytes);
                hasKey = true;
                return;
            }

            K key = this.key;
            this.key = null;
            this.hasKey = false;
            subscriber.onNext(output, KeyValue.fromNullable(key, bytes == null ? null : codec.decodeValue(bytes)));

        } else {
            if (keyIterator == null) {
                keyIterator = keys.iterator();
            }

            subscriber.onNext(output,
                    KeyValue.fromNullable(keyIterator.next(), bytes == null ? null : codec.decodeValue(bytes)));
        }
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(keys == null ? count / 2 : count);
            initialized = true;
        }
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
