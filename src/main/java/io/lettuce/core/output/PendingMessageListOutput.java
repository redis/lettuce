package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.stream.PendingMessage;

/**
 * Decodes a list of {@link PendingMessage}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
public class PendingMessageListOutput<K, V> extends CommandOutput<K, V, List<PendingMessage>>
        implements StreamingOutput<PendingMessage> {

    private boolean initialized;

    private Subscriber<PendingMessage> subscriber;

    private String messageId;

    private String consumer;

    private boolean hasConsumer;

    private Long msSinceLastDelivery;

    public PendingMessageListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (messageId == null) {
            messageId = decodeAscii(bytes);
            return;
        }

        if (!hasConsumer) {
            consumer = StringCodec.UTF8.decodeKey(bytes);
            hasConsumer = true;
            return;
        }

        set(Long.parseLong(decodeAscii(bytes)));
    }

    @Override
    public void set(long integer) {

        if (msSinceLastDelivery == null) {
            msSinceLastDelivery = integer;
            return;
        }

        PendingMessage message = new PendingMessage(messageId, consumer, msSinceLastDelivery, integer);
        messageId = null;
        consumer = null;
        hasConsumer = false;
        msSinceLastDelivery = null;
        subscriber.onNext(output, message);
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<PendingMessage> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<PendingMessage> getSubscriber() {
        return subscriber;
    }

}
