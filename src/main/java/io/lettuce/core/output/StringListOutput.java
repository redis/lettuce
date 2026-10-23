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
            // A null reply can reach this method either as the whole result
            // (e.g. a missing key on JSON.GET / jsonGetRaw) or as a nil element
            // inside an array. The default ListSubscriber simply collects into
            // the output list, so forwarding null preserves the slot (or keeps
            // the result as a single null) and is safe. Reactive subscribers
            // (RedisPublisher) reject null with "Data must not be null" and
            // would abort decoding, so null is skipped for those subscribers
            // instead of being forwarded.
            if (subscriber instanceof ListSubscriber) {
                if (!initialized) {
                    multi(1);
                }
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
