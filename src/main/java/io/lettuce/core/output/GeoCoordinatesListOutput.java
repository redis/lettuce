package io.lettuce.core.output;

import static java.lang.Double.*;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * A list output that creates a list with {@link GeoCoordinates}'s.
 *
 * @author Mark Paluch
 */
public class GeoCoordinatesListOutput<K, V> extends CommandOutput<K, V, List<GeoCoordinates>>
        implements StreamingOutput<GeoCoordinates> {

    private Double x;

    boolean hasX;

    private boolean initialized;

    private Subscriber<GeoCoordinates> subscriber;

    public GeoCoordinatesListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            subscriber.onNext(output, null);
            return;
        }

        double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));
        set(value);
    }

    @Override
    public void set(double number) {

        if (!hasX) {
            x = number;
            hasX = true;
            return;
        }

        subscriber.onNext(output, new GeoCoordinates(x, number));
        x = null;
        hasX = false;
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }

        if (count == -1) {
            subscriber.onNext(output, null);
        }
    }

    @Override
    public void setSubscriber(Subscriber<GeoCoordinates> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<GeoCoordinates> getSubscriber() {
        return subscriber;
    }

}
