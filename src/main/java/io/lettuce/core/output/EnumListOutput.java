package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link List} of enums output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 */
public class EnumListOutput<K, V, E extends Enum<E>> extends CommandOutput<K, V, List<E>> implements StreamingOutput<E> {

    private boolean initialized;

    private Subscriber<E> subscriber;

    private final Class<E> enumClass;

    public EnumListOutput(RedisCodec<K, V> codec, Class<E> enumClass) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
        this.enumClass = enumClass;
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(output, bytes == null ? null : valueOfOrNull(decodeAscii(bytes)));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<E> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<E> getSubscriber() {
        return subscriber;
    }

    private E valueOfOrNull(String value) {
        try {
            return Enum.valueOf(enumClass, value.toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
