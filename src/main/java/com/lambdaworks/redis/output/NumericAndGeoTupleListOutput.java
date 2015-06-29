package com.lambdaworks.redis.output;

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.GeoTuple;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

/**
 * A list output that creates a list with either double/long or {@link GeoTuple}'s.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class NumericAndGeoTupleListOutput<K, V> extends CommandOutput<K, V, List<Object>> {

    private GeoTuple geoTuple;
    private Integer elementCount;

    public NumericAndGeoTupleListOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(long integer) {

        if (geoTuple == null) {
            output.add(integer);
            return;
        }

        if (geoTuple.getX() == null) {
            geoTuple.setX(integer);
            return;
        }

        geoTuple.setY(integer);
        output.add(geoTuple);
        geoTuple = null;
    }

    @Override
    public void set(ByteBuffer bytes) {
        Double value = (bytes == null) ? null : parseDouble(decodeAscii(bytes));

        if (geoTuple == null) {
            output.add(value);
            return;
        }

        if (geoTuple.getX() == null) {
            geoTuple.setX(value);
            return;
        }

        geoTuple.setY(value);
        output.add(geoTuple);
        geoTuple = null;
    }

    @Override
    public void multi(int count) {
        geoTuple = null;
        if (elementCount == null) {
            elementCount = count;
            output = Lists.newArrayListWithCapacity(count);
        } else if (count == 2) {
            geoTuple = new GeoTuple();
        } else if (count == -1) {
            output.add(null);
        }
    }
}
