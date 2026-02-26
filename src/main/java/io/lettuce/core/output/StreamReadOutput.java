package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.codec.RedisCodec;

import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 5.1
 */
public class StreamReadOutput<K, V> extends CommandOutput<K, V, List<StreamMessage<K, V>>>
        implements StreamingOutput<StreamMessage<K, V>> {

    private boolean initialized;

    private Subscriber<StreamMessage<K, V>> subscriber;

    private boolean skipStreamKeyReset = false;

    private K stream;

    private K key;

    private String id;

    private Map<K, V> body;

    private Long msSinceLastDelivery;

    private Long redeliveryCount;

    private boolean bodyReceived = false;

    public StreamReadOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (stream == null) {
            if (bytes == null) {
                return;
            }

            stream = codec.decodeKey(bytes);
            skipStreamKeyReset = true;
            return;
        }

        if (id == null) {
            id = decodeString(bytes);
            return;
        }

        if (key == null) {
            bodyReceived = true;

            if (bytes == null) {
                return;
            }

            key = codec.decodeKey(bytes);
            return;
        }

        if (body == null) {
            body = new LinkedHashMap<>();
        }

        body.put(key, bytes == null ? null : codec.decodeValue(bytes));
        key = null;
    }

    @Override
    public void set(long integer) {

        // Extra integers appear only for claimed entries (XREADGROUP with CLAIM)
        if (id != null && bodyReceived) {
            if (msSinceLastDelivery == null) {
                msSinceLastDelivery = integer;
                return;
            }
            if (redeliveryCount == null) {
                redeliveryCount = integer;
                return;
            }
        }
        super.set(integer);
    }

    @Override
    public void multi(int count) {

        if (id != null && key == null && count == -1) {
            bodyReceived = true;
        }

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void complete(int depth) {

        // Emit the message when the entry array (id/body[/extras]) completes.
        if (depth == 2 && bodyReceived) {
            Map<K, V> map = body == null ? Collections.emptyMap() : body;
            if (msSinceLastDelivery != null && redeliveryCount != null) {
                subscriber.onNext(output, new StreamMessage<>(stream, id, map, msSinceLastDelivery, redeliveryCount));
            } else {
                subscriber.onNext(output, new StreamMessage<>(stream, id, map));
            }
            bodyReceived = false;
            key = null;
            body = null;
            id = null;
            msSinceLastDelivery = null;
            redeliveryCount = null;
        }

        // RESP2/RESP3 compat
        if (depth == 2 && skipStreamKeyReset) {
            skipStreamKeyReset = false;
        }

        if (depth == 1) {
            if (skipStreamKeyReset) {
                skipStreamKeyReset = false;
            } else {
                stream = null;
            }
        }
    }

    @Override
    public void setSubscriber(Subscriber<StreamMessage<K, V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<StreamMessage<K, V>> getSubscriber() {
        return subscriber;
    }

}
