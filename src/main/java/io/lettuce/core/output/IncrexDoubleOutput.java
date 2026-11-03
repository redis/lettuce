package io.lettuce.core.output;

import io.lettuce.core.IncrexValue;
import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * Parses the {@code INCREX} array response {@code [value, increment]} into {@link IncrexValue}{@code <Double>}.
 *
 * @since 7.6
 */
public class IncrexDoubleOutput<K, V> extends CommandOutput<K, V, IncrexValue<Double>> {

    private Double first;

    private int index = 0;

    public IncrexDoubleOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        double parsed = (bytes == null) ? 0d : Double.parseDouble(decodeString(bytes));
        capture(parsed);
    }

    @Override
    public void set(double number) {
        capture(number);
    }

    private void capture(double value) {
        if (index == 0) {
            first = value;
        } else {
            output = new IncrexValue<>(first, value);
        }
        index++;
    }

}
