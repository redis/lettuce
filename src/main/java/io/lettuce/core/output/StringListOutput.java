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

            // A nil reply can be the whole result (for example a missing key on
            // JSON.GET) or a nil element inside an array. The null must be kept in
            // the collected output so that positional results (jsonGetRaw,
            // jsonMGetRaw, jsonArrpopRaw) and MULTI/EXEC TransactionResult keep
            // their slots. It must however never be forwarded to a reactive
            // subscriber, because RedisPublisher rejects null with "Data must not
            // be null". Inside MULTI the subscriber is a CompositeSubscriber that
            // fans out to both the reactive and the collecting subscriber, so the
            // collector is fed through the output directly while the reactive
            // delegate is skipped.
            if (!initialized) {
                multi(1);
            }

            if (subscriber instanceof ListSubscriber) {
                subscriber.onNext(output, null);
            } else {
                output.add(null);
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
