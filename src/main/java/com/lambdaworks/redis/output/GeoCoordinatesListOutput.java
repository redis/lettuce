package com.lambdaworks.redis.output;

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

/**
 * A list output that creates a list with {@link GeoCoordinates}'s.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoCoordinatesListOutput<K, V> extends CommandOutput<K, V, List<GeoCoordinates>> {

    private Double x;

    public GeoCoordinatesListOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        Double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));

        if (x == null) {
            x = value;
            return;
        }

        output.add(new GeoCoordinates(x, value));
        x = null;
    }

    @Override
    public void multi(int count) {
        if (output == null) {
            output = Lists.newArrayListWithCapacity(count);
        } else if (count == -1) {
            output.add(null);
        }
    }
}
