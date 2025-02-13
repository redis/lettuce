package io.lettuce.core.output;

import static java.lang.Double.*;

import java.nio.ByteBuffer;
import java.util.List;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * A list output that creates a list with either double/long or {@link GeoCoordinates}'s.
 *
 * @author Mark Paluch
 */
public class GeoWithinListOutput<K, V> extends CommandOutput<K, V, List<GeoWithin<V>>>
        implements StreamingOutput<GeoWithin<V>> {

    private V member;

    private boolean hasMember;

    private Double distance;

    private boolean hasDistance;

    private Long geohash;

    private GeoCoordinates coordinates;

    private Double x;

    private boolean hasX;

    private final boolean withDistance;

    private final boolean withHash;

    private final boolean withCoordinates;

    private Subscriber<GeoWithin<V>> subscriber;

    public GeoWithinListOutput(RedisCodec<K, V> codec, boolean withDistance, boolean withHash, boolean withCoordinates) {
        super(codec, OutputFactory.newList(16));
        this.withDistance = withDistance;
        this.withHash = withHash;
        this.withCoordinates = withCoordinates;
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {

        if (!hasMember) {
            member = (V) (Long) integer;
            hasMember = true;
            return;
        }

        if (withHash) {
            geohash = integer;
        }
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (!hasMember) {
            member = codec.decodeValue(bytes);
            hasMember = true;
            return;
        }

        double value = (bytes == null) ? 0 : parseDouble(decodeString(bytes));
        set(value);
    }

    @Override
    public void set(double number) {

        if (withDistance) {
            if (!hasDistance) {
                distance = number;
                hasDistance = true;
                return;
            }
        }

        if (withCoordinates) {
            if (!hasX) {
                x = number;
                hasX = true;
                return;
            }

            coordinates = new GeoCoordinates(x, number);
        }
    }

    @Override
    public void complete(int depth) {

        if (depth == 1) {
            subscriber.onNext(output, new GeoWithin<V>(member, distance, geohash, coordinates));

            member = null;
            hasMember = false;
            distance = null;
            hasDistance = false;
            geohash = null;
            coordinates = null;
            x = null;
            hasX = false;
        }
    }

    @Override
    public void setSubscriber(Subscriber<GeoWithin<V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<GeoWithin<V>> getSubscriber() {
        return subscriber;
    }

}
