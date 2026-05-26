package io.lettuce.core.output;

import io.lettuce.core.IncrexValue;
import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * Parses the {@code INCREX} array response {@code [value, increment]} into {@link IncrexValue}{@code <Long>}.
 *
 * @since 7.6
 */
public class IncrexLongOutput<K, V> extends CommandOutput<K, V, IncrexValue<Long>> {

    private Long first;

    private int index = 0;

    public IncrexLongOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        long parsed = Long.parseLong(decodeString(bytes));
        capture(parsed);
    }

    @Override
    public void set(long integer) {
        capture(integer);
    }

    private void capture(long value) {
        if (index == 0) {
            first = value;
        } else {
            output = new IncrexValue<>(first, value);
        }
        index++;
    }

}
