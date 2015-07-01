package com.lambdaworks.redis.output;

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.GeoEncoded;
import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Encodes the command output to retrieve a {@link GeoEncoded} object.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoEncodedOutput<K, V> extends CommandOutput<K, V, GeoEncoded> {

    private Long geohash;
    private GeoCoordinates min;
    private GeoCoordinates max;
    private GeoCoordinates avg;

    private Double x;

    public GeoEncodedOutput(RedisCodec<K, V> codec, Long geohash) {
        super(codec, null);
        this.geohash = geohash;
    }

    @Override
    public void set(long integer) {
        if (geohash == null) {
            geohash = integer;
        }
    }

    @Override
    public void set(ByteBuffer bytes) {
        Double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));

        if (x == null) {
            x = value;
            return;
        }

        try {
            if (min == null) {
                min = new GeoCoordinates(x, value);
                return;
            }

            if (max == null) {
                max = new GeoCoordinates(x, value);
                return;
            }

            if (avg == null) {
                avg = new GeoCoordinates(x, value);
                return;
            }
        } finally {
            x = null;
        }
    }

    @Override
    public void complete(int depth) {
        if (depth == 0) {
            output = new GeoEncoded(geohash, min, max, avg);
        }
    }
}
