package com.lambdaworks.redis.output;

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * A list output that creates a list with {@link GeoCoordinates}'s.
 *
 * @author Mark Paluch
 */
public class GeoCoordinatesListOutput<K, V> extends CommandOutput<K, V, List<GeoCoordinates>> implements StreamingOutput<GeoCoordinates> {

    private Double x;
	private Subscriber<GeoCoordinates> subscriber;

	public GeoCoordinatesListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
		setSubscriber(ListSubscriber.of(output));
	}

    @Override
    public void set(ByteBuffer bytes) {

        Double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));

        if (x == null) {
            x = value;
            return;
        }

        subscriber.onNext(new GeoCoordinates(x, value));
        x = null;
    }

    @Override
    public void multi(int count) {
        if (count == -1) {
            subscriber.onNext(null);
        }
    }

	@Override
	public void setSubscriber(Subscriber<GeoCoordinates> subscriber) {
        LettuceAssert.notNull(subscriber, "subscriber must not be null");
		this.subscriber = subscriber;
	}

	@Override
	public Subscriber<GeoCoordinates> getSubscriber() {
		return subscriber;
	}
}
