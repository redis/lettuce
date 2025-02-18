package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.Range;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.stream.PendingMessages;

/**
 * Decodes {@link PendingMessages}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
public class PendingMessagesOutput<K, V> extends CommandOutput<K, V, PendingMessages> {

    private Long count;

    private String messageIdsFrom;

    private String messageIdsTo;

    private String consumer;

    private boolean hasConsumer;

    private final Map<String, Long> consumerMessageCount = new LinkedHashMap<>();

    public PendingMessagesOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (messageIdsFrom == null) {
            messageIdsFrom = decodeString(bytes);
            return;
        }

        if (messageIdsTo == null) {
            messageIdsTo = decodeString(bytes);
            return;
        }

        if (!hasConsumer) {
            consumer = StringCodec.UTF8.decodeKey(bytes);
            hasConsumer = true;
            return;
        }

        set(Long.parseLong(decodeString(bytes)));
    }

    @Override
    public void set(long integer) {

        if (count == null) {
            count = integer;
            return;
        }

        if (hasConsumer) {
            consumerMessageCount.put(consumer, integer);
            consumer = null;
            hasConsumer = false;
        }
    }

    @Override
    public void complete(int depth) {

        if (depth == 0) {

            Range<String> range = messageIdsFrom != null && messageIdsTo != null ? Range.create(messageIdsFrom, messageIdsTo)
                    : Range.unbounded();
            output = new PendingMessages(count == null ? 0 : count, range, consumerMessageCount);
        }
    }

}
