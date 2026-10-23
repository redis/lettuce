package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of string output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class StringListOutput<K, V> extends CommandOutput<K, V, List<String>> implements StreamingOutput<String> {

    private boolean initialized;

    private Subscriber<String> subscriber;

    public StringListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            // A nil reply for the whole list (e.g. a missing key) reaches this
            // method before the list is initialized. Forwarding null to the
            // subscriber makes the reactive RedisSubscription fail with
            // "Data must not be null", so the command errors instead of
            // completing empty. We therefore skip it and let the reply stay an
            // empty list.
            // Once the list is initialized (multi() was called to allocate the
            // array), a nil element is a real list member and must keep its
            // null slot so that length and element positions are preserved.
            if (initialized) {
                subscriber.onNext(output, null);
            }
            return;
        }

        if (!initialized) {
            multi(1);
        }

        subscriber.onNext(output, decodeString(bytes));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<String> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<String> getSubscriber() {
        return subscriber;
    }

}
