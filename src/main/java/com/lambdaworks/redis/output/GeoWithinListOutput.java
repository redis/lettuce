package com.lambdaworks.redis.output;

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.GeoWithin;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

/**
 * A list output that creates a list with either double/long or {@link GeoCoordinates}'s.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoWithinListOutput<K, V> extends CommandOutput<K, V, List<GeoWithin<V>>> {

    private V member;
    private Double distance;
    private Long geohash;
    private GeoCoordinates coordinates;

    private Double x;

    private boolean withDistance;
    private boolean withHash;
    private boolean withCoordinates;

    public GeoWithinListOutput(RedisCodec<K, V> codec, boolean withDistance, boolean withHash, boolean withCoordinates) {
        super(codec, null);
        this.withDistance = withDistance;
        this.withHash = withHash;
        this.withCoordinates = withCoordinates;
    }

    @Override
    public void set(long integer) {
        if (member == null) {
            member = (V) (Long) integer;
            return;
        }

        if (withHash) {
            geohash = integer;
        }
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (member == null) {
            member = codec.decodeValue(bytes);
            return;
        }

        Double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));
        if (withDistance) {
            if (distance == null) {
                distance = value;
                return;
            }
        }
        if (withCoordinates) {
            if (x == null) {
                x = value;
                return;
            }

            coordinates = new GeoCoordinates(x, value);
            return;
        }
    }

    @Override
    public void multi(int count) {
        if (output == null) {
            output = Lists.newArrayListWithCapacity(count);
        }
    }

    @Override
    public void complete(int depth) {
        if (depth == 1) {
            output.add(new GeoWithin<V>(member, distance, geohash, coordinates));

            member = null;
            distance = null;
            geohash = null;
            coordinates = null;
        }
    }
}
